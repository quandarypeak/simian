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

//import java.io.FileReader;

final class IbmSystem390AssemblerParserFactory extends AsbtractParserFactory {
    @Override
    public Parser createParser(final LineListener listener, final Options options) {
        TokenVisitor visitor = createBaseLanguageTokenVisitor(listener, options);
        visitor = new RecogniseIbmLanguageStringsTokenVisitor(visitor);
        return new IbmSystem390AssemblerParser(visitor);
    }
}
