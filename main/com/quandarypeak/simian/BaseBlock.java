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

import java.util.Objects;

/**
 * Convenience base class for blocks.
 */
class BaseBlock implements Block {
    private final SourceFile sourceFile;
    private final int startLineNumber;
    private final int endLineNumber;
    private final long blockHash;

    private BaseBlock next;
    private BaseBlock previous;

    private BaseBlock parentWhenStart;
    private BaseBlock parentWhenEnd;

    BaseBlock(final SourceFile sourceFile, final int startLineNumber, final int endLineNumber, final long blockHash, final BaseBlock previous) {
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
        this.startLineNumber = startLineNumber;
        this.endLineNumber = endLineNumber;
        this.blockHash = blockHash;
        setPrevious(previous);
    }

    @Override
    public final SourceFile getSourceFile() {
        return sourceFile;
    }

    @Override
    public final int getStartLineNumber() {
        return startLineNumber;
    }

    @Override
    public final int getEndLineNumber() {
        return endLineNumber;
    }

    public final long getBlockHash() {
        return blockHash;
    }

    public final boolean isStartOfChain() {
        return previous == null;
    }

    public final boolean hasNext() {
        return getNext() != null;
    }

    public final BaseBlock getNext() {
        return next;
    }

    @Override
    public final boolean isSubsumed() {
        return parentWhenStart != null || parentWhenEnd != null;
    }

    @Override
    public boolean isOverlapping(final Block other) {
        return sourceFile.equals(other.getSourceFile()) &&
                startLineNumber <= other.getEndLineNumber() &&
                endLineNumber >= other.getStartLineNumber();
    }

    public void unlink() {
        setNext(null);
        setPrevious(null);

        if (parentWhenStart != null) {
            parentWhenStart.clearStartChild(this);
        }

        if (parentWhenEnd != null) {
            parentWhenEnd.clearEndChild(this);
        }
    }

    protected final void setParentWhenStart(final BaseBlock block) {
        parentWhenStart = Objects.requireNonNull(block, "block can't be null");
    }

    protected final void setParentWhenEnd(final BaseBlock block) {
        parentWhenEnd = Objects.requireNonNull(block, "block can't be null");
    }

    protected final void clearParentWhenStart(final Block block) {
        if (parentWhenStart != block) {
            throw new IllegalArgumentException("Not a parent");
        }
        parentWhenStart = null;
    }

    protected final void clearParentWhenEnd(final Block block) {
        if (parentWhenEnd != block) {
            throw new IllegalArgumentException("Not a parent");
        }
        parentWhenEnd = null;
    }

    protected void clearStartChild(final Block block) {
        throw new IllegalArgumentException("No children");
    }

    protected void clearEndChild(final Block block) {
        throw new IllegalArgumentException("No children");
    }

    private void setNext(final BaseBlock block) {
        if (next == block) {
            return;
        }

        final BaseBlock oldNext = next;
        next = block;

        if (oldNext != null) {
            oldNext.setPrevious(null);
        }

        if (block != null) {
            block.setPrevious(this);
        }
    }

    private void setPrevious(final BaseBlock block) {
        if (previous == block) {
            return;
        }

        final BaseBlock oldPrevious = previous;
        previous = block;

        if (oldPrevious != null) {
            oldPrevious.setNext(null);
        }

        if (block != null) {
            block.setNext(this);
        }
    }
}
