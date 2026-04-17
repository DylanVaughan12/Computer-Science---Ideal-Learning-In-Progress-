/**
 * File:    DataStore.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Singleton data store that owns all runtime state: users, problems, blocks,
 *          and attempts. Loads and saves user accounts via SaveManager and
 *          supports instructor problem management features.
 *          
 * Changes: 
 * James Dylan Vaughan 4/13/2026
 * Renamed Variables to improve readability.
 * 
 * James Dylan Vaughan 4/14/2026
 * Moved all variable declarations to the top of the class/methods to follow best practices.
 * 
 * James Dylan Vaughan 4/16/2026
 * renamed some functions to follow best practices, added more comments.
 * 
 * Approval By:
 * 
 */
package com.macrosoff.csil.data;

import com.macrosoff.csil.model.*;
import com.macrosoff.csil.model.enums.*;
import com.macrosoff.csil.service.ProblemManager;

import java.util.*;

public class DataStore {

    private static DataStore instance;

    private ProblemManager       problemManager;
    private List<BlockCategory>  allBlocks;
    private List<User>           users;
    private List<Attempt>        attempts;

    /** username → set of solved problem IDs, kept in sync with disk */
    private final Map<String, Set<Integer>> solvedMap = new HashMap<>();
    
    /**
     * Set of problem IDs that were authored by instructors via CreateProblemDialog.
     * Used by InstructorPanel to decide which problems can be edited or deleted.
     */
    private final Set<Integer> customProblemIdSet = new HashSet<>();
    
    
    /**
     * Set of problem IDs that instructors have hidden from the student view.
     * Hidden problems are still shown in the InstructorPanel but not in the
     * student sidebar or the ProgressPanel category breakdown.
     *
     * TODO (team): Persist this set to disk in userdata.properties so hidden
     *   state survives application restarts.
     */
    private final Set<Integer> hiddenProblems = new HashSet<>();

    private DataStore() {
        problemManager = new ProblemManager();
        allBlocks      = new ArrayList<>();
        users          = new ArrayList<>();
        attempts       = new ArrayList<>();
        loadOrSeedUsers();
        seedBlocks();
        seedProblems();       // built-in problems are always seeded from code
        loadCustomProblems(); // instructor-created problems are loaded from disk
    }

    public static DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    // ─── Users ───────────────────────────────────────────────────────────────

    /** Load users from disk; fall back to default accounts if no save file exists. */
    private void loadOrSeedUsers() {
    	User user;
        List<SaveManager.UserRecord> records = SaveManager.loadUsers();
        if (records.isEmpty()) {
            // First run — create default accounts with compliant passwords
            addNewUser("demo",  "Demo@1234",  UserType.STUDENT);
            addNewUser("admin", "Admin@1234", UserType.INSTRUCTOR);
            persist();
        } else {
            for (SaveManager.UserRecord r : records) {
                user = User.fromHash(r.username, r.passwordHash, r.type, r.joinedMillis);
                users.add(user);
                solvedMap.put(r.username, new HashSet<>(r.solved));
            }
        }
    }

    /** Internal — adds a user with a plain-text password (hashes it). */
    private User addNewUser(String userName, String plainPassword, UserType type) {
        User user = new User(userName, plainPassword, type);
        users.add(user);
        solvedMap.put(userName, new HashSet<>());
        return user;
    }

    public User findUser(String userName, String plainPassword) {
        return users.stream()
                .filter(user -> user.login(userName, plainPassword))
                .findFirst().orElse(null);
    }

    public boolean userExists(String userName) {
        return users.stream().anyMatch(user -> user.getUserName().equalsIgnoreCase(userName));
    }

    /**
     * Register a new user.
     * @return null on success, or an error message string on failure.
     */
    public String registerUser(String userName, String plainPassword, UserType type) {
    	String passwordErrorMessage = null;
        // Capacity check
        if (users.size() >= 10) return "Profile limit reached (10 max).";
        // Uniqueness
        if (userExists(userName))  return "Username already taken.";
        // Password policy
        passwordErrorMessage  = SaveManager.validatePassword(plainPassword);
        if (passwordErrorMessage  != null) return passwordErrorMessage;

        addNewUser(userName, plainPassword, type);
        persist();
        return null; // success
    }

    /** Call after any change that should be written to disk. */
    public void persist() {
        SaveManager.saveUsers(users, solvedMap);
    }

    // ── Solved-problem tracking ───────────────────────────────────────────────

    public Set<Integer> getSolved(String userName) {
        return solvedMap.computeIfAbsent(userName, k -> new HashSet<>());
    }

    public void markSolved(String userName, int problemId) {
        getSolved(userName).add(problemId);
        persist();
    }

