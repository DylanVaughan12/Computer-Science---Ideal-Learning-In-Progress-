/**
 * File:    BlockEditDialog.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Modal dialog shown when the user adds or edits a code block. Displays
 *          a structured form for VARIABLE and ARRAY blocks and a free-text
 *          editor for all other block types.
 */
package com.macrosoff.csil.ui.panels;

import com.macrosoff.csil.model.BlockCategory;
import com.macrosoff.csil.model.enums.BlockType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Edit dialog for a block.
 *
 * VARIABLE blocks  → type dropdown + name + initial value + live preview
 * ARRAY blocks     → type dropdown + name + size + live preview
 * All other blocks → free-text editor pre-filled with the template
 */
public class BlockEditDialog extends JDialog {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG      = new Color(22, 26, 36);
    private static final Color BG2     = new Color(30, 35, 51);
    private static final Color BG3     = new Color(37, 43, 61);
    private static final Color ACCENT  = new Color(79, 142, 247);
    private static final Color ACCENT3 = new Color(247, 200, 79);
    private static final Color TEXT    = new Color(232, 236, 244);
    private static final Color TEXT2   = new Color(139, 147, 167);
    private static final Color TEXT3   = new Color(84, 93, 114);
    private static final Color BORDER  = new Color(42, 48, 71);

    private static final Map<BlockType, Color> TYPE_COLOR = new LinkedHashMap<>();
    static {
        TYPE_COLOR.put(BlockType.VARIABLE,   new Color(199, 146, 234));
        TYPE_COLOR.put(BlockType.ARRAY,      new Color(199, 146, 234));
        TYPE_COLOR.put(BlockType.IF,         new Color(79,  142, 247));
        TYPE_COLOR.put(BlockType.ELSE,       new Color(79,  142, 247));
        TYPE_COLOR.put(BlockType.FOR_LOOP,   new Color(61,  214, 140));
        TYPE_COLOR.put(BlockType.WHILE_LOOP, new Color(61,  214, 140));
        TYPE_COLOR.put(BlockType.FUNCTION,   new Color(247, 112, 79));
    }

    private static final Map<String, String> EXAMPLES = new LinkedHashMap<>();
    static {
        EXAMPLES.put("access element",     "nums[0]");
        EXAMPLES.put("if statement",       "if (score > 0) {");
        EXAMPLES.put("else clause",        "} else {");
        EXAMPLES.put("close brace",        "}");
        EXAMPLES.put("for loop",           "for (int i = 0; i < 10; i++) {");
        EXAMPLES.put("while loop",         "while (x > 0) {");
        EXAMPLES.put("define method",      "public static int add(int a, int b) {");
        EXAMPLES.put("return value",       "return result;");
        EXAMPLES.put("System.out.println", "System.out.println(\"Hello, World!\");");
    }

    // ── Shared type options ───────────────────────────────────────────────────
    private static final String[] VAR_TYPES = {"int", "double", "String", "boolean", "char", "long", "float"};

