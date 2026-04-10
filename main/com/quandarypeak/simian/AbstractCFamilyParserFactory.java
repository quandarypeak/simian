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

import java.util.Objects;
import java.util.Set;

/**
 * Convenience base class for c-style languages such as C#, C++ and C itself.
 */
abstract class AbstractCFamilyParserFactory extends AsbtractParserFactory {
    protected final TokenVisitor createBaseLanguageTokenVisitor(final LineListener listener, final Options options, final Set<String> modifiers, final Set<String> ignoreLineTriggers) {
        Objects.requireNonNull(options);

        TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options);

        if (options.hasOption(Option.BALANCE_SQUARE_BRACKETS)) {
            visitor = new BalanceSquareBracketsTokenVisitor(visitor);
        }

        if (options.hasOption(Option.IGNORE_CURLY_BRACES)) {
            visitor = new IgnoreCurlyBracesTokenVisitor(visitor);
        }

        if (options.hasOption(Option.IGNORE_MODIFIERS)) {
            visitor = new IgnoreWordsTokenVisitor(modifiers, visitor);
        }

        visitor = new IgnoreLinesTokenVisitor(visitor, ignoreLineTriggers);

        return visitor;
    }
}