    public List<User> getUsers() { return users; }

    // ─── Blocks ──────────────────────────────────────────────────────────────

    private void seedBlocks() {
        int id = 1;

        // ── VARIABLE — single unified block (type chosen via dialog dropdown) ──
        allBlocks.add(makeBlock(id++, BlockType.VARIABLE, "variable",
                "int variableName = 0;", "[var]",
                "A variable stores a value that your program can use and change. Choose the type — int for whole numbers, double for decimals, String for text — then give it a name and a starting value.",
                "int count = 0;\ndouble avg = 0.0;\nString name = \"Alice\";", "O(1)", "O(1)"));

        // ── ARRAY ────────────────────────────────────────────────────────────
        allBlocks.add(makeBlock(id++, BlockType.ARRAY, "declare array",
                "int[] arrayName = new int[10];", "[arr]",
                "An array is an ordered list of values of the same type. Declare it with a type, a name, and a size — for example, int[] scores = new int[5] creates space for 5 integers.",
                "int[] nums = new int[5];\nString[] words = new String[3];", "O(n)", "O(n)"));

        allBlocks.add(makeBlock(id++, BlockType.ARRAY, "access element",
                "arrayName[index]", "[a[i]]",
                "Access one item from your array by writing its name followed by the position number (index) in square brackets. Indexes start at 0, so the first item is at index 0.",
                "int x = nums[0];\nnums[2] = 42;", "O(1)", "O(1)"));

        // ── IF / ELSE ─────────────────────────────────────────────────────────
        allBlocks.add(makeBlock(id++, BlockType.IF, "if statement",
                "if (condition) {", "[if]",
                "An if statement runs a block of code only when a condition is true. Write the condition inside the parentheses — for example, if (score > 0) — and the code to run inside the curly braces.",
                "if (x > 0) {\n    // positive branch\n}", "O(1)", "O(1)"));

        allBlocks.add(makeBlock(id++, BlockType.ELSE, "else clause",
                "} else {", "[els]",
                "The else clause runs when the if condition is false. It always follows an if block and gives your program an alternative path to take.",
                "if (x > 0) {\n    ...\n} else {\n    ...\n}", "O(1)", "O(1)"));

        allBlocks.add(makeBlock(id++, BlockType.IF, "close brace",
                "}", "[}]",
                "A closing brace marks the end of a block — whether that is an if, else, loop, or method. Every opening brace { must have a matching closing brace }.",
                "if (cond) {\n    ...\n}  // closing brace", "O(1)", "O(1)"));

        // ── LOOPS ─────────────────────────────────────────────────────────────
        allBlocks.add(makeBlock(id++, BlockType.FOR_LOOP, "for loop",
                "for (int i = 0; i < n; i++) {", "[for]",
                "A for loop repeats a block of code a set number of times. It has three parts: a starting value, a condition to keep going, and a step that runs after each repetition.",
                "for (int i = 0; i < 10; i++) {\n    System.out.println(i);\n}", "O(n)", "O(1)"));

        allBlocks.add(makeBlock(id++, BlockType.WHILE_LOOP, "while loop",
                "while (condition) {", "[whl]",
                "A while loop keeps repeating as long as its condition stays true. Use it when you do not know in advance how many times the loop needs to run.",
                "while (x > 0) {\n    x--;\n}", "O(n)", "O(1)"));

        // ── FUNCTIONS ─────────────────────────────────────────────────────────
        allBlocks.add(makeBlock(id++, BlockType.FUNCTION, "define method",
                "public static void methodName() {", "[def]",
                "A method is a reusable block of code that performs a task. Define it once and call it as many times as you need. Give it a clear name that describes what it does.",
                "public int add(int a, int b) {\n    return a + b;\n}", "O(1)", "O(n) stack"));

        allBlocks.add(makeBlock(id++, BlockType.FUNCTION, "return value",
                "return value;", "[ret]",
                "The return statement ends a method and sends a value back to whoever called it. The type of the returned value must match the return type in the method definition.",
                "return result;\nreturn a + b;", "O(1)", "O(1)"));

        allBlocks.add(makeBlock(id++, BlockType.FUNCTION, "System.out.println",
                "System.out.println(value);", "[prt]",
                "System.out.println prints a value to the console followed by a new line. It is the most common way to display output and check what your program is doing.",
                "System.out.println(\"Hello!\");\nSystem.out.println(x);", "O(n)", "O(1)"));
    }

    private BlockCategory makeBlock(int id, BlockType type, String label, String template,
                                    String icon, String explanation,
                                    String snippet, String time, String space) {
        InfoPanel infoPanel = new InfoPanel(id);
        infoPanel.setExplanation(explanation);
        infoPanel.addCodeSnippet(snippet);
        infoPanel.setTimeComplexity(time);
        infoPanel.setSpaceComplexity(space);
        return new BlockCategory(id, type, label, template, icon, infoPanel);
    }

