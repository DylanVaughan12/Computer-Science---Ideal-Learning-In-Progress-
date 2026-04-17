/**
 * File:    ScratchCanvas.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Custom-painted free-form canvas where Scratch-style interlocking blocks
 *          can be dragged, snapped, split, and reordered. Supports Ctrl+Z undo
 *          and Delete-key removal.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.model.BlockCategory;
import com.macrosoff.csil.model.ScriptLine;
import com.macrosoff.csil.model.enums.BlockType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A free-form canvas where Scratch-style blocks can be:
 *  - dragged around individually or as a connected group
 *  - snapped together when released near a connection point
 *  - split apart by grabbing from the middle of a stack
 *
 * Block anatomy (top → bottom):
 *
 *   ┌─────────────────────────────────┐  ← body top (notch cut in if not first block)
 *   │  [TAG]  user code text     ✎ ✕ │  ← body content
 *   │                 ╔═╗            │  ← bump tab protruding from body bottom
 *   └─────────────────────────────────┘
 *
 * The bump of block N fits into the notch cut at the top of block N+1,
 * giving the characteristic interlocking Scratch appearance.
 */
public class ScratchCanvas extends JPanel {

    // ── Geometry ─────────────────────────────────────────────────────────────
    public  static final int BLOCK_W   = 240;
    public  static final int BLOCK_H   = 50;   // body height per slot
    public  static final int BUMP_W    = 30;
    public  static final int BUMP_H    = 10;
    public  static final int BUMP_X    = 20;   // bump left offset inside block
    private static final int ARC       = 7;
    private static final int SNAP_DIST = 28;   // snap threshold (pixels)

    // Icon hit areas (relative to block top-left)
    private static final int EDIT_ICON_X = BLOCK_W - 38;
    private static final int DEL_ICON_X  = BLOCK_W - 20;
    private static final int ICON_Y_OFF  = BLOCK_H / 2 - 9;
    private static final int ICON_W      = 16;
    private static final int ICON_H      = 18;

    // ── Inner class: a connected sequence of blocks at a canvas position ──────
    public static class BlockStack {
        public int x, y;
        public List<ScriptLine> lines = new ArrayList<>();

        public BlockStack(int x, int y) { this.x = x; this.y = y; }

        /** Total visual height including the trailing bump of the last block. */
        public int totalHeight() { return lines.size() * BLOCK_H + BUMP_H; }

        /** y-coordinate of the bottom snap point (below the last block body). */
        public int bottomSnapY() { return y + lines.size() * BLOCK_H; }

        /** x-coordinate of both snap points (aligned to bump centre). */
        public int snapX() { return x + BUMP_X + BUMP_W / 2; }

        /** Which line index (0-based) does a canvas-y coordinate fall in? -1 if outside. */
        public int lineAt(int canvasY) {
            int rel = canvasY - y;
            if (rel < 0 || rel >= totalHeight()) return -1;
            int idx = rel / BLOCK_H;
            return Math.min(idx, lines.size() - 1);
        }

        /** True if the canvas point is inside this stack's bounding box. */
        public boolean contains(int cx, int cy) {
            return cx >= x && cx <= x + BLOCK_W && cy >= y && cy <= y + totalHeight();
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<BlockStack> stacks    = new ArrayList<>();
    private BlockStack              dragging  = null;   // stack currently being dragged
    private int                     dragOffX, dragOffY; // offset from stack origin to mouse
    private BlockStack              snapTarget    = null;  // stack highlighted for snapping
    private boolean                 snapBelow;            // true = drag attaches below target

    /**
     * Undo history — each entry is a deep-copy snapshot of the stacks list.
     * Snapshots are taken before every mutation (add, delete, snap).
     * Ctrl+Z pops the most recent snapshot and restores it.
     *
     * Capped at MAX_UNDO_LEVELS to prevent unbounded memory growth.
     */
    private final Deque<List<BlockStack>> undoStack = new ArrayDeque<>();
    private static final int MAX_UNDO_LEVELS = 30;

    // ── Callbacks set by ProblemPanel ─────────────────────────────────────────
    private BiConsumer<BlockStack, Integer> onEditRequest;   // (stack, lineIdx)
    private BiConsumer<BlockStack, Integer> onDeleteRequest; // (stack, lineIdx)

    // ── Constructor ───────────────────────────────────────────────────────────

    public ScratchCanvas() {
        setBackground(new Color(14, 18, 28));
        setPreferredSize(new Dimension(900, 600));
        setFocusable(true);

        MouseAdapter ma = buildMouseAdapter();
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // Request focus on click so key bindings work even when the user hasn't
        // explicitly tabbed to this panel
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
        });

