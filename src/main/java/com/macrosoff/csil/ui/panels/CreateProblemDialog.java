/**
 * File:    CreateProblemDialog.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Provides a modal dialog that allows INSTRUCTOR and ADMIN users to
 *          author new programming problems directly inside the application
 *          without editing source code. The form collects all required fields
 *          (title, description, difficulty, category, info-panel explanations
 *          for all three skill levels, a hint, answer code, and up to five
 *          test cases) and adds the completed problem to DataStore on confirm.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.data.DataStore;
import com.macrosoff.csil.model.InfoPanel;
import com.macrosoff.csil.model.Problem;
import com.macrosoff.csil.model.Problem;
import com.macrosoff.csil.model.TestCase;
import com.macrosoff.csil.model.enums.Category;
import com.macrosoff.csil.model.enums.DifficultyLevel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * CreateProblemDialog
 * A tabbed modal dialog that walks the instructor through the four sections
 * of a problem definition:
 *
 *   Tab 1 — Basic Info  : title, description, difficulty, category
 *   Tab 2 — Info Panel  : explanations for High School / Undergraduate / Professional,
 *                         a single hint, and the reference answer code
 *   Tab 3 — Test Cases  : up to MAX_TEST_CASES input/expected-output pairs with
 *                         a hidden-from-student toggle on each
 *   Tab 4 — Preview     : read-only summary before the problem is saved
 *
 * On "Save Problem" the dialog validates that a title and at least one test case
 * are present, then calls DataStore.addCustomProblem() and closes.
 *
 * TODO (team): Add drag-and-drop ordering for test cases so instructors can
 *   control the order in which results are shown to students.
 * TODO (team): Allow instructors to attach a code snippet to the InfoPanel
 *   (currently the snippet list is not surfaced in this dialog).
 */
public class CreateProblemDialog extends JDialog {

    // ── Colour constants (static final per checklist item 10) ─────────────────
    private static final Color BACKGROUND_PRIMARY       = new Color(22, 26, 36);
    private static final Color BACKGROUND_SECONDARY     = new Color(30, 35, 51);
    private static final Color BACKGROUND_TERTIARY      = new Color(37, 43, 61);
    private static final Color ACCENT_BLUE              = new Color(79, 142, 247);
    private static final Color ACCENT_GREEN             = new Color(61, 214, 140);
    private static final Color ACCENT_RED               = new Color(247, 112, 79);
    private static final Color TEXT_PRIMARY             = new Color(232, 236, 244);
    private static final Color TEXT_SECONDARY           = new Color(139, 147, 167);
    private static final Color TEXT_HINT                = new Color(84, 93, 114);
    private static final Color BORDER_COLOR             = new Color(42, 48, 71);

    // Maximum number of test cases an instructor may add via this dialog
    private static final int MAX_TEST_CASES = 5;

    // Minimum auto-generated problem ID offset to avoid clashing with seeded IDs
    private static final int CUSTOM_PROBLEM_ID_OFFSET = 1000;

    // ── Form fields — Tab 1: Basic Info ───────────────────────────────────────
    private JTextField  problemTitleField;
    private JTextArea   problemDescriptionArea;
    private JComboBox<DifficultyLevel> difficultyComboBox;
    private JComboBox<Category>        categoryComboBox;

    // ── Form fields — Tab 2: Info Panel ───────────────────────────────────────
    private JTextArea explanationArea;
    private JTextArea hintTextArea;
    private JTextArea answerCodeArea;

    // ── Form fields — Tab 3: Test Cases ───────────────────────────────────────
    // Parallel lists: each index corresponds to one test-case row
    private List<JTextField> testCaseInputFieldList;
    private List<JTextField> testCaseExpectedOutputFieldList;
    private List<JCheckBox>  testCaseHiddenCheckBoxList;

    // ── Status / result ───────────────────────────────────────────────────────
    private JLabel  statusLabel;
    private boolean problemSavedSuccessfully;

    // ── Edit mode ─────────────────────────────────────────────────────────────
    // When non-null, the dialog is in edit mode and will update this problem
    // rather than creating a new one.
    private Problem problemBeingEdited;

