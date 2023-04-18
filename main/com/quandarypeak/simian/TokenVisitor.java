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

interface TokenVisitor {
    int UNKNOWN = 0;

    int METHOD = 1;

    int VARIABLE = 2;

    int CONSTANT = 3;

    int TYPE = 4;

    int KEYWORD = 5;

    void visitFile();

    void visit(int lineNumber);

    void visitNumber(double value);

    void visitIdentifier(String name, int type);

    void visitString(String text, char type);

    void visitComment(String text);

    void visitPunctuation(char c);

    void visitOther(String s);

    void visitEnd();
}
