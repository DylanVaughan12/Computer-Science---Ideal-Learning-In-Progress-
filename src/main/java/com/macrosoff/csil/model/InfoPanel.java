/**
 * File:    InfoPanel.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 2.0
 * Purpose: Stores the explanatory content shown in LearnPanel and InfoPanelDialog.
 *          Version 2.0 replaces the three skill-level explanation map with a single
 *          explanation string. All other content (code snippets, hints, answer code,
 *          and complexity) remains unchanged.
 */
package com.macrosoff.csil.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * InfoPanel
 * Holds the teaching content attached to a Problem or a BlockCategory.
 * The explanation is a single string — the skill-level distinction has been
 * removed so instructors write one clear explanation for all users.
 */
public class InfoPanel {

    // Minimum acceptable panel identifier value
    private static final int MINIMUM_PANEL_ID = 1;

    // Fallback text shown when no explanation has been set
    private static final String DEFAULT_EXPLANATION = "No explanation provided yet.";

    // Fallback text shown when no hint has been added
    private static final String DEFAULT_HINT = "No hint available for this topic.";

    // Unique identifier for this panel
    private int panelID;

    // The single teaching explanation shown in LearnPanel
    private String explanation;

    // Optional code snippet(s) shown alongside the explanation
    private List<String> codeSnippetList;

    // Optional hint shown when the student clicks the Hint button
    private String hint;

    // The reference answer code revealed after the student passes
    private String answerCode;

    // Big-O time complexity label for the related algorithm
    private String timeComplexity;

    // Big-O space complexity label for the related algorithm
    private String spaceComplexity;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs an InfoPanel with the given identifier.
     *
     * @param panelID unique numeric ID; stored as -1 if below minimum
     */
    public InfoPanel(int panelID) {
        // Validate identifier
        if (panelID >= MINIMUM_PANEL_ID) {
            this.panelID = panelID;
        } else {
            this.panelID = -1;
        }

        // Initialise all fields to safe defaults
        this.explanation     = DEFAULT_EXPLANATION;
        this.codeSnippetList = new ArrayList<>();
        this.hint            = DEFAULT_HINT;
        this.answerCode      = "";
        this.timeComplexity  = "O(1)";
        this.spaceComplexity = "O(1)";
    }

    // ── Mutators ──────────────────────────────────────────────────────────────

    /**
     * Sets the single teaching explanation for this panel.
     *
     * @param explanationText the explanation to display; ignored if null or blank
     */
    public void setExplanation(String explanationText) {
        if (explanationText != null && !explanationText.isBlank()) {
            this.explanation = explanationText;
        } else {
            this.explanation = DEFAULT_EXPLANATION;
        }
    }

    /**
     * Appends a code snippet to the snippet list.
     *
     * @param snippet the code snippet string; ignored if null
     */
    public void addCodeSnippet(String snippet) {
        if (snippet != null) {
            codeSnippetList.add(snippet);
        }
    }

    /**
     * Sets the student-facing hint text.
     *
     * @param hintText the hint to display; ignored if null or blank
     */
    public void setHint(String hintText) {
        if (hintText != null && !hintText.isBlank()) {
            this.hint = hintText;
        } else {
            this.hint = DEFAULT_HINT;
        }
    }

    /**
     * Sets the reference answer code shown after a successful submission.
     *
     * @param answerCode the answer code; stored as empty string if null
     */
    public void setAnswerCode(String answerCode) {
        if (answerCode != null) {
            this.answerCode = answerCode;
        } else {
            this.answerCode = "";
        }
    }

    /**
     * Sets the time complexity label (e.g. "O(n log n)").
     *
     * @param timeComplexity the label; ignored if null or blank
     */
    public void setTimeComplexity(String timeComplexity) {
        if (timeComplexity != null && !timeComplexity.isBlank()) {
            this.timeComplexity = timeComplexity;
        } else {
            this.timeComplexity = "O(1)";
        }
    }

    /**
     * Sets the space complexity label (e.g. "O(n)").
     *
     * @param spaceComplexity the label; ignored if null or blank
     */
    public void setSpaceComplexity(String spaceComplexity) {
        if (spaceComplexity != null && !spaceComplexity.isBlank()) {
            this.spaceComplexity = spaceComplexity;
        } else {
            this.spaceComplexity = "O(1)";
        }
    }

    // ── Legacy compatibility methods ──────────────────────────────────────────
    // These methods accept a SkillLevel parameter but ignore it, storing or
    // returning the single explanation. They exist so DataStore seed code that
    // was written for the old three-level API continues to compile without
    // modification to every call site.

    /**
     * Legacy method: stores the text as the single explanation regardless of level.
     * Kept for backward compatibility with DataStore seed calls.
     *
     * @param level   ignored
     * @param text    the explanation text to store
     */
    public void addExplanation(com.macrosoff.csil.model.enums.SkillLevel level, String text) {
        // Only store if no explanation has been set yet, or if this is HIGHSCHOOL
        // (the first level that was historically called), so the most relevant
        // text wins without needing to change all seed sites.
        if (level == com.macrosoff.csil.model.enums.SkillLevel.HIGHSCHOOL
                || this.explanation.equals(DEFAULT_EXPLANATION)) {
            setExplanation(text);
        }
    }

    /**
     * Legacy method: returns the single explanation regardless of the level argument.
     *
     * @param skillLevel ignored
     * @return the single stored explanation
     */
    public String getExplanation(com.macrosoff.csil.model.enums.SkillLevel skillLevel) {
        return explanation;
    }

    /**
     * Legacy method: stores the hint text regardless of the level argument.
     *
     * @param level    ignored
     * @param hintText the hint to store
     */
    public void addHint(com.macrosoff.csil.model.enums.SkillLevel level, String hintText) {
        setHint(hintText);
    }

    /**
     * Legacy method: returns a list containing the single hint regardless of level.
     *
     * @param skillLevel ignored
     * @return a list with the current hint, or an empty list if no hint is set
     */
    public java.util.List<String> getHints(com.macrosoff.csil.model.enums.SkillLevel skillLevel) {
        java.util.List<String> hintList = new java.util.ArrayList<>();
        if (!hint.equals(DEFAULT_HINT)) {
            hintList.add(hint);
        }
        return hintList;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the single teaching explanation for this panel. */
    public String getExplanation() {
        return explanation;
    }

    /** Returns the student-facing hint text. */
    public String getHint() {
        return hint;
    }

    /** Returns an unmodifiable view of the code snippet list. */
    public List<String> getCodeSnippets() {
        return Collections.unmodifiableList(codeSnippetList);
    }

    public String getAnswerCode()    { return answerCode; }
    public String getTimeComplexity()  { return timeComplexity; }
    public String getSpaceComplexity() { return spaceComplexity; }
    public int    getPanelID()         { return panelID; }
}
