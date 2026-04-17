/**
 * File:    ResultPanel.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Embedded side panel showing per-test-case pass/fail detail after
 *          each code submission.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.model.TestCase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;

/**
 * ResultPanel
 * ────────────
 * An embedded (non-modal) side panel that slides in on the right side of the
 * ProblemPanel when the user submits code.  It shows:
 *
 *   • A summary header (overall pass/fail + grade)
 *   • A row per test case with:
 *       - Pass (✓ green) or Fail (✕ red) badge
 *       - Input data (hidden test cases show "hidden")
 *       - Expected output
 *       - Actual output produced by the user's code
 *   • Compilation / runtime error output if execution failed
 *
 * The panel starts hidden (setVisible(false)) and is revealed by ProblemPanel
 * after each submission.  A close button hides it again.
 *
 * Usage (from ProblemPanel):
 *   resultPanel.show(testCases, actualOutputs, compiledOk, grade);
 *
 * TODO (team): Add a "Copy error" button so users can paste their error into
 *   a search engine or share it with an instructor easily.
 * TODO (team): If the output is multi-line, add a JScrollPane to each row so
 *   long outputs don't overflow.
 */
public class ResultPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG      = new Color(18, 22, 34);
    private static final Color BG2     = new Color(26, 31, 46);
    private static final Color BG3     = new Color(34, 40, 58);
    private static final Color PASS_BG = new Color(15, 36, 24);
    private static final Color FAIL_BG = new Color(38, 18, 18);
    private static final Color PASS    = new Color(61,  214, 140);
    private static final Color FAIL    = new Color(247, 112,  79);
    private static final Color WARN    = new Color(247, 200,  79);
    private static final Color TEXT    = new Color(232, 236, 244);
    private static final Color TEXT2   = new Color(139, 147, 167);
    private static final Color TEXT3   = new Color(84,  93,  114);
    private static final Color BORDER  = new Color(42,  48,   71);

    // ── Widgets ───────────────────────────────────────────────────────────────
    private final JLabel  summaryLabel  = new JLabel(" ");
    private final JPanel  caseContainer = new JPanel();  // holds one row per test case
    private final JLabel  errorLabel    = new JLabel();   // shown only when code didn't compile/run

    // ── Constructor ───────────────────────────────────────────────────────────

    public ResultPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        setBorder(new MatteBorder(0, 1, 0, 0, BORDER));
        setPreferredSize(new Dimension(310, 0));
        setVisible(false);  // hidden until a submission is made

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG2);
        header.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("TEST RESULTS");
        title.setFont(new Font("SansSerif", Font.BOLD, 10));
        title.setForeground(TEXT3);

        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        summaryLabel.setForeground(TEXT);
        summaryLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        // Close button — hides the panel again
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        closeBtn.setForeground(TEXT2);
        closeBtn.setBackground(BG2);
        closeBtn.setBorder(new EmptyBorder(2, 8, 2, 2));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> setVisible(false));

        JPanel headerTop = new JPanel(new BorderLayout());
        headerTop.setOpaque(false);
        headerTop.add(title,    BorderLayout.CENTER);
        headerTop.add(closeBtn, BorderLayout.EAST);

        header.add(headerTop,    BorderLayout.NORTH);
        header.add(summaryLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // ── Test-case rows ────────────────────────────────────────────────────
        caseContainer.setBackground(BG);
        caseContainer.setLayout(new BoxLayout(caseContainer, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(caseContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        add(scroll, BorderLayout.CENTER);

        // ── Error output (shown when code fails to run) ───────────────────────
        errorLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        errorLabel.setForeground(FAIL);
        errorLabel.setBackground(FAIL_BG);
        errorLabel.setOpaque(true);
        errorLabel.setBorder(new EmptyBorder(10, 12, 10, 12));
        errorLabel.setVerticalAlignment(SwingConstants.TOP);
        errorLabel.setVisible(false);
        add(errorLabel, BorderLayout.SOUTH);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Populate the panel with this submission's results and make it visible.
     *
     * @param testCases      the problem's test cases (may include hidden ones)
     * @param actualOutputs  actual stdout from each test case in the same order;
     *                       if code didn't compile this list may be empty
     * @param compiledOk     false → show compilation/runtime error section
     * @param errorText      compiler / runtime error message (empty when compiledOk)
     * @param grade          0–100 percentage score
     */
    public void show(List<TestCase> testCases, List<String> actualOutputs,
                     boolean compiledOk, String errorText, float grade) {
        // ── Summary ───────────────────────────────────────────────────────────
        if (!compiledOk) {
            summaryLabel.setForeground(FAIL);
            summaryLabel.setText("Build failed — fix errors below");
        } else {
            long passed = 0;
            for (int i = 0; i < testCases.size(); i++) {
                String actual   = i < actualOutputs.size() ? actualOutputs.get(i).strip() : "";
                String expected = testCases.get(i).getExpectedOutput().strip();
                if (actual.equals(expected)) passed++;
            }
            float pct = testCases.isEmpty() ? 0 : (passed * 100f / testCases.size());
            boolean allPass = (passed == testCases.size());
            summaryLabel.setForeground(allPass ? PASS : FAIL);
            summaryLabel.setText(String.format("%d / %d passed  (%.0f%%)",
                    passed, testCases.size(), pct));
        }

        // ── Test-case rows ────────────────────────────────────────────────────
        caseContainer.removeAll();
        caseContainer.add(Box.createVerticalStrut(8));

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc      = testCases.get(i);
            String actual    = i < actualOutputs.size() ? actualOutputs.get(i).strip() : "(not run)";
            String expected  = tc.getExpectedOutput().strip();
            boolean pass     = compiledOk && actual.equals(expected);

            caseContainer.add(buildCaseRow(i + 1, tc, actual, pass));
            caseContainer.add(Box.createVerticalStrut(6));
        }

        // ── Error section ─────────────────────────────────────────────────────
        if (!compiledOk && !errorText.isEmpty()) {
            errorLabel.setText("<html><pre style='font-size:10px'>"
                    + escHtml(errorText) + "</pre></html>");
            errorLabel.setVisible(true);
        } else {
            errorLabel.setVisible(false);
        }

        caseContainer.revalidate();
        caseContainer.repaint();
        setVisible(true);
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    /**
     * Build one test-case row showing pass/fail status, input, expected, and
     * actual output.
     */
    private JPanel buildCaseRow(int number, TestCase tc,
                                String actual, boolean pass) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(pass ? PASS_BG : FAIL_BG);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(pass ? new Color(30, 80, 50) : new Color(80, 30, 30), 1),
                new EmptyBorder(8, 10, 8, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Heading row: "Test 1  ✓" or "Test 1  ✕"
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel numLabel = new JLabel("Test " + number + (tc.isHidden() ? "  (hidden)" : ""));
        numLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        numLabel.setForeground(TEXT);
        JLabel badge = new JLabel(pass ? "✓ PASS" : "✕ FAIL");
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setForeground(pass ? PASS : FAIL);
        heading.add(numLabel, BorderLayout.WEST);
        heading.add(badge,    BorderLayout.EAST);
        row.add(heading);
        row.add(Box.createVerticalStrut(6));

        // Input (hide contents for hidden test cases)
        if (!tc.isHidden()) {
            row.add(dataRow("Input",    tc.getInputData().isEmpty() ? "(none)" : tc.getInputData()));
            row.add(Box.createVerticalStrut(4));
        }

        // Expected output
        row.add(dataRow("Expected", tc.getExpectedOutput()));
        row.add(Box.createVerticalStrut(4));

        // Actual output (only shown when code ran; coloured red if wrong)
        JPanel actualRow = dataRow("Actual", actual);
        if (!pass) {
            // Tint the actual output slightly to draw attention to the difference
            for (Component c : actualRow.getComponents()) {
                if (c instanceof JLabel) ((JLabel)c).setForeground(FAIL);
            }
        }
        row.add(actualRow);

        return row;
    }

    /** A small two-cell row: "Label:" on the left, monospaced value on the right. */
    private JPanel dataRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lbl.setForeground(TEXT3);
        lbl.setPreferredSize(new Dimension(54, 0));

        // Truncate long values for display
        String display = value.length() > 40 ? value.substring(0, 37) + "…" : value;
        JLabel val = new JLabel(display);
        val.setFont(new Font("Monospaced", Font.PLAIN, 11));
        val.setForeground(TEXT2);
        val.setToolTipText(value);  // show full value on hover

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    /** Basic HTML entity escaping for the error text area. */
    private String escHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
