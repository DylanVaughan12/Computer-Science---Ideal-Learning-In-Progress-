/**
 * File:    CodeBlock.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.0
 * Purpose: Manages a list of BlockCategory instances assembled by the user. Provides
 *          add, delete, clear, and script-build operations.
 */
package com.macrosoff.csil.model;

import java.util.ArrayList;
import java.util.List;

public class CodeBlock {
    private List<BlockCategory> codeBlocks;
    private int codeBlockCount;
    private int codeBlockId;

    public CodeBlock() {
        this.codeBlocks = new ArrayList<>();
        this.codeBlockCount = 0;
        this.codeBlockId = 0;
    }

    public int getCodeBlockCount() {
        return codeBlocks.size();
    }

    public void addBlock(BlockCategory block) {
        codeBlocks.add(block);
        codeBlockCount++;
    }

    public void deleteCodeBlock(int blockId) {
        codeBlocks.removeIf(b -> b.getCodeBlockId() == blockId);
        codeBlockCount = codeBlocks.size();
    }

    public boolean clearAllCodeBlocks() {
        codeBlocks.clear();
        codeBlockCount = 0;
        return true;
    }

    public List<BlockCategory> getCodeBlocks() {
        return codeBlocks;
    }

    public String buildScript() {
        StringBuilder sb = new StringBuilder();
        for (BlockCategory b : codeBlocks) {
            sb.append(b.getCodeTemplate()).append("\n");
        }
        return sb.toString();
    }
}
