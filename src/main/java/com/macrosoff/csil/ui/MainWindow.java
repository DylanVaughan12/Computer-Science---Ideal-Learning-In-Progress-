/**
 * File: MainWindow.java
 * Author:  Kyle LaVake
 * Review: Kyle LaVake
 * Approval: 
 * Date:    2026-04-06
 * Version: 2.0
 * Purpose: Application shell. Manages the login/app card layout, the top
 *          navigation bar, and tab switching. Version 2.0 places the Learn tab
 *          first so students encounter teaching content before practice problems.
 *          The "Practice This" button in LearnPanel wires directly to the
 *          Problems tab and auto-selects the relevant problem.
 *
 *          Tab order (all users):     Learn → Problems → Progress
 *          Additional tab (Instructor): + Instructor
 */
package com.macrosoff.csil.ui;

import com.macrosoff.csil.model.Problem;
import com.macrosoff.csil.model.User;
import com.macrosoff.csil.model.enums.UserType;
import com.macrosoff.csil.ui.panels.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

/**
 * MainWindow
 * The top-level JFrame. Contains a root CardLayout that swaps between the
 * LoginPanel (before authentication) and the app shell (after authentication).
 * The app shell itself contains a top navigation bar and a CardLayout that
 * swaps between the Learn, Problems, Progress, and (for instructors) Instructor
 * tab panels.
 */
public class MainWindow extends JFrame {

    // Colours
    private static final Color BACKGROUND_PRIMARY   = new Color(15, 17, 23);
    private static final Color BACKGROUND_SECONDARY = new Color(22, 26, 36);
    private static final Color ACCENT_BLUE          = new Color(79, 142, 247);
    private static final Color TEXT_PRIMARY         = new Color(232, 236, 244);
    private static final Color TEXT_SECONDARY       = new Color(139, 147, 167);
    private static final Color BORDER_COLOR         = new Color(42, 48, 71);

    // Card names for the root and main-area CardLayouts
    private static final String CARD_LOGIN = "login";
    private static final String CARD_APP   = "app";

    // Root layout
    private CardLayout rootCardLayout;
    private JPanel     rootPanel;

    // App-shell panels
    private JPanel          appShell;
    private LearnPanel      learnPanel;
    private ProblemPanel    problemPanel;
    private ProgressPanel   progressPanel;
    private InstructorPanel instructorPanel;

    // Navigation 
    // navCardNames maps each button index to its CardLayout card name
    private JToggleButton[] navButtons;
    private String[]        navCardNames;

    // User-area widgets (updated on login)
    private JLabel usernameLabel;
    private JLabel userTypeLabel;
    private JLabel avatarLabel;

    // Currently logged-in user — null when the login screen is showing
    private User currentUser;

    // Constructor

    public MainWindow() {
        super("CS — Ideal Learning");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setPreferredSize(new Dimension(1340, 820));
        getContentPane().setBackground(BACKGROUND_PRIMARY);

        rootCardLayout = new CardLayout();
        rootPanel      = new JPanel(rootCardLayout);
        rootPanel.setBackground(BACKGROUND_PRIMARY);

        // Build and register the login screen
        LoginPanel loginPanel = new LoginPanel(this);
        rootPanel.add(loginPanel, CARD_LOGIN);

        setContentPane(rootPanel);
        pack();
        setLocationRelativeTo(null);

        // Start on the login screen
        rootCardLayout.show(rootPanel, CARD_LOGIN);
    } // end of MainWindow()

    // Authentication callbacks

    /**
     * Called by LoginPanel after the user's credentials are verified.
     * Builds the app shell appropriate for the user's role and switches to it.
     *
     * @param user the authenticated user
     */
    public void onLoginSuccess(User user) {
        this.currentUser = user;

        // Remove any previously built app shell (e.g. after re-login)
        if (appShell != null) {
            rootPanel.remove(appShell);
        }

        // Build the shell and register it
        appShell = buildAppShell(user);
        rootPanel.add(appShell, CARD_APP);

        // Populate user-area labels
        usernameLabel.setText(user.getUserName());
        userTypeLabel.setText(user.getUserType().name());
        avatarLabel.setText(String.valueOf(user.getUserName().charAt(0)).toUpperCase());

        // Initialise the problem panel with the logged-in user's saved progress
        problemPanel.setUser(user);

        // Open on the Learn tab so students encounter teaching content first
        switchToTab(0);
        rootCardLayout.show(rootPanel, CARD_APP);
    } // end of onLoginSuccess()

