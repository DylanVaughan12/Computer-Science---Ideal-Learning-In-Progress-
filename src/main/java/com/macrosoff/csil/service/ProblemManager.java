/**
 * File:    ProblemManager.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Service class that owns the ordered list of Problem objects and provides
 *          query methods used by ProblemPanel and InstructorPanel.
 */
package com.macrosoff.csil.service;

import com.macrosoff.csil.model.Problem;
import com.macrosoff.csil.model.enums.Category;
import com.macrosoff.csil.model.enums.DifficultyLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProblemManager {
    private List<Problem> problems;

    public ProblemManager() {
        this.problems = new ArrayList<>();
    }

    public List<Problem> getProblemByCategory(Category category) {
        return problems.stream()
                .filter(p -> p.getCategory() == category)
                .collect(Collectors.toList());
    }

    public Problem getProblemByID(int problemID) {
        return problems.stream()
                .filter(p -> p.getProblemID() == problemID)
                .findFirst()
                .orElse(null);
    }

    public List<Problem> getProblemsByDifficulty(DifficultyLevel difficulty) {
        return problems.stream()
                .filter(p -> p.getDifficulty() == difficulty)
                .collect(Collectors.toList());
    }

    public void addProblem(Problem problem) {
        problems.add(problem);
    }

    public void updateDescription(int problemID, String newDescription) {
        Problem p = getProblemByID(problemID);
        if (p != null) p.setDescription(newDescription);
    }

    public List<Problem> getAllProblems() {
        return problems;
    }
}
