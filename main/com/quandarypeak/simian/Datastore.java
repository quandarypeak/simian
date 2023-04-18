package com.quandarypeak.simian;

import java.util.HashMap;
import java.util.Map;

final class Datastore {
    private final Map<String, SourceFile> _sourceFiles = new HashMap<>();
    private final BlockMap _blockMap;

    Datastore(final int threshold) {
        _blockMap = new BlockMap(threshold);
    }

    public SourceFile getSourceFile(final String filename) {
        return _sourceFiles.computeIfAbsent(filename, SourceFile::new);
    }

    public Block addBlock(final SourceFile sourceFile, final int start, final int end, final long blockHash, final Block previous) {
        return _blockMap.add(new BaseBlock(sourceFile, start, end, blockHash, (BaseBlock) previous));
    }

    public void check(final AuditListener listener) {
        _blockMap.check(listener);
    }
}
