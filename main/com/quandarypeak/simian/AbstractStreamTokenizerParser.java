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

/**
 * Convenience base class for stream tokenizing parsers.
 */
abstract class AbstractStreamTokenizerParser extends DecoratorTokenVisitor implements Parser {
    AbstractStreamTokenizerParser(final TokenVisitor visitor) {
        super(visitor);
    }

    public final int parse(final StreamTokenizer tokenizer) throws IOException {
        visitFile();

        tokenizer.eolIsSignificant(true);

        visit(tokenizer.lineno());

        int token;

        while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            switch (token) {
                case StreamTokenizer.TT_EOL:
                    visitEnd();
                    visit(tokenizer.lineno());
                    break;
                case StreamTokenizer.TT_NUMBER:
                    visitNumber(tokenizer.nval);
                    break;
                case StreamTokenizer.TT_WORD:
                    visitIdentifier(tokenizer.sval, TokenVisitor.UNKNOWN);
                    break;
                case StreamTokenizer.TT_COMMENT:
                    visitComment(tokenizer.sval);
                    break;
                case '"':
                case '\'':
                    visitString(tokenizer.sval, (char) token);
                    break;
                default:
                    visitPunctuation((char) token);
                    break;
            }
        }

        visitEnd();

        return tokenizer.lineCount();
    }
}
