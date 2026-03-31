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
 * An audit listener that generates a check summary as it goes.
 */
final class SummarisingAuditListener extends DecoratorAuditListener implements CheckSummary {
    private long _startTimeMillis;
    private long _endTimeMillis;
    private int _duplicateFileCount;
    private int _duplicateLineCount;
    private int _duplicateBlockCount;
    private int _totalFileCount;
    private int _totalRawLineCount;
    private int _totalSignificantLineCount;

    private int _currentLineCount;

    SummarisingAuditListener(final AuditListener listener) {
        super(listener);
    }

    @Override
    public int getDuplicateFileCount() {
        return _duplicateFileCount;
    }

    @Override
    public final int getDuplicateLineCount() {
        return _duplicateLineCount;
    }

    @Override
    public int getDuplicateBlockCount() {
        return _duplicateBlockCount;
    }

    @Override
    public final int getDuplicateLinePercentage() {
        return Math.round((float) _duplicateLineCount * 100 / (float) _totalSignificantLineCount);
    }

    @Override
    public int getTotalFileCount() {
        return _totalFileCount;
    }

    @Override
    public int getTotalRawLineCount() {
        return _totalRawLineCount;
    }

    @Override
    public int getTotalSignificantLineCount() {
        return _totalSignificantLineCount;
    }

    @Override
    public long getProcessingTime() {
        return _endTimeMillis - _startTimeMillis;
    }

    @Override
    public void startCheck(final Options options) {
        _startTimeMillis = System.currentTimeMillis();
        super.startCheck(options);
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
        Objects.requireNonNull(sourceFile, "sourceFile");

        ++_totalFileCount;
        _totalRawLineCount += sourceFile.getRawLineCount();
        _totalSignificantLineCount += sourceFile.getSignificantLineCount();

        super.fileProcessed(sourceFile);
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
        _currentLineCount = lineCount;
        super.startSet(lineCount, fingerprint);
    }

    @Override
    public void block(final Block block) {
        Objects.requireNonNull(block, "block");

        final SourceFile sourceFile = block.getSourceFile();
        final boolean subsumed = block.isSubsumed();

        if (!subsumed) {
            ++_duplicateBlockCount;
            _duplicateLineCount += _currentLineCount;
        }

        if (!sourceFile.isAudited()) {
            ++_duplicateFileCount;
        }

        super.block(block);
    }

    @Override
    public void endSet(final String text) {
        super.endSet(text);
    }

    @Override
    public void endCheck(final CheckSummary summary) {
        // TODO: This is REALLY a hack because I couldn't be arsed
        endCheck();
    }

    public final void endCheck() {
        _endTimeMillis = System.currentTimeMillis();
        super.endCheck(this);
    }
}
