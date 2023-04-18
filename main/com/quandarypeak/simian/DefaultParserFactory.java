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

import com.quandarypeak.simian.java.io.StreamTokenizer;

import java.io.IOException;
import java.io.Reader;

final class DefaultParserFactory extends AsbtractParserFactory {
    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        LineListener l = listener;

        if (options.hasOption(Option.IGNORE_BLOCKS)) {
            for (final BlockMarkers blockMarkers : options.<Iterable<BlockMarkers>>getOption(Option.IGNORE_BLOCKS)) {
                l = new IgnoreBlocksLineListener(l, blockMarkers);
            }
        }

        return new DefaultParser(new DefaultTokenVisitor(l));
    }

    private static final class DefaultParser extends AbstractStreamTokenizerParser {
        DefaultParser(final TokenVisitor visitor) {
            super(visitor);
        }

        @Override
        public int parse(final Reader reader) throws IOException {
            final StreamTokenizer tokenizer = new StreamTokenizer(reader);

            tokenizer.resetSyntax();

            tokenizer.wordChars('a', 'z');
            tokenizer.wordChars('A', 'Z');
            tokenizer.wordChars('0', '9');
            tokenizer.lowerCaseMode(true);

            tokenizer.whitespaceChars(0, ' ');
            tokenizer.whitespaceChars('!', '!');
            tokenizer.whitespaceChars('"', '"');
            tokenizer.whitespaceChars('\'', '\'');
            tokenizer.whitespaceChars('.', '.');
            tokenizer.whitespaceChars(':', ';');
            tokenizer.whitespaceChars('?', '?');
            tokenizer.whitespaceChars(',', ',');

            return parse(tokenizer);
        }
    }
}
