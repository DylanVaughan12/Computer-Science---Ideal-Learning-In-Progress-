/**
 * File:    ProgressPanel.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Displays the user's overall and per-category progress through the
 *          problem set, refreshed each time the Progress tab is opened.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.data.DataStore;
import com.macrosoff.csil.model.Problem;
import com.macrosoff.csil.model.User;
import com.macrosoff.csil.model.enums.Category;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProgressPanel extends JPanel {

    private static final Color BG     = new Color(15, 17, 23);
    private static final Color CARD   = new Color(26, 31, 48);
    private static final Color ACCENT = new Color(79, 142, 247);
    private static final Color TEXT   = new Color(232, 236, 244);
    private static final Color TEXT2  = new Color(139, 147, 167);
    private static final Color TEXT3  = new Color(84, 93, 114);
    private static final Color BORDER = new Color(42, 48, 71);

    private User currentUser;
    private Set<Integer> solvedProblems;

    public ProgressPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
    }

    public void refresh(User user, Set<Integer> solved) {
        this.currentUser = user;
        this.solvedProblems = solved;
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 28, 24, 28));

        List<Problem> allProblems = DataStore.getInstance().getProblemManager().getAllProblems();
        int totalSolved  = solvedProblems == null ? 0 : solvedProblems.size();
        int totalProblems = allProblems.size();
        float pct = totalProblems == 0 ? 0 : (totalSolved / (float) totalProblems) * 100f;

        // Header
        JLabel title = new JLabel("Your Progress");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(6));

        String username = currentUser == null ? "User" : currentUser.getUserName();
        JLabel sub = new JLabel("Welcome back, " + username + "! Keep it up.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sub.setForeground(TEXT2);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        content.add(sub);
        content.add(Box.createVerticalStrut(20));

        // Overall progress bar
        content.add(buildProgressBar((int) pct));
        content.add(Box.createVerticalStrut(6));

        JPanel statsRow = new JPanel(new BorderLayout());
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel countLabel = new JLabel(totalSolved + " of " + totalProblems + " problems solved");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        countLabel.setForeground(TEXT2);
        JLabel pctLabel = new JLabel(String.format("%.0f%%", pct));
        pctLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pctLabel.setForeground(TEXT2);
        statsRow.add(countLabel, BorderLayout.WEST);
        statsRow.add(pctLabel, BorderLayout.EAST);
        statsRow.setAlignmentX(LEFT_ALIGNMENT);
        content.add(statsRow);
        content.add(Box.createVerticalStrut(28));

        // Category grid
        JLabel gridTitle = new JLabel("BY CATEGORY");
        gridTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        gridTitle.setForeground(TEXT3);
        gridTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(gridTitle);
        content.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);

        for (Category cat : Category.values()) {
            List<Problem> catProbs = DataStore.getInstance().getProblemManager().getProblemByCategory(cat);
            if (catProbs.isEmpty()) continue;
            int catSolved = solvedProblems == null ? 0 :
                    (int) catProbs.stream().filter(p -> solvedProblems.contains(p.getProblemID())).count();
            grid.add(buildCategoryCard(cat, catSolved, catProbs.size()));
        }

        content.add(grid);

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);
    }

    private JPanel buildProgressBar(int pct) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        wrap.setAlignmentX(LEFT_ALIGNMENT);

        JPanel track = new JPanel(null) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 35, 51));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                int w = (int) (getWidth() * pct / 100.0);
                if (w > 0) {
                    GradientPaint gp = new GradientPaint(0, 0, ACCENT, w, 0, new Color(61, 214, 140));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, w, getHeight(), 8, 8);
                }
            }
        };
        track.setPreferredSize(new Dimension(0, 10));
        track.setOpaque(false);
        wrap.add(track, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildCategoryCard(Category cat, int solved, int total) {
        Color catColor = catColor(cat);
        float pct = total == 0 ? 0 : (solved / (float) total) * 100f;

        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(cat.name().replace("_", " "));
        name.setFont(new Font("SansSerif", Font.BOLD, 13));
        name.setForeground(catColor);
        name.setAlignmentX(LEFT_ALIGNMENT);
        card.add(name);
        card.add(Box.createVerticalStrut(8));

        // mini progress bar
        JPanel miniBar = new JPanel(null) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(42, 48, 71));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                int w = (int) (getWidth() * pct / 100.0);
                if (w > 0) {
                    g2.setColor(catColor);
                    g2.fillRoundRect(0, 0, w, getHeight(), 4, 4);
                }
            }
        };
        miniBar.setPreferredSize(new Dimension(0, 6));
        miniBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        miniBar.setOpaque(false);
        miniBar.setAlignmentX(LEFT_ALIGNMENT);
        card.add(miniBar);
        card.add(Box.createVerticalStrut(6));

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel prog = new JLabel(solved + "/" + total + " solved");
        prog.setFont(new Font("SansSerif", Font.PLAIN, 12));
        prog.setForeground(TEXT2);
        JLabel pctLbl = new JLabel(String.format("%.0f%%", pct));
        pctLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pctLbl.setForeground(TEXT2);
        row.add(prog,   BorderLayout.WEST);
        row.add(pctLbl, BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        card.add(row);

        return card;
    }

    private Color catColor(Category c) {
        switch (c) {
            case HELLO_WORLD:         return new Color(79, 142, 247);
            case CONTROL_FLOW:        return new Color(247, 112, 79);
            case DATA_STRUCTURES:     return new Color(247, 200, 79);
            case ALGORITHMS:          return new Color(199, 146, 234);
            case NUMERICAL_METHODS:   return new Color(61, 214, 140);
            case STUDY_METHODS:       return new Color(255, 126, 179);
            case WORKFORCE_SCENARIOS: return new Color(126, 184, 247);
            case PROBLEM_SOLVING:     return new Color(247, 160, 79);
            default:                  return TEXT2;
        }
    }
}
