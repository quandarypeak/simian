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

 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.Set;
 
 public final class TypeScriptParserFactory extends AbstractCFamilyParserFactory {
     /**
      * These modifiers may be optionally ignored
      */
     private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList(
         "abstract", "class", "const", "enum", "export", "extends", "final",
         "function", "implements", "interface", "native", "private", "protected",
         "public", "static", "throws", "transient", "volatile", "var", "let", 
         "tuple", "type"));
 
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
 