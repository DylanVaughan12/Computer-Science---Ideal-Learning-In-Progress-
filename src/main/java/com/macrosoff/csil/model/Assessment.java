/**
 * File:    Assessment.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Orchestrates the grading of a single submission attempt by delegating
 *          execution to AssessmentService and packaging the result.
 */
package com.macrosoff.csil.model;

public class Assessment {
    private static int nextId = 1;

    private int assessmentID;
    private int problemID;
    private float grade;
    private float passingThreshold;
    private String submittedCode;

    public Assessment() {
        this.assessmentID = nextId++;
        this.grade = 0f;
        this.passingThreshold = 60f;
        this.submittedCode = "";
    }

    public Assessment(String code, float passingThreshold) {
        this();
        this.submittedCode = code;
        this.passingThreshold = passingThreshold;
    }

    public AssessmentResult codeAssessment(String code, int problemId, Problem problem) {
        this.submittedCode = code;
        this.problemID = problemId;
        AssessmentService svc = new AssessmentService();
        boolean passed = svc.evaluate(code, problem);
        this.grade = passed ? 100f : calculatePartialGrade(code, problem);
        boolean hasPassed = grade >= passingThreshold;
        return new AssessmentResult(assessmentID, grade, hasPassed, passingThreshold);
    }

    private float calculatePartialGrade(String code, Problem problem) {
        String normCode = code.toLowerCase().replaceAll("\\s+", "");
        String answer = problem.getInfoPanel().getAnswerCode();
        if (answer == null || answer.isEmpty()) return 0f;
        String normAnswer = answer.toLowerCase().replaceAll("\\s+", "");
        String[] tokens = normAnswer.split("[^a-z0-9]+");
        int matches = 0;
        for (String t : tokens) {
            if (t.length() > 2 && normCode.contains(t)) matches++;
        }
        return tokens.length == 0 ? 0f : (matches / (float) tokens.length) * 100f;
    }

    public void setGrade()                   { /* set by codeAssessment */ }
    public void setPassingThreshold()        { /* set by constructor */ }
    public boolean resetGrade()              { this.grade = 0; return true; }
    public boolean resetPassingThreshold()   { this.passingThreshold = 60f; return true; }

    public int getAssessmentID()   { return assessmentID; }
    public int getProblemID()      { return problemID; }
    public float getGrade()        { return grade; }
    public float getPassingThreshold() { return passingThreshold; }
    public String getSubmittedCode()   { return submittedCode; }
}
