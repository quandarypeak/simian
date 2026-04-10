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

final class NullFormatter implements AuditListener {
    public static final NullFormatter INSTANCE = new NullFormatter();

    @Override
    public void startCheck(final Options options) {
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
    }

    @Override
    public void block(final Block block) {
    }

    @Override
    public void endSet(final String text) {
    }

    @Override
    public void endCheck(final CheckSummary summary) {
    }

    @Override
    public void error(final File file, final Throwable e) {
    }
}
