/**
 * File:    CanvasCheckpoint.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Saves and restores per-user, per-problem canvas block layouts to disk so
 *          students can resume exactly where they left off after restarting.
 * 
 * Changes:
 * James Dylan Vaughan 4/16/2026
 * renamed variables to follow best practices, added more comments.
 * 
 * Approval By:
 * 
 */
package com.macrosoff.csil.data;

import com.macrosoff.csil.model.BlockCategory;
import com.macrosoff.csil.model.ScriptLine;
import com.macrosoff.csil.ui.panels.ScratchCanvas;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * CanvasCheckpoint
 * ─────────────────
 * Saves and restores the state of a ScratchCanvas (block positions and edited
 * code) to a plain-text file on disk so users can pick up exactly where they
 * left off after closing the application.
 *
 * File location:
 *   ~/.csil/canvas_{username}_{problemId}.chk
 *
 * File format (one line per block, stacks separated by blank lines):
 *   STACK {x} {y}
 *   BLOCK {blockId} {editedCode...}
 *   BLOCK {blockId} {editedCode...}
 *   (blank line)
 *   STACK {x} {y}
 *   BLOCK ...
 *
 * Design notes:
 *   - editedCode may contain spaces and most special characters; it is stored
 *     after the blockId separated by a single space. On load everything after
 *     the first space following the blockId is treated as the code string.
 *   - BlockCategory instances are looked up by ID from DataStore on load, so
 *     only the numeric ID is persisted (not the full object).
 *   - If the file is missing or malformed the canvas starts empty (no crash).
 *
 * TODO (team): If problems are ever dynamically added/removed, add a version
 *   header to the checkpoint file so stale checkpoints can be detected and
 *   discarded gracefully.
 */
public class CanvasCheckpoint {

    // ── Path helpers ──────────────────────────────────────────────────────────

	//directory where data is stored
    private static final String DIR = System.getProperty("user.home") + File.separator + ".csil";

    private static String checkpointPath(String username, int problemId) {
        // Sanitise username to prevent directory traversal
        String safe = username.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return DIR + File.separator + "canvas_" + safe + "_" + problemId + ".chk";
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    /**
     * Write the current canvas stacks to disk.
     * Call this after every successful submit and when the user switches problems.
     *
     * @param username  the logged-in user's name (used in the file path)
     * @param problemId the problem whose canvas is being saved
     * @param stacks    live stacks from ScratchCanvas.getStacks()
     */
    public static void save(String username, int problemId, List<ScratchCanvas.BlockStack> stacks) {
        try {
            Files.createDirectories(Paths.get(DIR));
            StringBuilder stringBuilder = new StringBuilder();
            for (ScratchCanvas.BlockStack stack : stacks) {
                if (stack.lines.isEmpty()) continue;
                stringBuilder.append("STACK ").append(stack.x).append(' ').append(stack.y).append('\n');
                for (ScriptLine sl : stack.lines) {
                    // Format: "BLOCK {id} {editedCode}"
                	stringBuilder.append("BLOCK ")
                      .append(sl.getBlock().getCodeBlockId())
                      .append(' ')
                      .append(sl.getEditedCode().replace("\n", "\\n"))  // escape newlines
                      .append('\n');
                }
                stringBuilder.append('\n');  // blank line between stacks
            }
            Files.writeString(Paths.get(checkpointPath(username, problemId)), stringBuilder.toString(),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("CSIL: failed to save canvas checkpoint — " + e.getMessage());
        }
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    /**
     * Load saved stacks from disk and populate the canvas.
     * Silently does nothing if the checkpoint file does not exist.
     *
     * @param username  the logged-in user's name
     * @param problemId the problem being loaded
     * @param canvas    the ScratchCanvas to populate
     * @param allBlocks all BlockCategory instances (used to resolve IDs)
     */
    public static void load(String username, int problemId,
                            ScratchCanvas canvas, List<BlockCategory> allBlocks) {
        Path path = Paths.get(checkpointPath(username, problemId));
        if (!Files.exists(path)) return;  // first visit — start with empty canvas

        // Build a quick-lookup map: blockId → BlockCategory
        Map<Integer, BlockCategory> blockById = new HashMap<>();
        for (BlockCategory blockCategory : allBlocks) blockById.put(blockCategory.getCodeBlockId(), blockCategory);

        try {
            List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);
            canvas.clear();  // wipe any existing content before restoring

            ScratchCanvas.BlockStack currentStack = null;
            for (String line : lines) {
                line = line.strip();
                if (line.startsWith("STACK ")) {
                    // "STACK {x} {y}"
                    String[] parts = line.substring(6).split(" ", 2);
                    if (parts.length < 2) continue;
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    currentStack = new ScratchCanvas.BlockStack(x, y);
                    canvas.getStacks().add(currentStack);

                } else if (line.startsWith("BLOCK ") && currentStack != null) {
                    // "BLOCK {id} {editedCode}"
                    String rest = line.substring(6);
                    int spaceIdx = rest.indexOf(' ');
                    if (spaceIdx < 0) continue;
                    int blockId = Integer.parseInt(rest.substring(0, spaceIdx));
                    String code = rest.substring(spaceIdx + 1).replace("\\n", "\n");  // unescape
                    BlockCategory blockCategory = blockById.get(blockId);
                    if (blockCategory != null) {
                        currentStack.lines.add(new ScriptLine(blockCategory, code));
                    }
                } else if (line.isEmpty()) {
                    currentStack = null;  // blank line signals end of a stack
                }
            }
            canvas.repaint();
        } catch (Exception e) {
            // Malformed or outdated checkpoint — just leave the canvas empty
            System.err.println("CSIL: could not restore canvas checkpoint — " + e.getMessage());
            canvas.clear();
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Remove the checkpoint for a specific user + problem.
     * Call this after a user clears their canvas intentionally.
     */
    public static void delete(String username, int problemId) {
        try { Files.deleteIfExists(Paths.get(checkpointPath(username, problemId))); }
        catch (IOException ignored) {}
    }

    /**
     * Remove ALL checkpoints for the given user.
     * Call this when an account is deleted.
     */
    public static void deleteAll(String username) {
        String safe = username.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        try {
            Files.walk(Paths.get(DIR))
                 .filter(p -> p.getFileName().toString().startsWith("canvas_" + safe + "_"))
                 .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
