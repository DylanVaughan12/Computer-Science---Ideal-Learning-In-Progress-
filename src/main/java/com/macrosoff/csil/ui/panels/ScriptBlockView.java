package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.model.ScriptLine;
import com.macrosoff.csil.model.enums.BlockType;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Paints a single script line as a Scratch-style interlocking block.
 *
 * Visual anatomy (viewed top→bottom):
 *   ┌──────────────────────────────┐   ← straight top edge (first block)
 *   │  ╔═╗  notch slot cut into top │   ← notch (all except first block)
 *   │  block body with text         │
 *   │           ╔═╗                │   ← bump tab protruding from bottom
 *   └──────────────────────────────┘
 *
 * The notch on top aligns with the bump on the previous block so they
 * appear to lock together.
 */
public class ScriptBlockView extends JPanel {

    // ── Geometry constants ────────────────────────────────────────────────────
    private static final int BODY_HEIGHT  = 46;   // height of main body
    private static final int BUMP_W       = 28;   // width of connector bump
    private static final int BUMP_H       = 10;   // height of connector bump
    private static final int BUMP_X       = 22;   // x offset of bump from left
    private static final int ARC          = 8;    // corner radius
    private static final int TOTAL_HEIGHT = BODY_HEIGHT + BUMP_H; // component height

    // ── State ─────────────────────────────────────────────────────────────────
    private final ScriptLine scriptLine;
    private final int        lineIndex;
    private final boolean    isFirst;   // no notch on top if true
    private       boolean    dragging   = false;
    private       boolean    dropTarget = false;  // show drop indicator above

    // ── Drag callbacks ────────────────────────────────────────────────────────
    public interface DragListener {
        void onDragStart(int fromIndex);
        void onDragOver(int overIndex);
        void onDrop(int fromIndex, int toIndex);
        void onEdit(int index);
        void onDelete(int index);
    }

