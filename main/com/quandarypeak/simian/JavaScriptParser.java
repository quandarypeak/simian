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
import java.io.Reader;

/**
 * Parser for JavaScript and TypeScript source files.
 *
 * <p>Delegates to the standard C-family tokeniser ({@link CFamilyParser}) with
 * pre-processing via {@link JavaScriptTemplateLiteralNormalisingReader}, which converts
 * backtick template literals to double-quoted strings before tokenisation. This ensures
 * that {@code IGNORE_STRINGS} suppresses template-literal values and that two
 * template literals differing only in their interpolated expressions produce the
 * same fingerprint. Template literals are a JavaScript (ES6) feature that
 * TypeScript inherits unchanged, so this parser is shared by both languages'
 * factories.
 *
 * <p>Delegating to {@link CFamilyParser} (rather than duplicating its tokeniser setup)
 * means any future change to the shared C-family tokeniser configuration is picked up
 * here automatically, with no risk of the two drifting out of sync.
 */
final class JavaScriptParser implements Parser {
    private final Parser delegate;

    JavaScriptParser(final TokenVisitor visitor) {
        this.delegate = new CFamilyParser(visitor);
    }

    @Override
    public int parse(final Reader reader) throws IOException {
        return delegate.parse(new JavaScriptTemplateLiteralNormalisingReader(reader));
    }
}
