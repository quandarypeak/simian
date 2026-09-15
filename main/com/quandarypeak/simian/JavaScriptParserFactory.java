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

public final class JavaScriptParserFactory extends AbstractCFamilyParserFactory {
    /**
     * These modifiers may be optionally ignored
     */
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList(
        "abstract", "class", "const", "enum", "export", "extends", "final",
        "function", "implements", "interface", "native", "private", "protected",
        "public", "static", "throws", "transient", "volatile", "var", "let"));

    /**
     * Any of these will cause the line on which it appears to be ignored
     */
    private static final Set<String> IGNORE_LINE_TRIGGERS = new HashSet<>(
        Arrays.asList("import", "package"));

    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        return new CFamilyParser(createBaseLanguageTokenVisitor(
            listener, options, MODIFIERS, IGNORE_LINE_TRIGGERS));
    }
}
