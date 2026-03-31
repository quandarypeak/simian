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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;

/**
 * Convenience base class that simply logs sets of duplicate blocks to an output stream.
 */
abstract class AbstractFormatter implements AuditListener {
    private final PrintStream out;

    /**
     * Constructor.
     *
     * @param out The output stream to which messages are logged
     */
    AbstractFormatter(final OutputStream out) {
        this.out = out instanceof PrintStream ? (PrintStream) out : new PrintStream(out);
    }

    @Override
    public void endCheck(final CheckSummary summary) {
        out.close();
    }

    @Override
    public void error(final File file, final Throwable e) {
        System.err.println("Error processing file " + file.getPath() + ": " + e.getMessage());
    }

    protected final void println(final String message) {
        Objects.requireNonNull(message, "message");
        out.println(message);
    }

    protected final void print(final String message) {
        Objects.requireNonNull(message, "message");
        out.print(message);
    }
}
