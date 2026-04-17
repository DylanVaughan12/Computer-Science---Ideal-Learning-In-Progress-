/**
 * File:    Progress.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Tracks a single user's percentage completion through the problem
 *          set. Provides methods to increase, decrease, reset, and query
 *          completion state. All setter methods validate their parameters
 *          before assigning values to class attributes.
 */
package com.macrosoff.csil.model;

/**
 * Progress
 * Maintains a floating-point progress percentage between 0 and
 * progressPercentageCompletionThreshold. When the percentage reaches or
 * exceeds the threshold, hasCompleted is set to true automatically.
 */
public class Progress {

    // Auto-increment counter shared across all Progress instances
    private static int nextIdentifier = 1;

    // Minimum allowable progress percentage
    private static final float MINIMUM_PROGRESS_PERCENTAGE = 0.0f;

    // Unique identifier for this Progress record
    private int progressID;

    // Current completion percentage (0 to progressPercentageCompletionThreshold)
    private float progressPercentage;

    // The percentage at which the associated unit is considered complete
    private float progressPercentageCompletionThreshold;

    // True once progressPercentage reaches progressPercentageCompletionThreshold
    private boolean hasCompleted;

    /**
     * Constructs a Progress object with the default threshold of 100%.
     */
    public Progress() {
        // Assign a unique identifier
        this.progressID = nextIdentifier;
        nextIdentifier = nextIdentifier + 1;

        // Initialise all fields to their starting values
        this.progressPercentage = 0.0f;
        this.progressPercentageCompletionThreshold = 100.0f;
        this.hasCompleted = false;
    }

    /**
     * Constructs a Progress object with a custom completion threshold.
     *
     * @param completionThreshold percentage at which completion is declared;
     *                            must be greater than 0 — otherwise defaults to 100
     */
    public Progress(float completionThreshold) {
        // Delegate shared initialisation to the no-arg constructor
        this();

        // Validate threshold: must be a positive value
        if (completionThreshold > MINIMUM_PROGRESS_PERCENTAGE) {
            this.progressPercentageCompletionThreshold = completionThreshold;
        } else {
            // Assign a known-acceptable fallback
            this.progressPercentageCompletionThreshold = 100.0f;
        }
    }

    /**
     * Returns true when the progress percentage has reached the threshold.
     */
    public boolean isComplete() {
        return progressPercentage >= progressPercentageCompletionThreshold;
    }

    /**
     * Increases the progress percentage by the given amount.
     * Clamps the result to the completion threshold so it never exceeds 100%.
     *
     * @param amount positive number of percentage points to add
     */
    public void increaseProgress(float amount) {
        // Only add positive amounts to prevent accidental decreases via this method
        if (amount > MINIMUM_PROGRESS_PERCENTAGE) {
            progressPercentage = progressPercentage + amount;

            // Clamp to threshold so the percentage never exceeds it
            if (progressPercentage > progressPercentageCompletionThreshold) {
                progressPercentage = progressPercentageCompletionThreshold;
            }
        }

        // Re-evaluate completion after the update
        hasCompleted = isComplete();
    }

    /**
     * Decreases the progress percentage by the given amount.
     * Clamps the result to zero so it never goes negative.
     *
     * @param amount positive number of percentage points to subtract
     */
    public void decreaseProgress(float amount) {
        // Only subtract positive amounts
        if (amount > MINIMUM_PROGRESS_PERCENTAGE) {
            progressPercentage = progressPercentage - amount;

            // Clamp to minimum so the percentage never goes below zero
            if (progressPercentage < MINIMUM_PROGRESS_PERCENTAGE) {
                progressPercentage = MINIMUM_PROGRESS_PERCENTAGE;
            }
        }

        // Re-evaluate completion after the update
        hasCompleted = isComplete();
    }

    /**
     * Returns the current progress percentage.
     */
    public float getProgress() {
        return progressPercentage;
    }

    /**
     * Sets the progress percentage directly.
     * The value is clamped to [0, progressPercentageCompletionThreshold].
     *
     * @param percentage the new progress percentage
     */
    public void setProgress(float percentage) {
        // Validate lower bound
        if (percentage < MINIMUM_PROGRESS_PERCENTAGE) {
            this.progressPercentage = MINIMUM_PROGRESS_PERCENTAGE;
        } else if (percentage > progressPercentageCompletionThreshold) {
            // Validate upper bound
            this.progressPercentage = progressPercentageCompletionThreshold;
        } else {
            this.progressPercentage = percentage;
        }

        // Re-evaluate completion whenever progress is set directly
        hasCompleted = isComplete();
    }

    /**
     * Resets the progress percentage to zero and clears the completed flag.
     */
    public void resetProgress() {
        progressPercentage = MINIMUM_PROGRESS_PERCENTAGE;
        hasCompleted = false;
    }

    /**
     * Updates the completion threshold.
     *
     * @param threshold new threshold; must be greater than zero
     */
    public void setCompletionThreshold(float threshold) {
        // Reject non-positive thresholds
        if (threshold > MINIMUM_PROGRESS_PERCENTAGE) {
            this.progressPercentageCompletionThreshold = threshold;
        } else {
            this.progressPercentageCompletionThreshold = 100.0f;
        }

        // Completion status may have changed because the threshold changed
        hasCompleted = isComplete();
    }

    // Standard getters
    public int getProgressID() {
        return progressID;
    }

    public float getProgressPercentageCompletionThreshold() {
        return progressPercentageCompletionThreshold;
    }

    public boolean isHasCompleted() {
        return hasCompleted;
    }
}
