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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class JavaParserFactory extends AbstractCFamilyParserFactory {
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("boolean", "byte", "char", "double", "float", "int", "long", "short", "void"));

    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("abstract", "class", "extends", "final", "implements", "interface", "native", "private", "protected", "public", "static", "synchronized", "throws", "transient", "volatile"));

    private static final Set<String> IGNORE_LINE_TRIGGERS = new HashSet<>(Arrays.asList("assert", "import", "package"));

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList("for", "do", "while", "if", "else", "goto", "break", "continue", "switch", "return", "case", "new", "try", "catch", "finally", "throw", "instanceof"));

    static {
        KEYWORDS.addAll(MODIFIERS);
        KEYWORDS.addAll(IGNORE_LINE_TRIGGERS);
    }

    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options, MODIFIERS, IGNORE_LINE_TRIGGERS);

        visitor = new RecogniseIdentifiersTokenVisitor(visitor, TYPES, KEYWORDS);

        return new CFamilyParser(visitor);
    }
}
