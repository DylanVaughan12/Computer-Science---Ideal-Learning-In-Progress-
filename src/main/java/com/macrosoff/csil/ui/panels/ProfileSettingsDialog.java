/**
 * File:    ProfileSettingsDialog.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Modal dialog for changing passwords and deleting accounts.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.data.CanvasCheckpoint;
import com.macrosoff.csil.data.DataStore;
import com.macrosoff.csil.data.SaveManager;
import com.macrosoff.csil.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * ProfileSettingsDialog
 * ──────────────────────
 * A modal dialog reachable from the top navigation bar that lets a logged-in
 * user:
 *
 *   • View their account info (username, type, date joined, problems solved)
 *   • Change their password (enforces the same rules as registration)
 *   • Delete their account entirely (requires confirmation)
 *
 * On deletion the dialog calls the supplied {@code onLogout} Runnable so
 * MainWindow can transition back to the login screen.
 *
 * TODO (team): Add email editing once email-based password recovery is supported.
 * TODO (team): Add avatar / profile picture support here.
 */
public class ProfileSettingsDialog extends JDialog {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG      = new Color(22, 26, 36);
    private static final Color BG2     = new Color(30, 35, 51);
    private static final Color ACCENT  = new Color(79, 142, 247);
    private static final Color PASS    = new Color(61, 214, 140);
    private static final Color FAIL    = new Color(247, 112, 79);
    private static final Color TEXT    = new Color(232, 236, 244);
    private static final Color TEXT2   = new Color(139, 147, 167);
    private static final Color TEXT3   = new Color(84,  93, 114);
    private static final Color BORDER  = new Color(42,  48,  71);

    private final User     user;
    private final Runnable onLogout;   // called if the user deletes their account

    // Password change widgets
    private JPasswordField currentPwField;
    private JPasswordField newPwField;
    private JPasswordField confirmPwField;
    private JLabel         pwStatusLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ProfileSettingsDialog(Frame owner, User user, Runnable onLogout) {
        super(owner, "Profile Settings", true);
        this.user     = user;
        this.onLogout = onLogout;
        buildUI();
        pack();
        setMinimumSize(new Dimension(420, 480));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        getRootPane().setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // ── Account info ──────────────────────────────────────────────────────
        body.add(section("Account Information"));
        body.add(Box.createVerticalStrut(10));
        body.add(infoRow("Username",     user.getUserName()));
        body.add(Box.createVerticalStrut(6));
        body.add(infoRow("Account Type", user.getUserType().name()));
        body.add(Box.createVerticalStrut(6));
        body.add(infoRow("Member since", user.getDateJoined().toString().substring(0, 10)));
        body.add(Box.createVerticalStrut(6));
        int solved = DataStore.getInstance().getSolved(user.getUserName()).size();
        body.add(infoRow("Problems solved", String.valueOf(solved)));
        body.add(Box.createVerticalStrut(20));

        // ── Change password ───────────────────────────────────────────────────
        body.add(section("Change Password"));
        body.add(Box.createVerticalStrut(10));

        body.add(fieldLabel("Current Password"));
        body.add(Box.createVerticalStrut(4));
        currentPwField = pwField();
        body.add(currentPwField);
        body.add(Box.createVerticalStrut(10));

        body.add(fieldLabel("New Password"));
        body.add(Box.createVerticalStrut(4));
        newPwField = pwField();
        body.add(newPwField);
        body.add(Box.createVerticalStrut(4));

        JLabel hint = new JLabel("Min 7 chars · at least one special character");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 10));
        hint.setForeground(TEXT3);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        body.add(hint);
        body.add(Box.createVerticalStrut(10));

        body.add(fieldLabel("Confirm New Password"));
        body.add(Box.createVerticalStrut(4));
        confirmPwField = pwField();
        body.add(confirmPwField);
        body.add(Box.createVerticalStrut(10));

        // Status label for change-password feedback
        pwStatusLabel = new JLabel(" ");
        pwStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pwStatusLabel.setForeground(TEXT2);
        pwStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(pwStatusLabel);
        body.add(Box.createVerticalStrut(8));

        JButton changeBtn = actionButton("Update Password", ACCENT, Color.WHITE);
        changeBtn.addActionListener(e -> changePassword());
        body.add(changeBtn);
        body.add(Box.createVerticalStrut(24));

        // ── Danger zone ───────────────────────────────────────────────────────
        body.add(section("Danger Zone"));
        body.add(Box.createVerticalStrut(10));

        JLabel delInfo = new JLabel("<html><body style='width:340px;color:#8b93a7'>" +
                "Deleting your account is permanent. Your progress and saved canvas " +
                "checkpoints will be removed and cannot be recovered." +
                "</body></html>");
        delInfo.setAlignmentX(LEFT_ALIGNMENT);
        body.add(delInfo);
        body.add(Box.createVerticalStrut(10));

        JButton deleteBtn = actionButton("Delete My Account", FAIL, Color.WHITE);
        deleteBtn.addActionListener(e -> deleteAccount());
        body.add(deleteBtn);

        add(body, BorderLayout.CENTER);

        // Bottom close button
        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        closeBtn.setForeground(TEXT2);
        closeBtn.setBackground(BG2);
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(6, 18, 6, 18)));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(16, 0, 0, 0));
        south.add(closeBtn);
        add(south, BorderLayout.SOUTH);

        // Escape key closes
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Validate fields, hash, and update the password if everything checks out. */
    private void changePassword() {
        String current  = new String(currentPwField.getPassword());
        String newPw    = new String(newPwField.getPassword());
        String confirm  = new String(confirmPwField.getPassword());

        // Verify current password by attempting login
        if (!user.login(user.getUserName(), current)) {
            setStatus("Current password is incorrect.", FAIL);
            return;
        }
        // Enforce password policy
        String policyErr = SaveManager.validatePassword(newPw);
        if (policyErr != null) { setStatus(policyErr, FAIL); return; }
        // Confirm match
        if (!newPw.equals(confirm)) { setStatus("New passwords do not match.", FAIL); return; }

        // Apply and persist
        user.setPassword(newPw);
        DataStore.getInstance().persist();
        setStatus("Password updated successfully.", PASS);

        // Clear fields
        currentPwField.setText("");
        newPwField.setText("");
        confirmPwField.setText("");
    }

    /** Ask for confirmation then delete the user and all their data. */
    private void deleteAccount() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently delete the account \"" +
                user.getUserName() + "\"?\n\nThis cannot be undone.",
                "Confirm Account Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) return;

        // Remove from data store and from disk
        DataStore.getInstance().getUsers().remove(user);
        CanvasCheckpoint.deleteAll(user.getUserName());
        DataStore.getInstance().persist();

        dispose();
        // Tell MainWindow to log out and return to the login screen
        if (onLogout != null) onLogout.run();
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private JLabel section(String title) {
        JLabel l = new JLabel(title.toUpperCase());
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(TEXT3);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(TEXT3);
        lbl.setPreferredSize(new Dimension(120, 0));
        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.PLAIN, 12));
        val.setForeground(TEXT);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(TEXT2);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPasswordField pwField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Monospaced", Font.PLAIN, 13));
        f.setForeground(TEXT);
        f.setBackground(BG2);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(7, 10, 7, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        f.setAlignmentX(LEFT_ALIGNMENT);
        return f;
    }

    private JButton actionButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0,0,0,40), 1),
                new EmptyBorder(8, 18, 8, 18)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    private void setStatus(String msg, Color color) {
        pwStatusLabel.setText(msg);
        pwStatusLabel.setForeground(color);
    }
}
