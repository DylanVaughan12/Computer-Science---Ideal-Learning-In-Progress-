package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.model.BlockCategory;
import com.macrosoff.csil.model.enums.BlockType;
import com.macrosoff.csil.data.DataStore;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

// ── Data model for one block on the canvas ────────────────────────────────────
class CanvasBlock {
    private static int nextId = 1;
    final int id;
    int x, y;                        // absolute canvas position
    BlockCategory data;
    String editedCode;
    CanvasBlock parent, child;       // linked list (parent above, child below)

    CanvasBlock(BlockCategory data, String code, int x, int y) {
        this.id = nextId++;
        this.data = data;
        this.editedCode = code;
        this.x = x;
        this.y = y;
    }

    /** All blocks in this chain, starting from this block going downward. */
    List<CanvasBlock> chainDown() {
        List<CanvasBlock> list = new ArrayList<>();
        CanvasBlock cur = this;
        while (cur != null) { list.add(cur); cur = cur.child; }
        return list;
    }

    /** Walk up to the root (top of stack). */
    CanvasBlock root() {
        CanvasBlock cur = this;
        while (cur.parent != null) cur = cur.parent;
        return cur;
    }

    /** Last block in the chain below this one. */
    CanvasBlock tail() {
        CanvasBlock cur = this;
        while (cur.child != null) cur = cur.child;
        return cur;
    }

    boolean isFirst() { return parent == null; }
}

// ── The canvas ────────────────────────────────────────────────────────────────
public class BlockCanvas extends JPanel {

    // ── Layout constants ──────────────────────────────────────────────────────
    static final int BLOCK_W  = 290;   // block width
    static final int BLOCK_H  = 44;    // block body height (top-to-bump-start)
    static final int BUMP_H   = 11;    // connector bump height
    static final int BUMP_W   = 28;    // connector bump width
    static final int BUMP_X   = 20;    // bump x-offset from left edge
    static final int ARC      = 8;     // corner radius
    static final int SNAP_D   = 30;    // snap-to-connect distance (pixels)

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<CanvasBlock> blocks = new ArrayList<>();

    // Drag
    private CanvasBlock dragHead;     // top of the chain being dragged
    private int         offX, offY;  // offset from block (x,y) to mouse press
    private boolean     dragging;

    // Snap preview
    private CanvasBlock snapBelow;   // block that dragHead will snap below

    // Hover
    private CanvasBlock hovered;

    // Callbacks
    Consumer<CanvasBlock> onEdit;    // double-click → edit dialog
    Runnable              onChange;  // called whenever blocks change

    // ── Constructor ───────────────────────────────────────────────────────────

