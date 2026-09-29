/*
 * Copyright 2022-2026 Quandary Peak Research, Inc.
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

import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for JavaScriptParserFactory: fingerprint quality and option behaviours.
 *
 * <p><b>Parser internals:</b> JavaScript uses JavaScriptParser (the same C-family
 * StreamTokenizer as Java/C#/C++) with a pre-processing step via
 * JavaScriptTemplateLiteralNormalisingReader. Single-line (//) and block (/* *&#47;) comments
 * are stripped; semicolons are whitespace; '#' and '$' are word characters; '.' and '/' are
 * ordinary punctuation.
 *
 * <p><b>Unlike TypeScriptParserFactory, JavaScriptParserFactory does not wire
 * {@link RecogniseIdentifiersTokenVisitor} into its chain.</b> Every identifier keeps the
 * default {@code TokenVisitor.UNKNOWN} type from the tokenizer - nothing is ever classified
 * TYPE, KEYWORD, VARIABLE, METHOD, or CONSTANT. This is a real, pre-existing gap (shared by
 * CppParserFactory/CSharpParserFactory/RubyParserFactory) with concrete consequences for
 * three options, documented and pinned by tests in section 9 below rather than silently
 * left unverified.
 */
public class JavaScriptParserTest {

    // -------------------------------------------------------------------------
    // Test infrastructure
    // -------------------------------------------------------------------------

    static class CapturingLineListener implements LineListener {
        final List<String> lines = new ArrayList<>();

        @Override
        public void file() {
            lines.clear();
        }

        @Override
        public void line(final int lineNumber, final LineBuffer line) {
            lines.add(line.toString());
        }
    }

    static class ParseResult {
        final int rawLineCount;
        final List<String> fingerprints;

        ParseResult(final int rawLineCount, final List<String> fingerprints) {
            this.rawLineCount = rawLineCount;
            this.fingerprints = Collections.unmodifiableList(fingerprints);
        }

        String get(final int i) { return fingerprints.get(i); }
        int count() { return fingerprints.size(); }
    }

    /** All options cleared - only the parser's unconditional behaviours apply. */
    private static Options bare() {
        final Options opts = new Options();
        opts.clear();
        return opts;
    }

    private static ParseResult parse(final String code, final Options opts) throws IOException {
        final CapturingLineListener listener = new CapturingLineListener();
        final Parser parser = new JavaScriptParserFactory().createParser(listener, opts);
        final int rawLines = parser.parse(new StringReader(code));
        return new ParseResult(rawLines, listener.lines);
    }

    // -------------------------------------------------------------------------
    // 1. Comment stripping
    // -------------------------------------------------------------------------

