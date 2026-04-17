/**
 * File:    BlockCategory.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Represents one entry in the code-block palette. Stores the block type,
 *          display label, code template, icon text, and the associated InfoPanel
 *          that explains the block to the user.
 */
package com.macrosoff.csil.model;

import com.macrosoff.csil.model.enums.BlockType;

public class BlockCategory {
    private int codeBlockId;
    private BlockType blockType;
    private String label;
    private String codeTemplate;
    private String icon;
    private InfoPanel infoPanel;

    public BlockCategory(int codeBlockId, BlockType blockType, String label,
                         String codeTemplate, String icon, InfoPanel infoPanel) {
        this.codeBlockId = codeBlockId;
        this.blockType = blockType;
        this.label = label;
        this.codeTemplate = codeTemplate;
        this.icon = icon;
        this.infoPanel = infoPanel;
    }

    public String generateVariable(String dataType) {
        return dataType + " variableName = defaultValue;";
    }

    public String generateArray(String dataType) {
        return dataType + "[] arrayName = new " + dataType + "[size];";
    }

    public String generateIf() {
        return "if (condition) {";
    }

    public String generateElse() {
        return "} else {";
    }

    public String generateForLoop() {
        return "for (int i = 0; i < n; i++) {";
    }

    public String generateWhileLoop() {
        return "while (condition) {";
    }

    public String generateFunction() {
        return "public returnType methodName(params) {";
    }

    public int getCodeBlockId()     { return codeBlockId; }
    public BlockType getBlockType() { return blockType; }
    public String getLabel()        { return label; }
    public String getCodeTemplate() { return codeTemplate; }
    public String getIcon()         { return icon; }
    public InfoPanel getInfoPanel() { return infoPanel; }
}
