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