    // App shell construction

    /**
     * Builds the full app shell: top navigation bar + main content area.
     *
     * @param user the logged-in user (determines which tabs are shown)
     */
    private JPanel buildAppShell(User user) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(BACKGROUND_PRIMARY);

        // Determine whether the Instructor tab should be shown
        boolean isInstructor = user.getUserType() == UserType.INSTRUCTOR;

        // Build content panels before the top bar so learnPanel exists when
        // the Practice callback is wired
        learnPanel      = new LearnPanel();
        problemPanel    = new ProblemPanel(this);
        progressPanel   = new ProgressPanel();
        instructorPanel = new InstructorPanel();

        // Wire the Practice button: LearnPanel fires a callback with the chosen
        // Problem, and MainWindow switches to the Problems tab and loads it.
        learnPanel.setOnPracticeRequested(this::openProblemForPractice);

        // Set tab names based on role
        if (isInstructor) {
            navCardNames = new String[]{"learn", "problems", "progress", "instructor"};
        } else {
            navCardNames = new String[]{"learn", "problems", "progress"};
        }

        shell.add(buildTopBar(isInstructor), BorderLayout.NORTH);
        shell.add(buildMainArea(isInstructor), BorderLayout.CENTER);
        return shell;
    } // end of buildAppShell()

    // Top navigation bar

    /**
     * Builds the top bar with logo, navigation buttons, and user area.
     *
     * @param isInstructor true when the Instructor tab should be included
     */
    private JPanel buildTopBar(boolean isInstructor) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BACKGROUND_SECONDARY);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(8, 16, 8, 16)));

        // Logo
        JLabel logoLabel = new JLabel("CS — Ideal Learning");
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        logoLabel.setForeground(ACCENT_BLUE);
        topBar.add(logoLabel, BorderLayout.WEST);

        // Navigation buttons
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        navPanel.setOpaque(false);

        String[] tabLabels;
        if (isInstructor) {
            tabLabels = new String[]{"Learn", "Problems", "Progress", "Instructor"};
        } else {
            tabLabels = new String[]{"Learn", "Problems", "Progress"};
        }

        navButtons = new JToggleButton[tabLabels.length];
        ButtonGroup buttonGroup = new ButtonGroup();

        for (int tabIndex = 0; tabIndex < tabLabels.length; tabIndex++) {
            JToggleButton navButton = new JToggleButton(tabLabels[tabIndex]);
            styleNavButton(navButton, false);

            // Capture tabIndex for the lambda
            final int capturedIndex = tabIndex;
            navButton.addActionListener(actionEvent -> switchToTab(capturedIndex));

            navButtons[tabIndex] = navButton;
            buttonGroup.add(navButton);
            navPanel.add(navButton);
        }

        // Start with the first button (Learn) selected
        navButtons[0].setSelected(true);
        styleNavButton(navButtons[0], true);

        topBar.add(navPanel, BorderLayout.CENTER);

        // User area: type badge, avatar, username, settings, sign-out
        topBar.add(buildUserArea(), BorderLayout.EAST);

        return topBar;
    } // end of buildTopBar()

    /**
     * Builds the right-hand user area showing avatar, username, and action buttons.
     */
    private JPanel buildUserArea() {
        JPanel userArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        userArea.setOpaque(false);

        // Account type badge
        userTypeLabel = new JLabel("STUDENT");
        userTypeLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        userTypeLabel.setForeground(TEXT_SECONDARY);
        userTypeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(3, 8, 3, 8)));

        // Circular avatar showing the first letter of the username
        avatarLabel = new JLabel("U");
        avatarLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(30, 30));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(ACCENT_BLUE);

        usernameLabel = new JLabel("User");
        usernameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        usernameLabel.setForeground(TEXT_SECONDARY);

        // Profile settings button
        JButton settingsButton = createTopBarButton("Settings");
        settingsButton.setToolTipText("Change password or delete account");
        settingsButton.addActionListener(actionEvent -> openProfileSettings());

        // Sign-out button
        JButton signOutButton = createTopBarButton("Sign out");
        signOutButton.addActionListener(actionEvent -> signOut());

        userArea.add(userTypeLabel);
        userArea.add(avatarLabel);
        userArea.add(usernameLabel);
        userArea.add(settingsButton);
        userArea.add(signOutButton);

        return userArea;
    } // end of buildUserArea()

    // Main content area

    /**
     * Builds the main card-layout area and registers all tab panels.
     *
     * @param isInstructor true when the Instructor panel should be registered
     */
    private JPanel buildMainArea(boolean isInstructor) {
        JPanel mainArea = new JPanel(new CardLayout());
        mainArea.setBackground(BACKGROUND_PRIMARY);
        mainArea.setName("mainArea");

        // Register panels under their card names
        mainArea.add(learnPanel,                   "learn");
        mainArea.add(problemPanel,                 "problems");
        mainArea.add(progressPanel,                "progress");

        if (isInstructor) {
            mainArea.add(instructorPanel, "instructor");
        }

        // Store the CardLayout as a client property for use by switchToTab()
        mainArea.putClientProperty("cardLayout", mainArea.getLayout());

        return mainArea;
    } // end of buildMainArea()

    // Navigation

    /**
     * Switches the main content area to the tab at the given index.
     *
     * @param tabIndex 0-based index into navCardNames and navButtons
     */
    private void switchToTab(int tabIndex) {
        // Guard against stale indices (can happen if the shell was just rebuilt)
        if (navButtons == null || tabIndex >= navButtons.length) {
            return;
        }

        // Update button visual states
        for (int buttonIndex = 0; buttonIndex < navButtons.length; buttonIndex++) {
            styleNavButton(navButtons[buttonIndex], buttonIndex == tabIndex);
        }
        navButtons[tabIndex].setSelected(true);

        // Show the corresponding card
        JPanel mainArea = findMainArea();
        if (mainArea == null) {
            return;
        }
        CardLayout cardLayout = (CardLayout) mainArea.getClientProperty("cardLayout");
        cardLayout.show(mainArea, navCardNames[tabIndex]);

        // Refresh secondary panels when they become visible
        if ("progress".equals(navCardNames[tabIndex])) {
            progressPanel.refresh(currentUser,
                    problemPanel != null ? problemPanel.getSolvedProblems() : new HashSet<>());
        } else if ("instructor".equals(navCardNames[tabIndex])) {
            instructorPanel.refreshList();
        }
    } // end of switchToTab()

    /**
     * Called by LearnPanel when the user clicks "Practice This".
     * Switches to the Problems tab and loads the specified problem.
     *
     * @param targetProblem the problem the student wants to practise
     */
    private void openProblemForPractice(Problem targetProblem) {
        // Find the index of the Problems tab
        int problemsTabIndex = -1;
        for (int index = 0; index < navCardNames.length; index++) {
            if ("problems".equals(navCardNames[index])) {
                problemsTabIndex = index;
                break;
            }
        }

        if (problemsTabIndex < 0) {
            return;
        }

        // Tell ProblemPanel to load the target problem before switching tabs
        if (problemPanel != null && targetProblem != null) {
            problemPanel.loadProblemPublic(targetProblem);
        }

        // Switch to the Problems tab
        switchToTab(problemsTabIndex);
    } // end of openProblemForPractice()

    // Actions

    private void openProfileSettings() {
        if (currentUser == null) {
            return;
        }
        ProfileSettingsDialog settingsDialog = new ProfileSettingsDialog(
                this, currentUser, this::signOut);
        settingsDialog.setVisible(true);
    } // end of openProfileSettings

    private void signOut() {
        currentUser = null;
        rootCardLayout.show(rootPanel, CARD_LOGIN);
    } // end of signOut()

    // Helpers

    /**
     * Finds and returns the main content area panel by its name property.
     */
    private JPanel findMainArea() {
        if (appShell == null) {
            return null;
        }
        for (Component component : appShell.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                if ("mainArea".equals(panel.getName())) {
                    return panel;
                }
            }
        }
        return null;
    } // end of findMainArea()

    private void styleNavButton(JToggleButton button, boolean isActive) {
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setFocusPainted(false);
        if (isActive) {
            button.setBackground(ACCENT_BLUE);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_BLUE, 1),
                    new EmptyBorder(6, 14, 6, 14)));
        } else {
            button.setBackground(new Color(30, 35, 51));
            button.setForeground(TEXT_SECONDARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    new EmptyBorder(6, 14, 6, 14)));
        }
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    } // end of styleNavButton()

    private JButton createTopBarButton(String buttonText) {
        JButton button = new JButton(buttonText);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setForeground(TEXT_SECONDARY);
        button.setBackground(BACKGROUND_SECONDARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 12, 5, 12)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    } // end of createTopBarButton()
} // end of class MainWindow
