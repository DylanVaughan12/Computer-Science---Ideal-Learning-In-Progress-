/**
 * File:    SaveManager.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 2.0
 * Purpose: Handles persistence of user accounts, solved-problem sets, and
 *          instructor-created custom problems to disk between sessions.
 *          Passwords are stored as SHA-256 hashes. Custom problems are
 *          serialised to ~/.csil/custom_problems.properties.
 *          Version 2.0 adds saveCustomProblems() and loadCustomProblems().
 *          
 * Changes:
 * 
 */
package com.macrosoff.csil.data;

import com.macrosoff.csil.model.InfoPanel;
import com.macrosoff.csil.model.Problem;
import com.macrosoff.csil.model.TestCase;
import com.macrosoff.csil.model.User;
import com.macrosoff.csil.model.enums.Category;
import com.macrosoff.csil.model.enums.DifficultyLevel;
import com.macrosoff.csil.model.enums.UserType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * SaveManager
 * All file I/O is centralised here. Two save files are used:
 *
 *   ~/.csil/userdata.properties      — user accounts and solved sets
 *   ~/.csil/custom_problems.properties — instructor-created problems
 *
 * Both files are plain Java Properties files so they are human-readable
 * and easy to debug.
 */
public class SaveManager {

    // Directory for all save files
    private static final String SAVE_DIRECTORY = System.getProperty("user.home")
            + File.separator + ".csil";

    // File names within the save directory
    private static final String USER_DATA_FILE    = SAVE_DIRECTORY + File.separator + "userdata.properties";
    private static final String CUSTOM_PROB_FILE  = SAVE_DIRECTORY + File.separator + "custom_problems.properties";

    // ── Password policy ───────────────────────────────────────────────────────

    /**
     * Validates a plain-text password against the application's minimum rules.
     *
     * @param password the password to check
     * @return null when the password is valid, or an error message string when invalid
     */
    public static String validatePassword(String password) {
    	boolean hasSpecialCharacter = false;
    	
        // Rule 1: minimum length of 7 characters
        if (password == null || password.length() < 7) {
            return "Password must be at least 7 characters.";
        }

        // Rule 2: must contain at least one non-alphanumeric (special) character
        for (char character : password.toCharArray()) {
            if (!Character.isLetterOrDigit(character)) {
                hasSpecialCharacter = true;
                break;
            }
        }

        if (!hasSpecialCharacter) {
            return "Password must contain at least one special character (e.g. @, !, #).";
        }

        // Password passed all rules
        return null;
    }

