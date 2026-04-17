/**
 * File:    ProblemPanel.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: The main coding workspace: sidebar of problems, ScratchCanvas,
 *          block palette, action bar, and inline ResultPanel.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.data.CanvasCheckpoint;
import com.macrosoff.csil.data.DataStore;
import com.macrosoff.csil.model.*;
import com.macrosoff.csil.model.enums.*;
import com.macrosoff.csil.ui.MainWindow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * ProblemPanel
 * ─────────────
 * The main coding workspace shown on the "Problems" tab.  Composed of:
 *
 *   LEFT  — problem sidebar (sorted list, difficulty badges, solved ticks)
 *   CENTER — problem header + ScratchCanvas (the block coding area) + action bar
 *   RIGHT  — block palette (one entry per BlockCategory)
 *   FAR RIGHT — ResultPanel (hidden until Submit is pressed)
 *
 * One ScratchCanvas is created per problem and kept alive in a CardLayout so
 * the user's work-in-progress isn't lost when they switch between problems.
 * Canvas state is also saved to disk on every submit and problem switch via
 * CanvasCheckpoint, so it survives application restarts.
 *
 * Submission flow:
 *   1. Collect ordered script lines from the active ScratchCanvas.
 *   2. Build a single String of Java statements.
 *   3. Run each TestCase through AssessmentService (which uses ExecutionEngine
 *      to actually compile + run the code).
 *   4. Show detailed per-test-case results in ResultPanel.
 *   5. If all visible test cases pass: mark the problem solved, persist, refresh.
 *
 * TODO (team): Add a "Run" button that executes the code without grading it,
 *   so students can see output before committing to a submission.
 * TODO (team): Support multi-file problems where the user must implement a
 *   specific method signature (requires changing ExecutionEngine's wrapper).
 */
