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

import java.io.IOException;
import java.io.LineNumberReader;

/**
 * Retrieves the text for a duplicate block set.
 */
final class BlockTextRetrievingAuditListener extends DecoratorAuditListener {
    private Block _block;

    BlockTextRetrievingAuditListener(final AuditListener listener) {
        super(listener);
    }

    @Override
    public void block(final Block block) {
        _block = block;
        super.block(block);
    }

    @Override
    public void endSet(final String text) {
        try {
            super.endSet(readText());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            _block = null;
        }
    }

    private String readText() throws IOException {

        try (final LineNumberReader reader = new LineNumberReader(new UnicodeBOMAwareReader(_block.getSourceFile().getFilename()))) {
            String line = "";
            while (reader.getLineNumber() < _block.getStartLineNumber()) {
                line = reader.readLine();
            }

            final StringBuilder lines = new StringBuilder();
            lines.append(line);
            lines.append(System.lineSeparator());

            while (reader.getLineNumber() < _block.getEndLineNumber()) {
                lines.append(reader.readLine()).append(System.lineSeparator());
            }

            return lines.toString();
        }
    }
}