    @Test
    public void singleLineCommentProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("// a comment\n", bare());
        assertEquals(1, r.rawLineCount);
        assertEquals(0, r.count());
    }

    @Test
    public void blockCommentProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("/* block comment */\n", bare());
        assertEquals(1, r.rawLineCount);
        assertEquals(0, r.count());
    }

    @Test
    public void inlineCommentIsStrippedFromFingerprint() throws IOException {
        final ParseResult rWith    = parse("x = 5 // assign x\n", bare());
        final ParseResult rWithout = parse("x = 5\n", bare());
        assertEquals(1, rWith.count());
        assertEquals(rWithout.get(0), rWith.get(0));
    }

    // -------------------------------------------------------------------------
    // 2. Import and package suppression
    //    IgnoreLinesTokenVisitor is always active (unconditional, not option-gated).
    //    Unlike TypeScriptParserFactory (which omits 'package' - see its IMPORT_LINE_TRIGGERS
    //    comment), JavaScriptParserFactory's IGNORE_LINE_TRIGGERS includes both 'import' and
    //    'package'.
    // -------------------------------------------------------------------------

    @Test
    public void importLineIsSuppressed() throws IOException {
        final ParseResult r = parse("import { Component } from 'react';\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void packageLineIsSuppressed() throws IOException {
        final ParseResult r = parse("package com.example;\n", bare());
        assertEquals(0, r.count());
    }

    // -------------------------------------------------------------------------
    // 3. Semicolons treated as whitespace
    // -------------------------------------------------------------------------

    @Test
    public void semicolonDoesNotAppearInFingerprint() throws IOException {
        final ParseResult rSemi   = parse("x = 5;\n", bare());
        final ParseResult rNoSemi = parse("x = 5\n", bare());
        assertEquals(rNoSemi.get(0), rSemi.get(0));
    }

    // -------------------------------------------------------------------------
    // 4. Template literals
    //    JavaScriptTemplateLiteralNormalisingReader converts backtick template literals to
    //    double-quoted strings before the StreamTokenizer runs. This is the behaviour fixed
    //    this session when JavaScriptParserFactory switched from CFamilyParser to
    //    JavaScriptParser - these tests confirm it actually applies to .js, not just .ts.
    // -------------------------------------------------------------------------

    @Test
    public void templateLiteralProducesSameFingerprintAsRegularString() throws IOException {
        final ParseResult rTemplate = parse("const msg = `hello world`;\n", bare());
        final ParseResult rString   = parse("const msg = \"hello world\";\n", bare());
        assertEquals(rString.get(0), rTemplate.get(0));
    }

    @Test
    public void templateLiteralInterpolatedVariablesDoNotAppearInFingerprint() throws IOException {
        final ParseResult rFirst = parse("return `Hello, ${firstName}`;\n", bare());
        final ParseResult rLast  = parse("return `Hello, ${lastName}`;\n", bare());
        assertEquals(rFirst.get(0), rLast.get(0));
    }

    @Test
    public void ignoreStringsNormalisesTemplateLiterals() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_STRINGS, Boolean.TRUE);
        final ParseResult rStr      = parse("const msg = \"hello\";\n", opts);
        final ParseResult rTemplate = parse("const msg = `world`;\n", opts);
        assertEquals(rStr.get(0), rTemplate.get(0));
    }

    // -------------------------------------------------------------------------
    // 5. IGNORE_MODIFIERS
    //    Strips words in the MODIFIERS set: class, const, export, extends, function,
    //    static, var, let. IgnoreWordsTokenVisitor matches by raw identifier name, not by
    //    classified type, so this works correctly even without RecogniseIdentifiersTokenVisitor.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreModifiersStripsClassKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rWith    = parse("class Foo {\n", opts);
        final ParseResult rWithout = parse("Foo {\n", bare());
        assertEquals(rWithout.get(0), rWith.get(0));
    }

    @Test
    public void ignoreModifiersStripsExtendsKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rExtends = parse("class Foo extends Base {\n", opts);
        final ParseResult rPlain   = parse("class Foo Base {\n", opts);
        assertEquals(rPlain.get(0), rExtends.get(0));
    }

    @Test
    public void ignoreModifiersStripsExportKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rExport = parse("export const PI = 3;\n", opts);
        final ParseResult rPlain  = parse("PI = 3;\n", bare());
        assertEquals(rPlain.get(0), rExport.get(0));
    }

    @Test
    public void ignoreModifiersStripsFunctionKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rFunction = parse("function fetch() {\n", opts);
        final ParseResult rPlain    = parse("fetch() {\n", bare());
        assertEquals(rPlain.get(0), rFunction.get(0));
    }

    @Test
    public void ignoreModifiersStripsStaticKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rStatic = parse("static count() {\n", opts);
        final ParseResult rPlain  = parse("count() {\n", opts);
        assertEquals(rPlain.get(0), rStatic.get(0));
    }

    @Test
    public void ignoreModifiersNormalisesConstAndLetAndVar() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rConst = parse("const x = 5;\n", opts);
        final ParseResult rLet   = parse("let x = 5;\n", opts);
        final ParseResult rVar   = parse("var x = 5;\n", opts);
        assertEquals(rConst.get(0), rLet.get(0));
        assertEquals(rConst.get(0), rVar.get(0));
    }

    // -------------------------------------------------------------------------
    // 6. BALANCE_PARENTHESES
    //    Reacts to punctuation, not identifier classification - works the same for JS as TS.
    // -------------------------------------------------------------------------

    @Test
    public void balanceParenthesesMergesMultiLineCallIntoOneLine() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.BALANCE_PARENTHESES, Boolean.TRUE);
        final String multiLine = "foo(\n    a,\n    b\n);\n";
        final ParseResult rWith    = parse(multiLine, opts);
        final ParseResult rWithout = parse(multiLine, bare());
        assertNotEquals(rWithout.get(0), rWith.get(0));
        assertEquals(1, rWith.count());
    }

    // -------------------------------------------------------------------------
    // 7. BALANCE_SQUARE_BRACKETS
    // -------------------------------------------------------------------------

    @Test
    public void balanceSquareBracketsMergesMultiLineArrayLiteral() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.BALANCE_SQUARE_BRACKETS, Boolean.TRUE);
        final String multiLine = "const arr = [\n    1,\n    2,\n    3\n];\n";
        final ParseResult rWith    = parse(multiLine, opts);
        final ParseResult rWithout = parse(multiLine, bare());
        assertNotEquals(rWithout.get(0), rWith.get(0));
        assertEquals(1, rWith.count());
    }

    // -------------------------------------------------------------------------
    // 8. IGNORE_CURLY_BRACES
    // -------------------------------------------------------------------------

    @Test
    public void ignoreCurlyBracesRemovesBracesFromFingerprint() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_CURLY_BRACES, Boolean.TRUE);
        final ParseResult rBraces   = parse("if (x) { y(); }\n", opts);
        final ParseResult rNoBraces = parse("if (x) y();\n", bare());
        assertEquals(rNoBraces.get(0), rBraces.get(0));
    }

    // -------------------------------------------------------------------------
    // 9. Known gap: options that depend on identifier classification are no-ops or
    //    over-erase for JavaScript, because JavaScriptParserFactory never wires
    //    RecogniseIdentifiersTokenVisitor - every identifier keeps type UNKNOWN.
    //    These tests pin down and document the CURRENT behaviour; they are not an
    //    endorsement of it. If RecogniseIdentifiersTokenVisitor is ever wired in for JS,
    //    these specific tests should start failing, which is the intended signal to
    //    update them.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreVariableNamesHasNoEffectOnJavaScript() throws IOException {
        // IgnoreVariableNamesTokenVisitor only replaces type == VARIABLE. Since nothing is
        // ever classified VARIABLE for JS, two different variable names remain different -
        // the option has no effect.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rA = parse("x + y\n", opts);
        final ParseResult rB = parse("alpha + beta\n", opts);
        assertNotEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void ignoreSubtypeNamesHasNoEffectOnJavaScript() throws IOException {
        // IgnoreSubtypeNamesTokenVisitor only reduces type == TYPE. Since nothing is ever
        // classified TYPE for JS, a compound PascalCase name is left untouched instead of
        // being reduced to its last capitalised segment.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_SUBTYPE_NAMES, Boolean.TRUE);
        final ParseResult r = parse("new StringBuilder()\n", opts);
        assertTrue(r.get(0).contains("StringBuilder"));
    }

    @Test
    public void ignoreIdentifiersErasesRealKeywordsToo() throws IOException {
        // IgnoreIdentifiersTokenVisitor only preserves type == KEYWORD. Since nothing is
        // ever classified KEYWORD for JS (unlike TypeScriptParserFactory, which wires
        // RecogniseIdentifiersTokenVisitor), IGNORE_IDENTIFIERS erases EVERY identifier for
        // JS - including real language keywords like 'if' and 'return' - not just
        // user-defined names. Contrast with TypeScriptParserTest's
        // ignoreIdentifiersPreservesControlFlowKeywords, where 'if'/'while' do survive.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rIf    = parse("if (x) {\n", opts);
        final ParseResult rWhile = parse("while (x) {\n", opts);
        assertEquals(rIf.get(0), rWhile.get(0));
    }

    // -------------------------------------------------------------------------
    // 10. Sample file
    // -------------------------------------------------------------------------

    @Test
    public void sampleFileParsesSuccessfully() throws Exception {
        final LineListener lineListener = new TestLineListener();
        final Parser parser = new JavaScriptParserFactory().createParser(lineListener, new Options());
        final int ret = parser.parse(new FileReader(new File("test/data/javascript/test.js")));
        assertEquals(47, ret);
    }
}
