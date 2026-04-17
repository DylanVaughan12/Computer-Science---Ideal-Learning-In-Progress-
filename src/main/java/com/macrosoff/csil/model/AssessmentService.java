/**
 * File:    AssessmentService.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Grades a user submission by compiling and running each test case through
 *          ExecutionEngine and comparing actual stdout to expected output.
 */
package com.macrosoff.csil.model;

import com.macrosoff.csil.service.ExecutionEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * AssessmentService
 * ──────────────────
 * Grades a user's submitted code by actually compiling and running it against
 * the problem's test cases via {@link ExecutionEngine}.
 *
 * Grading strategy:
 *   1. The submitted code is compiled once.  If compilation fails, all test
 *      cases score 0 and the error is reported.
 *   2. For each non-hidden test case the code is run (re-compiled each time
 *      to ensure isolation; TODO: cache the compiled class for speed).
 *   3. The actual stdout is compared to the expected output after stripping
 *      leading/trailing whitespace from both strings.
 *   4. The overall score = passed / total visible test cases × 100.
 *
 * The hidden test cases (TestCase.isHidden() == true) are included in the
 * result list but their input is shown as "hidden" in the UI.  They ARE run
 * and do affect the score — the student just can't see what they test.
 *
 * TODO (team): Cache the compiled bytecode so all test cases share one
 *   compilation step (currently each test case triggers a re-compile, which
 *   is slow but correct).
 * TODO (team): Add a method-level grader for problems that require the user
 *   to define a specific method (e.g. int sum(int a, int b)) rather than
 *   writing a top-level script.
 */
public class AssessmentService {

    /** Shared engine instance — stateless, so safe to reuse across calls. */
    private static final ExecutionEngine ENGINE = new ExecutionEngine();

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * Detailed result of running one test case.
     * Collected into a list by {@link #evaluateAll} and consumed by ResultPanel.
     */
    public static class TestResult {
        public final TestCase testCase;
        public final String   actualOutput;   // stdout captured from user code
        public final boolean  passed;
        public final String   errorMsg;       // non-empty only when execution failed

        TestResult(TestCase tc, String actual, boolean passed, String error) {
            this.testCase     = tc;
            this.actualOutput = actual;
            this.passed       = passed;
            this.errorMsg     = error;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Run the user's code against ALL test cases for the problem.
     *
     * @param userCode  the assembled script from the canvas (newline-separated statements)
     * @param problem   the problem whose test cases are used
     * @return a list of TestResult, one per test case, in order
     */
    public List<TestResult> evaluateAll(String userCode, Problem problem) {
        List<TestResult> results = new ArrayList<>();

        // If there are no test cases fall back to a single compile-only check
        if (problem.getTestCases().isEmpty()) {
            ExecutionEngine.ExecutionResult run = ENGINE.run(userCode);
            return results;  // empty — no test cases to report
        }

        for (TestCase tc : problem.getTestCases()) {
            results.add(runOne(userCode, tc));
        }
        return results;
    }

    /**
     * Quick boolean check used by the Assessment model.
     * Returns true if at least 60% of test cases pass.
     */
    public boolean evaluate(String userCode, Problem problem) {
        List<TestResult> results = evaluateAll(userCode, problem);
        if (results.isEmpty()) {
            // No test cases: fall back to compile-only check
            return ENGINE.run(userCode).compiled;
        }
        long passed = results.stream().filter(r -> r.passed).count();
        return (passed / (double) results.size()) >= 0.60;
    }

    /**
     * Alias kept for backward-compatibility with Assessment.java.
     * Delegates to {@link #evaluate(String, Problem)}.
     */
    public boolean checkAgainstConfirmedSolutions(String code, Problem problem) {
        return evaluate(code, problem);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Compile and run the user's code for a single test case.
     *
     * The test case's inputData is injected by prepending it as a String constant
     * named {@code INPUT} in the wrapper so simple problems can reference it.
     * More sophisticated input parsing is a TODO.
     */
    private TestResult runOne(String userCode, TestCase tc) {
        // Inject the test input as a constant available inside the snippet.
        // This keeps the user's code simple — they can reference INPUT directly.
        // TODO (team): For multi-argument problems, split INPUT on whitespace and
        //   assign each token to a separate typed variable (int a, int b, etc.)
        String wrappedCode = "String INPUT = \"" + escapeString(tc.getInputData()) + "\";\n"
                + userCode;

        ExecutionEngine.ExecutionResult run = ENGINE.run(wrappedCode);

        if (!run.compiled || run.timedOut) {
            return new TestResult(tc, "", false, run.errorMsg);
        }

        // Normalise whitespace before comparing — trailing newlines shouldn't fail a test
        boolean pass = run.output.strip().equals(tc.getExpectedOutput().strip());
        return new TestResult(tc, run.output, pass, run.errorMsg);
    }

    /** Escape a string for embedding in a Java string literal. */
    private static String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