public class ProblemPanel extends JPanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG      = new Color(15, 17, 23);
    private static final Color BG2     = new Color(22, 26, 36);
    private static final Color BG3     = new Color(30, 35, 51);
    private static final Color ACCENT  = new Color(79, 142, 247);
    private static final Color ACCENT2 = new Color(61, 214, 140);
    private static final Color ACCENT3 = new Color(247, 200, 79);
    private static final Color ACCENT4 = new Color(247, 112, 79);
    private static final Color TEXT    = new Color(232, 236, 244);
    private static final Color TEXT2   = new Color(139, 147, 167);
    private static final Color TEXT3   = new Color(84, 93, 114);
    private static final Color BORDER  = new Color(42, 48, 71);

    // ── State ─────────────────────────────────────────────────────────────────
    private final MainWindow mainWindow;
    private User    currentUser;
    private Problem currentProblem;

    /**
     * One ScratchCanvas per problem, keyed by problemID.
     * Lazy-created the first time a problem is loaded.
     * Persisted to disk on problem switch / submit via CanvasCheckpoint.
     */
    private final Map<Integer, ScratchCanvas> canvases = new HashMap<>();

    /** Problem IDs the current user has solved this session (plus loaded from disk). */
    private final Set<Integer> solvedProblems = new HashSet<>();

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JPanel      problemListPanel;
    private JLabel      probTitleLabel;
    private JLabel      probDescLabel;
    private JPanel           canvasHost;
    private CardLayout       canvasCards;
    private JLabel           resultLabel;       // one-line summary in the action bar
    private ResultPanel      resultPanel;       // detailed per-test-case side panel

    // ── Constructor ───────────────────────────────────────────────────────────

    public ProblemPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setBackground(BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    /**
     * Called by MainWindow after a successful login.
     * Loads the user's saved solved-problem set and opens the first problem.
     */
    public void setUser(User user) {
        this.currentUser = user;
        // Restore saved progress from disk
        solvedProblems.clear();
        solvedProblems.addAll(DataStore.getInstance().getSolved(user.getUserName()));
        List<Problem> visible = DataStore.getInstance().getVisibleProblems();
        if (!visible.isEmpty()) loadProblem(visible.get(0));
        renderProblemList();
    }

    /** Exposed so MainWindow can pass the set to ProgressPanel. */
    public Set<Integer> getSolvedProblems() { return solvedProblems; }

    // ── Build UI ──────────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildSidebar(),     BorderLayout.WEST);
        add(buildCenter(),      BorderLayout.CENTER);
        add(buildBlocksPalette(), BorderLayout.EAST);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private JScrollPane buildSidebar() {
        problemListPanel = new JPanel();
        problemListPanel.setBackground(BG2);
        problemListPanel.setLayout(new BoxLayout(problemListPanel, BoxLayout.Y_AXIS));
        problemListPanel.setBorder(new EmptyBorder(12, 10, 12, 10));

        JLabel lbl = new JLabel("PROBLEMS");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(TEXT3);
        lbl.setBorder(new EmptyBorder(0, 0, 10, 0));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        problemListPanel.add(lbl);

        JScrollPane sp = new JScrollPane(problemListPanel);
        sp.setPreferredSize(new Dimension(200, 0));
        sp.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));
        sp.getViewport().setBackground(BG2);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    private void renderProblemList() {
        // Remove all rows except the heading label (index 0)
        Component[] cs = problemListPanel.getComponents();
        for (int i = cs.length - 1; i >= 1; i--) problemListPanel.remove(cs[i]);

        // Only show problems that aren't hidden by an instructor
        for (Problem p : DataStore.getInstance().getVisibleProblems()) {
            problemListPanel.add(buildProblemItem(p));
            problemListPanel.add(Box.createVerticalStrut(4));
        }
        problemListPanel.revalidate();
        problemListPanel.repaint();
    }

    private JPanel buildProblemItem(Problem p) {
        boolean active = currentProblem != null && currentProblem.getProblemID() == p.getProblemID();
        boolean solved = solvedProblems.contains(p.getProblemID());

        JPanel item = new JPanel();
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setBackground(active ? BG3 : BG2);
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? ACCENT : BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        item.setAlignmentX(LEFT_ALIGNMENT);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel nameLabel = new JLabel((solved ? "✓ " : "") + p.getTitle());
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        nameLabel.setForeground(solved ? ACCENT2 : TEXT);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        Color dc = p.getDifficulty() == DifficultyLevel.BEGINNER  ? ACCENT2
                 : p.getDifficulty() == DifficultyLevel.INTERMEDIATE ? ACCENT3 : ACCENT4;
        JLabel diffLabel = new JLabel(p.getDifficulty().name());
        diffLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        diffLabel.setForeground(dc);
        diffLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel catLabel = new JLabel(p.getCategory().name().replace("_", " "));
        catLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        catLabel.setForeground(TEXT3);
        catLabel.setAlignmentX(LEFT_ALIGNMENT);

        item.add(nameLabel);
        item.add(Box.createVerticalStrut(3));
        item.add(diffLabel);
        item.add(catLabel);

        item.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { loadProblem(p); }
            public void mouseEntered(MouseEvent e) { if (!active) item.setBackground(BG3); }
            public void mouseExited(MouseEvent e)  { if (!active) item.setBackground(BG2); }
        });
        return item;
    }

    // ── Center: header + canvas host + action bar ─────────────────────────────

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);

        // Problem header
        JPanel header = new JPanel();
        header.setBackground(BG2);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(14, 18, 14, 18)));
        probTitleLabel = new JLabel(" ");
        probTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        probTitleLabel.setForeground(TEXT);
        probTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        probDescLabel = new JLabel(" ");
        probDescLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        probDescLabel.setForeground(TEXT2);
        probDescLabel.setAlignmentX(LEFT_ALIGNMENT);
        header.add(probTitleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(probDescLabel);
        center.add(header, BorderLayout.NORTH);

        // Canvas host — CardLayout swaps per-problem canvases in/out
        canvasCards = new CardLayout();
        canvasHost  = new JPanel(canvasCards);
        canvasHost.setBackground(new Color(14, 18, 28));

        // ResultPanel lives to the right of the canvas inside a split pane
        resultPanel = new ResultPanel();

        JPanel canvasArea = new JPanel(new BorderLayout());
        canvasArea.setBackground(new Color(14, 18, 28));
        JScrollPane canvasScroll = new JScrollPane(canvasHost);
        canvasScroll.setBorder(null);
        canvasScroll.getViewport().setBackground(new Color(14, 18, 28));
        canvasArea.add(canvasScroll, BorderLayout.CENTER);
        canvasArea.add(resultPanel,  BorderLayout.EAST);

        center.add(BorderFactory.createCompoundBorder(
                new MatteBorder(8, 8, 8, 8, BG),
                BorderFactory.createLineBorder(BORDER, 1)) != null
                ? wrapInBorder(canvasArea) : canvasArea, BorderLayout.CENTER);

        center.add(buildActionBar(), BorderLayout.SOUTH);
        return center;
    }

    /** Wraps the canvas area in the styled border used throughout the app. */
    private JPanel wrapInBorder(JPanel inner) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(8, 8, 8, 8, BG),
                BorderFactory.createLineBorder(BORDER, 1)));
        wrapper.add(inner, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(BG2);
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));

        JButton sub    = makeButton("Submit",   ACCENT,                   Color.WHITE);
        JButton testRun = makeButton("Test Run", new Color(28, 110, 80),  Color.WHITE);
        JButton clr    = makeButton("Clear",    BG3,                      TEXT2);
        JButton hint   = makeButton("Hint",     BG3,                      TEXT2);
        JButton undoB  = makeButton("Undo",     BG3,                      TEXT2);

        sub    .setToolTipText("Grade your code against all test cases");
        testRun.setToolTipText("Run your code and see the output without submitting");
        clr    .setToolTipText("Remove all blocks from the canvas");
        hint   .setToolTipText("Show a hint for this problem");
        undoB  .setToolTipText("Undo last canvas change  (Ctrl+Z)");

        sub    .addActionListener(e -> submitCode());
        testRun.addActionListener(e -> testRunCode());
        clr    .addActionListener(e -> clearCanvas());
        hint   .addActionListener(e -> showHint());
        undoB  .addActionListener(e -> {
            if (currentProblem != null) getCanvasFor(currentProblem).undo();
        });

        resultLabel = new JLabel(" ");
        resultLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resultLabel.setForeground(TEXT2);
        resultLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        bar.add(sub); bar.add(testRun); bar.add(clr); bar.add(hint); bar.add(undoB); bar.add(resultLabel);
        return bar;
    }

    // ── Block palette ─────────────────────────────────────────────────────────

    private JScrollPane buildBlocksPalette() {
        JPanel panel = new JPanel();
        panel.setBackground(BG2);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(12, 10, 12, 10));

        JLabel title = new JLabel("CODE BLOCKS");
        title.setFont(new Font("SansSerif", Font.BOLD, 10));
        title.setForeground(TEXT3);
        title.setBorder(new EmptyBorder(0, 0, 4, 0));
        title.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(title);

        // Short keyboard hint
        JLabel tip = new JLabel("<html><i>Click to add &amp; edit.<br>" +
                "Double-click block to re-edit.<br>" +
                "Ctrl+Z to undo  ·  Del to remove selected.</i></html>");
        tip.setFont(new Font("SansSerif", Font.PLAIN, 10));
        tip.setForeground(TEXT3);
        tip.setAlignmentX(LEFT_ALIGNMENT);
        tip.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(tip);

        BlockType lastType = null;
        for (BlockCategory block : DataStore.getInstance().getAllBlocks()) {
            if (block.getBlockType() != lastType) {
                if (lastType != null) panel.add(Box.createVerticalStrut(6));
                JLabel cat = new JLabel(block.getBlockType().name().replace("_", " "));
                cat.setFont(new Font("SansSerif", Font.PLAIN, 10));
                cat.setForeground(TEXT3);
                cat.setAlignmentX(LEFT_ALIGNMENT);
                panel.add(cat);
                panel.add(Box.createVerticalStrut(4));
                lastType = block.getBlockType();
            }
            panel.add(buildPaletteEntry(block));
            panel.add(Box.createVerticalStrut(4));
        }

        JScrollPane sp = new JScrollPane(panel);
        sp.setPreferredSize(new Dimension(196, 0));
        sp.setBorder(new MatteBorder(0, 1, 0, 0, BORDER));
        sp.getViewport().setBackground(BG2);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    private JPanel buildPaletteEntry(BlockCategory block) {
        Color fill   = paletteColor(block.getBlockType());
        Color border = darken(fill, 32);

        JPanel item = new JPanel(new BorderLayout(6, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(8, 10, 8, 8));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        item.setAlignmentX(LEFT_ALIGNMENT);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // White-on-vivid is always readable regardless of block colour
        JLabel name = new JLabel(block.getLabel());
        name.setFont(new Font("SansSerif", Font.BOLD, 12));
        name.setForeground(Color.WHITE);

        // ⓘ opens the non-modal InfoPanel reference side-panel
        JLabel info = new JLabel("ⓘ");
        info.setFont(new Font("SansSerif", Font.PLAIN, 13));
        info.setForeground(new Color(255, 255, 255, 150));
        info.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        info.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { e.consume(); showInfoDialog(block); }
        });

        item.add(name, BorderLayout.CENTER);
        item.add(info, BorderLayout.EAST);

        item.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { if (e.getSource() == item) addBlockToCanvas(block); }
        });

        // Drag support — transfers a "PALETTE:{id}" token to the canvas drop handler
        item.setTransferHandler(new TransferHandler("text") {
            protected Transferable createTransferable(JComponent c) {
                return new StringSelection("PALETTE:" + block.getCodeBlockId());
            }
            public int getSourceActions(JComponent c) { return COPY; }
        });
        item.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                ((JComponent) e.getSource()).getTransferHandler()
                        .exportAsDrag((JComponent) e.getSource(), e, TransferHandler.COPY);
            }
        });
        return item;
    }

    // ── Canvas management ─────────────────────────────────────────────────────

    /**
     * Return the ScratchCanvas for a problem, creating it if this is the first visit.
     * On first creation the canvas is loaded from the disk checkpoint (if any).
     */
    private ScratchCanvas getCanvasFor(Problem p) {
        if (!canvases.containsKey(p.getProblemID())) {
            ScratchCanvas canvas = new ScratchCanvas();
            wireCanvasCallbacks(canvas, p);
            canvasHost.add(canvas, String.valueOf(p.getProblemID()));
            canvases.put(p.getProblemID(), canvas);

            // Restore previous work from disk if it exists
            if (currentUser != null) {
                CanvasCheckpoint.load(currentUser.getUserName(), p.getProblemID(),
                        canvas, DataStore.getInstance().getAllBlocks());
            }
        }
        return canvases.get(p.getProblemID());
    }

    /**
     * Wire the edit / delete callbacks on a newly created ScratchCanvas.
     * These lambdas are the bridge between the canvas UI events and the
     * ProblemPanel business logic.
     */
    private void wireCanvasCallbacks(ScratchCanvas canvas, Problem p) {
        // Edit icon / double-click — open BlockEditDialog for the clicked line
        canvas.setOnEditRequest((stack, lineIdx) -> {
            if (lineIdx < 0 || lineIdx >= stack.lines.size()) return;
            ScriptLine sl = stack.lines.get(lineIdx);
            Frame frame = (Frame) SwingUtilities.getWindowAncestor(ProblemPanel.this);
            BlockEditDialog dlg = new BlockEditDialog(frame, sl.getBlock(), sl.getEditedCode());
            dlg.setVisible(true);
            if (dlg.isConfirmed()) {
                sl.setEditedCode(dlg.getResult());
                canvas.repaint();
                }
        });

        // Delete icon or Delete key — remove the line, split the stack if needed
        canvas.setOnDeleteRequest((stack, lineIdx) -> {
            if (lineIdx < 0 || lineIdx >= stack.lines.size()) return;
            if (stack.lines.size() == 1) {
                // Last block in the stack — remove the whole stack
                canvas.getStacks().remove(stack);
            } else if (lineIdx == 0) {
                stack.lines.remove(0);
            } else {
                // Split: leave the top portion, create a new stack for the tail
                List<ScriptLine> tail = new ArrayList<>(
                        stack.lines.subList(lineIdx + 1, stack.lines.size()));
                stack.lines = new ArrayList<>(stack.lines.subList(0, lineIdx));
                if (!tail.isEmpty()) {
                    ScratchCanvas.BlockStack ns = new ScratchCanvas.BlockStack(
                            stack.x + 20, stack.y + (lineIdx + 1) * ScratchCanvas.BLOCK_H);
                    ns.lines = tail;
                    canvas.getStacks().add(ns);
                }
            }
            canvas.repaint();
        });

        // Accept palette drops onto the canvas surface
        canvas.setTransferHandler(new TransferHandler("text") {
            public boolean canImport(TransferSupport ts) {
                return ts.isDataFlavorSupported(DataFlavor.stringFlavor);
            }
            public boolean importData(TransferSupport ts) {
                try {
                    String data = (String) ts.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    if (data.startsWith("PALETTE:")) {
                        int id = Integer.parseInt(data.substring(8).trim());
                        BlockCategory block = DataStore.getInstance().getAllBlocks().stream()
                                .filter(b -> b.getCodeBlockId() == id).findFirst().orElse(null);
                        if (block != null) { addBlockToCanvas(block); return true; }
                    }
                } catch (Exception ignored) {}
                return false;
            }
        });
    }

    /** Open edit dialog then add the resulting block to the current canvas. */
    private void addBlockToCanvas(BlockCategory block) {
        if (currentProblem == null) return;
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        BlockEditDialog dlg = new BlockEditDialog(frame, block, block.getCodeTemplate());
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            getCanvasFor(currentProblem).addBlock(new ScriptLine(block, dlg.getResult()));
        }
    }

    private void clearCanvas() {
        if (currentProblem == null) return;
        getCanvasFor(currentProblem).clear();
        // Delete the checkpoint so the cleared state persists after restart
        if (currentUser != null) {
            CanvasCheckpoint.delete(currentUser.getUserName(), currentProblem.getProblemID());
        }
        resultLabel.setText(" ");
        resultPanel.setVisible(false);
    }

    // ── Problem loading ───────────────────────────────────────────────────────

    /**
     * Public entry point used by MainWindow when the student clicks
     * "Practice This" in LearnPanel. Delegates to the private loadProblem().
     *
     * @param targetProblem the problem to display and load onto the canvas
     */
    public void loadProblemPublic(Problem targetProblem) {
        loadProblem(targetProblem);
    }

    private void loadProblem(Problem p) {
        // Save the current canvas before switching away
        if (currentProblem != null && currentUser != null) {
            ScratchCanvas prev = canvases.get(currentProblem.getProblemID());
            if (prev != null) {
                CanvasCheckpoint.save(currentUser.getUserName(),
                        currentProblem.getProblemID(), prev.getStacks());
            }
        }

        currentProblem = p;
        probTitleLabel.setText("#" + p.getProblemID() + "  " + p.getTitle()
                + "   [" + p.getCategory().name().replace("_", " ") + "]");
        probDescLabel.setText("<html><body style='width:500px'>" + p.getDescription() + "</body></html>");
        resultLabel.setText(" ");
        resultPanel.setVisible(false);

        // Ensure the canvas exists and is loaded from disk, then show it
        getCanvasFor(p);
        canvasCards.show(canvasHost, String.valueOf(p.getProblemID()));
        renderProblemList();
    }

    // ── Submission ────────────────────────────────────────────────────────────

    /**
     * Compiles and runs the user's code without grading it.
     * Shows stdout output in ResultPanel so the student can see what their
     * code actually produces before committing to a graded submission.
     */
    private void testRunCode() {
        if (currentProblem == null || currentUser == null) {
            return;
        }

        ScratchCanvas canvas = getCanvasFor(currentProblem);
        List<ScriptLine> scriptLines = canvas.getOrderedScript();

        if (scriptLines.isEmpty()) {
            showResult(false, "No blocks on the canvas — add some first!");
            return;
        }

        // Build the code string from assembled blocks
        StringBuilder codeBuilder = new StringBuilder();
        for (ScriptLine scriptLine : scriptLines) {
            codeBuilder.append(scriptLine.getEditedCode()).append("\n");
        }
        String userScript = codeBuilder.toString();

        // Run via ExecutionEngine — no test cases, just raw output
        com.macrosoff.csil.service.ExecutionEngine engine =
                new com.macrosoff.csil.service.ExecutionEngine();
        com.macrosoff.csil.service.ExecutionEngine.ExecutionResult runResult =
                engine.run(userScript);

        // Build a synthetic single-row result for the ResultPanel
        // We reuse ResultPanel to show output, but pass a dummy test case
        List<TestCase> dummyTestCases = new ArrayList<>();
        List<String>   actualOutputs  = new ArrayList<>();
        String         errorText      = "";
        boolean        compiledOk     = runResult.compiled;

        if (runResult.compiled && !runResult.timedOut) {
            // Show the raw output as the "actual" column
            TestCase outputTestCase = new TestCase(0, 0, "(test run)", runResult.output, false);
            dummyTestCases.add(outputTestCase);
            actualOutputs.add(runResult.output);
        } else {
            errorText = runResult.errorMsg;
        }

        // Show output in ResultPanel — grade not meaningful here
        resultPanel.show(dummyTestCases, actualOutputs, compiledOk, errorText, 0f);
        showResult(true, "Test run complete — see output in the panel →");
    }

    /**
     * Compile and run the user's assembled script against all test cases,
     * then show detailed results in ResultPanel.
     *
     * Uses AssessmentService → ExecutionEngine for real compilation and
     * execution, replacing the old heuristic token-matching approach.
     */
    private void submitCode() {
        if (currentProblem == null || currentUser == null) return;

        ScratchCanvas canvas = getCanvasFor(currentProblem);
        List<ScriptLine> lines = canvas.getOrderedScript();
        if (lines.isEmpty()) {
            showResult(false, "No blocks on the canvas — add some first!");
            return;
        }

        // Build the script string from the ordered script lines
        StringBuilder sb = new StringBuilder();
        for (ScriptLine sl : lines) sb.append(sl.getEditedCode()).append("\n");
        String script = sb.toString();

        // Disable the submit button while running to prevent double-clicks
        // (In a real app we'd run this on a background thread; left as TODO)
        // TODO (team): Move execution to a SwingWorker to keep the UI responsive

        // Run against all test cases using the real execution engine
        AssessmentService svc = new AssessmentService();
        List<AssessmentService.TestResult> testResults =
                svc.evaluateAll(script, currentProblem);

        // Collect actual outputs and pass/fail flags for ResultPanel
        List<String>  actualOutputs = new ArrayList<>();
        List<TestCase> testCases    = currentProblem.getTestCases();
        boolean compiledOk = true;
        String  errorText  = "";

        for (AssessmentService.TestResult tr : testResults) {
            actualOutputs.add(tr.actualOutput);
            if (!tr.errorMsg.isEmpty()) { compiledOk = false; errorText = tr.errorMsg; }
        }

        // Calculate overall grade
        long passed = testResults.stream().filter(r -> r.passed).count();
        float grade = testCases.isEmpty() ? 0f : (passed * 100f / testCases.size());
        boolean allPassed = (passed == testCases.size()) && !testCases.isEmpty();

        // Show detailed results in the side panel
        resultPanel.show(testCases, actualOutputs, compiledOk, errorText, grade);

        // Update the one-line summary in the action bar
        if (!compiledOk) {
            showResult(false, "Build failed — see results panel");
        } else if (allPassed) {
            showResult(true, String.format("✓  All %d test cases passed!  Grade: %.0f%%",
                    testCases.size(), grade));
            solvedProblems.add(currentProblem.getProblemID());
            DataStore.getInstance().markSolved(currentUser.getUserName(), currentProblem.getProblemID());
            currentUser.getProgress().increaseProgress(
                    100f / DataStore.getInstance().getProblemManager().getAllProblems().size());
            // Save checkpoint of the passing solution
            CanvasCheckpoint.save(currentUser.getUserName(),
                    currentProblem.getProblemID(), canvas.getStacks());
        } else {
            showResult(false, String.format("✕  %d / %d test cases passed — see results panel",
                    passed, testCases.size()));
        }

        renderProblemList();
    }

    private void showHint() {
        if (currentProblem == null) return;
        List<String> hints = currentProblem.getInfoPanel().getHints(SkillLevel.HIGHSCHOOL);
        resultLabel.setForeground(ACCENT3);
        resultLabel.setText("💡 " + (hints.isEmpty() ? "Check the ⓘ on any palette block!" : hints.get(0)));
    }

    /**
     * Open the InfoPanelDialog for a block.
     * The dialog is modal for simplicity; see recommendations for making it
     * a non-modal side panel in a future iteration.
     */
    private void showInfoDialog(BlockCategory block) {
        new InfoPanelDialog((Frame) SwingUtilities.getWindowAncestor(this), block).setVisible(true);
    }

    private void showResult(boolean pass, String msg) {
        resultLabel.setForeground(pass ? ACCENT2 : ACCENT4);
        resultLabel.setText(msg);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setForeground(fg); b.setBackground(bg);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(6, 16, 6, 16)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Vivid, saturated colours so white label text is always readable. */
    private Color paletteColor(BlockType t) {
        switch (t) {
            case VARIABLE:   return new Color(130,  75, 200);
            case ARRAY:      return new Color(108,  55, 175);
            case IF:         return new Color( 28,  98, 205);
            case ELSE:       return new Color( 18,  75, 175);
            case FOR_LOOP:   return new Color( 25, 148,  88);
            case WHILE_LOOP: return new Color( 15, 125,  72);
            case FUNCTION:   return new Color(200,  78,  38);
            default:         return new Color( 55,  68,  90);
        }
    }

    private Color darken(Color c, int a) {
        return new Color(Math.max(0, c.getRed()-a), Math.max(0, c.getGreen()-a), Math.max(0, c.getBlue()-a));
    }
}
