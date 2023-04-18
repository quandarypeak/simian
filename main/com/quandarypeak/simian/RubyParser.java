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

import com.quandarypeak.simian.java.io.StreamTokenizer;

import java.io.IOException;
import java.io.Reader;

final class RubyParser extends AbstractStreamTokenizerParser {
    RubyParser(final TokenVisitor visitor) {
        super(visitor);
    }

    @Override
    public int parse(final Reader reader) throws IOException {
        final StreamTokenizer tokenizer = new StreamTokenizer(reader);

        tokenizer.parseNumbers();
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('=', '=');
        tokenizer.slashSlashComments(false);
        tokenizer.slashStarComments(false);
        tokenizer.commentChar('#');
        tokenizer.whitespaceChars(';', ';');
        tokenizer.ordinaryChar('.');
        tokenizer.ordinaryChar('/');

        return parse(tokenizer);
    }
}