    public List<BlockCategory> getAllBlocks() { return allBlocks; }

    // ─── Problems ─────────────────────────────────────────────────────────────
    // Sorted: BEGINNER → INTERMEDIATE → ADVANCED, then by ID within each tier.
    // The list and implementation of the coding problems.
    private void seedProblems() {

        // ── 1 Hello World — BEGINNER ─────────────────────────────────────────
        InfoPanel HelloWorldInfoPanel = new InfoPanel(101);
        HelloWorldInfoPanel.setExplanation(
                "Every programmer in history has started here: printing \"Hello, World!\" to the screen.\n\n"
                + "What is printing? When a program \"prints\" something it means it sends text to the console — "
                + "the black window where programs display their output. You cannot see this text on a website or in "
                + "a document; it lives in the program's own output area.\n\n"
                + "In Java, the instruction to print a line of text is:\n"
                + "    System.out.println(\"Hello, World!\")\n\n"
                + "Break that down piece by piece:\n"
                + "  System   — Java's built-in toolbox for talking to the computer's system.\n"
                + "  out      — the part of that toolbox that handles output (sending text out).\n"
                + "  println  — short for \"print line\"; it prints your text and then moves to the next line.\n"
                + "  The text you want to print goes inside double quotes inside the parentheses.\n\n"
                + "Try it: click the 'System.out.println' block, type Hello, World! (with the quotes) and press Submit."
        );
        HelloWorldInfoPanel.addCodeSnippet("System.out.println(\"Hello, World!\");");
        HelloWorldInfoPanel.setAnswerCode("public static void main(String[] args) {\n    System.out.println(\"Hello, World!\");\n}");
        HelloWorldInfoPanel.setTimeComplexity("O(1)"); HelloWorldInfoPanel.setSpaceComplexity("O(1)");
        HelloWorldInfoPanel.addHint(SkillLevel.HIGHSCHOOL, "Use the println block and type: \"Hello, World!\"");
        Problem HelloWorldProblem = new Problem(1, "Hello World", "Write a program that prints \"Hello, World!\" to the console.",
                DifficultyLevel.BEGINNER, Category.HELLO_WORLD, HelloWorldInfoPanel);
        HelloWorldProblem.addTestCase(new TestCase(1, 1, "", "Hello, World!", false));
        problemManager.addProblem(HelloWorldProblem);

        // ── 2 Sum of Two Numbers — BEGINNER ──────────────────────────────────
        InfoPanel SumOfTwoNumbersInfoPanel = new InfoPanel(102);
        SumOfTwoNumbersInfoPanel.setExplanation(
                "A method (also called a function) is a named block of instructions you can run whenever you need it.\n\n"
                + "Think of it like a recipe. A recipe called \"makeToast\" would say: put bread in toaster, wait, "
                + "take bread out. You write the recipe once but you can follow it any time you want toast.\n\n"
                + "A method that adds two numbers looks like this:\n"
                + "    public static int sum(int a, int b) {\n"
                + "        int result = a + b;\n"
                + "        return result;\n"
                + "    }\n\n"
                + "Breaking it down:\n"
                + "  public static — these words let the method be called from anywhere in the program.\n"
                + "  int           — the type of value this method gives back (an integer, meaning a whole number).\n"
                + "  sum           — the name we chose for this method.\n"
                + "  (int a, int b)— the two ingredients the method needs; we give them names a and b.\n"
                + "  int result = a + b; — creates a new variable called result and stores the sum in it.\n"
                + "  return result; — hands the answer back to whoever called the method.\n\n"
                + "Variables are named containers for data. int result means \"I want a container that holds "
                + "a whole number, and I will call it result\"."
        );
        SumOfTwoNumbersInfoPanel.addCodeSnippet("public static int sum(int a, int b) {\n    return a + b;\n}");
        SumOfTwoNumbersInfoPanel.setAnswerCode("public static int sum(int a, int b) {\n    int result = a + b;\n    return result;\n}");
        SumOfTwoNumbersInfoPanel.setTimeComplexity("O(1)"); SumOfTwoNumbersInfoPanel.setSpaceComplexity("O(1)");
        SumOfTwoNumbersInfoPanel.addHint(SkillLevel.HIGHSCHOOL, "Declare an int variable to store the result, then return it.");
        Problem SumOfNumbersProblem = new Problem(2, "Sum of Two Numbers", "Create a method that takes two integers and returns their sum.",
                DifficultyLevel.BEGINNER, Category.CONTROL_FLOW, SumOfTwoNumbersInfoPanel);
        SumOfNumbersProblem.addTestCase(new TestCase(2, 2, "2 3",   "5",  false));
        SumOfNumbersProblem.addTestCase(new TestCase(3, 2, "10 20", "30", false));
        SumOfNumbersProblem.addTestCase(new TestCase(4, 2, "-5 5",  "0",  true));
        problemManager.addProblem(SumOfNumbersProblem);

        // ── 3 FizzBuzz — BEGINNER ─────────────────────────────────────────────
        InfoPanel FizzBuzzInfoPanel = new InfoPanel(103);
        FizzBuzzInfoPanel.setExplanation(
                "FizzBuzz is a classic beginner exercise that tests three fundamental ideas: loops, conditions, and modulo.\n\n"
                + "LOOPS — doing something repeatedly.\n"
                + "A for loop repeats a block of code a certain number of times. This one counts from 1 to 15:\n"
                + "    for (int i = 1; i <= 15; i++) { ... }\n"
                + "  int i = 1   means start counting from 1.\n"
                + "  i <= 15     means keep going while i is 15 or less.\n"
                + "  i++         means add 1 to i after each repetition.\n\n"
                + "CONDITIONS — making decisions.\n"
                + "An if/else statement runs different code depending on whether something is true or false:\n"
                + "    if (i % 15 == 0) { print \"FizzBuzz\" }\n"
                + "    else if (i % 3 == 0) { print \"Fizz\" }\n"
                + "    else if (i % 5 == 0) { print \"Buzz\" }\n"
                + "    else { print the number itself }\n\n"
                + "MODULO — the remainder of division.\n"
                + "The % symbol gives you the remainder when one number is divided by another.\n"
                + "  10 % 3 = 1  (10 divided by 3 is 3 remainder 1)\n"
                + "  15 % 3 = 0  (15 divided by 3 is exactly 5, no remainder)\n"
                + "When the remainder is 0, the number divides evenly — that is how we know it is a multiple.\n\n"
                + "IMPORTANT: always check % 15 FIRST. If you check % 3 first, you will print \"Fizz\" for 15 "
                + "instead of \"FizzBuzz\" because 15 is also divisible by 3."
        );
        FizzBuzzInfoPanel.addCodeSnippet("for (int i = 1; i <= 15; i++) {\n    if (i % 15 == 0) println(\"FizzBuzz\");\n    else if (i % 3 == 0) println(\"Fizz\");\n    else if (i % 5 == 0) println(\"Buzz\");\n    else println(i);\n}");
        FizzBuzzInfoPanel.setAnswerCode("for (int i = 1; i <= 15; i++) {\n    if (i % 15 == 0) {\n        System.out.println(\"FizzBuzz\");\n    } else if (i % 3 == 0) {\n        System.out.println(\"Fizz\");\n    } else if (i % 5 == 0) {\n        System.out.println(\"Buzz\");\n    } else {\n        System.out.println(i);\n    }\n}");
        FizzBuzzInfoPanel.setTimeComplexity("O(n)"); FizzBuzzInfoPanel.setSpaceComplexity("O(1)");
        FizzBuzzInfoPanel.addHint(SkillLevel.HIGHSCHOOL, "Use a for loop and check i%15 FIRST, before i%3 or i%5.");
        Problem FizzBuzzProblem = new Problem(3, "FizzBuzz",
                "Print numbers 1–15. For multiples of 3 print \"Fizz\", for multiples of 5 print \"Buzz\", and for multiples of both print \"FizzBuzz\".",
                DifficultyLevel.BEGINNER, Category.CONTROL_FLOW, FizzBuzzInfoPanel);
        FizzBuzzProblem.addTestCase(new TestCase(5, 3, "", "1\n2\nFizz\n4\nBuzz\nFizz\n7\n8\nFizz\nBuzz\n11\nFizz\n13\n14\nFizzBuzz", false));
        problemManager.addProblem(FizzBuzzProblem);

        // ── 4 Validate Employee ID — BEGINNER ─────────────────────────────────
        InfoPanel ValidateEmployeeIdInfoPanel = new InfoPanel(104);
        ValidateEmployeeIdInfoPanel.setExplanation(
                "Validation is the process of checking that input meets the rules before your program uses it. "
                + "Almost every real program does validation — think about a website that tells you your password "
                + "is too short, or a phone that rejects a badly formatted phone number.\n\n"
                + "This problem validates that an employee ID is exactly 6 digits long and contains only the "
                + "characters 0 through 9.\n\n"
                + "How to solve it step by step:\n\n"
                + "Step 1: Check the length.\n"
                + "    if (id.length() != 6) return false;\n"
                + "  id.length() tells you how many characters are in the string.\n"
                + "  != means \"not equal to\". If the length is not 6, immediately return false (invalid).\n\n"
                + "Step 2: Check every character.\n"
                + "    for (char c : id.toCharArray()) {\n"
                + "        if (!Character.isDigit(c)) return false;\n"
                + "    }\n"
                + "  id.toCharArray() breaks the string into individual characters.\n"
                + "  char c : ...  means: go through each character one at a time, calling it c.\n"
                + "  Character.isDigit(c) is true when c is 0–9, false for letters or symbols.\n"
                + "  ! means NOT, so !Character.isDigit(c) is true when c is NOT a digit.\n\n"
                + "Step 3: If both checks pass, the ID is valid.\n"
                + "    return true;\n\n"
                + "This two-step pattern — check length, then check each character — is one of the most "
                + "common validation patterns you will ever write."
        );
        ValidateEmployeeIdInfoPanel.addCodeSnippet("if (id.length() != 6) return false;\nfor (char c : id.toCharArray()) {\n    if (!Character.isDigit(c)) return false;\n}\nreturn true;");
        ValidateEmployeeIdInfoPanel.setAnswerCode("if (id.length() != 6) return false;\nfor (char c : id.toCharArray()) {\n    if (!Character.isDigit(c)) return false;\n}\nreturn true;");
        ValidateEmployeeIdInfoPanel.setTimeComplexity("O(n)"); ValidateEmployeeIdInfoPanel.setSpaceComplexity("O(1)");
        ValidateEmployeeIdInfoPanel.addHint(SkillLevel.HIGHSCHOOL, "Check the length first, then loop and make sure every character is a digit.");
        Problem ValidateEmployeeIdProblem = new Problem(4, "Validate Employee ID",
                "A valid employee ID is exactly 6 digits. Write a method that returns true if the ID is valid, false otherwise.",
                DifficultyLevel.BEGINNER, Category.WORKFORCE_SCENARIOS, ValidateEmployeeIdInfoPanel);
        ValidateEmployeeIdProblem.addTestCase(new TestCase(14, 4, "123456", "true",  false));
        ValidateEmployeeIdProblem.addTestCase(new TestCase(15, 4, "12AB56", "false", false));
        ValidateEmployeeIdProblem.addTestCase(new TestCase(16, 4, "12345",  "false", true));
        problemManager.addProblem(ValidateEmployeeIdProblem);

        // ── 5 Reverse Array — INTERMEDIATE ───────────────────────────────────
        InfoPanel ReverseArrayInfoPanel = new InfoPanel(105);
        ReverseArrayInfoPanel.setExplanation(
                "An array is an ordered list of values. Each value sits at a numbered position called an index, "
                + "and indexes always start at 0, not 1. So the first item is at index 0, the second at index 1, "
                + "and so on.\n\n"
                + "To reverse the array [1, 2, 3, 4, 5] in-place (without creating a second array) we use "
                + "a technique called the two-pointer approach:\n\n"
                + "  Start with one pointer at the far left (index 0) and another at the far right (last index).\n"
                + "  Swap the values at those two positions.\n"
                + "  Move the left pointer one step right and the right pointer one step left.\n"
                + "  Repeat until the pointers meet in the middle.\n\n"
                + "In code:\n"
                + "    int left  = 0;\n"
                + "    int right = arr.length - 1;\n"
                + "    while (left < right) {\n"
                + "        int temp     = arr[left];   // save the left value\n"
                + "        arr[left]    = arr[right];  // overwrite left with right\n"
                + "        arr[right]   = temp;        // put the saved value in right\n"
                + "        left++;                     // move left pointer inward\n"
                + "        right--;                    // move right pointer inward\n"
                + "    }\n\n"
                + "The temp variable is essential. If you wrote arr[left] = arr[right] first, you would lose "
                + "the original left value before you had a chance to copy it to the right side. Always use a "
                + "temp variable when swapping."
        );
        ReverseArrayInfoPanel.addCodeSnippet("int left = 0, right = arr.length - 1;\nwhile (left < right) {\n    int temp = arr[left];\n    arr[left++] = arr[right];\n    arr[right--] = temp;\n}");
        ReverseArrayInfoPanel.setAnswerCode("int left = 0;\nint right = arr.length - 1;\nwhile (left < right) {\n    int temp = arr[left];\n    arr[left] = arr[right];\n    arr[right] = temp;\n    left++;\n    right--;\n}");
        ReverseArrayInfoPanel.addHint(SkillLevel.HIGHSCHOOL,    "Start from both ends and swap, moving inward until they meet.");
        ReverseArrayInfoPanel.addHint(SkillLevel.UNDERGRADUATE, "Use a temp variable to swap without losing data.");
        ReverseArrayInfoPanel.setTimeComplexity("O(n)"); ReverseArrayInfoPanel.setSpaceComplexity("O(1)");
        Problem ReverseArrayProblem = new Problem(5, "Reverse Array",
                "Write a method that reverses an integer array in-place.",
                DifficultyLevel.INTERMEDIATE, Category.DATA_STRUCTURES, ReverseArrayInfoPanel);
        ReverseArrayProblem.addTestCase(new TestCase(6, 5, "1 2 3 4 5", "5 4 3 2 1", false));
        ReverseArrayProblem.addTestCase(new TestCase(7, 5, "10 20 30",  "30 20 10",  false));
        problemManager.addProblem(ReverseArrayProblem);

        // ── 6 Binary Search — INTERMEDIATE ───────────────────────────────────
        InfoPanel BinarySearchInfoPanel = new InfoPanel(106);
        BinarySearchInfoPanel.setExplanation(
                "Imagine you are guessing a number between 1 and 100 and after each guess you are told whether "
                + "the answer is higher or lower. What is the best strategy? Always guess the middle!\n\n"
                + "If the answer is higher than your guess, you know it is in the upper half — the lower half "
                + "is eliminated. Guess the middle of the upper half. Keep halving the remaining range until "
                + "you find the exact number. This is Binary Search.\n\n"
                + "Why is it fast? Each guess cuts the remaining possibilities in half. For 1,000 items you "
                + "need at most 10 guesses. For 1,000,000 items you need at most 20. Compare that to checking "
                + "each item one by one — that could take 1,000,000 guesses in the worst case.\n\n"
                + "REQUIREMENT: the array must be sorted beforehand. Binary Search only works on sorted data.\n\n"
                + "The code uses three variables: left (start of search range), right (end of search range), "
                + "and mid (the middle index we compute each step):\n"
                + "    int left  = 0;\n"
                + "    int right = arr.length - 1;\n"
                + "    while (left <= right) {\n"
                + "        int mid = left + (right - left) / 2;   // safe midpoint formula\n"
                + "        if      (arr[mid] == target) return mid;      // found it!\n"
                + "        else if (arr[mid] < target)  left = mid + 1; // search right half\n"
                + "        else                         right = mid - 1;// search left half\n"
                + "    }\n"
                + "    return -1;  // not found\n\n"
                + "Note: we write left + (right - left) / 2 instead of (left + right) / 2 to avoid an "
                + "integer overflow bug when left and right are very large numbers."
        );
        BinarySearchInfoPanel.addCodeSnippet("int left = 0, right = arr.length - 1;\nwhile (left <= right) {\n    int mid = left + (right - left) / 2;\n    if (arr[mid] == target) return mid;\n    else if (arr[mid] < target) left = mid + 1;\n    else right = mid - 1;\n}\nreturn -1;");
        BinarySearchInfoPanel.setAnswerCode("int left = 0;\nint right = arr.length - 1;\nwhile (left <= right) {\n    int mid = left + (right - left) / 2;\n    if (arr[mid] == target) {\n        return mid;\n    } else if (arr[mid] < target) {\n        left = mid + 1;\n    } else {\n        right = mid - 1;\n    }\n}\nreturn -1;");
        BinarySearchInfoPanel.addHint(SkillLevel.HIGHSCHOOL,    "The array must be sorted! Always check the middle first.");
        BinarySearchInfoPanel.addHint(SkillLevel.UNDERGRADUATE, "If the middle is too small, move the left pointer up. If too large, move the right pointer down.");
        BinarySearchInfoPanel.setTimeComplexity("O(log n)"); BinarySearchInfoPanel.setSpaceComplexity("O(1)");
        Problem BinarySearchProblem = new Problem(6, "Binary Search",
                "Implement binary search on a sorted array. Return the index of the target, or -1 if not found.",
                DifficultyLevel.INTERMEDIATE, Category.ALGORITHMS, BinarySearchInfoPanel);
        BinarySearchProblem.addTestCase(new TestCase(8,  6, "1 3 5 7 9 | 5", "2",  false));
        BinarySearchProblem.addTestCase(new TestCase(9,  6, "2 4 6 8   | 7", "-1", false));
        BinarySearchProblem.addTestCase(new TestCase(10, 6, "1 2 3 4 5 | 1", "0",  true));
        problemManager.addProblem(BinarySearchProblem);

        // ── 7 Newton-Raphson Square Root — ADVANCED ───────────────────────────
        InfoPanel NewtonRaphsonInfoPanel = new InfoPanel(107);
        NewtonRaphsonInfoPanel.setExplanation(
                "The square root of a number n is the value x where x * x = n. For n = 9, x = 3 because 3 * 3 = 9.\n\n"
                + "But what about the square root of 2? There is no simple fraction that equals it exactly — "
                + "it goes on forever: 1.41421356... We need an algorithm that produces a good enough approximation.\n\n"
                + "THE IDEA — Start with a rough guess and keep improving it.\n\n"
                + "If our current guess is x and the real answer is the square root of n, then:\n"
                + "  If x is too big,  then n/x is too small.\n"
                + "  If x is too small, then n/x is too big.\n"
                + "  The truth lies somewhere in between — so average them: (x + n/x) / 2.\n\n"
                + "This new average is always a better guess than x was. Do this again and again until the "
                + "guess is close enough (within 0.0001 of the true answer).\n\n"
                + "In code:\n"
                + "    double x = n / 2.0;              // start with n/2 as first guess\n"
                + "    while (Math.abs(x * x - n) > 0.0001) {  // while not close enough\n"
                + "        x = (x + n / x) / 2.0;      // improve the guess\n"
                + "    }\n"
                + "    return x;\n\n"
                + "Math.abs gives the absolute value (removes the minus sign) so the check works whether "
                + "x*x is slightly too big or slightly too small.\n\n"
                + "This method is called Newton-Raphson and it converges extraordinarily fast — the number "
                + "of correct decimal places roughly doubles with each iteration."
        );
        NewtonRaphsonInfoPanel.addCodeSnippet("double x = n / 2.0;\nwhile (Math.abs(x * x - n) > 0.0001) {\n    x = (x + n / x) / 2.0;\n}\nreturn x;");
        NewtonRaphsonInfoPanel.setAnswerCode("double x = n / 2.0;\nwhile (Math.abs(x * x - n) > 0.0001) {\n    x = (x + n / x) / 2.0;\n}\nreturn x;");
        NewtonRaphsonInfoPanel.addHint(SkillLevel.HIGHSCHOOL,    "Start with x = n/2 and keep applying x = (x + n/x) / 2 until x*x ≈ n.");
        NewtonRaphsonInfoPanel.addHint(SkillLevel.UNDERGRADUATE, "The formula linearises f(x)=x²-n around the current guess.");
        NewtonRaphsonInfoPanel.setTimeComplexity("O(log log n)"); NewtonRaphsonInfoPanel.setSpaceComplexity("O(1)");
        Problem NewtonRaphsonProblem = new Problem(7, "Newton-Raphson Square Root",
                "Use the Newton-Raphson method to approximate the square root of n. Stop when |x²-n| < 0.0001.",
                DifficultyLevel.ADVANCED, Category.NUMERICAL_METHODS, NewtonRaphsonInfoPanel);
        NewtonRaphsonProblem.addTestCase(new TestCase(11, 7, "9",  "3.0",  false));
        NewtonRaphsonProblem.addTestCase(new TestCase(12, 7, "16", "4.0",  false));
        NewtonRaphsonProblem.addTestCase(new TestCase(13, 7, "2",  "1.414", true));
        problemManager.addProblem(NewtonRaphsonProblem);

        // Sort: BEGINNER → INTERMEDIATE → ADVANCED, then by problemID
        problemManager.getAllProblems().sort(Comparator
                .comparingInt(p -> difficultyOrder(((Problem)p).getDifficulty()))
                .thenComparingInt(p -> ((Problem)p).getProblemID()));
    }

