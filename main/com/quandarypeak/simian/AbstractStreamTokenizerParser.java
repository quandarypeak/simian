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
