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

abstract class AsbtractParserFactory implements ParserFactory {
    protected final TokenVisitor createBaseLanguageTokenVisitor(final LineListener listener, final Options options) {
        TokenVisitor visitor = new DefaultTokenVisitor(listener);

        if (options.hasOption(Option.IGNORE_LITERALS)) {
            visitor = new IgnoreLiteralsTokenVisitor(visitor);
        } else {
            if (options.hasOption(Option.IGNORE_CHARACTERS)) {
                visitor = new IgnoreCharactersTokenVisitor(visitor);
            } else if (options.hasOption(Option.IGNORE_CHARACTER_CASE)) {
                visitor = new IgnoreCharacterCaseTokenVisitor(visitor);
            }

            if (options.hasOption(Option.IGNORE_STRINGS)) {
                visitor = new IgnoreStringsTokenVisitor(visitor);
            } else if (options.hasOption(Option.IGNORE_STRING_CASE)) {
                visitor = new IgnoreStringCaseTokenVisitor(visitor);
            }

            if (options.hasOption(Option.IGNORE_NUMBERS)) {
                visitor = new IgnoreNumbersTokenVisitor(visitor);
            }
        }

        if (options.hasOption(Option.IGNORE_IDENTIFIERS)) {
            visitor = new IgnoreIdentifiersTokenVisitor(visitor);
        } else {
            if (options.hasOption(Option.IGNORE_IDENTIFIER_CASE)) {
                visitor = new IgnoreIdentifierCaseTokenVisitor(visitor);
            }

            if (options.hasOption(Option.IGNORE_VARIABLE_NAMES)) {
                visitor = new IgnoreVariableNamesTokenVisitor(visitor);
            }

            if (options.hasOption(Option.IGNORE_SUBTYPE_NAMES)) {
                visitor = new IgnoreSubtypeNamesTokenVisitor(visitor);
            }
        }

        if (options.hasOption(Option.BALANCE_PARENTHESES)) {
            visitor = new BalanceParenthesesTokenVisitor(visitor);
        }

        if (options.hasOption(Option.IGNORE_BLOCKS)) {
            for (final BlockMarkers blockMarkers : options.<Iterable<BlockMarkers>>getOption(Option.IGNORE_BLOCKS)) {
                visitor = new IgnoreBlocksTokenVisitor(visitor, blockMarkers);
            }
        }

        return visitor;
    }
}