    //list of difficulties
    //renamed from diffOrder to difficultyOrder to follow best practices
    private static int difficultyOrder(DifficultyLevel difficulty) {
        switch (difficulty) {
            case BEGINNER:     return 0;
            case INTERMEDIATE: return 1;
            case ADVANCED:     return 2;
            default:           return 3;
        }
    }

    // ─── Instructor features ──────────────────────────────────────────────────

    /**
     * Toggle the visibility of a problem.
     * Hidden → visible and visible → hidden on each call.
     */
    public void toggleProblemVisibility(int problemId) {
        if (hiddenProblems.contains(problemId)) {
        	hiddenProblems.remove(problemId);
        }
        else {
        	hiddenProblems.add(problemId);
        }
    }

    /** Returns true if an instructor has hidden this problem from students. */
    public boolean isProblemHidden(int problemId) {
        return hiddenProblems.contains(problemId);
    }

    /**
     * Restore all problems to visible.
     * Called by InstructorPanel.resetOrder().
     */
    public void clearHiddenProblems() {
        hiddenProblems.clear();
    }

    /**
     * Move a problem from one index to another in the ordered list.
     * Both indices must be valid (0 ≤ index < list.size()).
     * Called by InstructorPanel's ▲ / ▼ buttons.
     *
     * @param fromIdx  current position of the problem
     * @param toIdx    desired new position
     */
    public void moveProblem(int fromIdx, int toIdx) {
        List<Problem> list = problemManager.getAllProblems();
        if (fromIdx < 0 || fromIdx >= list.size() ||
            toIdx   < 0 || toIdx   >= list.size()) return;
        Problem moving = list.remove(fromIdx);
        list.add(toIdx, moving);
    }

