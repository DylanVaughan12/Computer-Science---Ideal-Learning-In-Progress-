/**
 * File:    Problem.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.1
 * Purpose: Represents a single programming problem presented to the user.
 *          Stores the problem's metadata (title, difficulty, category),
 *          its associated InfoPanel, and the list of TestCase objects
 *          used for automated grading.
 */
package com.macrosoff.csil.model;

import com.macrosoff.csil.model.enums.Category;
import com.macrosoff.csil.model.enums.DifficultyLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Problem
 * The central domain object linking a programming challenge with its
 * explanatory InfoPanel and grading TestCases. Instructors can create
 * new Problem instances via CreateProblemDialog.
 */
public class Problem {

    // Minimum acceptable value for a problem identifier
    private static final int MINIMUM_PROBLEM_IDENTIFIER = 1;

    // Unique identifier for this problem (used as map key throughout the application)
    private int problemIdentifier;

    // Short display title shown in the sidebar and result panel
    private String problemTitle;

    // Full problem statement displayed in the problem header area
    private String problemDescription;

    // The difficulty tier (BEGINNER, INTERMEDIATE, ADVANCED)
    private DifficultyLevel difficultyLevel;

    // The subject-area category this problem belongs to
    private Category problemCategory;

    // The explanatory panel attached to this problem
    private InfoPanel infoPanel;

    // All test cases (visible and hidden) used to grade the user's submission
    private List<TestCase> testCaseList;

    /**
     * Constructs a Problem with all required fields.
     *
     * @param problemIdentifier  unique ID; stored as -1 if below minimum
     * @param problemTitle       display title; stored as "Untitled" if null or blank
     * @param problemDescription full problem statement; stored as empty string if null
     * @param difficultyLevel    difficulty tier; defaults to BEGINNER if null
     * @param problemCategory    subject category; defaults to PROBLEM_SOLVING if null
     * @param infoPanel          explanatory panel; may be null (no panel shown)
     */
    public Problem(int problemIdentifier, String problemTitle, String problemDescription,
                   DifficultyLevel difficultyLevel, Category problemCategory,
                   InfoPanel infoPanel) {

        // Validate problem identifier
        if (problemIdentifier >= MINIMUM_PROBLEM_IDENTIFIER) {
            this.problemIdentifier = problemIdentifier;
        } else {
            this.problemIdentifier = -1;
        }

        // Validate title — must be a non-blank string
        if (problemTitle != null && !problemTitle.isBlank()) {
            this.problemTitle = problemTitle;
        } else {
            this.problemTitle = "Untitled";
        }

        // Guard against null description
        if (problemDescription != null) {
            this.problemDescription = problemDescription;
        } else {
            this.problemDescription = "";
        }

        // Guard against null difficulty — default to the entry-level tier
        if (difficultyLevel != null) {
            this.difficultyLevel = difficultyLevel;
        } else {
            this.difficultyLevel = DifficultyLevel.BEGINNER;
        }

        // Guard against null category
        if (problemCategory != null) {
            this.problemCategory = problemCategory;
        } else {
            this.problemCategory = Category.PROBLEM_SOLVING;
        }

        // InfoPanel is optional and may be null
        this.infoPanel = infoPanel;

        // Initialise the test case list to an empty mutable list
        this.testCaseList = new ArrayList<>();
    }

    /**
     * Appends a TestCase to this problem's grading list.
     *
     * @param testCase the test case to add; ignored if null
     */
    public void addTestCase(TestCase testCase) {
        if (testCase != null) {
            testCaseList.add(testCase);
        }
    }

    // Getters

    public String getDescription() {
        return problemDescription;
    }

    /**
     * Updates the problem description.
     *
     * @param newDescription the new description text; ignored if null
     */
    public void setDescription(String newDescription) {
        if (newDescription != null) {
            this.problemDescription = newDescription;
        } else {
            this.problemDescription = "";
        }
    }

    public DifficultyLevel getDifficulty() {
        return difficultyLevel;
    }

    public InfoPanel getInfoPanel() {
        return infoPanel;
    }

    public List<TestCase> getTestCases() {
        return testCaseList;
    }

    public int getProblemID() {
        return problemIdentifier;
    }

    public String getTitle() {
        return problemTitle;
    }

    public Category getCategory() {
        return problemCategory;
    }

    /**
     * Updates the problem title.
     *
     * @param newTitle new title; ignored if null or blank
     */
    public void setTitle(String newTitle) {
        if (newTitle != null && !newTitle.isBlank()) {
            this.problemTitle = newTitle;
        }
    }

    /**
     * Replaces the info panel.
     *
     * @param newInfoPanel the new panel (may be null to remove the panel)
     */
    public void setInfoPanel(InfoPanel newInfoPanel) {
        this.infoPanel = newInfoPanel;
    }
}
