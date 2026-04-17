/**
 * File:    AssessmentResult.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Stores the outcome of an Assessment evaluation including the final
 *          grade, pass/fail status, and the threshold used for grading. All
 *          fields are validated at construction time.
 */
package com.macrosoff.csil.model;

/**
 * AssessmentResult
 * An immutable value object returned by Assessment.codeAssessment().
 * Callers query this object to determine whether the student passed and
 * what grade they achieved.
 */
public class AssessmentResult {

    // Grade must fall within this range to be considered valid
    private static final float MINIMUM_VALID_GRADE = 0.0f;
    private static final float MAXIMUM_VALID_GRADE = 100.0f;

    // Passing threshold must be a positive percentage
    private static final float MINIMUM_VALID_THRESHOLD = 0.0f;

    // Identifier of the assessment this result belongs to
    private int assessmentIdentifier;

    // The grade the student achieved, expressed as a percentage
    private float finalGrade;

    // True when finalGrade is at or above passingThreshold
    private boolean hasPassed;

    // The minimum grade required to pass
    private float passingThreshold;

    /**
     * Constructs an AssessmentResult.
     *
     * @param assessmentIdentifier the ID of the owning Assessment
     * @param finalGrade           percentage grade (0.0–100.0); clamped if out of range
     * @param hasPassed            true when the student met the threshold
     * @param passingThreshold     minimum passing grade; defaults to 60 if invalid
     */
    public AssessmentResult(int assessmentIdentifier, float finalGrade,
                            boolean hasPassed, float passingThreshold) {

        // Identifiers have no meaningful minimum in this context
        this.assessmentIdentifier = assessmentIdentifier;

        // Clamp finalGrade to [0, 100] so downstream formatting is safe
        if (finalGrade < MINIMUM_VALID_GRADE) {
            this.finalGrade = MINIMUM_VALID_GRADE;
        } else if (finalGrade > MAXIMUM_VALID_GRADE) {
            this.finalGrade = MAXIMUM_VALID_GRADE;
        } else {
            this.finalGrade = finalGrade;
        }

        // Boolean assignment requires no validation
        this.hasPassed = hasPassed;

        // Validate threshold — must be a positive value
        if (passingThreshold > MINIMUM_VALID_THRESHOLD) {
            this.passingThreshold = passingThreshold;
        } else {
            // Fall back to the conventional passing threshold
            this.passingThreshold = 60.0f;
        }
    }

    /**
     * Prints a formatted summary of this assessment result to standard output.
     */
    public void printFullAssessment() {
        System.out.println("=== Assessment Result ===");
        System.out.println("Assessment ID      : " + assessmentIdentifier);
        System.out.printf ("Final Grade        : %.1f%%\n", finalGrade);
        System.out.println("Passing Threshold  : " + passingThreshold + "%");
        System.out.println("Passed             : " + (hasPassed ? "YES" : "NO"));
    }

    /**
     * Returns the final grade as a percentage.
     */
    public float getFinalGrade() {
        return finalGrade;
    }

    /**
     * Updates the final grade (used by Assessment when re-grading).
     *
     * @param newFinalGrade new grade value; clamped to [0, 100]
     */
    public void setFinalGrade(float newFinalGrade) {
        if (newFinalGrade < MINIMUM_VALID_GRADE) {
            this.finalGrade = MINIMUM_VALID_GRADE;
        } else if (newFinalGrade > MAXIMUM_VALID_GRADE) {
            this.finalGrade = MAXIMUM_VALID_GRADE;
        } else {
            this.finalGrade = newFinalGrade;
        }
    }

    /**
     * Returns true if the student's grade met or exceeded the passing threshold.
     */
    public boolean hasPassed() {
        return hasPassed;
    }

    public int getAssessmentID() {
        return assessmentIdentifier;
    }

    public float getPassingThreshold() {
        return passingThreshold;
    }
}