        // ── Keyboard shortcuts ────────────────────────────────────────────────
        // Ctrl+Z  → undo last canvas mutation
        // Delete  → remove the last block in the topmost stack (convenience)
        //
        // We use WHEN_IN_FOCUSED_WINDOW so the shortcut works as long as any
        // component inside the scroll pane has focus, not just the canvas itself.
        InputMap  im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Ctrl+Z undo
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "undo");
        am.put("undo", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { undo(); }
        });

        // Delete key — removes the block the user last interacted with
        // (tracked via lastClickedStack / lastClickedLine fields below)
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "deleteBlock");
        am.put("deleteBlock", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { deleteLastSelected(); }
        });
    }

    public void setOnEditRequest(BiConsumer<BlockStack, Integer> cb)   { this.onEditRequest   = cb; }
    public void setOnDeleteRequest(BiConsumer<BlockStack, Integer> cb) { this.onDeleteRequest = cb; }

    // ── Undo helpers ──────────────────────────────────────────────────────────

    /**
     * Push a deep-copy snapshot of the current stacks onto the undo stack.
     * Call this BEFORE every mutation so Ctrl+Z can revert to it.
     * Capped at MAX_UNDO_LEVELS to limit memory use.
     */
    private void pushUndo() {
        List<BlockStack> snapshot = new ArrayList<>();
        for (BlockStack st : stacks) {
            BlockStack copy = new BlockStack(st.x, st.y);
            copy.lines.addAll(st.lines);  // ScriptLine is immutable enough for our purposes
            snapshot.add(copy);
        }
        undoStack.push(snapshot);
        if (undoStack.size() > MAX_UNDO_LEVELS) undoStack.removeLast();
    }

    /**
     * Restore the most recent snapshot from the undo stack.
     * Does nothing if there is no history.
     */
    public void undo() {
        if (undoStack.isEmpty()) return;
        List<BlockStack> prev = undoStack.pop();
        stacks.clear();
        stacks.addAll(prev);
        dragging   = null;
        snapTarget = null;
        repaint();
    }

    // ── Delete-key support ────────────────────────────────────────────────────

    /** Most-recently clicked stack and line index — used by the Delete key handler. */
    private BlockStack lastClickedStack = null;
    private int        lastClickedLine  = -1;

    /**
     * Remove the block the user last clicked/selected via the Delete key.
     * Delegates to the onDeleteRequest callback so ProblemPanel handles the
     * actual list mutation (keeping the canvas and model in sync).
     */
    private void deleteLastSelected() {
        if (lastClickedStack != null && lastClickedLine >= 0 && onDeleteRequest != null) {
            pushUndo();
            onDeleteRequest.accept(lastClickedStack, lastClickedLine);
            lastClickedStack = null;
            lastClickedLine  = -1;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Drop a new single-block stack onto the canvas in a sensible position. */
    public void addBlock(ScriptLine sl) {
        pushUndo();  // snapshot before mutating so Ctrl+Z can reverse the add
        // Stack below the lowest existing stack, aligned left
        int x = 28;
        int y = 28;
        for (BlockStack st : stacks) {
            int bottom = st.y + st.totalHeight() + 14;
            if (st.x < x + BLOCK_W && bottom > y) y = bottom;
        }
        BlockStack ns = new BlockStack(x, y);
        ns.lines.add(sl);
        stacks.add(ns);
        repaint();
    }

    /** Return all script lines in reading order (top → bottom, left → right across stacks). */
    public List<ScriptLine> getOrderedScript() {
        List<BlockStack> sorted = new ArrayList<>(stacks);
        sorted.sort(Comparator.comparingInt((BlockStack s) -> s.y).thenComparingInt(s -> s.x));
        List<ScriptLine> result = new ArrayList<>();
        for (BlockStack st : sorted) result.addAll(st.lines);
        return result;
    }

    public void clear() {
        pushUndo();  // allow undoing a full clear
        stacks.clear();
        dragging = null;
        snapTarget = null;
        repaint();
    }

    public List<BlockStack> getStacks() { return stacks; }

    // ── Mouse handling ────────────────────────────────────────────────────────

    private MouseAdapter buildMouseAdapter() {
        long[] pressTime = {0};
        int[]  pressXY   = {0, 0};

        return new MouseAdapter() {

            @Override public void mousePressed(MouseEvent e) {
                pressTime[0] = System.currentTimeMillis();
                pressXY[0]   = e.getX();
                pressXY[1]   = e.getY();
                beginDrag(e.getX(), e.getY());
            }

            @Override public void mouseReleased(MouseEvent e) {
                boolean wasQuickClick = System.currentTimeMillis() - pressTime[0] < 250
                        && Math.abs(e.getX() - pressXY[0]) < 6
                        && Math.abs(e.getY() - pressXY[1]) < 6;

                if (wasQuickClick) {
                    dragging = null;
                    snapTarget = null;
                    handleClick(e.getX(), e.getY(), e.getClickCount());
                } else {
                    finishDrag(e.getX(), e.getY());
                }
                repaint();
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (dragging == null) return;
                dragging.x = e.getX() - dragOffX;
                dragging.y = e.getY() - dragOffY;
                // Keep on canvas
                if (dragging.x < 0) dragging.x = 0;
                if (dragging.y < 0) dragging.y = 0;
                updateSnapPreview();

                // Auto-expand canvas if block dragged near edge
                int needed = Math.max(getPreferredSize().height, dragging.y + dragging.totalHeight() + 60);
                if (needed != getPreferredSize().height) {
                    setPreferredSize(new Dimension(getPreferredSize().width, needed));
                    revalidate();
                }
                repaint();
            }
        };
    }

    private void beginDrag(int mx, int my) {
        dragging = null;
        // Find topmost stack under cursor (iterate in reverse = top of render order)
        for (int i = stacks.size() - 1; i >= 0; i--) {
            BlockStack st = stacks.get(i);
            if (!st.contains(mx, my)) continue;
            int lineIdx = st.lineAt(my);
            if (lineIdx < 0) continue;

            // Snapshot before splitting/moving so Ctrl+Z can undo the drag
            pushUndo();

            if (lineIdx == 0) {
                // Grab the whole stack
                dragging = st;
                stacks.remove(i);
            } else {
                // Split: keep the top portion in the original stack, drag the rest
                List<ScriptLine> top  = new ArrayList<>(st.lines.subList(0, lineIdx));
                List<ScriptLine> tail = new ArrayList<>(st.lines.subList(lineIdx, st.lines.size()));
                st.lines = top;
                dragging = new BlockStack(st.x, st.y + lineIdx * BLOCK_H);
                dragging.lines = tail;
            }
            stacks.add(dragging);          // render last (on top)
            dragOffX = mx - dragging.x;
            dragOffY = my - dragging.y;
            break;
        }
    }

    private void finishDrag(int mx, int my) {
        if (dragging == null) return;

        if (snapTarget != null) {
            performSnap();
        }
        dragging   = null;
        snapTarget = null;
    }

    private void updateSnapPreview() {
        if (dragging == null) { snapTarget = null; return; }
        snapTarget = null;

        for (BlockStack st : stacks) {
            if (st == dragging) continue;

            // Case 1: dragging attaches BELOW st
            int dx1 = Math.abs(dragging.snapX() - st.snapX());
            int dy1 = Math.abs(dragging.y - st.bottomSnapY());
            if (dx1 < SNAP_DIST && dy1 < SNAP_DIST) {
                snapTarget = st;
                snapBelow  = true;
                return;
            }

            // Case 2: dragging attaches ABOVE st (dragging's bottom → st's top)
            int dx2 = Math.abs(dragging.snapX() - st.snapX());
            int dy2 = Math.abs(dragging.bottomSnapY() - st.y);
            if (dx2 < SNAP_DIST && dy2 < SNAP_DIST) {
                snapTarget = st;
                snapBelow  = false;
                return;
            }
        }
    }

    private void performSnap() {
        if (dragging == null || snapTarget == null) return;

        if (snapBelow) {
            // Merge dragging onto the bottom of snapTarget
            dragging.x = snapTarget.x;
            dragging.y = snapTarget.y + snapTarget.lines.size() * BLOCK_H;
            snapTarget.lines.addAll(dragging.lines);
            stacks.remove(dragging);
        } else {
            // Merge snapTarget onto the bottom of dragging
            List<ScriptLine> combined = new ArrayList<>(dragging.lines);
            combined.addAll(snapTarget.lines);
            dragging.lines = combined;
            stacks.remove(snapTarget);
        }
    }

    private void handleClick(int mx, int my, int clickCount) {
        for (int i = stacks.size() - 1; i >= 0; i--) {
            BlockStack st = stacks.get(i);
            if (!st.contains(mx, my)) continue;
            int lineIdx = st.lineAt(my);
            if (lineIdx < 0) continue;

            // Track for Delete-key support
            lastClickedStack = st;
            lastClickedLine  = lineIdx;

            int bx  = st.x;
            int by  = st.y + lineIdx * BLOCK_H;
            int relX = mx - bx;
            int relY = my - by;

            // Delete icon
            if (relX >= DEL_ICON_X && relX <= DEL_ICON_X + ICON_W
                    && relY >= ICON_Y_OFF && relY <= ICON_Y_OFF + ICON_H) {
                if (onDeleteRequest != null) onDeleteRequest.accept(st, lineIdx);
                return;
            }

            // Edit icon (or double-click anywhere)
            boolean onEditIcon = relX >= EDIT_ICON_X && relX <= EDIT_ICON_X + ICON_W
                    && relY >= ICON_Y_OFF && relY <= ICON_Y_OFF + ICON_H;
            if (onEditIcon || clickCount >= 2) {
                if (onEditRequest != null) onEditRequest.accept(st, lineIdx);
                return;
            }
            break;
        }
    }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);

        // Empty-canvas hint — disappears once any block is placed
        if (stacks.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.ITALIC, 13));
            g2.setColor(new Color(255, 255, 255, 35));
            String msg = "Click a block in the palette, or drag one here  •  Ctrl+Z to undo";
            int mw = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (getWidth() - mw) / 2, getHeight() / 2);
        }

        // Draw snap highlight first (below all blocks)
        if (snapTarget != null) drawSnapHighlight(g2, snapTarget);

        // Draw all stacks except the one being dragged
        for (BlockStack st : stacks) {
            if (st != dragging) paintStack(g2, st, false);
        }

        // Dragging stack on top, semi-transparent
        if (dragging != null) paintStack(g2, dragging, true);

        g2.dispose();
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 12));
        for (int gx = 20; gx < getWidth(); gx += 24) {
            for (int gy = 20; gy < getHeight(); gy += 24) {
                g2.fillOval(gx - 1, gy - 1, 2, 2);
            }
        }
    }

    private void drawSnapHighlight(Graphics2D g2, BlockStack st) {
        // Glow around the snap target
        g2.setColor(new Color(79, 142, 247, 60));
        g2.fillRoundRect(st.x - 4, st.y - 4, BLOCK_W + 8, st.totalHeight() + 8, 12, 12);
        g2.setColor(new Color(79, 142, 247, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(st.x - 4, st.y - 4, BLOCK_W + 8, st.totalHeight() + 8, 12, 12);
        g2.setStroke(new BasicStroke(1f));
    }

    private void paintStack(Graphics2D g2, BlockStack st, boolean ghost) {
        for (int i = 0; i < st.lines.size(); i++) {
            paintBlock(g2, st, i, ghost);
        }
    }

    private void paintBlock(Graphics2D g2, BlockStack st, int idx, boolean ghost) {
        ScriptLine sl = st.lines.get(idx);
        int bx = st.x;
        int by = st.y + idx * BLOCK_H;
        boolean isFirst = (idx == 0);

        Color fill   = blockFill(sl.getBlock().getBlockType());
        Color border  = darken(fill, 35);

        if (ghost) {
            fill   = withAlpha(fill,   180);
            border = withAlpha(border, 180);
        }

        // Drop shadow (non-ghost only)
        if (!ghost) {
            Shape shadowShape = buildBlockPath(bx + 2, by + 3, isFirst);
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fill(shadowShape);
        }

        // Block body
        Shape blockShape = buildBlockPath(bx, by, isFirst);
        g2.setColor(fill);
        g2.fill(blockShape);

        // Inner highlight (top-edge lighter strip for depth)
        if (!ghost) {
            g2.setColor(withAlpha(Color.WHITE, 35));
            g2.setClip(blockShape);
            g2.fillRect(bx, by, BLOCK_W, 6);
            g2.setClip(null);
        }

        // Border
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(blockShape);
        g2.setStroke(new BasicStroke(1f));

        // ── C-shaped left rail for container blocks ───────────────────────────
        // IF, FOR_LOOP, WHILE_LOOP, and FUNCTION blocks are "container" types —
        // they logically wrap the blocks below them.  We draw a coloured vertical
        // rail on the left side of every block that follows in the same stack
        // until the stack ends (or until a closing-brace / else block is found).
        //
        // This gives a visual indent cue just like Scratch's C-shaped blocks,
        // without requiring a separate C-block component.
        if (isContainerType(sl.getBlock().getBlockType()) && idx < st.lines.size() - 1) {
            // Draw the rail starting from this block's bottom down to the last block
            Color railColor = withAlpha(fill, ghost ? 80 : 120);
            int railX  = bx + 6;
            int railY  = by + BLOCK_H + BUMP_H;
            int railH  = (st.lines.size() - idx - 1) * BLOCK_H;
            g2.setColor(railColor);
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(railX, railY, railX, railY + railH - BUMP_H);
            g2.setStroke(new BasicStroke(1f));
        }

        paintBlockContent(g2, sl, bx, by, fill);
    }

    /**
     * Returns true for block types that visually "contain" the blocks below them.
     * Used to decide whether to draw the C-shaped left rail.
     */
    private boolean isContainerType(com.macrosoff.csil.model.enums.BlockType t) {
        switch (t) {
            case IF:
            case FOR_LOOP:
            case WHILE_LOOP:
            case FUNCTION:
                return true;
            default:
                return false;
        }
    }

    private void paintBlockContent(Graphics2D g2, ScriptLine sl, int bx, int by, Color fill) {
        int midY = by + BLOCK_H / 2;

        // Line number (faint)
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        g2.setColor(withAlpha(Color.WHITE, 60));
        g2.drawString(String.valueOf(sl.hashCode() % 100), bx + 5, midY + 4); // placeholder shown below

        // Type tag pill
        String tag   = shortTag(sl.getBlock().getBlockType());
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(tag);
        int tagX = bx + 10;
        int tagY = midY - 10;
        g2.setColor(darken(fill, 50));
        g2.fillRoundRect(tagX, tagY, tw + 10, 18, 6, 6);
        g2.setColor(Color.WHITE);
        g2.drawString(tag, tagX + 5, midY + 4);

        // Code text — white, always readable
        String code = sl.getEditedCode();
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        fm = g2.getFontMetrics();
        int maxW = BLOCK_W - (tw + 10 + 18) - 52; // space left before icons
        while (code.length() > 4 && fm.stringWidth(code) > maxW) {
            code = code.substring(0, code.length() - 2) + "…";
        }
        g2.setColor(Color.WHITE);
        g2.drawString(code, tagX + tw + 16, midY + 4);

        // Edit icon
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.setColor(withAlpha(Color.WHITE, 140));
        g2.drawString("✎", bx + EDIT_ICON_X, midY + 5);

        // Delete icon
        g2.setColor(withAlpha(Color.WHITE, 140));
        g2.drawString("✕", bx + DEL_ICON_X, midY + 5);
    }

    /**
     * Build the Scratch-style block outline path.
     *
     * Every block has a bump protruding from its bottom.
     * Every block except the first in a stack has a notch cut into its top
     * (which is exactly filled by the bump of the preceding block).
     */
    private Shape buildBlockPath(int bx, int by, boolean noNotch) {
        Path2D p = new Path2D.Float();

        // ── Top edge ──────────────────────────────────────────────────────────
        p.moveTo(bx + ARC, by);

        if (!noNotch) {
            // Notch slot cut into top
            p.lineTo(bx + BUMP_X,          by);
            p.lineTo(bx + BUMP_X,          by + BUMP_H);
            p.lineTo(bx + BUMP_X + BUMP_W, by + BUMP_H);
            p.lineTo(bx + BUMP_X + BUMP_W, by);
        }

        // Top-right corner
        p.lineTo(bx + BLOCK_W - ARC, by);
        p.quadTo(bx + BLOCK_W, by, bx + BLOCK_W, by + ARC);

        // ── Right edge → bottom-right corner ─────────────────────────────────
        p.lineTo(bx + BLOCK_W, by + BLOCK_H - ARC);
        p.quadTo(bx + BLOCK_W, by + BLOCK_H, bx + BLOCK_W - ARC, by + BLOCK_H);

        // ── Bottom edge with outward bump ─────────────────────────────────────
        p.lineTo(bx + BUMP_X + BUMP_W, by + BLOCK_H);
        p.lineTo(bx + BUMP_X + BUMP_W, by + BLOCK_H + BUMP_H);
        p.lineTo(bx + BUMP_X,          by + BLOCK_H + BUMP_H);
        p.lineTo(bx + BUMP_X,          by + BLOCK_H);

        // Bottom-left corner
        p.lineTo(bx + ARC, by + BLOCK_H);
        p.quadTo(bx, by + BLOCK_H, bx, by + BLOCK_H - ARC);

        // ── Left edge back to start ───────────────────────────────────────────
        p.lineTo(bx, by + ARC);
        p.quadTo(bx, by, bx + ARC, by);

        p.closePath();
        return p;
    }

    // ── Colour helpers ────────────────────────────────────────────────────────

    private Color blockFill(BlockType t) {
        switch (t) {
            case VARIABLE:               return new Color(130,  75, 200);
            case ARRAY:                  return new Color(108,  55, 175);
            case IF:                     return new Color( 28,  98, 205);
            case ELSE:                   return new Color( 18,  75, 175);
            case FOR_LOOP:               return new Color( 25, 148,  88);
            case WHILE_LOOP:             return new Color( 15, 125,  72);
            case FUNCTION:               return new Color(200,  78,  38);
            default:                     return new Color( 55,  68,  90);
        }
    }

    private Color darken(Color c, int a) {
        return new Color(Math.max(0, c.getRed()   - a),
                         Math.max(0, c.getGreen() - a),
                         Math.max(0, c.getBlue()  - a),
                         c.getAlpha());
    }

    private Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
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
