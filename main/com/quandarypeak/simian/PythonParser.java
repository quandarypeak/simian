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
 * Parser for Python source files.
 *
 * <p>Python uses indentation and newlines as syntactic block and statement delimiters rather than
 * curly braces and semicolons. This parser does not interpret INDENT/DEDENT events or logical
 * line boundaries — it treats every physical source line as an independent unit, exactly as
 * Simian does for all other languages.
 *
 * <p><b>Practical impact on duplicate detection:</b> In practice this limitation has very little
 * effect. Simian's threshold-based detection requires {@code N} consecutive matching lines (default
 * N=6), so a duplicate block almost always spans multiple physical lines regardless of language.
 * The cases where indent-awareness would matter are narrow:
 * <ul>
 *   <li><b>Backslash line continuation</b> ({@code \} at end of line) — a single logical statement
 *       split across two physical lines is fingerprinted as two separate lines. If the continuation
 *       pattern differs between two otherwise-identical blocks, they will not match on those lines.
 *       Impact is minor: continuation lines are uncommon in modern Python (parenthesised expressions
 *       are preferred), and a mismatch on one line does not break a duplicate run that is already
 *       {@code N} lines long.</li>
 *   <li><b>Block boundaries</b> — {@code def}/{@code class} header lines and their bodies are
 *       fingerprinted as ordinary lines. Simian does not know that a DEDENT ends a function, so it
 *       can report a duplicate run that straddles two adjacent functions in one file but only one
 *       function in another. This is consistent with how Simian treats all C-family languages
 *       (where closing {@code }} lines are also just ordinary lines).</li>
 *   <li><b>Indentation itself</b> — leading whitespace is consumed by the tokenizer as whitespace
 *       and does not appear in fingerprints, so two blocks that are structurally identical but
 *       indented differently (e.g. nested vs. top-level) still match. This is generally desirable
 *       behaviour for a similarity analyser.</li>
 * </ul>
 */
final class PythonParser extends AbstractStreamTokenizerParser {
    PythonParser(final TokenVisitor visitor) {
        super(visitor);
    }

    @Override
    public int parse(final Reader reader) throws IOException {
        final StreamTokenizer tokenizer = new StreamTokenizer(new PythonTripleQuoteNormalisingReader(reader));

        tokenizer.parseNumbers();
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('0', '9');
        tokenizer.slashSlashComments(false);
        tokenizer.slashStarComments(false);
        tokenizer.commentChar('#');
        tokenizer.whitespaceChars(';', ';');
        tokenizer.ordinaryChar('.');
        tokenizer.ordinaryChar('/');

        return parse(tokenizer);
    }
}
