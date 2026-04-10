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
import java.util.Objects;
import java.util.Set;

final class RubyParserFactory extends AbstractCFamilyParserFactory {
    private static final BlockMarkers COMMENT_BLOCK_MARKERS = new BlockMarkers("=begin", "=end");
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("begin", "end", "class", "def", "module", "private", "protected", "public", "do"));
    private static final Set<String> IGNORE_LINE_TRIGGERS = new HashSet<>(Arrays.asList("require", "require_gem"));

    @Override
    public final Parser createParser(final LineListener listener, final Options options) {
        Objects.requireNonNull(options, "options");

        TokenVisitor visitor = createBaseLanguageTokenVisitor(new IgnoreBlocksLineListener(listener, COMMENT_BLOCK_MARKERS), options, MODIFIERS, IGNORE_LINE_TRIGGERS);

        visitor = new RecogniseRubyBlockCommentKeywordsTokenVisitor(visitor);

        return new RubyParser(visitor);
    }
}