    // ── Callback ──────────────────────────────────────────────────────────────
    // Called after the problem is successfully saved so the caller can refresh its UI
    private final Runnable onProblemSavedCallback;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Create mode: opens a blank form for authoring a brand-new problem.
     *
     * @param parentFrame             the owning frame (for modal positioning)
     * @param onProblemSavedCallback  runnable invoked after a successful save;
     *                                may be null if no callback is needed
     */
    public CreateProblemDialog(Frame parentFrame, Runnable onProblemSavedCallback) {
        super(parentFrame, "Create New Problem", true);

        // No problem being edited in create mode
        this.problemBeingEdited = null;

        // Store callback (null is acceptable)
        this.onProblemSavedCallback = onProblemSavedCallback;

        // Initialise lists before buildUI() populates them
        this.testCaseInputFieldList         = new ArrayList<>();
        this.testCaseExpectedOutputFieldList = new ArrayList<>();
        this.testCaseHiddenCheckBoxList      = new ArrayList<>();

        // Flag starts false; set to true only on a valid save
        this.problemSavedSuccessfully = false;

        buildUserInterface();
        pack();
        setMinimumSize(new Dimension(680, 600));
        setLocationRelativeTo(parentFrame);
        setResizable(true);
    }

    /**
     * Edit mode: opens the form pre-filled with the given problem's existing data.
     *
     * @param parentFrame             the owning frame
     * @param existingProblem         the custom problem to edit
     * @param onProblemSavedCallback  runnable invoked after the update is saved
     */
    public CreateProblemDialog(Frame parentFrame, Problem existingProblem,
                               Runnable onProblemSavedCallback) {
        super(parentFrame, "Edit Problem", true);

        // Store reference to the problem being edited
        this.problemBeingEdited = existingProblem;

        this.onProblemSavedCallback = onProblemSavedCallback;

        // Initialise lists before buildUI() populates them
        this.testCaseInputFieldList         = new ArrayList<>();
        this.testCaseExpectedOutputFieldList = new ArrayList<>();
        this.testCaseHiddenCheckBoxList      = new ArrayList<>();

        this.problemSavedSuccessfully = false;

        buildUserInterface();

        // Pre-fill all fields from the existing problem after the UI is built
        prefillFromExistingProblem(existingProblem);

        pack();
        setMinimumSize(new Dimension(680, 600));
        setLocationRelativeTo(parentFrame);
        setResizable(true);
    }

    // ── UI construction ───────────────────────────────────────────────────────

    /**
     * Assembles the full dialog layout: a JTabbedPane for the four sections,
     * a status label, and Save/Cancel buttons.
     */
    private void buildUserInterface() {
        getContentPane().setBackground(BACKGROUND_PRIMARY);
        setLayout(new BorderLayout(0, 0));
        getRootPane().setBorder(new EmptyBorder(16, 18, 16, 18));

        // ── Tabbed pane ───────────────────────────────────────────────────────
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BACKGROUND_PRIMARY);
        tabbedPane.setForeground(TEXT_PRIMARY);
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabbedPane.addTab("1. Basic Info",  buildBasicInfoTab());
        tabbedPane.addTab("2. Info Panel",  buildInfoPanelTab());
        tabbedPane.addTab("3. Test Cases",  buildTestCasesTab());

        add(tabbedPane, BorderLayout.CENTER);

