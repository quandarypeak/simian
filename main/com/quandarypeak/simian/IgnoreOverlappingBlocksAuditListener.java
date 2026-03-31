/*
 * Copyright 2022-2026 Quandary Peak Research, Inc.
 * Original authorship by Simon Harris.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