    public BlockCanvas() {
        setLayout(null);
        setBackground(new Color(13, 17, 27));
        setPreferredSize(new Dimension(900, 700));
        setTransferHandler(new CanvasDropHandler());
        wire();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Place a new block on the canvas at (x,y) with the given code text. */
    public void addBlock(BlockCategory data, String code, int x, int y) {
        CanvasBlock cb = new CanvasBlock(data, code, x, y);
        blocks.add(cb);
        expandCanvas();
        repaint();
        if (onChange != null) onChange.run();
    }

    /** Ordered list of all blocks for script submission (root stacks top-to-bottom, chains top-to-bottom). */
    public List<CanvasBlock> orderedBlocks() {
        List<CanvasBlock> roots = new ArrayList<>();
        for (CanvasBlock b : blocks) if (b.parent == null) roots.add(b);
        roots.sort(Comparator.comparingInt(a -> a.y));
        List<CanvasBlock> result = new ArrayList<>();
        for (CanvasBlock root : roots) result.addAll(root.chainDown());
        return result;
    }

    /** Remove all blocks. */
    public void clear() {
        blocks.clear();
        repaint();
        if (onChange != null) onChange.run();
    }

    // ── Mouse wiring ──────────────────────────────────────────────────────────

    private void wire() {
        MouseAdapter ma = new MouseAdapter() {

            @Override public void mousePressed(MouseEvent e) {
                CanvasBlock hit = hitTest(e.getX(), e.getY());
                if (hit == null) return;

                // ── Delete button ──
                if (isDeleteZone(hit, e.getX(), e.getY())) {
                    detach(hit);
                    List<CanvasBlock> chain = hit.chainDown();
                    blocks.removeAll(chain);
                    hovered = null;
                    expandCanvas(); repaint();
                    if (onChange != null) onChange.run();
                    return;
                }

                // ── Start drag ──
                // Sever from parent if connected
                if (hit.parent != null) {
                    hit.parent.child = null;
                    hit.parent = null;
                }

                dragHead = hit;
                offX = e.getX() - hit.x;
                offY = e.getY() - hit.y;
                dragging = true;

                // Bring dragged chain to top z-order
                List<CanvasBlock> chain = hit.chainDown();
                blocks.removeAll(chain);
                blocks.addAll(chain);
                repaint();
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (!dragging || dragHead == null) return;
                int nx = e.getX() - offX;
                int ny = e.getY() - offY;
                int dx = nx - dragHead.x;
                int dy = ny - dragHead.y;
                for (CanvasBlock b : dragHead.chainDown()) { b.x += dx; b.y += dy; }
                snapBelow = findSnapTarget();
                expandCanvas(); repaint();
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (!dragging) return;
                dragging = false;

                if (snapBelow != null && dragHead != null) {
                    // Save existing child of snap target
                    CanvasBlock displaced = snapBelow.child;

                    // Connect dragHead below snapBelow
                    snapBelow.child = dragHead;
                    dragHead.parent = snapBelow;

                    // Restack positions
                    restackFrom(dragHead, snapBelow.x, snapBelow.y + BLOCK_H);

                    // If snapBelow had a child, attach it below the dragged chain's tail
                    if (displaced != null && !dragHead.chainDown().contains(displaced)) {
                        CanvasBlock tail = dragHead.tail();
                        tail.child = displaced;
                        displaced.parent = tail;
                        restackFrom(displaced, tail.x, tail.y + BLOCK_H);
                    }
                    snapBelow = null;
                }

                dragHead = null;
                expandCanvas(); repaint();
                if (onChange != null) onChange.run();
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    CanvasBlock hit = hitTest(e.getX(), e.getY());
                    if (hit != null && !isDeleteZone(hit, e.getX(), e.getY()) && onEdit != null)
                        onEdit.accept(hit);
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                CanvasBlock prev = hovered;
                hovered = hitTest(e.getX(), e.getY());
                if (hovered != prev) repaint();
            }

            @Override public void mouseExited(MouseEvent e) {
                hovered = null; repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    // ── Hit testing ───────────────────────────────────────────────────────────

    private CanvasBlock hitTest(int mx, int my) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            CanvasBlock b = blocks.get(i);
            if (mx >= b.x && mx <= b.x + BLOCK_W &&
                my >= b.y && my <= b.y + BLOCK_H + BUMP_H) {
                // Exclude the notch area (it belongs to the parent visually)
                if (!b.isFirst() && mx >= b.x + BUMP_X && mx <= b.x + BUMP_X + BUMP_W
                        && my >= b.y && my <= b.y + BUMP_H) continue;
                return b;
            }
        }
        return null;
    }

    private boolean isDeleteZone(CanvasBlock b, int mx, int my) {
        int bodyTop = b.y + (b.isFirst() ? 0 : BUMP_H);
        return mx >= b.x + BLOCK_W - 28 && mx <= b.x + BLOCK_W - 8
            && my >= bodyTop + 8 && my <= bodyTop + 24;
    }

    // ── Snap logic ────────────────────────────────────────────────────────────

    private CanvasBlock findSnapTarget() {
        if (dragHead == null) return null;
        Set<CanvasBlock> dragChain = new HashSet<>(dragHead.chainDown());

        // dragHead top notch center
        int headSnapX = dragHead.x + BUMP_X + BUMP_W / 2;
        int headSnapY = dragHead.y;  // top of dragHead

        CanvasBlock best = null;
        double bestDist = SNAP_D;

        for (CanvasBlock b : blocks) {
            if (dragChain.contains(b)) continue;
            // b's bottom snap point (where next block attaches)
            int bx = b.x + BUMP_X + BUMP_W / 2;
            int by = b.y + BLOCK_H;
            double dist = Math.hypot(headSnapX - bx, headSnapY - by);
            if (dist < bestDist) { bestDist = dist; best = b; }
        }
        return best;
    }

    // ── Stack positioning ─────────────────────────────────────────────────────

    private void restackFrom(CanvasBlock head, int x, int y) {
        CanvasBlock cur = head;
        while (cur != null) {
            cur.x = x;
            cur.y = y;
            y += BLOCK_H;
            cur = cur.child;
        }
    }

    private void detach(CanvasBlock b) {
        if (b.parent != null) { b.parent.child = null; b.parent = null; }
        if (b.child  != null) { b.child.parent  = null; b.child  = null; }
    }

    // ── Canvas sizing ─────────────────────────────────────────────────────────

    private void expandCanvas() {
        int w = 900, h = 700;
        for (CanvasBlock b : blocks) {
            w = Math.max(w, b.x + BLOCK_W + 60);
            h = Math.max(h, b.y + BLOCK_H + BUMP_H + 60);
        }
        if (!getPreferredSize().equals(new Dimension(w, h))) {
            setPreferredSize(new Dimension(w, h));
            revalidate();
        }
    }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        // Subtle dot grid
        g2.setColor(new Color(255, 255, 255, 12));
        for (int x = 20; x < getWidth(); x += 28)
            for (int y = 20; y < getHeight(); y += 28)
                g2.fillOval(x, y, 2, 2);

        // Empty hint
        if (blocks.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g2.setColor(new Color(255, 255, 255, 35));
            String msg = "Drag blocks from the palette, or click them to add here";
            int mw = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (getWidth() - mw) / 2, getHeight() / 2);
        }

        // Snap glow
        if (snapBelow != null) {
            g2.setColor(new Color(79, 142, 247, 60));
            g2.fillRoundRect(snapBelow.x - 3, snapBelow.y - 3, BLOCK_W + 6, BLOCK_H + 6, ARC + 4, ARC + 4);
            g2.setColor(new Color(79, 142, 247, 180));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{5, 3}, 0f));
            g2.drawRoundRect(snapBelow.x, snapBelow.y, BLOCK_W, BLOCK_H, ARC, ARC);
            g2.setStroke(new BasicStroke(1f));
        }

        // Determine dragged chain
        Set<CanvasBlock> dragSet = dragHead != null ? new HashSet<>(dragHead.chainDown()) : Collections.emptySet();

        // Render non-dragged blocks (roots first → children, so notch holes show parent bumps)
        List<CanvasBlock> roots = new ArrayList<>();
        for (CanvasBlock b : blocks) if (b.parent == null && !dragSet.contains(b)) roots.add(b);
        roots.sort(Comparator.comparingInt(a -> a.y));
        for (CanvasBlock root : roots) renderChain(g2, root, dragSet);

        // Render dragged chain on top with slight transparency
        if (dragHead != null) {
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            renderChain(g2, dragHead, Collections.emptySet());
            g2.setComposite(prev);
        }

        g2.dispose();
    }

    private void renderChain(Graphics2D g2, CanvasBlock head, Set<CanvasBlock> skip) {
        CanvasBlock cur = head;
        while (cur != null) {
            if (!skip.contains(cur)) renderBlock(g2, cur);
            cur = cur.child;
        }
    }

    private void renderBlock(Graphics2D g2, CanvasBlock b) {
        boolean first   = b.isFirst();
        boolean hov     = (b == hovered && !dragging);
        Color   fill    = hov ? brighten(fillColor(b), 18) : fillColor(b);
        Color   border  = darken(fill, 35);

        // Shape
        Path2D shape = buildShape(b.x, b.y, BLOCK_W, first);
        g2.setColor(fill);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(shape);
        g2.setStroke(new BasicStroke(1f));

        // Compute the y of the visible text area
        int bodyTop = b.y + (first ? 0 : BUMP_H);
        int midY    = bodyTop + (BLOCK_H - (first ? 0 : BUMP_H)) / 2 + 5;

        // Type badge
        String tag     = shortTag(b.data.getBlockType());
        Font   tagFont = new Font("SansSerif", Font.BOLD, 10);
        g2.setFont(tagFont);
        int tagW = g2.getFontMetrics().stringWidth(tag);
        int bx   = b.x + 10;
        int bby  = bodyTop + 8;
        g2.setColor(darken(fill, 50));
        g2.fillRoundRect(bx, bby, tagW + 10, 18, 4, 4);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawString(tag, bx + 5, bby + 13);

        // Code text — white, always readable on the vivid fill
        String code = b.editedCode;
        if (code.length() > 38) code = code.substring(0, 35) + "…";
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.setColor(Color.WHITE);
        g2.drawString(code, b.x + 10 + tagW + 16, midY);

        // Line number (subtle, top-right)
        // (not shown — clutters small blocks)

        // Delete ✕ button — only on hover
        if (hov) {
            int dx = b.x + BLOCK_W - 28;
            int dy = bodyTop + 8;
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(dx, dy, 20, 20, 5, 5);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(255, 90, 70));
            g2.drawString("✕", dx + 4, dy + 14);
        }

        // Drag handle dots
        g2.setColor(new Color(255, 255, 255, 55));
        for (int i = 0; i < 3; i++)
            g2.fillOval(b.x + BLOCK_W - 14, midY - 10 + i * 7, 4, 4);
    }

    // ── Block shape ───────────────────────────────────────────────────────────
    //
    //  FIRST BLOCK (no notch on top):
    //  ╔═════════════════════╗
    //  ║  body               ║
    //  ╚═════╗ bump ╔════════╝
    //        ╚══════╝
    //
    //  NON-FIRST BLOCK (notch on top):
    //       ╔═╗notch╔═╗
    //  ╔════╝       ╚════════╗
    //  ║  body               ║
    //  ╚═════╗ bump ╔════════╝
    //        ╚══════╝
    //
    //  child.y = parent.y + BLOCK_H  (bump and notch overlap perfectly)
    //
    private static Path2D buildShape(int x, int y, int w, boolean isFirst) {
        Path2D p = new Path2D.Float();
        int r = ARC;

        if (isFirst) {
            p.moveTo(x + r, y);
            p.lineTo(x + w - r, y);
            p.quadTo(x + w, y, x + w, y + r);
        } else {
            // Top edge with notch slot
            p.moveTo(x + r, y);
            p.lineTo(x + BUMP_X, y);
            p.lineTo(x + BUMP_X, y + BUMP_H);          // notch left wall ↓
            p.lineTo(x + BUMP_X + BUMP_W, y + BUMP_H); // notch floor →
            p.lineTo(x + BUMP_X + BUMP_W, y);          // notch right wall ↑
            p.lineTo(x + w - r, y);
            p.quadTo(x + w, y, x + w, y + r);
        }

        // Right edge ↓
        p.lineTo(x + w, y + BLOCK_H - r);
        p.quadTo(x + w, y + BLOCK_H, x + w - r, y + BLOCK_H);

        // Bottom edge with bump protruding downward
        p.lineTo(x + BUMP_X + BUMP_W, y + BLOCK_H);
        p.lineTo(x + BUMP_X + BUMP_W, y + BLOCK_H + BUMP_H); // bump right ↓
        p.lineTo(x + BUMP_X,          y + BLOCK_H + BUMP_H); // bump floor ←
        p.lineTo(x + BUMP_X,          y + BLOCK_H);          // bump left ↑
        p.lineTo(x + r, y + BLOCK_H);
        p.quadTo(x, y + BLOCK_H, x, y + BLOCK_H - r);

        // Left edge ↑
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.closePath();
        return p;
    }

    // ── Colours ───────────────────────────────────────────────────────────────

    private static Color fillColor(CanvasBlock b) {
        switch (b.data.getBlockType()) {
            case VARIABLE:   return new Color(128,  72, 196);
            case ARRAY:      return new Color(104,  52, 172);
            case IF:         return new Color( 28,  98, 196);
            case ELSE:       return new Color( 18,  76, 164);
            case FOR_LOOP:   return new Color( 28, 148,  88);
            case WHILE_LOOP: return new Color( 18, 124,  72);
            case FUNCTION:   return new Color(196,  76,  36);
            default:         return new Color( 56,  68,  92);
        }
    }

    private static Color brighten(Color c, int a) {
        return new Color(Math.min(255, c.getRed()+a), Math.min(255, c.getGreen()+a), Math.min(255, c.getBlue()+a));
    }
    private static Color darken(Color c, int a) {
        return new Color(Math.max(0, c.getRed()-a), Math.max(0, c.getGreen()-a), Math.max(0, c.getBlue()-a));
    }

    private static String shortTag(BlockType t) {
        switch (t) {
            case VARIABLE:   return "VAR";
            case ARRAY:      return "ARR";
            case IF:         return "IF";
            case ELSE:       return "ELSE";
            case FOR_LOOP:   return "FOR";
            case WHILE_LOOP: return "WHL";
            case FUNCTION:   return "FN";
            default:         return "BLK";
        }
    }

    // ── Drop handler for palette drags onto canvas ────────────────────────────

    private class CanvasDropHandler extends TransferHandler {
        public boolean canImport(TransferSupport ts) {
            return ts.isDataFlavorSupported(DataFlavor.stringFlavor);
        }
        public boolean importData(TransferSupport ts) {
            try {
                String data = (String) ts.getTransferable().getTransferData(DataFlavor.stringFlavor);
                if (!data.startsWith("PALETTE:")) return false;
                int id = Integer.parseInt(data.substring(8).trim());
                BlockCategory bc = DataStore.getInstance().getAllBlocks().stream()
                        .filter(b -> b.getCodeBlockId() == id).findFirst().orElse(null);
                if (bc == null) return false;
                Point pt = ts.getDropLocation().getDropPoint();
                // Request edit dialog via a deferred Runnable (needs to be triggered from ProblemPanel)
                if (onEdit != null) {
                    // Create a temporary block at drop location; caller will open dialog
                    CanvasBlock tmp = new CanvasBlock(bc, bc.getCodeTemplate(), pt.x, pt.y);
                    // Use onEdit callback with a sentinel to signal "new block from palette"
                    // We encode this by temporarily adding it; ProblemPanel checks editedCode == template
                    blocks.add(tmp);
                    onEdit.accept(tmp);
                }
                return true;
            } catch (Exception ex) { ex.printStackTrace(); return false; }
        }
    }
}