    /**
     * Return only the problems that are visible to students (not hidden).
     * Use this in ProblemPanel instead of getProblemManager().getAllProblems()
     * so hidden problems don't appear in the student sidebar.
     */
    public List<Problem> getVisibleProblems() {
        List<Problem> visible = new ArrayList<>();
        for (Problem p : problemManager.getAllProblems()) {
            if (!hiddenProblems.contains(p.getProblemID())) visible.add(p);
        }
        return visible;
    }

    // ── Custom problem tracking ──────────────────────────────────────────────

    /**
     * Loads custom problems from disk and re-registers them in both the
     * problem manager and the customProblemIdSet so isCustomProblem() returns
     * true for them and the Edit/Delete buttons appear in InstructorPanel.
     */
    private void loadCustomProblems() {
        List<Problem> savedCustomProblems = SaveManager.loadCustomProblems();
        for (Problem customProblem : savedCustomProblems) {
            problemManager.addProblem(customProblem);
            customProblemIdSet.add(customProblem.getProblemID());
        }
    }

    /**
     * Returns true when the given problem was created by an instructor via the
     * Create Problem dialog (as opposed to being a built-in seeded problem).
     *
     * @param problemId the ID to check
     */
    public boolean isCustomProblem(int problemId) {
        return customProblemIdSet.contains(problemId);
    }

