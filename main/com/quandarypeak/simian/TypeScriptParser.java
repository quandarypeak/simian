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
 * Parser for TypeScript (and JavaScript) source files.
 *
 * <p>Extends the standard C-family tokeniser with pre-processing via
 * {@link TypeScriptTemplateLiteralNormalisingReader}, which converts backtick
 * template literals to double-quoted strings before tokenisation. This ensures
 * that {@code IGNORE_STRINGS} suppresses template-literal values and that two
 * template literals differing only in their interpolated expressions produce the
 * same fingerprint.
 */
final class TypeScriptParser extends AbstractStreamTokenizerParser {
    TypeScriptParser(final TokenVisitor visitor) {
        super(visitor);
    }

    @Override
    public int parse(final Reader reader) throws IOException {
        final StreamTokenizer tokenizer =
                new StreamTokenizer(new TypeScriptTemplateLiteralNormalisingReader(reader));

        tokenizer.parseNumbers();
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('#', '#');
        tokenizer.wordChars('$', '$');
        tokenizer.slashSlashComments(true);
        tokenizer.slashStarComments(true);
        tokenizer.whitespaceChars(';', ';');
        tokenizer.ordinaryChar('.');
        tokenizer.ordinaryChar('/');

        return parse(tokenizer);
    }
}
