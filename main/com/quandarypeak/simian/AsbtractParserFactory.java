/*
 * Simian Similarity Analyzer
 * 
 * Copyright (c) 2023 Quandary Peak Research.
 * Original authorship by Simon Harris.
 * 
 * Use of this software is permitted for educational or academic research
 * purposes only and is subject to the Quandary Peak Academic Software License.
 * See docs/license.txt for details.
 * 
 * Redistribution of this software in source or binary form is not permitted.
 * 
 * For non-academic or commercial use, please contact simian@quandarypeak.com.
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