    private static final Map<String, String> VAR_DEFAULTS = new LinkedHashMap<>();
    static {
        VAR_DEFAULTS.put("int",     "0");
        VAR_DEFAULTS.put("double",  "0.0");
        VAR_DEFAULTS.put("String",  "\"\"");
        VAR_DEFAULTS.put("boolean", "false");
        VAR_DEFAULTS.put("char",    "'a'");
        VAR_DEFAULTS.put("long",    "0L");
        VAR_DEFAULTS.put("float",   "0.0f");
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final BlockCategory block;
    private boolean confirmed = false;

    // VARIABLE-mode widgets
    private JComboBox<String> typeCombo;
    private JTextField        nameField;
    private JTextField        valueField;
    private JLabel            previewLabel;

    // ARRAY-mode widgets
    private JComboBox<String> arrTypeCombo;
    private JTextField        arrNameField;
    private JTextField        arrSizeField;
    private JLabel            arrPreviewLabel;

    // Free-text mode
    private JTextArea codeArea;

    // ── Constructor ───────────────────────────────────────────────────────────

    public BlockEditDialog(Frame owner, BlockCategory block, String existingCode) {
        super(owner, "Edit Block — " + block.getLabel(), true);
        this.block = block;
        switch (block.getBlockType()) {
            case VARIABLE: buildVariableUI(existingCode); break;
            case ARRAY:    buildArrayUI(existingCode);    break;
            default:       buildFreeTextUI(existingCode); break;
        }
        pack();
        setMinimumSize(new Dimension(440, 260));
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    // ── VARIABLE UI ───────────────────────────────────────────────────────────

    private void buildVariableUI(String existingCode) {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        getRootPane().setBorder(new EmptyBorder(18, 22, 18, 22));
        add(makeHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        String[] parsed  = parseVarCode(existingCode);
        String initType  = parsed[0];
        String initName  = parsed[1];
        String initValue = parsed[2];

        // Type
        body.add(fieldLabel("Data Type"));
        body.add(Box.createVerticalStrut(5));
        typeCombo = styledCombo(VAR_TYPES, initType);
        body.add(typeCombo);
        body.add(Box.createVerticalStrut(12));

        // Name
        body.add(fieldLabel("Variable Name"));
        body.add(Box.createVerticalStrut(5));
        nameField = styledField(initName.isEmpty() ? "myVariable" : initName);
        body.add(nameField);
        body.add(Box.createVerticalStrut(12));

        // Value
        body.add(fieldLabel("Initial Value"));
        body.add(Box.createVerticalStrut(5));
        valueField = styledField(initValue.isEmpty() ? VAR_DEFAULTS.get(initType) : initValue);
        body.add(valueField);
        body.add(Box.createVerticalStrut(14));

        // Preview
        body.add(fieldLabel("Preview"));
        body.add(Box.createVerticalStrut(5));
        previewLabel = previewLabel(buildVarCode());
        body.add(previewLabel);

        add(body, BorderLayout.CENTER);
        add(makeButtons("Add to Script"), BorderLayout.SOUTH);

        // Wire live updates
        DocumentListener dl = docListener(() -> previewLabel.setText(buildVarCode()));
        nameField .getDocument().addDocumentListener(dl);
        valueField.getDocument().addDocumentListener(dl);
        typeCombo.addActionListener(e -> {
            String t = (String) typeCombo.getSelectedItem();
            // Auto-replace default value when type changes
            for (String dv : VAR_DEFAULTS.values()) {
                if (valueField.getText().trim().equals(dv)) {
                    valueField.setText(VAR_DEFAULTS.getOrDefault(t, "0"));
                    break;
                }
            }
            previewLabel.setText(buildVarCode());
        });
        escapeClose();
    }

    private String[] parseVarCode(String code) {
        if (code == null || code.isBlank()) return new String[]{"int", "", ""};
        code = code.trim().replaceAll(";$", "").trim();
        int eq = code.indexOf('=');
        if (eq > 0) {
            String lhs = code.substring(0, eq).trim();
            String rhs = code.substring(eq + 1).trim();
            String[] lhsParts = lhs.split("\\s+");
            if (lhsParts.length >= 2)
                return new String[]{lhsParts[0], lhsParts[lhsParts.length - 1], rhs};
        }
        return new String[]{"int", "", ""};
    }

    private String buildVarCode() {
        String t = typeCombo  != null ? (String) typeCombo.getSelectedItem() : "int";
        String n = nameField  != null ? nameField.getText().trim()            : "myVar";
        String v = valueField != null ? valueField.getText().trim()           : "0";
        if (n.isEmpty()) n = "myVariable";
        if (v.isEmpty()) v = VAR_DEFAULTS.getOrDefault(t, "0");
        return t + " " + n + " = " + v + ";";
    }

    // ── ARRAY UI ──────────────────────────────────────────────────────────────

    private void buildArrayUI(String existingCode) {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        getRootPane().setBorder(new EmptyBorder(18, 22, 18, 22));
        add(makeHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // Only show the structured UI for "declare array"; "access element" stays free-text
        if (block.getLabel().equals("declare array")) {
            String[] parsed = parseArrayCode(existingCode);
            String initType = parsed[0];
            String initName = parsed[1];
            String initSize = parsed[2];

            // Type
            body.add(fieldLabel("Element Type"));
            body.add(Box.createVerticalStrut(5));
            arrTypeCombo = styledCombo(VAR_TYPES, initType);
            body.add(arrTypeCombo);
            body.add(Box.createVerticalStrut(12));

            // Name
            body.add(fieldLabel("Array Name"));
            body.add(Box.createVerticalStrut(5));
            arrNameField = styledField(initName.isEmpty() ? "myArray" : initName);
            body.add(arrNameField);
            body.add(Box.createVerticalStrut(12));

            // Size
            body.add(fieldLabel("Size  (number of elements)"));
            body.add(Box.createVerticalStrut(5));
            arrSizeField = styledField(initSize.isEmpty() ? "10" : initSize);
            body.add(arrSizeField);
            body.add(Box.createVerticalStrut(14));

            // Preview
            body.add(fieldLabel("Preview"));
            body.add(Box.createVerticalStrut(5));
            arrPreviewLabel = previewLabel(buildArrayCode());
            body.add(arrPreviewLabel);

            add(body, BorderLayout.CENTER);
            add(makeButtons("Add to Script"), BorderLayout.SOUTH);

            // Wire live updates
            DocumentListener dl = docListener(() -> arrPreviewLabel.setText(buildArrayCode()));
            arrNameField.getDocument().addDocumentListener(dl);
            arrSizeField.getDocument().addDocumentListener(dl);
            arrTypeCombo.addActionListener(e -> arrPreviewLabel.setText(buildArrayCode()));

        } else {
            // "access element" — free-text is fine
            add(body, BorderLayout.CENTER);
            buildFreeTextBody(body, existingCode);
            add(makeButtons("Add to Script"), BorderLayout.SOUTH);
        }
        escapeClose();
    }

    /** Parse "int[] myArr = new int[10];" → ["int","myArr","10"]. */
    private String[] parseArrayCode(String code) {
        if (code == null || code.isBlank()) return new String[]{"int", "", "10"};
        code = code.trim().replaceAll(";$", "").trim();
        // e.g. "int[] myArr = new int[10]"
        try {
            // Extract size from [N]
            int lb = code.lastIndexOf('[');
            int rb = code.lastIndexOf(']');
            String size = (lb >= 0 && rb > lb) ? code.substring(lb + 1, rb).trim() : "10";

            // Extract type from the LHS before the first '['
            String lhs = code.substring(0, code.indexOf('[')).trim();  // "int[] myArr = new int"
            // Name is the token before '='
            int eq = lhs.indexOf('=');
            String beforeEq = (eq > 0 ? lhs.substring(0, eq) : lhs).trim();
            String[] parts = beforeEq.split("\\s+");
            // parts[0] is "int[]" or "String[]" etc.
            String rawType = parts[0].replace("[]", "").trim();
            String name    = parts.length >= 2 ? parts[1].trim() : "myArray";
            return new String[]{rawType, name, size};
        } catch (Exception ex) {
            return new String[]{"int", "", "10"};
        }
    }

    private String buildArrayCode() {
        String t = arrTypeCombo != null ? (String) arrTypeCombo.getSelectedItem() : "int";
        String n = arrNameField != null ? arrNameField.getText().trim()            : "myArray";
        String s = arrSizeField != null ? arrSizeField.getText().trim()            : "10";
        if (n.isEmpty()) n = "myArray";
        if (s.isEmpty()) s = "10";
        return t + "[] " + n + " = new " + t + "[" + s + "];";
    }

    // ── FREE-TEXT UI ──────────────────────────────────────────────────────────

    private void buildFreeTextUI(String existingCode) {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        getRootPane().setBorder(new EmptyBorder(18, 22, 18, 22));
        add(makeHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        buildFreeTextBody(body, existingCode);

        add(body, BorderLayout.CENTER);
        add(makeButtons("Add to Script"), BorderLayout.SOUTH);
        escapeClose();
    }

    /** Shared body builder used by both buildFreeTextUI and buildArrayUI's fallback. */
    private void buildFreeTextBody(JPanel body, String existingCode) {
        // Template reminder
        JLabel tmpl = new JLabel("Template:  " + block.getCodeTemplate());
        tmpl.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tmpl.setForeground(TEXT3);
        tmpl.setAlignmentX(LEFT_ALIGNMENT);
        tmpl.setBorder(new EmptyBorder(0, 0, 10, 0));
        body.add(tmpl);

        body.add(fieldLabel("Your Code  (Ctrl+Enter to confirm)"));
        body.add(Box.createVerticalStrut(5));

        codeArea = new JTextArea(existingCode, 4, 36);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        codeArea.setForeground(TEXT);
        codeArea.setBackground(BG2);
        codeArea.setCaretColor(ACCENT);
        codeArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        codeArea.setLineWrap(true);
        codeArea.setWrapStyleWord(true);
        codeArea.setSelectedTextColor(Color.WHITE);
        codeArea.setSelectionColor(new Color(79, 142, 247, 120));
        JScrollPane scroll = new JScrollPane(codeArea);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        body.add(scroll);

        // Example row
        String example = EXAMPLES.get(block.getLabel());
        if (example != null) {
            body.add(Box.createVerticalStrut(10));
            JPanel exRow = new JPanel(new BorderLayout(8, 0));
            exRow.setBackground(BG3);
            exRow.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(7, 12, 7, 12)));
            exRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            exRow.setAlignmentX(LEFT_ALIGNMENT);

            JLabel exLbl = new JLabel("e.g.  " + example);
            exLbl.setFont(new Font("Monospaced", Font.PLAIN, 12));
            exLbl.setForeground(ACCENT3);

            JButton useBtn = new JButton("Use");
            useBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            useBtn.setForeground(TEXT2);
            useBtn.setBackground(BG3);
            useBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(2, 10, 2, 10)));
            useBtn.setFocusPainted(false);
            useBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            useBtn.addActionListener(e -> { codeArea.setText(example); codeArea.requestFocus(); });

            exRow.add(exLbl,  BorderLayout.CENTER);
            exRow.add(useBtn, BorderLayout.EAST);
            body.add(exRow);
        }

        codeArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) confirm();
            }
        });
        SwingUtilities.invokeLater(() -> codeArea.requestFocusInWindow());
    }

    // ── Shared widget factories ───────────────────────────────────────────────

    private JPanel makeHeader() {
        Color tc = TYPE_COLOR.getOrDefault(block.getBlockType(), TEXT2);
        JPanel h = new JPanel();
        h.setOpaque(false);
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(block.getLabel());
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(tc);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel(block.getBlockType().name().replace("_", " ").toLowerCase() + " block");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sub.setForeground(TEXT3);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        h.add(title);
        h.add(Box.createVerticalStrut(3));
        h.add(sub);
        h.add(Box.createVerticalStrut(12));
        h.add(sep);
        h.add(Box.createVerticalStrut(14));
        return h;
    }

    private JPanel makeButtons(String confirmText) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(14, 0, 0, 0));

        JButton cancel  = btn("Cancel",      BG2,    TEXT2);
        JButton confirm = btn(confirmText,   ACCENT, Color.WHITE);
        cancel .addActionListener(e -> { confirmed = false; dispose(); });
        confirm.addActionListener(e -> confirm());
        row.add(cancel);
        row.add(confirm);
        return row;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("SansSerif", Font.PLAIN, 10));
        l.setForeground(TEXT3);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField styledField(String text) {
        JTextField f = new JTextField(text);
        f.setFont(new Font("Monospaced", Font.PLAIN, 13));
        f.setForeground(TEXT);
        f.setBackground(BG2);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(7, 10, 7, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.addActionListener(e -> confirm());
        return f;
    }

    private JComboBox<String> styledCombo(String[] items, String selected) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(new Font("Monospaced", Font.PLAIN, 13));
        c.setBackground(BG2);
        c.setForeground(TEXT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        c.setAlignmentX(LEFT_ALIGNMENT);
        // Pre-select matching item
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(selected)) { c.setSelectedIndex(i); break; }
        }
        return c;
    }

    private JLabel previewLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 13));
        l.setForeground(ACCENT3);
        l.setOpaque(true);
        l.setBackground(BG2);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(8, 12, 8, 12)));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JButton btn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(7, 18, 7, 18)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private DocumentListener docListener(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { r.run(); }
            public void removeUpdate(DocumentEvent e)  { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }

    private void escapeClose() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        getRootPane().getActionMap().put("esc", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { confirmed = false; dispose(); }
        });
    }

    private void confirm() {
        if (!getResult().isEmpty()) { confirmed = true; dispose(); }
    }

    // ── Result ────────────────────────────────────────────────────────────────

    public boolean isConfirmed() { return confirmed; }

    public String getResult() {
        switch (block.getBlockType()) {
            case VARIABLE:
                return buildVarCode();
            case ARRAY:
                if (block.getLabel().equals("declare array")) return buildArrayCode();
                return codeArea != null ? codeArea.getText().trim() : "";
            default:
                return codeArea != null ? codeArea.getText().trim() : "";
        }
    }
}
