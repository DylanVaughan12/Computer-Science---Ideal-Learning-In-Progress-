/**
 * File:    TestCase.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Represents a single input/output pair used to verify whether the
 *          user's submitted code produces the expected result. Hidden test
 *          cases are run during grading but their input is not revealed to
 *          the student.
 */
package com.macrosoff.csil.model;

/**
 * TestCase
 * Stores the input data, expected output, and visibility flag for one
 * automated test. All fields are validated at construction time.
 */
public class TestCase {

    // Minimum acceptable value for a test case identifier
    private static final int MINIMUM_IDENTIFIER_VALUE = 1;

    // Minimum acceptable value for a problem identifier
    private static final int MINIMUM_PROBLEM_IDENTIFIER_VALUE = 1;

    // Unique identifier for this test case
    private int testCaseIdentifier;

    // Identifier of the problem this test case belongs to
    private int problemIdentifier;

    // The input data string fed to the user's program (may be empty for no-input problems)
    private String inputData;

    // The expected standard-output string the user's program must produce
    private String expectedOutput;

    // When true, the test case input is hidden from the student in the results panel
    private boolean isHiddenFromStudent;

    /**
     * Constructs a TestCase with full field initialisation and validation.
     *
     * @param testCaseIdentifier   unique ID; stored as -1 if less than MINIMUM_IDENTIFIER_VALUE
     * @param problemIdentifier    owning problem ID; stored as -1 if invalid
     * @param inputData            input string; stored as empty string if null
     * @param expectedOutput       expected stdout; stored as empty string if null
     * @param isHiddenFromStudent  true to hide input from the results panel
     */
    public TestCase(int testCaseIdentifier, int problemIdentifier,
                    String inputData, String expectedOutput, boolean isHiddenFromStudent) {

        // Validate test case identifier
        if (testCaseIdentifier >= MINIMUM_IDENTIFIER_VALUE) {
            this.testCaseIdentifier = testCaseIdentifier;
        } else {
            this.testCaseIdentifier = -1;
        }

        // Validate problem identifier
        if (problemIdentifier >= MINIMUM_PROBLEM_IDENTIFIER_VALUE) {
            this.problemIdentifier = problemIdentifier;
        } else {
            this.problemIdentifier = -1;
        }

        // Guard against null input data
        if (inputData != null) {
            this.inputData = inputData;
        } else {
            this.inputData = "";
        }

        // Guard against null expected output
        if (expectedOutput != null) {
            this.expectedOutput = expectedOutput;
        } else {
            this.expectedOutput = "";
        }

        // Boolean assignment requires no validation
        this.isHiddenFromStudent = isHiddenFromStudent;
    }

    // Getters — named to match the original interface used by ResultPanel and AssessmentService

    public int getTestCaseID() {
        return testCaseIdentifier;
    }

    public int getProblemID() {
        return problemIdentifier;
    }

    public String getInputData() {
        return inputData;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public boolean isHidden() {
        return isHiddenFromStudent;
    }

    /**
     * Updates the expected output string.
     *
     * @param expectedOutput new expected output; ignored if null
     */
    public void setExpectedOutput(String expectedOutput) {
        if (expectedOutput != null) {
            this.expectedOutput = expectedOutput;
        } else {
            this.expectedOutput = "";
        }
    }
}
