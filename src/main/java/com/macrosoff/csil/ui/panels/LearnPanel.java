/**
 * File:    LearnPanel.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: The opening panel of the CS-IL application. Presents all subject-area
 *          categories in a left sidebar. Clicking a category reveals the first
 *          topic (problem + explanation) in that category on the right. The user
 *          can page through topics with "Previous" and "Next" buttons, or jump
 *          directly to the related practice problem via the "Practice This" button.
 *
 *          Flow:
 *            App opens → LearnPanel (this screen)
 *            Sidebar: click a category
 *            Right panel: explanation for the first topic in that category
 *            "Next →"          → advance to the next topic in the category
 *            "← Previous"      → go back to the previous topic
 *            "Practice This →" → switch to the Problems tab and load that problem
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.data.DataStore;
import com.macrosoff.csil.model.Problem;
import com.macrosoff.csil.model.enums.Category;
import com.macrosoff.csil.model.enums.DifficultyLevel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * LearnPanel
 * Two-column layout:
 *   LEFT  — scrollable category list (sidebar)
 *   RIGHT — topic explanation view with prev / next / practice navigation
 *
 * The panel is driven entirely by the Problem objects in DataStore. Each
 * problem's InfoPanel.getExplanation() becomes one "lesson" within its
 * category. Categories with no problems show a placeholder message.
 */
