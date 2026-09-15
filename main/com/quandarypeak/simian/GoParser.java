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

/**
 * Parser for Go source files.
 *
 * <p>Extends the standard C-family tokeniser configuration with pre-processing via
 * {@link GoRawStringNormalisingReader}, which converts backtick raw string literals to
 * double-quoted strings before tokenisation. This ensures that:
 * <ul>
 *   <li>Raw string content (SQL, HTML, templates, …) does not appear as code tokens</li>
 *   <li>{@code IGNORE_STRINGS} suppresses raw string values correctly</li>
 *   <li>Two raw strings differing only in content produce identical structural fingerprints</li>
 * </ul>
 *
 * <p>Differences from {@link CFamilyParser}:
 * <ul>
 *   <li>Reader is wrapped in {@link GoRawStringNormalisingReader}</li>
 *   <li>{@code $} is not configured as a word character — it is not a valid Go identifier
 *       character (unlike JavaScript/TypeScript where {@code $} is common)</li>
 *   <li>{@code #} is not configured as a word character — Go has no preprocessor directives</li>
 * </ul>
 */
final class GoParser extends AbstractStreamTokenizerParser {

    GoParser(final TokenVisitor visitor) {
        super(visitor);
    }

    @Override
    public int parse(final Reader reader) throws IOException {
        final StreamTokenizer tokenizer =
                new StreamTokenizer(new GoRawStringNormalisingReader(reader));

        tokenizer.parseNumbers();
        tokenizer.wordChars('_', '_');
        tokenizer.slashSlashComments(true);
        tokenizer.slashStarComments(true);
        tokenizer.whitespaceChars(';', ';');
        tokenizer.ordinaryChar('.');
        tokenizer.ordinaryChar('/');

        return parse(tokenizer);
    }
}
