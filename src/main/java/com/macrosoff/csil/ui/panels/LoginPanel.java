/**
 * File:    LoginPanel.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.1
 * Purpose: Provides the login and registration screens displayed before a user
 *          authenticates. Handles input validation, user type selection
 *          (STUDENT or INSTRUCTOR only — EMPLOYEE removed), and delegates
 *          authentication to DataStore. Password policy is enforced by
 *          SaveManager.validatePassword() before registration is attempted.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.data.DataStore;
import com.macrosoff.csil.model.User;
import com.macrosoff.csil.model.enums.UserType;
import com.macrosoff.csil.ui.MainWindow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class LoginPanel extends JPanel {

    private static final Color BG       = new Color(15, 17, 23);
    private static final Color CARD_BG  = new Color(22, 26, 36);
    private static final Color ACCENT   = new Color(79, 142, 247);
    private static final Color TEXT     = new Color(232, 236, 244);
    private static final Color TEXT2    = new Color(139, 147, 167);
    private static final Color BORDER   = new Color(42, 48, 71);
    private static final Color ERROR    = new Color(247, 112, 79);

    private final MainWindow mainWindow;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // Login fields
    private JTextField loginUserField;
    private JPasswordField loginPassField;
    private JLabel loginErrorLabel;

    // Register fields
    private JTextField regUserField;
    private JPasswordField regPassField;
    private JLabel regErrorLabel;
    private UserType selectedType = UserType.STUDENT;

    public LoginPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setBackground(BG);
        setLayout(new GridBagLayout());
        buildCard();
    }

    private void buildCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(36, 32, 36, 32)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(340, 999));
        card.setPreferredSize(new Dimension(340, 420));

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.add(buildLoginForm(), "login");
        cardPanel.add(buildRegisterForm(), "register");

        card.add(cardPanel);
        add(card);
    }

    private JPanel buildLoginForm() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(makeTitle("CS — Ideal Learning"));
        p.add(makeSubtitle("Sign in to continue your learning journey"));
        p.add(Box.createVerticalStrut(24));

        loginUserField = makeTextField("Username");
        loginPassField = makePasswordField("Password");
        loginErrorLabel = makeErrorLabel("Invalid username or password.");

        p.add(makeLabel("Username"));
        p.add(Box.createVerticalStrut(4));
        p.add(loginUserField);
        p.add(Box.createVerticalStrut(12));
        p.add(makeLabel("Password"));
        p.add(Box.createVerticalStrut(4));
        p.add(loginPassField);
        p.add(Box.createVerticalStrut(6));
        p.add(loginErrorLabel);
        p.add(Box.createVerticalStrut(12));
        p.add(makePrimaryButton("Sign In", e -> doLogin()));
        p.add(Box.createVerticalStrut(14));
        p.add(makeSwitchLink("New user?", "Create account", e -> cardLayout.show(cardPanel, "register")));

        loginPassField.addActionListener(e -> doLogin());
        return p;
    }

    private JPanel buildRegisterForm() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(makeTitle("CS — Ideal Learning"));
        p.add(makeSubtitle("Create your account"));
        p.add(Box.createVerticalStrut(24));

        regUserField = makeTextField("Choose a username");
        regPassField = makePasswordField("Choose a password");
        regErrorLabel = makeErrorLabel("Username already taken.");

        p.add(makeLabel("Username"));
        p.add(Box.createVerticalStrut(4));
        p.add(regUserField);
        p.add(Box.createVerticalStrut(12));
        p.add(makeLabel("Password"));
        p.add(Box.createVerticalStrut(4));
        p.add(regPassField);
        p.add(Box.createVerticalStrut(4));
        JLabel pwHint = new JLabel("Min 7 chars · at least one special character (e.g. @, !, #)");
        pwHint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        pwHint.setForeground(new Color(84, 93, 114));
        pwHint.setAlignmentX(LEFT_ALIGNMENT);
        p.add(pwHint);
        p.add(Box.createVerticalStrut(10));
        p.add(makeLabel("Account Type"));
        p.add(Box.createVerticalStrut(6));
        p.add(buildTypeSelector());
        p.add(Box.createVerticalStrut(6));
        p.add(regErrorLabel);
        p.add(Box.createVerticalStrut(12));
        p.add(makePrimaryButton("Create Account", e -> doRegister()));
        p.add(Box.createVerticalStrut(14));
        p.add(makeSwitchLink("Have an account?", "Sign in", e -> cardLayout.show(cardPanel, "login")));

        return p;
    }

    private JPanel buildTypeSelector() {
        // EMPLOYEE role has been removed; only STUDENT and INSTRUCTOR are available
        UserType[] types  = {UserType.STUDENT, UserType.INSTRUCTOR};
        String[]   labels = {"Student", "Instructor"};
        JPanel row = new JPanel(new GridLayout(1, types.length, 6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JToggleButton[] btns = new JToggleButton[types.length];
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < types.length; i++) {
            JToggleButton b = new JToggleButton(labels[i]);
            styleTypeButton(b, false);
            final UserType t = types[i];
            final JToggleButton fb = b;
            b.addActionListener(e -> {
                selectedType = t;
                for (JToggleButton tb : btns) styleTypeButton(tb, tb == fb);
            });
            btns[i] = b;
            bg.add(b);
            row.add(b);
        }
        btns[0].setSelected(true);
        styleTypeButton(btns[0], true);
        return row;
    }

    private void styleTypeButton(JToggleButton b, boolean active) {
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(active ? ACCENT : BORDER, 1));
        b.setBackground(active ? new Color(26, 37, 64) : new Color(30, 35, 51));
        b.setForeground(active ? ACCENT : TEXT2);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    private void doLogin() {
        String u = loginUserField.getText().trim();
        String p = new String(loginPassField.getPassword());
        User user = DataStore.getInstance().findUser(u, p);
        if (user == null) {
            loginErrorLabel.setVisible(true);
        } else {
            loginErrorLabel.setVisible(false);
            mainWindow.onLoginSuccess(user);
        }
    }

    private void doRegister() {
        String u = regUserField.getText().trim();
        String p = new String(regPassField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            regErrorLabel.setText("Please fill all fields.");
            regErrorLabel.setVisible(true);
            return;
        }
        String error = DataStore.getInstance().registerUser(u, p, selectedType);
        if (error != null) {
            regErrorLabel.setText(error);
            regErrorLabel.setVisible(true);
        } else {
            regErrorLabel.setVisible(false);
            User user = DataStore.getInstance().findUser(u, p);
            mainWindow.onLoginSuccess(user);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private JLabel makeTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 20));
        l.setForeground(ACCENT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel makeSubtitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(TEXT2);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(TEXT2);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField f = new JTextField(20);
        styleInput(f);
        return f;
    }

    private JPasswordField makePasswordField(String placeholder) {
        JPasswordField f = new JPasswordField(20);
        styleInput(f);
        return f;
    }

    private void styleInput(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setForeground(TEXT);
        f.setBackground(new Color(30, 35, 51));
        f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(7, 10, 7, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JLabel makeErrorLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(ERROR);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setVisible(false);
        return l;
    }

    private JButton makePrimaryButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(ACCENT);
        b.setBorder(new EmptyBorder(10, 20, 10, 20));
        b.setFocusPainted(false);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(58, 125, 232)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(ACCENT); }
        });
        return b;
    }

    private JPanel makeSwitchLink(String prefix, String linkText, ActionListener al) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel pre = new JLabel(prefix);
        pre.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pre.setForeground(TEXT2);
        JLabel link = new JLabel("<html><u>" + linkText + "</u></html>");
        link.setFont(new Font("SansSerif", Font.PLAIN, 13));
        link.setForeground(ACCENT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { al.actionPerformed(null); }
        });
        row.add(pre);
        row.add(link);
        return row;
    }
}
