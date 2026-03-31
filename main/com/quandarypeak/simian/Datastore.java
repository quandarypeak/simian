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
