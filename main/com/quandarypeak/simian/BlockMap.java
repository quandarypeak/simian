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

import com.quandarypeak.simian.gnu.trove.TLongObjectHashMap;
import com.quandarypeak.simian.gnu.trove.TLongObjectIterator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

// TODO:One day re-write this to make it nicer:-)Like thats ever gonna happen hahaha!
// TODO:Most of this really belongs in Checker.
final class BlockMap {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final MessageDigest MD5;

    static {
        try {
            MD5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private final TLongObjectHashMap _blocksByHash = new TLongObjectHashMap();
    private final int _lineCount;

    BlockMap(final int lineCount) {
        _lineCount = lineCount;
    }

    public int getLineCount() {
        return _lineCount;
    }

    public BaseBlock add(final BaseBlock block) {
        final long key = block.getBlockHash();

        BlockCollection set = (BlockCollection) _blocksByHash.get(key);
        if (set == null) {
            set = new BlockCollection();
            _blocksByHash.put(key, set);
        }
        set.add(block);

        return block;
    }

    /**
     * @param listener
     */
    public void check(final AuditListener listener) {
        removeNonDuplicates();

        BlockMap currentMap = this;

        do {
            final BlockMap superMap = currentMap.tile();

            currentMap.notify(listener);

            currentMap.unlink();

            currentMap = superMap;
        } while (currentMap != null);
    }

    private void unlink() {
        for (final TLongObjectIterator i = _blocksByHash.iterator(); i.hasNext(); ) {
            i.advance();
            ((BlockCollection) i.value()).unlink();
        }

        _blocksByHash.clear();
    }

    private BlockMap tile() {
        if (_blocksByHash.isEmpty()) {
            return null;
        }

        BlockMap superMap = tile(1);

        superMap.removeNonDuplicates();

        return superMap;
    }

    private BlockMap tile(final int distance) {
        final BlockMap superMap = new BlockMap(_lineCount + distance);

        // For every bucket...
        for (final TLongObjectIterator i = _blocksByHash.iterator(); i.hasNext(); ) {
            i.advance();

            // For every block within the bucket...
            final BlockCollection blocks = (BlockCollection) i.value();
            for (final Iterator<BaseBlock> j = blocks.iterator(); j.hasNext(); ) {
                // Find the start of a chain
                BaseBlock start = j.next();
                if (!start.isStartOfChain()) {
                    continue;
                }

                // This will barf if it doesn't meet the suggested distance but that's a programmatic error!

                // Tile
                BaseBlock end = start.getNext();
                BaseBlock previousSuperBlock = null;
                while (end != null) {
                    previousSuperBlock = superMap.add(SuperBlock.create(previousSuperBlock, start, end));
                    start = start.getNext();
                    end = end.getNext();
                }
            }
        }

        return superMap;
    }

    private void notify(final AuditListener listener) {
        for (final TLongObjectIterator i = _blocksByHash.iterator(); i.hasNext(); ) {
            i.advance();
            final BlockCollection blocks = (BlockCollection) i.value();
            if (blocks.isAuditable()) {
                listener.startSet(_lineCount, fingerprint(i.key()));
                for (final Iterator<BaseBlock> j = blocks.iterator(); j.hasNext(); ) {
                    final Block block = j.next();
                    final SourceFile sourceFile = block.getSourceFile();
                    listener.block(block);
                    sourceFile.markAudited();
                }
                listener.endSet(null);
            }
        }
    }

    private void removeNonDuplicates() {
        for (final TLongObjectIterator i = _blocksByHash.iterator(); i.hasNext(); ) {
            i.advance();
            final BlockCollection blocks = (BlockCollection) i.value();
            if (!blocks.containsDuplicates()) {
                blocks.unlink();
                i.remove();
            }
        }
    }

    private static String fingerprint(final long blockHash) {
        try {
            final MessageDigest md5 = (MessageDigest) MD5.clone();
            final byte[] digest = md5.digest(Long.toString(blockHash).getBytes(StandardCharsets.UTF_8));
            return hexString(digest);
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hexString(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            final int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