        // ── Status + buttons ──────────────────────────────────────────────────
        JPanel bottomPanel = buildBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        // Escape key closes without saving
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escapeClose");
        getRootPane().getActionMap().put("escapeClose", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });
    }

    // ── Tab 1: Basic Info ─────────────────────────────────────────────────────

    private JPanel buildBasicInfoTab() {
        JPanel tabPanel = new JPanel();
        tabPanel.setBackground(BACKGROUND_PRIMARY);
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
        tabPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Problem title
        tabPanel.add(createFieldLabel("Problem Title *"));
        tabPanel.add(Box.createVerticalStrut(5));
        problemTitleField = createTextField("e.g. Reverse a String");
        tabPanel.add(problemTitleField);
        tabPanel.add(Box.createVerticalStrut(14));

        // Problem description
        tabPanel.add(createFieldLabel("Problem Description *"));
        tabPanel.add(Box.createVerticalStrut(5));
        problemDescriptionArea = createTextArea(
                "Describe what the student must do. Be specific about inputs and expected outputs.", 5);
        tabPanel.add(new JScrollPane(problemDescriptionArea));
        tabPanel.add(Box.createVerticalStrut(14));

        // Difficulty dropdown
        tabPanel.add(createFieldLabel("Difficulty Level"));
        tabPanel.add(Box.createVerticalStrut(5));
        difficultyComboBox = new JComboBox<>(DifficultyLevel.values());
        styleComboBox(difficultyComboBox);
        tabPanel.add(difficultyComboBox);
        tabPanel.add(Box.createVerticalStrut(14));

        // Category dropdown
        tabPanel.add(createFieldLabel("Category"));
        tabPanel.add(Box.createVerticalStrut(5));
        categoryComboBox = new JComboBox<>(Category.values());
        styleComboBox(categoryComboBox);
        tabPanel.add(categoryComboBox);

        return wrapInScrollPane(tabPanel);
    }

    // ── Tab 2: Info Panel ─────────────────────────────────────────────────────

    private JPanel buildInfoPanelTab() {
        JPanel tabPanel = new JPanel();
        tabPanel.setBackground(BACKGROUND_PRIMARY);
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
        tabPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Single explanation shown in LearnPanel
        tabPanel.add(createFieldLabel("Explanation (shown to students in the Learn tab)"));
        tabPanel.add(Box.createVerticalStrut(5));
        explanationArea = createTextArea(
                "Explain this concept clearly. This is what the student reads before practising.", 6);
        tabPanel.add(new JScrollPane(explanationArea));
        tabPanel.add(Box.createVerticalStrut(14));

        // Student-facing hint
        tabPanel.add(createFieldLabel("Hint (shown when student clicks Hint during practice)"));
        tabPanel.add(Box.createVerticalStrut(5));
        hintTextArea = createTextArea("e.g. Think about using a while loop with two pointers.", 2);
        tabPanel.add(new JScrollPane(hintTextArea));
        tabPanel.add(Box.createVerticalStrut(14));

        // Reference answer code
        tabPanel.add(createFieldLabel("Answer Code (reference solution — not shown until student passes)"));
        tabPanel.add(Box.createVerticalStrut(5));
        answerCodeArea = createTextArea("// Write the full correct Java solution here", 5);
        answerCodeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        answerCodeArea.setForeground(ACCENT_GREEN);
        tabPanel.add(new JScrollPane(answerCodeArea));

        return wrapInScrollPane(tabPanel);
    }

    // ── Tab 3: Test Cases ─────────────────────────────────────────────────────

    private JPanel buildTestCasesTab() {
        JPanel tabPanel = new JPanel();
        tabPanel.setBackground(BACKGROUND_PRIMARY);
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
        tabPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Instruction label
        JLabel instructionLabel = new JLabel(
                "<html><body style='color:#8b93a7;width:580px'>" +
                "Add up to " + MAX_TEST_CASES + " test cases. Each test case compares the " +
                "program's standard output against the expected output string. " +
                "Hidden test cases are run but their input is not shown to the student." +
                "</body></html>");
        instructionLabel.setAlignmentX(LEFT_ALIGNMENT);
        tabPanel.add(instructionLabel);
        tabPanel.add(Box.createVerticalStrut(16));

        // Column headers
        JPanel headerRow = new JPanel(new GridLayout(1, 3, 8, 0));
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        headerRow.add(createColumnHeader("Input (leave blank if no input)"));
        headerRow.add(createColumnHeader("Expected Output *"));
        headerRow.add(createColumnHeader("Hidden from student?"));
        tabPanel.add(headerRow);
        tabPanel.add(Box.createVerticalStrut(6));

        // Add MAX_TEST_CASES rows
        for (int rowIndex = 0; rowIndex < MAX_TEST_CASES; rowIndex++) {
            tabPanel.add(buildTestCaseRow(rowIndex + 1));
            tabPanel.add(Box.createVerticalStrut(6));
        }

        return wrapInScrollPane(tabPanel);
    }

    /**
     * Builds one test-case input row and registers its widgets in the parallel lists.
     *
     * @param rowNumber 1-based display number shown in the row label
     */
    private JPanel buildTestCaseRow(int rowNumber) {
        JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
        rowPanel.setBackground(BACKGROUND_SECONDARY);
        rowPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 10, 8, 10)));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        rowPanel.setAlignmentX(LEFT_ALIGNMENT);

        // Row number label
        JLabel rowNumberLabel = new JLabel("TC " + rowNumber);
        rowNumberLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        rowNumberLabel.setForeground(TEXT_HINT);
        rowNumberLabel.setPreferredSize(new Dimension(36, 0));
        rowPanel.add(rowNumberLabel, BorderLayout.WEST);

        // Three-column content area
        JPanel fieldsPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        fieldsPanel.setOpaque(false);

        JTextField inputField          = createCompactTextField("");
        JTextField expectedOutputField = createCompactTextField("");
        JCheckBox  hiddenCheckBox      = new JCheckBox("Hidden");
        hiddenCheckBox.setForeground(TEXT_SECONDARY);
        hiddenCheckBox.setBackground(BACKGROUND_SECONDARY);
        hiddenCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 12));

        fieldsPanel.add(inputField);
        fieldsPanel.add(expectedOutputField);
        fieldsPanel.add(hiddenCheckBox);
        rowPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Register in parallel lists so buildProblem() can read them
        testCaseInputFieldList.add(inputField);
        testCaseExpectedOutputFieldList.add(expectedOutputField);
        testCaseHiddenCheckBoxList.add(hiddenCheckBox);

        return rowPanel;
    }

    // ── Bottom panel (status + buttons) ───────────────────────────────────────

    private JPanel buildBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(12, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        // Status label — shows validation errors or success message
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_SECONDARY);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        // Button row
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);

        JButton cancelButton = createDialogButton("Cancel", BACKGROUND_SECONDARY, TEXT_SECONDARY);
        cancelButton.addActionListener(actionEvent -> dispose());

        String saveBtnLabel = (problemBeingEdited != null) ? "Save Changes" : "Save Problem";
        JButton saveButton = createDialogButton(saveBtnLabel, ACCENT_BLUE, Color.WHITE);
        saveButton.addActionListener(actionEvent -> saveProblem());

        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);
        bottomPanel.add(buttonRow, BorderLayout.EAST);

        return bottomPanel;
    }

    // ── Save logic ────────────────────────────────────────────────────────────

    // ── Prefill for edit mode ─────────────────────────────────────────────────

    /**
     * Pre-fills all form fields from the given existing problem.
     * Called only in edit mode after buildUserInterface() has run.
     *
     * @param existingProblem the problem whose data populates the form
     */
    private void prefillFromExistingProblem(Problem existingProblem) {
        // Tab 1 — Basic Info
        problemTitleField.setText(existingProblem.getTitle());
        problemDescriptionArea.setText(existingProblem.getDescription());
        difficultyComboBox.setSelectedItem(existingProblem.getDifficulty());
        categoryComboBox.setSelectedItem(existingProblem.getCategory());

        // Tab 2 — Info Panel
        if (existingProblem.getInfoPanel() != null) {
            explanationArea.setText(existingProblem.getInfoPanel().getExplanation());
            answerCodeArea.setText(existingProblem.getInfoPanel().getAnswerCode());

            // Prefill the hint if one is stored
            String storedHint = existingProblem.getInfoPanel().getHint();
            String defaultHint = "No hint available for this topic.";
            if (storedHint != null && !storedHint.equals(defaultHint)) {
                hintTextArea.setText(storedHint);
            }
        }

        // Tab 3 — Test Cases: fill in the rows from the existing test case list
        int rowIndex = 0;
        for (TestCase existingTestCase : existingProblem.getTestCases()) {
            if (rowIndex >= MAX_TEST_CASES) {
                break;
            }
            testCaseInputFieldList.get(rowIndex).setText(existingTestCase.getInputData());
            testCaseExpectedOutputFieldList.get(rowIndex).setText(
                    existingTestCase.getExpectedOutput());
            testCaseHiddenCheckBoxList.get(rowIndex).setSelected(existingTestCase.isHidden());
            rowIndex = rowIndex + 1;
        }
    }

    /**
     * Validates the form, builds a Problem object, and adds it to DataStore.
     * Sets statusLabel to an appropriate message in all cases.
     */
    private void saveProblem() {
        // Collect and trim the title
        String problemTitle = problemTitleField.getText().trim();

        // Validate: title is mandatory
        if (problemTitle.isEmpty()) {
            showStatus("Please enter a problem title.", ACCENT_RED);
            return;
        }

        // Collect description
        String problemDescription = problemDescriptionArea.getText().trim();

        // Validate: description is mandatory
        if (problemDescription.isEmpty()) {
            showStatus("Please enter a problem description.", ACCENT_RED);
            return;
        }

        // Collect at least one test case with a non-empty expected output
        List<TestCase> collectedTestCases = collectTestCases(problemTitle);
        if (collectedTestCases.isEmpty()) {
            showStatus("Please provide at least one test case with an expected output.", ACCENT_RED);
            return;
        }

        // Determine the problem ID: reuse existing ID in edit mode, generate new in create mode
        int newProblemIdentifier;
        if (problemBeingEdited != null) {
            newProblemIdentifier = problemBeingEdited.getProblemID();
        } else {
            newProblemIdentifier = CUSTOM_PROBLEM_ID_OFFSET
                    + DataStore.getInstance().getProblemManager().getAllProblems().size() + 1;
        }

        // Build the InfoPanel from the explanation fields
        InfoPanel newInfoPanel = buildInfoPanel(newProblemIdentifier);

        // Get selected difficulty and category from the combo boxes
        DifficultyLevel selectedDifficulty = (DifficultyLevel) difficultyComboBox.getSelectedItem();
        Category selectedCategory          = (Category) categoryComboBox.getSelectedItem();

        // Construct the Problem
        Problem newProblem = new Problem(
                newProblemIdentifier,
                problemTitle,
                problemDescription,
                selectedDifficulty,
                selectedCategory,
                newInfoPanel);

        // Attach all collected test cases
        for (TestCase testCase : collectedTestCases) {
            newProblem.addTestCase(testCase);
        }

        // Persist to DataStore — either update existing or add new
        if (problemBeingEdited != null) {
            // Edit mode: replace the existing problem's fields
            DataStore.getInstance().updateCustomProblem(newProblem);
        } else {
            // Create mode: add as a brand-new problem
            DataStore.getInstance().addCustomProblem(newProblem);
        }

        // Mark success and fire callback
        problemSavedSuccessfully = true;
        showStatus("Problem \"" + problemTitle + "\" saved successfully!", ACCENT_GREEN);

        // Notify the InstructorPanel to refresh its list
        if (onProblemSavedCallback != null) {
            onProblemSavedCallback.run();
        }

        // Delay close slightly so the instructor can read the success message
        Timer closeTimer = new Timer(1200, timerEvent -> dispose());
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    /**
     * Builds an InfoPanel populated from the explanation and hint text areas.
     *
     * @param panelIdentifier the ID to assign to the new InfoPanel
     */
    private InfoPanel buildInfoPanel(int panelIdentifier) {
        InfoPanel newInfoPanel = new InfoPanel(panelIdentifier);

        // Add skill-level explanations — fall back to a default message if the field is blank
        // Store the single explanation text
        String explanationText = explanationArea.getText().trim();
        if (explanationText.isEmpty()) {
            explanationText = "No explanation provided.";
        }
        newInfoPanel.setExplanation(explanationText);

        // Add the hint if one was provided
        String hintText = hintTextArea.getText().trim();
        if (!hintText.isEmpty()) {
            newInfoPanel.setHint(hintText);
        }

        // Set the answer code
        String answerCodeText = answerCodeArea.getText().trim();
        newInfoPanel.setAnswerCode(answerCodeText);

        return newInfoPanel;
    }

    /**
     * Iterates the test-case field lists and returns only the rows where the
     * expected output field is non-empty.
     *
     * @param problemTitle used to assign matching problem IDs to each TestCase
     */
    private List<TestCase> collectTestCases(String problemTitle) {
        // Determine the next available problem ID for naming purposes
        int expectedProblemIdentifier;
        if (problemBeingEdited != null) {
            expectedProblemIdentifier = problemBeingEdited.getProblemID();
        } else {
            expectedProblemIdentifier = CUSTOM_PROBLEM_ID_OFFSET
                    + DataStore.getInstance().getProblemManager().getAllProblems().size() + 1;
        }

        List<TestCase> collectedTestCases = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < testCaseExpectedOutputFieldList.size(); rowIndex++) {
            String expectedOutput = testCaseExpectedOutputFieldList.get(rowIndex).getText().trim();

            // Skip rows where the instructor left the expected output blank
            if (!expectedOutput.isEmpty()) {
                String inputData      = testCaseInputFieldList.get(rowIndex).getText().trim();
                boolean isHidden      = testCaseHiddenCheckBoxList.get(rowIndex).isSelected();

                // Generate a unique test-case ID from the problem ID and row index
                int testCaseIdentifier = expectedProblemIdentifier * 100 + rowIndex + 1;

                TestCase newTestCase = new TestCase(
                        testCaseIdentifier,
                        expectedProblemIdentifier,
                        inputData,
                        expectedOutput,
                        isHidden);

                collectedTestCases.add(newTestCase);
            }
        }
        return collectedTestCases;
    }

    // ── Status helper ─────────────────────────────────────────────────────────

    private void showStatus(String message, Color textColor) {
        statusLabel.setText(message);
        statusLabel.setForeground(textColor);
    }

    // ── Widget factory methods ────────────────────────────────────────────────

    private JLabel createFieldLabel(String labelText) {
        JLabel label = new JLabel(labelText.toUpperCase());
        label.setFont(new Font("SansSerif", Font.PLAIN, 10));
        label.setForeground(TEXT_HINT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createColumnHeader(String headerText) {
        JLabel label = new JLabel(headerText);
        label.setFont(new Font("SansSerif", Font.PLAIN, 10));
        label.setForeground(TEXT_HINT);
        return label;
    }

    private JTextField createTextField(String placeholderText) {
        JTextField textField = new JTextField();
        textField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        textField.setForeground(TEXT_PRIMARY);
        textField.setBackground(BACKGROUND_SECONDARY);
        textField.setCaretColor(ACCENT_BLUE);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(7, 10, 7, 10)));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        textField.setAlignmentX(LEFT_ALIGNMENT);
        return textField;
    }

    private JTextField createCompactTextField(String initialText) {
        JTextField textField = new JTextField(initialText);
        textField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textField.setForeground(TEXT_PRIMARY);
        textField.setBackground(BACKGROUND_TERTIARY);
        textField.setCaretColor(ACCENT_BLUE);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(4, 8, 4, 8)));
        return textField;
    }

    private JTextArea createTextArea(String placeholderText, int rowCount) {
        JTextArea textArea = new JTextArea(rowCount, 40);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        textArea.setForeground(TEXT_PRIMARY);
        textArea.setBackground(BACKGROUND_SECONDARY);
        textArea.setCaretColor(ACCENT_BLUE);
        textArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    private <E> void styleComboBox(JComboBox<E> comboBox) {
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        comboBox.setBackground(BACKGROUND_SECONDARY);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        comboBox.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JButton createDialogButton(String buttonText, Color backgroundColor, Color foregroundColor) {
        JButton button = new JButton(buttonText);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setForeground(foregroundColor);
        button.setBackground(backgroundColor);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(7, 18, 7, 18)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Wraps a content panel in a JScrollPane with the application's dark styling.
     *
     * @param contentPanel the panel to wrap
     */
    private JPanel wrapInScrollPane(JPanel contentPanel) {
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND_PRIMARY);

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(BACKGROUND_PRIMARY);
        wrapperPanel.add(scrollPane, BorderLayout.CENTER);
        return wrapperPanel;
    }

    // ── Result accessor ───────────────────────────────────────────────────────

    /**
     * Returns true if the instructor successfully saved a problem before closing.
     */
    public boolean wasProblemSaved() {
        return problemSavedSuccessfully;
    }
}
