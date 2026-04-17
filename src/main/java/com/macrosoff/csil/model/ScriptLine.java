/**
 * File:    ScriptLine.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Pairs a BlockCategory (the palette block providing type and colour
 *          information) with the user-edited Java code string for that line.
 *          Stored inside ScratchCanvas.BlockStack instances to represent the
 *          user's assembled script.
 */
package com.macrosoff.csil.model;

/**
 * ScriptLine
 * Immutable with respect to its BlockCategory, but allows the editedCode
 * string to be updated when the user re-opens the BlockEditDialog.
 */
public class ScriptLine {

    // The palette block that was used to create this line (provides type, colour, template)
    private final BlockCategory sourceBlock;

    // The Java code string the user typed or accepted for this line
    private String editedCode;

    /**
     * Constructs a ScriptLine.
     *
     * @param sourceBlock the palette block — must not be null
     * @param editedCode  the user's code text — stored as empty string if null
     */
    public ScriptLine(BlockCategory sourceBlock, String editedCode) {
        // Guard: a ScriptLine without a block has no type or colour information
        if (sourceBlock != null) {
            this.sourceBlock = sourceBlock;
        } else {
            // This should never occur in normal flow; fail loudly during development
            throw new IllegalArgumentException("sourceBlock must not be null");
        }

        // Guard against null code text
        if (editedCode != null) {
            this.editedCode = editedCode;
        } else {
            this.editedCode = "";
        }
    }

    /**
     * Returns the palette BlockCategory that this line originates from.
     */
    public BlockCategory getBlock() {
        return sourceBlock;
    }

    /**
     * Returns the user-edited code string for this line.
     */
    public String getEditedCode() {
        return editedCode;
    }

    /**
     * Updates the code string after the user re-edits it via BlockEditDialog.
     *
     * @param updatedCode the new code text; ignored if null
     */
    public void setEditedCode(String updatedCode) {
        if (updatedCode != null) {
            this.editedCode = updatedCode;
        } else {
            this.editedCode = "";
        }
    }
}