    /**
     * Returns a SHA-256 hex digest of the given plain-text string.
     * Falls back to returning the plain text if the JVM lacks SHA-256 (never in practice).
     *
     * @param plaintext the text to hash
     * @return a 64-character lowercase hex string
     */
    public static String hashPassword(String plaintext) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = messageDigest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexBuilder = new StringBuilder(64);
            for (byte digestByte : digestBytes) {
                hexBuilder.append(String.format("%02x", digestByte));
            }
            return hexBuilder.toString();
        } catch (Exception exception) {
            // Fallback — should never be reached on a standard JVM
            return plaintext;
        }
    }

    // ── User data: load ───────────────────────────────────────────────────────

    /**
     * Loads all saved user records from disk.
     * Returns an empty list on the first run (no save file exists yet).
     *
     * @return a list of UserRecord DTOs, one per saved account
     */
    public static List<UserRecord> loadUsers() {
        List<UserRecord> recordList = new ArrayList<>();
        File saveFile = new File(USER_DATA_FILE);
        String passwordHash = null;
        String typeString = null;
        String solvedString = null;
        String joinedString = null;

        if (!saveFile.exists()) {
            return recordList;
        }

        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(saveFile)) {
            properties.load(inputStream);
        } catch (IOException ioException) {
            System.err.println("CSIL: could not read user save file — " + ioException.getMessage());
            return recordList;
        }

        String rawUserList = properties.getProperty("users", "").trim();
        if (rawUserList.isEmpty()) {
            return recordList;
        }

        for (String username : rawUserList.split(",")) {
            username = username.trim();
            if (username.isEmpty()) {
                continue;
            }

            passwordHash = properties.getProperty("user." + username + ".password", "");
            typeString   = properties.getProperty("user." + username + ".type",     "STUDENT");
            solvedString = properties.getProperty("user." + username + ".solved",   "");
            joinedString = properties.getProperty("user." + username + ".joined",   "");

            UserType accountType;
            try {
                accountType = UserType.valueOf(typeString);
            } catch (Exception parseException) {
                accountType = UserType.STUDENT;
            }

            Set<Integer> solvedProblemIds = new HashSet<>();
            if (!solvedString.isEmpty()) {
                for (String idToken : solvedString.split(",")) {
                    try {
                        solvedProblemIds.add(Integer.parseInt(idToken.trim()));
                    } catch (NumberFormatException ignored) {
                        // Skip malformed tokens
                    }
                }
            }

            long joinedTimestamp = 0;
            try {
                joinedTimestamp = Long.parseLong(joinedString);
            } catch (Exception ignored) {
                // Leave as 0; User.fromHash() will substitute the current time
            }

            recordList.add(new UserRecord(username, passwordHash, accountType,
                    solvedProblemIds, joinedTimestamp));
        }

        return recordList;
    }

    // ── User data: save ───────────────────────────────────────────────────────

    /**
     * Persists all user accounts and their solved-problem sets to disk.
     *
     * @param userList  the live list of User objects
     * @param solvedMap username → set of solved problem IDs
     */
    public static void saveUsers(List<User> userList, Map<String, Set<Integer>> solvedMap) {
        try {
            Files.createDirectories(Paths.get(SAVE_DIRECTORY));
        } catch (IOException ioException) {
            System.err.println("CSIL: could not create save directory — " + ioException.getMessage());
            return;
        }

        Properties properties = new Properties();
        StringJoiner usernameJoiner = new StringJoiner(",");

        for (User user : userList) {
            String username = user.getUserName();
            usernameJoiner.add(username);
            properties.setProperty("user." + username + ".password", user.getPasswordHash());
            properties.setProperty("user." + username + ".type",     user.getUserType().name());
            properties.setProperty("user." + username + ".joined",
                    String.valueOf(user.getDateJoined().getTime()));

            Set<Integer> solvedSet = solvedMap.getOrDefault(username, Collections.emptySet());
            if (solvedSet.isEmpty()) {
                properties.setProperty("user." + username + ".solved", "");
            } else {
                StringJoiner solvedJoiner = new StringJoiner(",");
                for (Integer problemId : solvedSet) {
                    solvedJoiner.add(problemId.toString());
                }
                properties.setProperty("user." + username + ".solved", solvedJoiner.toString());
            }
        }

        properties.setProperty("users", usernameJoiner.toString());

        try (FileOutputStream outputStream = new FileOutputStream(USER_DATA_FILE)) {
            properties.store(outputStream, "CS-IL User Data — do not edit manually");
        } catch (IOException ioException) {
            System.err.println("CSIL: could not write user save file — " + ioException.getMessage());
        }
    }

    // ── Custom problems: save ─────────────────────────────────────────────────

    /**
     * Persists all instructor-created custom problems to disk.
     * Each problem's ID, title, description, difficulty, category, explanation,
     * hint, answer code, and up to MAX_TEST_CASES test cases are stored.
     *
     * @param customProblems the list of Problem objects to save
     */
    public static void saveCustomProblems(List<Problem> customProblems) {
        try {
            Files.createDirectories(Paths.get(SAVE_DIRECTORY));
        } catch (IOException ioException) {
            System.err.println("CSIL: could not create save directory — " + ioException.getMessage());
            return;
        }

        Properties properties = new Properties();
        StringJoiner idJoiner = new StringJoiner(",");

        for (Problem problem : customProblems) {
            String prefix = "problem." + problem.getProblemID();
            idJoiner.add(String.valueOf(problem.getProblemID()));

            properties.setProperty(prefix + ".title",       encode(problem.getTitle()));
            properties.setProperty(prefix + ".description", encode(problem.getDescription()));
            properties.setProperty(prefix + ".difficulty",  problem.getDifficulty().name());
            properties.setProperty(prefix + ".category",    problem.getCategory().name());

            if (problem.getInfoPanel() != null) {
                InfoPanel infoPanel = problem.getInfoPanel();
                properties.setProperty(prefix + ".explanation", encode(infoPanel.getExplanation()));
                properties.setProperty(prefix + ".hint",        encode(infoPanel.getHint()));
                properties.setProperty(prefix + ".answerCode",  encode(infoPanel.getAnswerCode()));
            }

            // Save each test case
            List<TestCase> testCases = problem.getTestCases();
            properties.setProperty(prefix + ".testCaseCount", String.valueOf(testCases.size()));
            for (int index = 0; index < testCases.size(); index++) {
                TestCase testCase = testCases.get(index);
                String tcPrefix = prefix + ".tc." + index;
                properties.setProperty(tcPrefix + ".input",    encode(testCase.getInputData()));
                properties.setProperty(tcPrefix + ".expected", encode(testCase.getExpectedOutput()));
                properties.setProperty(tcPrefix + ".hidden",   String.valueOf(testCase.isHidden()));
            }
        }

        properties.setProperty("ids", idJoiner.toString());

        try (FileOutputStream outputStream = new FileOutputStream(CUSTOM_PROB_FILE)) {
            properties.store(outputStream, "CS-IL Custom Problems — do not edit manually");
        } catch (IOException ioException) {
            System.err.println("CSIL: could not write custom problems file — " + ioException.getMessage());
        }
    }

    // ── Custom problems: load ─────────────────────────────────────────────────

    /**
     * Loads all previously saved custom problems from disk.
     * Returns an empty list if the file does not exist yet.
     *
     * @return a list of reconstructed Problem objects
     */
    public static List<Problem> loadCustomProblems() {
        List<Problem> loadedProblems = new ArrayList<>();
        File saveFile = new File(CUSTOM_PROB_FILE);

        if (!saveFile.exists()) {
            return loadedProblems;
        }

        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(saveFile)) {
            properties.load(inputStream);
        } catch (IOException ioException) {
            System.err.println("CSIL: could not read custom problems file — " + ioException.getMessage());
            return loadedProblems;
        }

        String rawIds = properties.getProperty("ids", "").trim();
        if (rawIds.isEmpty()) {
            return loadedProblems;
        }

        for (String idToken : rawIds.split(",")) {
            idToken = idToken.trim();
            if (idToken.isEmpty()) {
                continue;
            }

            int problemId;
            try {
                problemId = Integer.parseInt(idToken);
            } catch (NumberFormatException ignored) {
                continue;
            }

            String prefix = "problem." + problemId;

            String title       = decode(properties.getProperty(prefix + ".title",       "Untitled"));
            String description = decode(properties.getProperty(prefix + ".description", ""));
            String diffString  = properties.getProperty(prefix + ".difficulty", "BEGINNER");
            String catString   = properties.getProperty(prefix + ".category",   "PROBLEM_SOLVING");

            DifficultyLevel difficulty;
            try {
                difficulty = DifficultyLevel.valueOf(diffString);
            } catch (Exception ex) {
                difficulty = DifficultyLevel.BEGINNER;
            }

            Category category;
            try {
                category = Category.valueOf(catString);
            } catch (Exception ex) {
                category = Category.PROBLEM_SOLVING;
            }

            // Rebuild the InfoPanel
            InfoPanel infoPanel = new InfoPanel(problemId);
            infoPanel.setExplanation(decode(properties.getProperty(prefix + ".explanation", "")));
            infoPanel.setHint(decode(properties.getProperty(prefix + ".hint", "")));
            infoPanel.setAnswerCode(decode(properties.getProperty(prefix + ".answerCode", "")));

            Problem restoredProblem = new Problem(problemId, title, description,
                    difficulty, category, infoPanel);

            // Restore test cases
            int testCaseCount = 0;
            try {
                testCaseCount = Integer.parseInt(
                        properties.getProperty(prefix + ".testCaseCount", "0"));
            } catch (NumberFormatException ignored) {
                // Leave count as 0
            }

            for (int index = 0; index < testCaseCount; index++) {
                String tcPrefix = prefix + ".tc." + index;
                String input    = decode(properties.getProperty(tcPrefix + ".input",    ""));
                String expected = decode(properties.getProperty(tcPrefix + ".expected", ""));
                boolean hidden  = Boolean.parseBoolean(
                        properties.getProperty(tcPrefix + ".hidden", "false"));

                int testCaseId = problemId * 100 + index + 1;
                restoredProblem.addTestCase(new TestCase(testCaseId, problemId,
                        input, expected, hidden));
            }

            loadedProblems.add(restoredProblem);
        }

        return loadedProblems;
    }

    // ── Encoding helpers ──────────────────────────────────────────────────────

    /**
     * Encodes a string for safe storage in a Properties file by replacing
     * newlines and backslashes so they survive the round-trip.
     *
     * @param text the raw string to encode
     * @return an encoded string with no literal newlines
     */
    private static String encode(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Reverses the encoding applied by encode().
     *
     * @param encoded the encoded string from disk
     * @return the original plain-text string
     */
    private static String decode(String encoded) {
        if (encoded == null) {
            return "";
        }
        return encoded.replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    /**
     * Immutable data transfer object loaded from the user save file.
     * The password field already contains the SHA-256 hash.
     */
    public static class UserRecord {
        public final String      username;
        public final String      passwordHash;
        public final UserType    type;
        public final Set<Integer> solved;
        public final long        joinedMillis;

        UserRecord(String username, String passwordHash, UserType type,
                   Set<Integer> solved, long joinedMillis) {
            this.username     = username;
            this.passwordHash = passwordHash;
            this.type         = type;
            this.solved       = Collections.unmodifiableSet(solved);
            this.joinedMillis = joinedMillis;
        }
    }
}
