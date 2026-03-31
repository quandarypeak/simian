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
