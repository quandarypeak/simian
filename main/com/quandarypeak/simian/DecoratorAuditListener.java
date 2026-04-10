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