    private final DragListener dragListener;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ScriptBlockView(ScriptLine sl, int index, boolean isFirst, DragListener dl) {
        this.scriptLine   = sl;
        this.lineIndex    = index;
        this.isFirst      = isFirst;
        this.dragListener = dl;

        setOpaque(false);
        setPreferredSize(new Dimension(Integer.MAX_VALUE, TOTAL_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, TOTAL_HEIGHT));
        setMinimumSize(new Dimension(100, TOTAL_HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Drag to reorder  •  Double-click to edit");
    }

    // ── State setters (called by ProblemPanel) ────────────────────────────────

    public void setDragging(boolean d)   { this.dragging   = d; repaint(); }
    public void setDropTarget(boolean t) { this.dropTarget = t; repaint(); }
    public int  getLineIndex()           { return lineIndex; }
    public ScriptLine getScriptLine()    { return scriptLine; }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();

        // Drop-target indicator: blue line above this block
        if (dropTarget) {
            g2.setColor(new Color(79, 142, 247));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(BUMP_X + BUMP_W + 4, 0, w - 8, 0);
            g2.setStroke(new BasicStroke(1f));
        }

        // Build the block shape
        Shape blockShape = buildBlockShape(w);

        // Fill
        Color fill = dragging ? darken(blockFill()) : blockFill();
        g2.setColor(fill);
        g2.fill(blockShape);

        // Border
        g2.setColor(blockBorder());
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(blockShape);

        // Line number badge
        String numStr = String.valueOf(lineIndex + 1);
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawString(numStr, 10, BODY_HEIGHT / 2 + 4);

        // Type tag
        String tag = shortTag(scriptLine.getBlock().getBlockType());
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(tagColor());
        int tagW = g2.getFontMetrics().stringWidth(tag);
        g2.fillRoundRect(32, 14, tagW + 8, 16, 4, 4);
        g2.setColor(Color.WHITE);
        g2.drawString(tag, 36, 26);

        // Code text — always WHITE for maximum visibility
        String code = scriptLine.getEditedCode();
        if (code.length() > 52) code = code.substring(0, 49) + "…";
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.setColor(Color.WHITE);
        g2.drawString(code, 32 + tagW + 14, BODY_HEIGHT / 2 + 5);

        // Drag handle (three dots on right side)
        g2.setColor(new Color(255, 255, 255, 100));
        int hx = w - 20;
        for (int i = 0; i < 3; i++) {
            g2.fillOval(hx, BODY_HEIGHT / 2 - 8 + i * 7, 4, 4);
        }

        g2.dispose();
    }

    /**
     * Builds the Scratch-style block outline:
     * - Rounded rectangle body
     * - Notch cut into the top (all blocks except the first)
     * - Bump tab protruding from the bottom
     */
    private Shape buildBlockShape(int w) {
        Path2D path = new Path2D.Float();
        int r = ARC;

        // Start top-left corner
        path.moveTo(r, 0);

        if (!isFirst) {
            // Notch slot cut into top edge
            path.lineTo(BUMP_X, 0);
            path.lineTo(BUMP_X, BUMP_H);
            path.lineTo(BUMP_X + BUMP_W, BUMP_H);
            path.lineTo(BUMP_X + BUMP_W, 0);
        }

        // Top-right
        path.lineTo(w - r, 0);
        path.quadTo(w, 0, w, r);

        // Right edge
        path.lineTo(w, BODY_HEIGHT - r);
        path.quadTo(w, BODY_HEIGHT, w - r, BODY_HEIGHT);

        // Bottom edge with bump protruding downward
        path.lineTo(BUMP_X + BUMP_W, BODY_HEIGHT);
        path.lineTo(BUMP_X + BUMP_W, BODY_HEIGHT + BUMP_H);
        path.lineTo(BUMP_X,          BODY_HEIGHT + BUMP_H);
        path.lineTo(BUMP_X,          BODY_HEIGHT);

        // Bottom-left
        path.lineTo(r, BODY_HEIGHT);
        path.quadTo(0, BODY_HEIGHT, 0, BODY_HEIGHT - r);

        // Left edge back to start
        path.lineTo(0, r);
        path.quadTo(0, 0, r, 0);

        path.closePath();
        return path;
    }

    // ── Colour helpers ────────────────────────────────────────────────────────

    private Color blockFill() {
        switch (scriptLine.getBlock().getBlockType()) {
            case VARIABLE:               return new Color(130, 80, 200);  // purple
            case ARRAY:                  return new Color(110, 60, 180);  // deep purple
            case IF:                     return new Color(30, 100, 200);  // blue
            case ELSE:                   return new Color(20,  80, 170);  // darker blue
            case FOR_LOOP:               return new Color(30, 150,  90);  // green
            case WHILE_LOOP:             return new Color(20, 130,  75);  // darker green
            case FUNCTION:               return new Color(200,  80,  40); // orange-red
            default:                     return new Color( 60,  70,  90);
        }
    }

    private Color blockBorder() {
        Color f = blockFill();
        return darken(f);
    }

    private Color tagColor() {
        switch (scriptLine.getBlock().getBlockType()) {
            case VARIABLE: case ARRAY:   return new Color(90,  50, 140);
            case IF: case ELSE:          return new Color(20,  70, 150);
            case FOR_LOOP: case WHILE_LOOP: return new Color(15, 100,  55);
            case FUNCTION:               return new Color(160,  55,  25);
            default:                     return new Color(40,  50,  70);
        }
    }

    private Color darken(Color c) {
        return new Color(Math.max(0, c.getRed()   - 30),
                         Math.max(0, c.getGreen() - 30),
                         Math.max(0, c.getBlue()  - 30));
    }

    private String shortTag(BlockType t) {
        switch (t) {
            case VARIABLE:   return "VAR";
            case ARRAY:      return "ARR";
            case IF:         return "IF";
            case ELSE:       return "ELSE";
            case FOR_LOOP:   return "FOR";
            case WHILE_LOOP: return "WHILE";
            case FUNCTION:   return "FN";
            default:         return "BLK";
        }
    }
}
