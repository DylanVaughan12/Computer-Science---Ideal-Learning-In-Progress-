/**
 * File:    InstructorPanel.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.1
 * Purpose: Provides a management interface visible only to INSTRUCTOR and ADMIN
 *          users. Allows reordering problems with move-up / move-down buttons,
 *          toggling problem visibility from the student view, resetting to the
 *          default difficulty-based order, and creating brand-new problems via
 *          CreateProblemDialog.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.data.DataStore;
import com.macrosoff.csil.model.Problem;
import com.macrosoff.csil.model.enums.DifficultyLevel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * InstructorPanel
 * Shown in the top navigation bar only for INSTRUCTOR users. Displays all
 * problems in the current order with inline controls for each row.
 *
 * TODO (team): Persist the instructor-customised problem order to disk so
 *   the sequence survives application restarts.
 * TODO (team): Add a "Preview as Student" mode that renders the ProblemPanel
 *   for a selected problem without granting submit/score privileges.
 */
public class InstructorPanel extends JPanel {

    // ── Colour constants ──────────────────────────────────────────────────────
    private static final Color BACKGROUND_PRIMARY   = new Color(15, 17, 23);
    private static final Color BACKGROUND_SECONDARY = new Color(22, 26, 36);
    private static final Color BACKGROUND_TERTIARY  = new Color(30, 35, 51);
    private static final Color ACCENT_BLUE          = new Color(79, 142, 247);
    private static final Color ACCENT_GREEN         = new Color(61, 214, 140);
    private static final Color ACCENT_YELLOW        = new Color(247, 200, 79);
    private static final Color ACCENT_RED           = new Color(247, 112, 79);
    private static final Color TEXT_PRIMARY         = new Color(232, 236, 244);
    private static final Color TEXT_SECONDARY       = new Color(139, 147, 167);
    private static final Color TEXT_HINT            = new Color(84, 93, 114);
    private static final Color BORDER_COLOR         = new Color(42, 48, 71);

    // The panel that holds one row per problem — rebuilt on every refresh
    private final JPanel problemListPanel = new JPanel();

    // ── Constructor ───────────────────────────────────────────────────────────

