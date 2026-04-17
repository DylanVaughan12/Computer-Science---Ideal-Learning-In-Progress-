/**
 * File:    App.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Entry point for the CS-IL desktop application. Configures the global Swing
 *          look-and-feel, applies the dark-theme UI defaults, and launches MainWindow
 *          on the Swing Event Dispatch Thread.
 */
package com.macrosoff.csil;

import com.macrosoff.csil.ui.MainWindow;

import javax.swing.*;

public class App {

    public static void main(String[] args) {
        // Use system look-and-feel as a base, then our custom dark theme overrides it
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Global UI defaults for dark theme
        UIManager.put("Panel.background",              new java.awt.Color(15, 17, 23));
        UIManager.put("ScrollPane.background",         new java.awt.Color(15, 17, 23));
        UIManager.put("Viewport.background",           new java.awt.Color(15, 17, 23));
        UIManager.put("ScrollBar.thumb",               new java.awt.Color(42, 48, 71));
        UIManager.put("ScrollBar.track",               new java.awt.Color(22, 26, 36));
        UIManager.put("ScrollBar.thumbHighlight",      new java.awt.Color(60, 68, 100));
        UIManager.put("ScrollBar.width",               8);
        UIManager.put("Button.focus",                  new java.awt.Color(0, 0, 0, 0));
        UIManager.put("ToggleButton.focus",            new java.awt.Color(0, 0, 0, 0));
        UIManager.put("TextField.caretForeground",     new java.awt.Color(232, 236, 244));
        UIManager.put("PasswordField.caretForeground", new java.awt.Color(232, 236, 244));
        UIManager.put("OptionPane.background",         new java.awt.Color(22, 26, 36));
        UIManager.put("OptionPane.messageForeground",  new java.awt.Color(232, 236, 244));

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
