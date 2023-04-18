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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * An audit listener that removes overlapping blocks.
 */
final class IgnoreOverlappingBlocksAuditListener extends DecoratorAuditListener {
    private int _lineCount;
    private String _fingerprint;
    private Set<Block> _overlappingBlocks;
    private Set<Block> _nonOverlappingBlocks;

    IgnoreOverlappingBlocksAuditListener(final AuditListener listener) {
        super(listener);
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
        _lineCount = lineCount;
        _fingerprint = fingerprint;
        _overlappingBlocks = new HashSet<>();
        _nonOverlappingBlocks = new HashSet<>();
    }

    @Override
    public void block(final Block block) {
        Objects.requireNonNull(block, "block");

        // Check previously overlapping blocks
        for (final Iterator<Block> i = _overlappingBlocks.iterator(); i.hasNext(); ) {
            final Block other = i.next();
            if (!block.isOverlapping(other)) {
                _nonOverlappingBlocks.add(block);
                _nonOverlappingBlocks.add(other);
                i.remove();
            }
        }

        // Check previously non-overlapping blocks
        if (!_nonOverlappingBlocks.contains(block)) {
            for (final Block _nonOverlappingBlock : _nonOverlappingBlocks) {
                if (!block.isOverlapping(_nonOverlappingBlock)) {
                    _nonOverlappingBlocks.add(block);
                    return;
                }
            }

            // Otherwise, we assume that it was overlapping
            _overlappingBlocks.add(block);
        }
    }

    @Override
    public void endSet(final String text) {
        if (!_nonOverlappingBlocks.isEmpty()) {
            super.startSet(_lineCount, _fingerprint);
            for (final Block nonOverlappingBlock : _nonOverlappingBlocks) {
                super.block(nonOverlappingBlock);
            }
            super.endSet(text);
        }
        _nonOverlappingBlocks = null;
        _overlappingBlocks = null;
    }
}
