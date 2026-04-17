/**
 * File:    ExecutionEngine.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Compiles and executes user-submitted Java code entirely in memory using
 *          javax.tools, capturing standard output for test-case comparison.
 */
package com.macrosoff.csil.service;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * ExecutionEngine
 * ───────────────
 * Compiles and runs a user's code snippet entirely in-memory using the
 * javax.tools API that ships with every JDK.
 *
 * How it works:
 *   1. The snippet is wrapped in a full Java class with a static main().
 *   2. JavaCompiler compiles it to bytecode held in a Map (no temp files).
 *   3. A custom ClassLoader loads the bytecode.
 *   4. main() is invoked via reflection with System.out redirected to a buffer.
 *   5. The captured output is returned for comparison with expected test output.
 *
 * Known limitations (see team TODOs below):
 *   - Requires a JDK (not just JRE) — ToolProvider.getSystemJavaCompiler() returns
 *     null when running on a plain JRE. Eclipse always ships with a JDK, so this
 *     is safe for the current development environment.
 *   - SecurityManager is deprecated/removed in Java 17+.  Malicious code is
 *     mitigated by the TIMEOUT_SECONDS hard limit, which kills the thread pool
 *     if the code takes too long (e.g. infinite loop).
 *   - Only System.out is captured; System.err goes to the IDE console.
 *
 * TODO (team): Add a restricted SecurityManager (Java 11 only) or move execution
 *   to a subprocess via ProcessBuilder so the JVM isolates the user's code fully.
 * TODO (team): Support multiple-method problems by allowing the snippet to define
 *   helper methods — change WRAPPER_TEMPLATE to wrap only the body, keeping any
 *   method definitions at class level.
 */
public class ExecutionEngine {

    // Wraps the user's snippet in a compilable class.
    // %s is replaced with the actual code lines.
    private static final String WRAPPER_TEMPLATE =
            "public class UserSolution {\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        %s\n" +
            "    }\n" +
            "}\n";

    /** Simple container returned to callers. */
    public static class ExecutionResult {
        public final boolean compiled;   // false → compilation errors prevented running
        public final boolean timedOut;   // true  → execution exceeded TIMEOUT_SECONDS
        public final String  output;     // stdout captured during execution (or error msg)
        public final String  errorMsg;   // compiler/runtime error details if any

        ExecutionResult(boolean compiled, boolean timedOut, String output, String errorMsg) {
            this.compiled  = compiled;
            this.timedOut  = timedOut;
            this.output    = output;
            this.errorMsg  = errorMsg;
        }

        /** Convenience factory for compile-or-tool errors. */
        static ExecutionResult error(String msg) {
            return new ExecutionResult(false, false, "", msg);
        }

        /** Returns true when execution finished and produced output (even if wrong). */
        public boolean ranSuccessfully() { return compiled && !timedOut; }
    }

    // Hard execution timeout in seconds — prevents infinite loops from hanging the UI.
    private static final int TIMEOUT_SECONDS = 5;

    // Maximum characters captured from stdout — prevents memory exhaustion.
    private static final int MAX_OUTPUT_CHARS = 8192;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Compile and run the given code snippet.
     *
     * @param userCode  Java statement(s) the user assembled from blocks.
     * @return  ExecutionResult with stdout output and any error details.
     */
    public ExecutionResult run(String userCode) {
        // Check that the JDK compiler is reachable
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return ExecutionResult.error(
                "Java compiler not found.\n" +
                "Make sure CS-IL is launched from a JDK, not a plain JRE.\n" +
                "In Eclipse: Run → Run Configurations → JRE → choose a JDK.");
        }

        // Build the complete source string
        String source = String.format(WRAPPER_TEMPLATE, userCode.replace("\n", "\n        "));

