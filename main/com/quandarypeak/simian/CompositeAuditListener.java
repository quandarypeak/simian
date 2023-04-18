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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Wraps a list of audit listeners ensuring each of them is notified of audit events.
 */
public final class CompositeAuditListener implements AuditListener {
    /**
     * The audit listeners
     */
    private final List<AuditListener> _listeners;

    /**
     * Constructor.
     *
     * @param components The component audit listeners.
     */
    public CompositeAuditListener(final Collection<AuditListener> components) {
        _listeners = new ArrayList<>(components);
    }

    /**
     * Default constructor.
     */
    public CompositeAuditListener() {
        _listeners = new ArrayList<>();
    }

    /**
     * Adds a listener. No checking is performed to see if the listener already exists.
     *
     * @param listener The listener to add.
     */
    public void add(final AuditListener listener) {
        _listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void startCheck(final Options options) {
        for (final AuditListener _listener : _listeners) {
            _listener.startCheck(options);
        }
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
        for (final AuditListener _listener : _listeners) {
            _listener.fileProcessed(sourceFile);
        }
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
        for (final AuditListener _listener : _listeners) {
            _listener.startSet(lineCount, fingerprint);
        }
    }

    @Override
    public void block(final Block block) {
        for (final AuditListener _listener : _listeners) {
            _listener.block(block);
        }
    }

    @Override
    public void endSet(final String text) {
        for (final AuditListener _listener : _listeners) {
            _listener.endSet(text);
        }
    }

    @Override
    public void endCheck(final CheckSummary summary) {
        for (final Object _listener : _listeners) {
            ((AuditListener) _listener).endCheck(summary);
        }
    }

    @Override
    public void error(final File file, final Throwable e) {
        for (final AuditListener _listener : _listeners) {
            _listener.error(file, e);
        }
    }
}
