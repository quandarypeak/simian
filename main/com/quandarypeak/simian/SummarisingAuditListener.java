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
