/*
 * Simian Similarity Analyzer
 * 
 * Copyright (c) 2023 Quandary Peak Research.
 * Original authorship by Simon Harris.
 * 
 * Use of this software is permitted for educational or academic research
 * purposes only and is subject to the Quandary Peak Academic Software License.
 * See docs/license.txt for details.
 * 
 * Redistribution of this software in source or binary form is not permitted.
 * 
 * For non-academic or commercial use, please contact simian@quandarypeak.com.
 */
 
package com.quandarypeak.simian;

/**
 * The join of two consecutive blocks.
 */
final class SuperBlock extends BaseBlock {
    private BaseBlock startChild;
    private BaseBlock endChild;

    public static BaseBlock create(final BaseBlock previous, final BaseBlock startChild, final BaseBlock endChild) {
        final BaseBlock superBlock = new SuperBlock(startChild, endChild, hash(startChild, endChild), previous);

        startChild.setParentWhenStart(superBlock);
        endChild.setParentWhenEnd(superBlock);

        return superBlock;
    }

    private static long hash(final BaseBlock startChild, final BaseBlock endChild) {
        long hash = startChild.getBlockHash();
        BaseBlock child = startChild;
        do {
            child = child.getNext();
            hash = Hasher.hash(hash, child.getBlockHash());
        } while (child != endChild);
        return hash;
    }

    private SuperBlock(final BaseBlock startChild, final BaseBlock endChild, final long hash, final BaseBlock previous) {
        super(startChild.getSourceFile(), startChild.getStartLineNumber(), endChild.getEndLineNumber(), hash, previous);

        this.startChild = startChild;
        this.endChild = endChild;
    }

    @Override
    public void unlink() {
        super.unlink();

        if (startChild != null) {
            startChild.clearParentWhenStart(this);
            startChild = null;
        }

        if (endChild != null) {
            endChild.clearParentWhenEnd(this);
            endChild = null;
        }
    }

    @Override
    protected void clearStartChild(final Block block) {
        if (block != startChild) {
            throw new IllegalArgumentException("Not a child");
        }
        startChild = null;
    }

    @Override
    protected void clearEndChild(final Block block) {
        if (block != endChild) {
            throw new IllegalArgumentException("Not a child");
        }
        endChild = null;
    }
}
