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

import java.io.File;
import java.util.Objects;

/**
 * Convenience base class for decorating audit listeners.
 */
public class DecoratorAuditListener implements AuditListener {
    private final AuditListener _listener;

    public DecoratorAuditListener(final AuditListener listener) {
        _listener = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void startCheck(final Options options) {
        _listener.startCheck(options);
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
        _listener.fileProcessed(sourceFile);
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
        _listener.startSet(lineCount, fingerprint);
    }

    @Override
    public void block(final Block block) {
        _listener.block(block);
    }

    @Override
    public void endSet(final String text) {
        _listener.endSet(text);
    }

    @Override
    public void endCheck(final CheckSummary summary) {
        _listener.endCheck(summary);
    }

    @Override
    public void error(final File file, final Throwable e) {
        _listener.error(file, e);
    }
}