    /**
     * Updates the fields of an existing custom problem in-place.
     * Only custom problems may be edited; built-in seeded problems are read-only.
     *
     * @param updatedProblem the Problem object with new field values;
     *                       its ID is used to locate the original in the list
     */
    public void updateCustomProblem(Problem updatedProblem) {
        // Guard against null input
        if (updatedProblem == null) {
            return;
        }

        // Guard: only custom problems are editable
        if (!customProblemIdSet.contains(updatedProblem.getProblemID())) {
            System.err.println("CSIL: attempted to edit a non-custom problem — ignored.");
            return;
        }

        // Locate the problem in the live list and update it in-place
        List<Problem> allProblems = problemManager.getAllProblems();
        for (int problemIndex = 0; problemIndex < allProblems.size(); problemIndex++) {
            if (allProblems.get(problemIndex).getProblemID() == updatedProblem.getProblemID()) {
                allProblems.set(problemIndex, updatedProblem);
                // Persist the updated state immediately
                persistCustomProblems();
                return;
            }
        }
    }

    /**
     * Removes a custom problem from the live list entirely.
     * Only custom problems may be deleted.
     *
     * @param problemId the ID of the problem to remove
     */
    public void deleteCustomProblem(int problemId) {
        // Remove from the live problem list
        problemManager.getAllProblems().removeIf(
                problem -> problem.getProblemID() == problemId);

        // Remove from tracking sets — use Integer.valueOf() to call remove(Object)
        // rather than remove(int index) which would cause an IndexOutOfBoundsException
        customProblemIdSet.remove(Integer.valueOf(problemId));
        hiddenProblems.remove(Integer.valueOf(problemId));

        // Persist the updated custom problem list immediately
        persistCustomProblems();
    }

