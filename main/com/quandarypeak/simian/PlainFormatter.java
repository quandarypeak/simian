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

import java.io.OutputStream;

/**
 * Simply logs sets of duplicate blocks to an output stream.
 */
final class PlainFormatter extends AbstractTextFormatter {
    /**
     * Constructor.
     *
     * @param out         The output stream to which messages are logged
     * @param printBanner Should the copyright banner be printed
     */
    PlainFormatter(final OutputStream out, final boolean printBanner) {
        super(out, printBanner);
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
    }

    @Override
    public void block(final Block block) {
        println(" Between lines " + block.getStartLineNumber() + " and " + block.getEndLineNumber() + " in " + block.getSourceFile().getFilename());
    }
}