public class LearnPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BACKGROUND_PRIMARY    = new Color(15, 17, 23);
    private static final Color BACKGROUND_SECONDARY  = new Color(22, 26, 36);
    private static final Color BACKGROUND_TERTIARY   = new Color(30, 35, 51);
    private static final Color BACKGROUND_CARD       = new Color(26, 31, 46);
    private static final Color ACCENT_BLUE           = new Color(79, 142, 247);
    private static final Color ACCENT_GREEN          = new Color(61, 214, 140);
    private static final Color ACCENT_YELLOW         = new Color(247, 200, 79);
    private static final Color ACCENT_RED            = new Color(247, 112, 79);
    private static final Color TEXT_PRIMARY          = new Color(232, 236, 244);
    private static final Color TEXT_SECONDARY        = new Color(139, 147, 167);
    private static final Color TEXT_HINT             = new Color(84, 93, 114);
    private static final Color BORDER_COLOR          = new Color(42, 48, 71);

    // ── State ─────────────────────────────────────────────────────────────────

    // The category currently selected in the sidebar
    private Category selectedCategory;

    // Index of the currently displayed topic within the selected category
    private int currentTopicIndex;

    // Callback fired when the user clicks "Practice This" — navigates to Problems tab
    private java.util.function.Consumer<Problem> onPracticeRequested;

    // ── UI references ─────────────────────────────────────────────────────────
    private JPanel categoryListPanel;    // sidebar content panel
    private JLabel topicTitleLabel;      // large title shown in the right panel
    private JLabel difficultyLabel;      // difficulty badge next to the title
    private JTextArea explanationArea;   // the main explanation text
    private JTextArea codeSnippetArea;   // optional code snippet beneath the explanation
    private JLabel   progressLabel;      // "Topic 2 of 4" counter
    private JButton  previousButton;     // navigate to previous topic
    private JButton  nextButton;         // navigate to next topic
    private JButton  practiceButton;     // jump to Problems tab for this problem

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs the LearnPanel and shows the first available category.
     */
    public LearnPanel() {
        // Initialise state before building the UI
        this.selectedCategory  = null;
        this.currentTopicIndex = 0;

        setBackground(BACKGROUND_PRIMARY);
        setLayout(new BorderLayout());
        buildLayout();

        // Auto-select the first non-empty category so the screen isn't blank on open
        selectFirstAvailableCategory();
    }

    /**
     * Registers the callback invoked when the user clicks "Practice This".
     * The consumer receives the Problem the student wants to practise.
     *
     * @param practiceCallback a Consumer<Problem>; may be null (button still shows)
     */
    public void setOnPracticeRequested(java.util.function.Consumer<Problem> practiceCallback) {
        this.onPracticeRequested = practiceCallback;
    }

    // ── Layout construction ───────────────────────────────────────────────────

    /**
     * Builds the two-column layout: sidebar on the left, explanation view on the right.
     */
    private void buildLayout() {
        add(buildCategorySidebar(), BorderLayout.WEST);
        add(buildExplanationView(), BorderLayout.CENTER);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    /**
     * Builds the left sidebar containing one button per Category enum value.
     */
    private JScrollPane buildCategorySidebar() {
        categoryListPanel = new JPanel();
        categoryListPanel.setBackground(BACKGROUND_SECONDARY);
        categoryListPanel.setLayout(new BoxLayout(categoryListPanel, BoxLayout.Y_AXIS));
        categoryListPanel.setBorder(new EmptyBorder(16, 12, 16, 12));

        // Section heading
        JLabel headingLabel = new JLabel("TOPICS");
        headingLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        headingLabel.setForeground(TEXT_HINT);
        headingLabel.setAlignmentX(LEFT_ALIGNMENT);
        headingLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
        categoryListPanel.add(headingLabel);

        // One entry per category
        for (Category category : Category.values()) {
            categoryListPanel.add(buildCategoryEntry(category));
            categoryListPanel.add(Box.createVerticalStrut(4));
        }

        JScrollPane sidebarScrollPane = new JScrollPane(categoryListPanel);
        sidebarScrollPane.setPreferredSize(new Dimension(210, 0));
        sidebarScrollPane.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_COLOR));
        sidebarScrollPane.getViewport().setBackground(BACKGROUND_SECONDARY);
        sidebarScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sidebarScrollPane;
    }

    /**
     * Builds one clickable category entry for the sidebar.
     *
     * @param category the category this entry represents
     */
    private JPanel buildCategoryEntry(Category category) {
        boolean isSelected = category == selectedCategory;
        Color categoryColor = resolveCategoryColor(category);

        JPanel entryPanel = new JPanel(new BorderLayout(8, 0));
        entryPanel.setBackground(isSelected ? BACKGROUND_TERTIARY : BACKGROUND_SECONDARY);
        entryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSelected ? categoryColor : BORDER_COLOR, 1),
                new EmptyBorder(10, 12, 10, 12)));
        entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        entryPanel.setAlignmentX(LEFT_ALIGNMENT);
        entryPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Category name label
        JLabel nameLabel = new JLabel(formatCategoryName(category));
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        nameLabel.setForeground(isSelected ? TEXT_PRIMARY : TEXT_SECONDARY);

        // Topic count indicator (e.g. "3 topics")
        int topicCount = getProblemsForCategory(category).size();
        String topicCountText;
        if (topicCount == 1) {
            topicCountText = "1 topic";
        } else {
            topicCountText = topicCount + " topics";
        }
        JLabel countLabel = new JLabel(topicCountText);
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        countLabel.setForeground(isSelected ? categoryColor : TEXT_HINT);

        JPanel textColumn = new JPanel();
        textColumn.setOpaque(false);
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
        textColumn.add(nameLabel);
        textColumn.add(Box.createVerticalStrut(2));
        textColumn.add(countLabel);

        // Coloured left accent bar
        JPanel accentBar = new JPanel();
        accentBar.setBackground(isSelected ? categoryColor : new Color(0, 0, 0, 0));
        accentBar.setPreferredSize(new Dimension(3, 0));
        accentBar.setOpaque(isSelected);

        entryPanel.add(accentBar,   BorderLayout.WEST);
        entryPanel.add(textColumn,  BorderLayout.CENTER);

        entryPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                selectCategory(category);
            }
            public void mouseEntered(MouseEvent mouseEvent) {
                if (category != selectedCategory) {
                    entryPanel.setBackground(BACKGROUND_TERTIARY);
                }
            }
            public void mouseExited(MouseEvent mouseEvent) {
                if (category != selectedCategory) {
                    entryPanel.setBackground(BACKGROUND_SECONDARY);
                }
            }
        });

        return entryPanel;
    }

    // ── Explanation view ──────────────────────────────────────────────────────

    /**
     * Builds the right-hand explanation panel with title, explanation text,
     * optional code snippet, and navigation buttons.
     */
    private JPanel buildExplanationView() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(BACKGROUND_PRIMARY);

        // ── Top bar: topic title and difficulty badge ──────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(BACKGROUND_SECONDARY);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(16, 24, 16, 24)));

        topicTitleLabel = new JLabel("Select a topic from the left to begin");
        topicTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        topicTitleLabel.setForeground(TEXT_PRIMARY);

        difficultyLabel = new JLabel(" ");
        difficultyLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        difficultyLabel.setForeground(TEXT_HINT);
        difficultyLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel titleColumn = new JPanel();
        titleColumn.setOpaque(false);
        titleColumn.setLayout(new BoxLayout(titleColumn, BoxLayout.Y_AXIS));
        titleColumn.add(topicTitleLabel);
        titleColumn.add(Box.createVerticalStrut(4));
        titleColumn.add(difficultyLabel);

        progressLabel = new JLabel(" ");
        progressLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        progressLabel.setForeground(TEXT_HINT);
        progressLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        topBar.add(titleColumn,    BorderLayout.CENTER);
        topBar.add(progressLabel,  BorderLayout.EAST);
        outerPanel.add(topBar, BorderLayout.NORTH);

        // ── Centre: scrollable explanation + code snippet ─────────────────────
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(BACKGROUND_PRIMARY);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(28, 28, 28, 28));

        // Main explanation text area — non-editable, word-wrapped
        explanationArea = new JTextArea();
        explanationArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
        explanationArea.setForeground(TEXT_PRIMARY);
        explanationArea.setBackground(BACKGROUND_PRIMARY);
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setBorder(null);
        explanationArea.setOpaque(false);
        explanationArea.setText("Select a category on the left to start learning.");
        explanationArea.setAlignmentX(LEFT_ALIGNMENT);
        contentPanel.add(explanationArea);
        contentPanel.add(Box.createVerticalStrut(24));

        // Code snippet area — shown only when a snippet is available
        codeSnippetArea = new JTextArea();
        codeSnippetArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        codeSnippetArea.setForeground(ACCENT_GREEN);
        codeSnippetArea.setBackground(BACKGROUND_CARD);
        codeSnippetArea.setEditable(false);
        codeSnippetArea.setLineWrap(true);
        codeSnippetArea.setWrapStyleWord(true);
        codeSnippetArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        codeSnippetArea.setAlignmentX(LEFT_ALIGNMENT);
        codeSnippetArea.setVisible(false);   // hidden until a snippet is loaded
        contentPanel.add(codeSnippetArea);

        JScrollPane contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(null);
        contentScrollPane.getViewport().setBackground(BACKGROUND_PRIMARY);
        outerPanel.add(contentScrollPane, BorderLayout.CENTER);

        // ── Bottom navigation bar ─────────────────────────────────────────────
        outerPanel.add(buildNavigationBar(), BorderLayout.SOUTH);

        return outerPanel;
    }

    /**
     * Builds the bottom bar containing the Previous, Next, and Practice buttons.
     */
    private JPanel buildNavigationBar() {
        JPanel navigationBar = new JPanel(new BorderLayout());
        navigationBar.setBackground(BACKGROUND_SECONDARY);
        navigationBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(12, 24, 12, 24)));

        // Left side: Previous and Next buttons
        JPanel leftButtonGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftButtonGroup.setOpaque(false);

        previousButton = createNavButton("← Previous", BACKGROUND_TERTIARY, TEXT_SECONDARY);
        nextButton     = createNavButton("Next →",     BACKGROUND_TERTIARY, TEXT_SECONDARY);

        previousButton.addActionListener(actionEvent -> navigateToPreviousTopic());
        nextButton.addActionListener(actionEvent -> navigateToNextTopic());

        leftButtonGroup.add(previousButton);
        leftButtonGroup.add(nextButton);
        navigationBar.add(leftButtonGroup, BorderLayout.WEST);

        // Right side: Practice This button (prominent, coloured)
        practiceButton = createNavButton("Practice This →", ACCENT_BLUE, Color.WHITE);
        practiceButton.addActionListener(actionEvent -> requestPractice());

        JPanel rightButtonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightButtonGroup.setOpaque(false);
        rightButtonGroup.add(practiceButton);
        navigationBar.add(rightButtonGroup, BorderLayout.EAST);

        // Disable all buttons initially — enabled once a category is selected
        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
        practiceButton.setEnabled(false);

        return navigationBar;
    }

    // ── Category / topic selection ────────────────────────────────────────────

    /**
     * Selects the first non-empty category and shows its first topic.
     * Called once on construction so the screen is never fully blank.
     */
    private void selectFirstAvailableCategory() {
        for (Category category : Category.values()) {
            if (!getProblemsForCategory(category).isEmpty()) {
                selectCategory(category);
                return;
            }
        }
        // All categories are empty (shouldn't happen in normal use)
    }

    /**
     * Switches the display to the given category and shows its first topic.
     *
     * @param category the category the user clicked in the sidebar
     */
    private void selectCategory(Category category) {
        // Update state
        this.selectedCategory  = category;
        this.currentTopicIndex = 0;

        // Rebuild the sidebar to update the active highlight
        rebuildSidebar();

        // Display the first topic in this category
        displayCurrentTopic();
    }

    /**
     * Moves to the next topic in the current category if one exists.
     */
    private void navigateToNextTopic() {
        List<Problem> topicsInCategory = getProblemsForCategory(selectedCategory);

        // Guard: only advance if there is a next topic
        if (currentTopicIndex < topicsInCategory.size() - 1) {
            currentTopicIndex = currentTopicIndex + 1;
            displayCurrentTopic();
        }
    }

    /**
     * Moves to the previous topic in the current category if one exists.
     */
    private void navigateToPreviousTopic() {
        // Guard: only go back if we are not already on the first topic
        if (currentTopicIndex > 0) {
            currentTopicIndex = currentTopicIndex - 1;
            displayCurrentTopic();
        }
    }

    /**
     * Fires the practice callback with the currently displayed problem so
     * MainWindow can switch to the Problems tab and load that problem.
     */
    private void requestPractice() {
        if (selectedCategory == null) {
            return;
        }

        List<Problem> topicsInCategory = getProblemsForCategory(selectedCategory);
        if (topicsInCategory.isEmpty() || currentTopicIndex >= topicsInCategory.size()) {
            return;
        }

        Problem selectedProblem = topicsInCategory.get(currentTopicIndex);

        // Invoke the callback registered by MainWindow
        if (onPracticeRequested != null) {
            onPracticeRequested.accept(selectedProblem);
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Rebuilds the sidebar panel to reflect the current selectedCategory.
     */
    private void rebuildSidebar() {
        categoryListPanel.removeAll();

        // Section heading
        JLabel headingLabel = new JLabel("TOPICS");
        headingLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        headingLabel.setForeground(TEXT_HINT);
        headingLabel.setAlignmentX(LEFT_ALIGNMENT);
        headingLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
        categoryListPanel.add(headingLabel);

        for (Category category : Category.values()) {
            categoryListPanel.add(buildCategoryEntry(category));
            categoryListPanel.add(Box.createVerticalStrut(4));
        }

        categoryListPanel.revalidate();
        categoryListPanel.repaint();
    }

    /**
     * Populates the right-hand explanation view with the topic at currentTopicIndex
     * within the selectedCategory.
     */
    private void displayCurrentTopic() {
        if (selectedCategory == null) {
            return;
        }

        List<Problem> topicsInCategory = getProblemsForCategory(selectedCategory);

        // Handle empty categories gracefully
        if (topicsInCategory.isEmpty()) {
            topicTitleLabel.setText(formatCategoryName(selectedCategory));
            difficultyLabel.setText(" ");
            progressLabel.setText("No topics yet");
            explanationArea.setText(
                    "No topics have been added to this category yet.\n\n" +
                    "Instructors can create problems from the Instructor tab.");
            codeSnippetArea.setVisible(false);
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            practiceButton.setEnabled(false);
            return;
        }

        Problem currentProblem = topicsInCategory.get(currentTopicIndex);

        // Update title and difficulty badge
        topicTitleLabel.setText(currentProblem.getTitle());
        difficultyLabel.setText(
                formatCategoryName(selectedCategory) + "  ·  " +
                currentProblem.getDifficulty().name());
        difficultyLabel.setForeground(resolveDifficultyColor(currentProblem.getDifficulty()));

        // Update the topic counter
        progressLabel.setText("Topic " + (currentTopicIndex + 1) + " of " + topicsInCategory.size());

        // Load the explanation text
        String explanationText;
        if (currentProblem.getInfoPanel() != null) {
            explanationText = currentProblem.getInfoPanel().getExplanation();
        } else {
            explanationText = "No explanation available for this topic.";
        }
        explanationArea.setText(explanationText);
        // Scroll the explanation back to the top whenever the topic changes
        explanationArea.setCaretPosition(0);

        // Load the first code snippet if one exists
        if (currentProblem.getInfoPanel() != null
                && !currentProblem.getInfoPanel().getCodeSnippets().isEmpty()) {
            codeSnippetArea.setText(currentProblem.getInfoPanel().getCodeSnippets().get(0));
            codeSnippetArea.setVisible(true);
        } else {
            codeSnippetArea.setVisible(false);
        }

        // Update button states
        previousButton.setEnabled(currentTopicIndex > 0);
        nextButton.setEnabled(currentTopicIndex < topicsInCategory.size() - 1);
        practiceButton.setEnabled(true);

        // Change "Next" label to make the end of a category clear
        if (currentTopicIndex == topicsInCategory.size() - 1) {
            nextButton.setText("Next →");
            nextButton.setEnabled(false);
        } else {
            nextButton.setText("Next →");
            nextButton.setEnabled(true);
        }

        revalidate();
        repaint();
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    /**
     * Returns all visible problems that belong to the given category, in the
     * order maintained by DataStore (difficulty-sorted by default).
     *
     * @param category the category to filter by
     */
    private List<Problem> getProblemsForCategory(Category category) {
        List<Problem> categoryProblems = new ArrayList<>();

        for (Problem problem : DataStore.getInstance().getVisibleProblems()) {
            if (problem.getCategory() == category) {
                categoryProblems.add(problem);
            }
        }

        return categoryProblems;
    }

    // ── Widget factories ──────────────────────────────────────────────────────

    private JButton createNavButton(String buttonText, Color backgroundColor, Color foregroundColor) {
        JButton button = new JButton(buttonText);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setForeground(foregroundColor);
        button.setBackground(backgroundColor);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 20, 8, 20)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    /**
     * Converts a Category enum constant to a human-readable title-case string.
     * E.g. HELLO_WORLD → "Hello World".
     */
    private String formatCategoryName(Category category) {
        String rawName = category.name().replace("_", " ");
        String[] words = rawName.split(" ");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0)));
                formattedName.append(word.substring(1).toLowerCase());
                formattedName.append(" ");
            }
        }

        return formattedName.toString().trim();
    }

    /**
     * Returns the accent colour associated with a given category for sidebar
     * highlights and accent bars.
     */
    private Color resolveCategoryColor(Category category) {
        switch (category) {
            case HELLO_WORLD:         return ACCENT_BLUE;
            case CONTROL_FLOW:        return ACCENT_RED;
            case DATA_STRUCTURES:     return ACCENT_YELLOW;
            case ALGORITHMS:          return new Color(199, 146, 234);
            case NUMERICAL_METHODS:   return ACCENT_GREEN;
            case STUDY_METHODS:       return new Color(255, 126, 179);
            case WORKFORCE_SCENARIOS: return new Color(126, 184, 247);
            case PROBLEM_SOLVING:     return new Color(247, 160, 79);
            default:                  return TEXT_SECONDARY;
        }
    }

    /**
     * Returns the display colour for a difficulty level badge.
     */
    private Color resolveDifficultyColor(DifficultyLevel difficulty) {
        if (difficulty == DifficultyLevel.BEGINNER) {
            return ACCENT_GREEN;
        } else if (difficulty == DifficultyLevel.INTERMEDIATE) {
            return ACCENT_YELLOW;
        } else {
            return ACCENT_RED;
        }
    }
}