    /**
     * Adds a custom instructor-authored problem to the live problem list.
     * The problem is inserted at the position appropriate for its difficulty
     * tier so that the ordering invariant (BEGINNER first) is preserved.
     *
     * @param customProblem the newly created Problem; ignored if null
     */
    public void addCustomProblem(Problem customProblem) {
        // Guard against null input
        if (customProblem == null) {
            return;
        }

        // Append to the live problem list
        problemManager.addProblem(customProblem);

        // Record this ID so it can be edited or deleted later
        customProblemIdSet.add(customProblem.getProblemID());

        // Write to disk immediately so the problem survives a restart
        persistCustomProblems();
    }

    /**
     * Collects all custom problems from the live list and writes them to disk.
     * Called automatically after any create, update, or delete operation.
     */
    private void persistCustomProblems() {
        List<Problem> customProblemList = new ArrayList<>();
        for (Problem problem : problemManager.getAllProblems()) {
            if (customProblemIdSet.contains(problem.getProblemID())) {
                customProblemList.add(problem);
            }
        }
        SaveManager.saveCustomProblems(customProblemList);
    }

    public ProblemManager getProblemManager() { return problemManager; }
    public List<Attempt>  getAttempts()       { return attempts; }
    public void           addAttempt(Attempt attemptRecord) { attempts.add(attemptRecord); }
}