    public InstructorPanel() {
        setBackground(BACKGROUND_PRIMARY);
        setLayout(new BorderLayout());
        buildPanel();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    /**
     * (Re)builds the full panel layout. Called once in the constructor and again
     * via refreshList() whenever the problem list changes.
     */
    private void buildPanel() {
        removeAll();

        // ── Header ────────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BACKGROUND_SECONDARY);
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Instructor — Problem Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(
                "Use \u25b2 \u25bc to reorder.  Toggle eye icon to hide from students.");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(TEXT_SECONDARY);

        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setOpaque(false);
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(Box.createVerticalStrut(4));
        headerTextPanel.add(subtitleLabel);

        // "Reset to Default Order" button
        JButton resetOrderButton = new JButton("Reset to Default Order");
        resetOrderButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resetOrderButton.setForeground(TEXT_SECONDARY);
        resetOrderButton.setBackground(BACKGROUND_TERTIARY);
        resetOrderButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 14, 5, 14)));
        resetOrderButton.setFocusPainted(false);
        resetOrderButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetOrderButton.addActionListener(actionEvent -> resetOrder());

        // "Create Problem" button — opens the authoring dialog
        JButton createProblemButton = new JButton("+ Create Problem");
        createProblemButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        createProblemButton.setForeground(Color.WHITE);
        createProblemButton.setBackground(ACCENT_BLUE);
        createProblemButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1),
                new EmptyBorder(5, 14, 5, 14)));
        createProblemButton.setFocusPainted(false);
        createProblemButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createProblemButton.addActionListener(actionEvent -> openCreateProblemDialog());

        JPanel buttonGroupPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonGroupPanel.setOpaque(false);
        buttonGroupPanel.add(resetOrderButton);
        buttonGroupPanel.add(createProblemButton);

        headerPanel.add(headerTextPanel, BorderLayout.CENTER);
        headerPanel.add(buttonGroupPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // ── Problem list ──────────────────────────────────────────────────────
        problemListPanel.setBackground(BACKGROUND_PRIMARY);
        problemListPanel.setLayout(new BoxLayout(problemListPanel, BoxLayout.Y_AXIS));
        problemListPanel.setBorder(new EmptyBorder(8, 16, 16, 16));

        JScrollPane scrollPane = new JScrollPane(problemListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND_PRIMARY);
        add(scrollPane, BorderLayout.CENTER);

        refreshList();
        revalidate();
        repaint();
    }

    /**
     * Rebuilds the problem rows from the current DataStore order.
     * Call this after any change that alters the problem list or order.
     */
    public void refreshList() {
        problemListPanel.removeAll();

        List<Problem> allProblems = DataStore.getInstance().getProblemManager().getAllProblems();
        int totalProblemCount = allProblems.size();

        for (int problemIndex = 0; problemIndex < totalProblemCount; problemIndex++) {
            problemListPanel.add(buildProblemRow(allProblems.get(problemIndex),
                    problemIndex, totalProblemCount));
            problemListPanel.add(Box.createVerticalStrut(5));
        }

        problemListPanel.revalidate();
        problemListPanel.repaint();
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    /**
     * Constructs one editable row for a problem in the list.
     *
     * @param problem            the problem this row represents
     * @param problemIndex       its 0-based position in the list
     * @param totalProblemCount  total number of problems (used to disable boundary buttons)
     */
    private JPanel buildProblemRow(Problem problem, int problemIndex, int totalProblemCount) {
        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
        rowPanel.setBackground(BACKGROUND_SECONDARY);
        rowPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(10, 14, 10, 14)));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        rowPanel.setAlignmentX(LEFT_ALIGNMENT);

        // Row number indicator
        JLabel rowNumberLabel = new JLabel(String.valueOf(problemIndex + 1));
        rowNumberLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        rowNumberLabel.setForeground(TEXT_HINT);
        rowNumberLabel.setPreferredSize(new Dimension(26, 0));

        // Problem title (greyed out if hidden from students)
        boolean problemIsHidden = DataStore.getInstance().isProblemHidden(problem.getProblemID());
        JLabel problemTitleLabel = new JLabel(problem.getTitle());
        problemTitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        if (problemIsHidden) {
            problemTitleLabel.setForeground(TEXT_HINT);
        } else {
            problemTitleLabel.setForeground(TEXT_PRIMARY);
        }

        // Difficulty badge — coloured by tier
        Color difficultyColor = determineDifficultyColor(problem.getDifficulty());
        JLabel difficultyLabel = new JLabel(problem.getDifficulty().name().substring(0, 3));
        difficultyLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        difficultyLabel.setForeground(difficultyColor);
        difficultyLabel.setPreferredSize(new Dimension(36, 0));

        // Category label
        JLabel categoryLabel = new JLabel(problem.getCategory().name().replace("_", " "));
        categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        categoryLabel.setForeground(TEXT_HINT);
        categoryLabel.setPreferredSize(new Dimension(140, 0));

        JPanel leftContentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftContentPanel.setOpaque(false);
        leftContentPanel.add(rowNumberLabel);
        leftContentPanel.add(problemTitleLabel);
        leftContentPanel.add(difficultyLabel);
        leftContentPanel.add(categoryLabel);
        rowPanel.add(leftContentPanel, BorderLayout.CENTER);

        // Right side: visibility toggle + move buttons
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controlsPanel.setOpaque(false);

        // Visibility toggle button
        String visibilityButtonText;
        Color visibilityButtonColor;

        if (problemIsHidden) {
            visibilityButtonText  = "Show";
            visibilityButtonColor = ACCENT_GREEN;
        } else {
            visibilityButtonText  = "Hide";
            visibilityButtonColor = TEXT_HINT;
        }

        JButton visibilityButton = createControlButton(visibilityButtonText, visibilityButtonColor);
        visibilityButton.addActionListener(actionEvent -> {
            DataStore.getInstance().toggleProblemVisibility(problem.getProblemID());
            refreshList();
        });
        controlsPanel.add(visibilityButton);

        // Edit and Delete buttons — only shown for instructor-authored (custom) problems
        boolean isCustomProblem = DataStore.getInstance().isCustomProblem(problem.getProblemID());
        if (isCustomProblem) {
            JButton editButton = createControlButton("Edit", ACCENT_BLUE);
            editButton.setToolTipText("Edit this problem");
            editButton.addActionListener(actionEvent -> openEditProblemDialog(problem));
            controlsPanel.add(editButton);

            JButton deleteButton = createControlButton("Delete", ACCENT_RED);
            deleteButton.setToolTipText("Permanently remove this problem");
            deleteButton.addActionListener(actionEvent -> deleteCustomProblem(problem));
            controlsPanel.add(deleteButton);
        }

        // Move up button — disabled for the first problem
        JButton moveUpButton = createControlButton("\u25b2", TEXT_HINT);
        moveUpButton.setEnabled(problemIndex > 0);
        moveUpButton.addActionListener(actionEvent -> {
            DataStore.getInstance().moveProblem(problemIndex, problemIndex - 1);
            refreshList();
        });

        // Move down button — disabled for the last problem
        JButton moveDownButton = createControlButton("\u25bc", TEXT_HINT);
        moveDownButton.setEnabled(problemIndex < totalProblemCount - 1);
        moveDownButton.addActionListener(actionEvent -> {
            DataStore.getInstance().moveProblem(problemIndex, problemIndex + 1);
            refreshList();
        });

        controlsPanel.add(moveUpButton);
        controlsPanel.add(moveDownButton);
        rowPanel.add(controlsPanel, BorderLayout.EAST);

        return rowPanel;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Opens the CreateProblemDialog and refreshes the list if a problem was saved.
     */
    private void openCreateProblemDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);

        // The callback refreshes this panel's list immediately after a save
        CreateProblemDialog createDialog = new CreateProblemDialog(parentFrame, this::refreshList);
        createDialog.setVisible(true);
    }

    /**
     * Opens CreateProblemDialog in edit mode for the given custom problem.
     *
     * @param problemToEdit the custom problem to edit
     */
    private void openEditProblemDialog(Problem problemToEdit) {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        CreateProblemDialog editDialog = new CreateProblemDialog(
                parentFrame, problemToEdit, this::refreshList);
        editDialog.setVisible(true);
    }

    /**
     * Shows a confirmation dialog then permanently removes a custom problem.
     *
     * @param problemToDelete the custom problem to remove
     */
    private void deleteCustomProblem(Problem problemToDelete) {
        int confirmationChoice = JOptionPane.showConfirmDialog(
                this,
                "Permanently delete the problem: " + problemToDelete.getTitle() + "?\n"
                        + "This cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmationChoice == JOptionPane.YES_OPTION) {
            DataStore.getInstance().deleteCustomProblem(problemToDelete.getProblemID());
            refreshList();
        }
    }

    /**
     * Resets the problem list to the default sort order:
     *   BEGINNER first, then INTERMEDIATE, then ADVANCED, then by problem ID.
     * Also restores all hidden problems to visible.
     */
    private void resetOrder() {
        DataStore.getInstance().getProblemManager().getAllProblems()
                .sort(java.util.Comparator
                        .comparingInt(problem -> determineDifficultyOrder(
                                ((Problem) problem).getDifficulty()))
                        .thenComparingInt(problem -> ((Problem) problem).getProblemID()));

        // Restore all hidden problems so students see the full list
        DataStore.getInstance().clearHiddenProblems();

        refreshList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a sort-order integer for the given difficulty level.
     * Lower numbers appear first in the list.
     */
    private static int determineDifficultyOrder(DifficultyLevel difficultyLevel) {
        int sortOrder;

        if (difficultyLevel == DifficultyLevel.BEGINNER) {
            sortOrder = 0;
        } else if (difficultyLevel == DifficultyLevel.INTERMEDIATE) {
            sortOrder = 1;
        } else if (difficultyLevel == DifficultyLevel.ADVANCED) {
            sortOrder = 2;
        } else {
            sortOrder = 3;
        }

        return sortOrder;
    }

    /**
     * Returns the display colour for the given difficulty level badge.
     */
    private Color determineDifficultyColor(DifficultyLevel difficultyLevel) {
        Color badgeColor;

        if (difficultyLevel == DifficultyLevel.BEGINNER) {
            badgeColor = ACCENT_GREEN;
        } else if (difficultyLevel == DifficultyLevel.INTERMEDIATE) {
            badgeColor = ACCENT_YELLOW;
        } else {
            badgeColor = ACCENT_RED;
        }

        return badgeColor;
    }

    private JButton createControlButton(String buttonText, Color foregroundColor) {
        JButton button = new JButton(buttonText);
        button.setFont(new Font("SansSerif", Font.PLAIN, 11));
        button.setForeground(foregroundColor);
        button.setBackground(BACKGROUND_TERTIARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(3, 8, 3, 8)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
