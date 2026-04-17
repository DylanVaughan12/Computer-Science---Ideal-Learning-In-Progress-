/**
 * File:    SubmissionResult.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Represents the outcome of a user's code submission. Holds a list
 *          of ErrorDetail objects describing individual errors and provides
 *          methods to query overall pass/fail status and error counts.
 */
package com.macrosoff.csil.model;

import java.util.ArrayList;
import java.util.List;

/**
 * SubmissionResult
 * Returned by Attempt.submit() and consumed by the grading pipeline.
 * All numeric fields are validated at construction time.
 */
public class SubmissionResult {

    // Auto-incrementing identifier shared across all SubmissionResult instances
    private static int nextSubmissionIdentifier = 1;

    // Minimum acceptable value for attempt-related identifiers
    private static final int MINIMUM_IDENTIFIER_VALUE = 1;

    // Minimum acceptable value for the total attempts counter
    private static final int MINIMUM_TOTAL_ATTEMPTS = 1;

    // Unique identifier for this submission result
    private int submissionIdentifier;

    // Identifier of the attempt that produced this result
    private int attemptIdentifier;

    // True when all test cases passed
    private boolean passed;

    // Number of submission attempts made by the user for this problem
    private int totalAttempts;

    // List of individual errors found during evaluation
    private List<ErrorDetail> errorDetailList;

    /**
     * Constructs a SubmissionResult.
     *
     * @param attemptIdentifier the ID of the owning Attempt; stored as -1 if invalid
     * @param passed            true when the submission passed all test cases
     * @param totalAttempts     how many times the user has submitted; stored as 1 if invalid
     */
    public SubmissionResult(int attemptIdentifier, boolean passed, int totalAttempts) {
        // Assign the next available unique identifier
        this.submissionIdentifier = nextSubmissionIdentifier;
        nextSubmissionIdentifier = nextSubmissionIdentifier + 1;

        // Validate attempt identifier
        if (attemptIdentifier >= MINIMUM_IDENTIFIER_VALUE) {
            this.attemptIdentifier = attemptIdentifier;
        } else {
            this.attemptIdentifier = -1;
        }

        // Boolean field requires no validation
        this.passed = passed;

        // Validate total attempts — must be at least 1
        if (totalAttempts >= MINIMUM_TOTAL_ATTEMPTS) {
            this.totalAttempts = totalAttempts;
        } else {
            this.totalAttempts = 1;
        }

        // Initialise the error list to an empty mutable list
        this.errorDetailList = new ArrayList<>();
    }

    /**
     * Appends an ErrorDetail to the list of errors for this submission.
     *
     * @param errorDetail the error to add; ignored if null
     */
    public void addError(ErrorDetail errorDetail) {
        if (errorDetail != null) {
            errorDetailList.add(errorDetail);
        }
    }

    /**
     * Returns true when the submission passed all test cases.
     */
    public boolean isPassed() {
        return passed;
    }

    /**
     * Returns the number of errors recorded in the error list.
     */
    public int getTotalFailures() {
        return errorDetailList.size();
    }

    /**
     * Returns an unmodifiable view of the error list.
     */
    public List<ErrorDetail> getErrorList() {
        return java.util.Collections.unmodifiableList(errorDetailList);
    }

    /**
     * Returns 1 if the submission passed, 0 otherwise.
     */
    public int getSolvedNumber() {
        int solvedCount = 0;
        if (passed) {
            solvedCount = 1;
        }
        return solvedCount;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public int getSubmissionID() {
        return submissionIdentifier;
    }

    public int getAttemptID() {
        return attemptIdentifier;
    }
}