        // ── Step 1: compile ───────────────────────────────────────────────────
        InMemoryFileManager fileManager = new InMemoryFileManager(
                compiler.getStandardFileManager(null, null, null));

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics,
                Collections.singletonList("-nowarn"),  // suppress spurious lint warnings
                null,
                Collections.singletonList(new SourceObject("UserSolution", source)));

        boolean compiledOk = task.call();
        if (!compiledOk) {
            // Collect readable error messages from diagnostics
            StringBuilder sb = new StringBuilder("Compilation errors:\n");
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    // Adjust line numbers — the wrapper adds 2 lines above the user's code
                    long userLine = d.getLineNumber() - 2;
                    sb.append("  Line ").append(userLine > 0 ? userLine : "?")
                      .append(": ").append(d.getMessage(null)).append('\n');
                }
            }
            return new ExecutionResult(false, false, "", sb.toString().trim());
        }

        // ── Step 2: load bytecode ─────────────────────────────────────────────
        InMemoryClassLoader loader = new InMemoryClassLoader(fileManager.classBytes);

        // ── Step 3: redirect stdout and invoke main() ─────────────────────────
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(outBuf, true, StandardCharsets.UTF_8);
        PrintStream originalOut = System.out;

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<String> future = exec.submit(() -> {
            // Redirect stdout for this thread
            System.setOut(capture);
            try {
                Class<?> cls = loader.loadClass("UserSolution");
                Method main = cls.getMethod("main", String[].class);
                main.invoke(null, (Object) new String[]{});
            } finally {
                // Always restore stdout so Eclipse console still works after submission
                System.setOut(originalOut);
            }
            // Return the captured bytes as a string, trimmed to MAX_OUTPUT_CHARS
            String raw = outBuf.toString(StandardCharsets.UTF_8);
            return raw.length() > MAX_OUTPUT_CHARS
                    ? raw.substring(0, MAX_OUTPUT_CHARS) + "\n…(output truncated)"
                    : raw;
        });

        try {
            String output = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return new ExecutionResult(true, false, output.strip(), "");
        } catch (TimeoutException e) {
            future.cancel(true);
            return new ExecutionResult(true, true, "",
                    "Execution timed out after " + TIMEOUT_SECONDS + " seconds.\n" +
                    "Check for an infinite loop in your code.");
        } catch (Exception e) {
            // Runtime exception from inside the user's code
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return new ExecutionResult(true, false, "",
                    "Runtime error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        } finally {
            exec.shutdownNow();
            System.setOut(originalOut);  // safety — always restore
        }
    }

    // ── In-memory compiler plumbing ───────────────────────────────────────────

    /** A JavaFileObject backed by a String — gives the compiler the source. */
    private static class SourceObject extends SimpleJavaFileObject {
        private final String src;
        SourceObject(String className, String src) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                  Kind.SOURCE);
            this.src = src;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return src; }
    }

    /** A JavaFileObject that writes compiled bytecode into a byte array. */
    private static class BytecodeObject extends SimpleJavaFileObject {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BytecodeObject(String className) {
            super(URI.create("bytes:///" + className.replace('.', '/') + Kind.CLASS.extension),
                  Kind.CLASS);
        }
        @Override public OutputStream openOutputStream() { return bytes; }
    }

    /**
     * A JavaFileManager that intercepts output class files and stores their
     * bytecode in a Map keyed by class name, instead of writing to disk.
     */
    private static class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        final Map<String, byte[]> classBytes = new HashMap<>();

        InMemoryFileManager(StandardJavaFileManager delegate) { super(delegate); }

        @Override
        public JavaFileObject getJavaFileForOutput(Location loc, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            BytecodeObject bo = new BytecodeObject(className);
            // Wire a close-listener so bytes are captured after the compiler writes them
            return new SimpleJavaFileObject(bo.toUri(), kind) {
                @Override public OutputStream openOutputStream() {
                    return new FilterOutputStream(bo.bytes) {
                        @Override public void close() throws IOException {
                            super.close();
                            classBytes.put(className, bo.bytes.toByteArray());
                        }
                    };
                }
            };
        }
    }

    /**
     * ClassLoader that resolves class names from the in-memory bytecode map.
     * Falls back to the parent (system) ClassLoader for standard library classes.
     */
    private static class InMemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytes;

        InMemoryClassLoader(Map<String, byte[]> classBytes) {
            super(InMemoryClassLoader.class.getClassLoader());
            this.classBytes = classBytes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytes.get(name);
            if (bytes == null) throw new ClassNotFoundException(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
