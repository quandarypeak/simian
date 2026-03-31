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
