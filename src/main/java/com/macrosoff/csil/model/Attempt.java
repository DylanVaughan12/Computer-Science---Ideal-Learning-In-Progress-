/**
 * File:    Attempt.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Records a user's attempt at a problem, including start and submit timestamps,
 *          the last saved checkpoint, and the final AttemptStatus.
 */
package com.macrosoff.csil.model;

import com.macrosoff.csil.model.enums.AttemptStatus;
import java.util.Date;

public class Attempt {
    private static int nextId = 1;

    private int attemptID;
    private int userID;
    private int problemID;
    private Date dateStarted;
    private Date dateSubmitted;
    private Date lastCheckpoint;
    private String currentWorkSpace;
    private AttemptStatus attemptStatus;

    public Attempt(int userID, int problemID) {
        this.attemptID = nextId++;
        this.userID = userID;
        this.problemID = problemID;
        this.dateStarted = new Date();
        this.currentWorkSpace = "";
        this.attemptStatus = AttemptStatus.IN_PROGRESS;
    }

    public void saveCheckpoint() {
        this.lastCheckpoint = new Date();
    }

    public SubmissionResult submit(String script, Problem problem) {
        this.dateSubmitted = new Date();
        AssessmentService svc = new AssessmentService();
        boolean passed = svc.evaluate(script, problem);
        this.attemptStatus = passed ? AttemptStatus.PASSED : AttemptStatus.FAILED;
        SubmissionResult result = new SubmissionResult(attemptID, passed, 1);
        if (!passed) {
            result.addError(new ErrorDetail(0, "One or more test cases did not produce the expected output."));
        }
        return result;
    }

    public AttemptStatus getAttemptStatus()  { return attemptStatus; }
    public String getWorkspace()             { return currentWorkSpace; }
    public boolean isPassed()                { return attemptStatus == AttemptStatus.PASSED; }
    public boolean isFailed()                { return attemptStatus == AttemptStatus.FAILED; }
    public boolean isInProgress()            { return attemptStatus == AttemptStatus.IN_PROGRESS; }
    public int getAttemptID()                { return attemptID; }
    public int getUserID()                   { return userID; }
    public int getProblemID()                { return problemID; }
    public Date getDateStarted()             { return dateStarted; }
    public Date getDateSubmitted()           { return dateSubmitted; }
    public Date getLastCheckpoint()          { return lastCheckpoint; }

    public void setCurrentWorkSpace(String ws) { this.currentWorkSpace = ws; }

    public static int getPreviousPassedAttempts(java.util.List<Attempt> allAttempts, int userID, int problemID) {
        return (int) allAttempts.stream()
                .filter(a -> a.getUserID() == userID && a.getProblemID() == problemID && a.isPassed())
                .count();
    }
}
