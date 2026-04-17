/**
 * File:    InfoPanelDialog.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Modal dialog displaying a block's InfoPanel content at the user's
 *          chosen skill level (High School, Undergraduate, Professional).
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.model.BlockCategory;
import com.macrosoff.csil.model.InfoPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class InfoPanelDialog extends JDialog {

    private static final Color BG      = new Color(22, 26, 36);
    private static final Color BG2     = new Color(30, 35, 51);
    private static final Color ACCENT  = new Color(79, 142, 247);
    private static final Color ACCENT2 = new Color(61, 214, 140);
    private static final Color ACCENT3 = new Color(247, 200, 79);
    private static final Color TEXT    = new Color(232, 236, 244);
    private static final Color TEXT2   = new Color(139, 147, 167);
    private static final Color BORDER  = new Color(42, 48, 71);

    private final InfoPanel infoPanel;
    private final String blockTitle;

    private JLabel explanationLabel;
    private JTextArea snippetArea;
    private JToggleButton[] skillButtons;

    public InfoPanelDialog(Frame owner, BlockCategory block) {
        super(owner, "Info Panel — " + block.getLabel(), true);
        this.infoPanel = block.getInfoPanel();
        this.blockTitle = block.getLabel();
        buildUI();
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        getRootPane().setBorder(new EmptyBorder(20, 24, 20, 24));

        // Title
        JLabel title = new JLabel(blockTitle);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(0, 0, 14, 0));
        add(title, BorderLayout.NORTH);

        // Center
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Skill-level tabs

        // Explanation section
        center.add(sectionLabel("Explanation"));
        center.add(Box.createVerticalStrut(6));
        explanationLabel = new JLabel("<html><body style='width:380px'>" +
                escape(infoPanel.getExplanation()) + "</body></html>");
        explanationLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        explanationLabel.setForeground(TEXT2);
        explanationLabel.setAlignmentX(LEFT_ALIGNMENT);
        center.add(explanationLabel);
        center.add(Box.createVerticalStrut(14));

        // Code snippet
        List<String> snippets = infoPanel.getCodeSnippets();
        if (!snippets.isEmpty()) {
            center.add(sectionLabel("Code Example"));
            center.add(Box.createVerticalStrut(6));
            snippetArea = new JTextArea(snippets.get(0));
            snippetArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
            snippetArea.setForeground(ACCENT2);
            snippetArea.setBackground(BG2);
            snippetArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    new EmptyBorder(10, 12, 10, 12)));
            snippetArea.setEditable(false);
            snippetArea.setLineWrap(true);
            snippetArea.setWrapStyleWord(true);
            snippetArea.setAlignmentX(LEFT_ALIGNMENT);
            snippetArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            center.add(snippetArea);
            center.add(Box.createVerticalStrut(14));
        }

        // Complexity
        center.add(sectionLabel("Complexity"));
        center.add(Box.createVerticalStrut(6));
        center.add(buildComplexityRow());

        // Hints
        List<String> hints = infoPanel.getHints(null);
        if (!hints.isEmpty()) {
            center.add(Box.createVerticalStrut(14));
            center.add(sectionLabel("Hint"));
            center.add(Box.createVerticalStrut(6));
            JLabel hintLabel = new JLabel("<html><body style='width:380px'>\uD83D\uDCA1 " + escape(hints.get(0)) + "</body></html>");
            hintLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
            hintLabel.setForeground(ACCENT3);
            hintLabel.setAlignmentX(LEFT_ALIGNMENT);
            center.add(hintLabel);
        }

        add(center, BorderLayout.CENTER);

        // Close button
        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        closeBtn.setForeground(TEXT2);
        closeBtn.setBackground(BG2);
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(6, 18, 6, 18)));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(16, 0, 0, 0));
        south.add(closeBtn);
        add(south, BorderLayout.SOUTH);

        getContentPane().setPreferredSize(new Dimension(460, 420));
    }


    private void styleTab(JToggleButton b, boolean active) {
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? ACCENT : BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        b.setBackground(active ? new Color(26, 37, 64) : new Color(30, 35, 51));
        b.setForeground(active ? ACCENT : TEXT2);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }


    private JPanel buildComplexityRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(complexityCard("Time", infoPanel.getTimeComplexity()));
        row.add(complexityCard("Space", infoPanel.getSpaceComplexity()));
        return row;
    }

    private JPanel complexityCard(String label, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG2);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(6, 14, 6, 14)));
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lbl.setForeground(TEXT2);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Monospaced", Font.BOLD, 14));
        val.setForeground(ACCENT3);
        card.add(lbl);
        card.add(val);
        return card;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("SansSerif", Font.PLAIN, 10));
        l.setForeground(new Color(84, 93, 114));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
