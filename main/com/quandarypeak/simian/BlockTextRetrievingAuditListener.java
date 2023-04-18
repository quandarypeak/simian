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
