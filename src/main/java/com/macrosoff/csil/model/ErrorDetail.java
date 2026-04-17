/**
 * File:    ErrorDetail.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Stores a single compilation or runtime error produced when
 *          evaluating user-submitted code, including the line number and
 *          a human-readable error message.
 */
package com.macrosoff.csil.model;

/**
 * ErrorDetail
 * Represents one entry in the error list returned by SubmissionResult.
 * Both the lineNumber and the errorMessage are validated in the constructor
 * to ensure they hold meaningful values before being stored.
 */
public class ErrorDetail {

    // Minimum acceptable line number (lines are 1-indexed in user-facing output)
    private static final int MINIMUM_LINE_NUMBER = 1;

    // The source-code line on which the error was detected
    private int lineNumber;

    // Human-readable description of the error
    private String errorMessage;

    /**
     * Constructs an ErrorDetail.
     *
     * @param lineNumber   1-based line number of the error; stored as -1 if invalid
     * @param errorMessage description of the error; stored as empty string if null
     */
    public ErrorDetail(int lineNumber, String errorMessage) {
        // Validate lineNumber: must be at least 1
        if (lineNumber >= MINIMUM_LINE_NUMBER) {
            this.lineNumber = lineNumber;
        } else {
            // Assign a known-invalid sentinel so callers can detect bad input
            this.lineNumber = -1;
        }

        // Validate errorMessage: guard against null references
        if (errorMessage != null) {
            this.errorMessage = errorMessage;
        } else {
            this.errorMessage = "";
        }
    }

    /**
     * Returns the 1-based line number where the error occurred.
     * Returns -1 if the line number was not valid at construction time.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the human-readable error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the line number after construction.
     * Rejects values less than MINIMUM_LINE_NUMBER.
     *
     * @param lineNumber the new 1-based line number
     */
    public void setLineNumber(int lineNumber) {
        // Only update if the supplied value is within the acceptable range
        if (lineNumber >= MINIMUM_LINE_NUMBER) {
            this.lineNumber = lineNumber;
        } else {
            this.lineNumber = -1;
        }
    }

    /**
     * Sets the error message after construction.
     *
     * @param errorMessage the new error description; ignored if null
     */
    public void setErrorMessage(String errorMessage) {
        // Guard against null to preserve the invariant that errorMessage is never null
        if (errorMessage != null) {
            this.errorMessage = errorMessage;
        } else {
            this.errorMessage = "";
        }
    }

    @Override
    public String toString() {
        return "Line " + lineNumber + ": " + errorMessage;
    }
}
