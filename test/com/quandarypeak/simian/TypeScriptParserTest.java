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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for TypeScriptParserFactory: fingerprint quality, option behaviours, and known limitations.
 *
 * <p><b>Parser internals:</b> TypeScript uses JavaScriptParser (shared with JavaScriptParserFactory,
 * backed by the same C-family StreamTokenizer as Java/C#/C++) with a pre-processing step via
 * JavaScriptTemplateLiteralNormalisingReader. Single-line (//) and block (/* *&#47;) comments
 * are stripped; semicolons are whitespace; '#' and '$' are word characters; '.' and '/' are
 * ordinary punctuation. RecogniseIdentifiersTokenVisitor promotes keywords to type=KEYWORD
 * and built-in types to type=TYPE. Import lines are unconditionally suppressed by
 * IgnoreLinesTokenVisitor.
 *
 * <p><b>Fingerprint format:</b>
 * <ul>
 *   <li>Numbers are doubles: 42 becomes "42.0"</li>
 *   <li>Space appears only between two consecutive identifiers; punctuation carries no space</li>
 *   <li>Semicolons are whitespace and do not appear in fingerprints</li>
 *   <li>Indentation is ignored</li>
 * </ul>
 *
 */
public class TypeScriptParserTest {

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

        boolean contains(final String fp) { return fingerprints.contains(fp); }
    }

    /** All options cleared - only the parser's unconditional behaviours apply. */
    private static Options bare() {
        final Options opts = new Options();
        opts.clear();
        return opts;
    }

    private static ParseResult parse(final String code, final Options opts) throws IOException {
        final CapturingLineListener listener = new CapturingLineListener();
        final Parser parser = new TypeScriptParserFactory().createParser(listener, opts);
        final int rawLines = parser.parse(new StringReader(code));
        return new ParseResult(rawLines, listener.lines);
    }

    private static ParseResult parseResource(final String path, final Options opts) throws IOException {
        final CapturingLineListener listener = new CapturingLineListener();
        final Parser parser = new TypeScriptParserFactory().createParser(listener, opts);
        try (InputStream is = TypeScriptParserTest.class.getResourceAsStream(path);
             Reader reader = new InputStreamReader(is, "UTF-8")) {
            final int rawLines = parser.parse(reader);
            return new ParseResult(rawLines, listener.lines);
        }
    }

    // -------------------------------------------------------------------------
    // 1. Comment stripping
    // -------------------------------------------------------------------------

    @Test
    public void singleLineCommentProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("// a TypeScript comment\n", bare());
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
    public void multiLineBlockCommentProducesNoFingerprints() throws IOException {
        final ParseResult r = parse("/**\n * JSDoc comment\n * @param x a number\n */\n", bare());
        assertEquals(4, r.rawLineCount);
        assertEquals(0, r.count());
    }

    @Test
    public void inlineCommentIsStrippedFromFingerprint() throws IOException {
        // Tokens before the '//' survive; everything after is dropped.
        final ParseResult rWith    = parse("x = 5 // assign x\n", bare());
        final ParseResult rWithout = parse("x = 5\n", bare());
        assertEquals(1, rWith.count());
        assertEquals(rWithout.get(0), rWith.get(0));
    }

    @Test
    public void blankLineProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("x = 1\n\ny = 2\n", bare());
        assertEquals(3, r.rawLineCount);
        assertEquals(2, r.count());
    }

    // -------------------------------------------------------------------------
    // 2. Import and package suppression
    //    IgnoreLinesTokenVisitor is always active (unconditional, not option-gated).
    // -------------------------------------------------------------------------

    @Test
    public void importLineIsSuppressed() throws IOException {
        final ParseResult r = parse("import { Component } from '@angular/core';\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void namedImportLineIsSuppressed() throws IOException {
        final ParseResult r = parse("import type { Foo, Bar } from './types';\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void defaultImportLineIsSuppressed() throws IOException {
        final ParseResult r = parse("import React from 'react';\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void packageLineIsNotSuppressed() throws IOException {
        // 'package' was removed from IMPORT_LINE_TRIGGERS — TypeScript has no package
        // declarations, so suppressing on that word would be incorrect.
        final ParseResult r = parse("package com.example;\n", bare());
        assertEquals(1, r.count());
    }

    @Test
    public void exportDoesNotSuppressLine() throws IOException {
        // 'export' is a MODIFIER, not an import-line trigger - the line is NOT suppressed.
        final ParseResult r = parse("export const PI = 3;\n", bare());
        assertEquals(1, r.count());
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
    // 4. Type annotations
    //    The colon ':' is ordinary punctuation; the type name is an identifier.
    //    Result: 'param: string' -> 'param:string' (no space around the colon).
    // -------------------------------------------------------------------------

    @Test
    public void typeAnnotationColonHasNoSpacing() throws IOException {
        // Confirm that ':' and the type name are adjacent in the fingerprint.
        final ParseResult r = parse("let x: number = 5;\n", bare());
        assertEquals(1, r.count());
        assertEquals("let x:number=5.0", r.get(0));
    }

    @Test
    public void differentTypeAnnotationsProduceDifferentFingerprints() throws IOException {
        final ParseResult rNum = parse("let x: number = 0;\n", bare());
        final ParseResult rStr = parse("let x: string = 0;\n", bare());
        assertNotEquals(rNum.get(0), rStr.get(0));
    }

    @Test
    public void returnTypeAnnotationDifferentiatesFunctions() throws IOException {
        final ParseResult rVoid = parse("function foo(): void {\n", bare());
        final ParseResult rNum  = parse("function foo(): number {\n", bare());
        assertNotEquals(rVoid.get(0), rNum.get(0));
    }

    @Test
    public void typeAnnotationsNormalisedByIgnoreIdentifiers() throws IOException {
        // With IGNORE_IDENTIFIERS all identifiers - including type names - become '_',
        // so 'let x: number' and 'let x: string' collapse to the same fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rNum = parse("let x: number = 0;\n", opts);
        final ParseResult rStr = parse("let x: string = 0;\n", opts);
        assertEquals(rNum.get(0), rStr.get(0));
    }

    // -------------------------------------------------------------------------
    // 5. Template literals
    //    JavaScriptTemplateLiteralNormalisingReader converts backtick template literals
    //    to double-quoted strings before the StreamTokenizer runs.
    //    Static text is preserved; ${...} interpolation expressions are replaced with a
    //    single space, so the expression body does not appear as code tokens.
    //    Multi-line template literals are collapsed to a single line (blank lines are
    //    re-inserted after the closing quote to preserve total line count).
    // -------------------------------------------------------------------------

    @Test
    public void templateLiteralProducesSameFingerprintAsRegularString() throws IOException {
        // After normalisation, backtick literals and double-quoted strings are both
        // represented as a StreamTokenizer string token, producing identical fingerprints.
        final ParseResult rTemplate = parse("const msg = `hello world`;\n", bare());
        final ParseResult rString   = parse("const msg = \"hello world\";\n", bare());
        assertEquals(rTemplate.get(0), rString.get(0));
    }

    @Test
    public void templateLiteralInterpolatedVariablesDoNotAppearInFingerprint() throws IOException {
        // ${...} expressions are replaced with a space before tokenisation, so two template
        // literals that differ only in the interpolated variable produce identical fingerprints.
        final ParseResult rFirst = parse("return `Hello, ${firstName}`;\n", bare());
        final ParseResult rLast  = parse("return `Hello, ${lastName}`;\n", bare());
        assertEquals(rFirst.get(0), rLast.get(0));
    }

    @Test
    public void twoIdenticalTemplateLiteralCallsProduceSameFingerprint() throws IOException {
        // Structurally identical template literal calls produce the same fingerprint.
        final ParseResult rA = parse("console.log(`${x} and ${y}`);\n", bare());
        final ParseResult rB = parse("console.log(`${x} and ${y}`);\n", bare());
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void multiLineTemplateLiteralPreservesLineCount() throws IOException {
        // A template literal spanning 3 lines reports 3 raw lines (same as without
        // normalisation): the literal is collapsed onto the first line, two blank
        // lines are inserted after it, and the trailing ';' + newline remain.
        final ParseResult r = parse("const s = `line one\nline two\nline three`;\n", bare());
        assertEquals(3, r.rawLineCount);
        assertEquals(1, r.count()); // only one non-blank fingerprint line
    }

    @Test
    public void ignoreStringsNormalisesTemplateLiterals() throws IOException {
        // With IGNORE_STRINGS, both regular strings and template literals are suppressed,
        // so two assignments that differ only in their string/template-literal value match.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_STRINGS, Boolean.TRUE);
        final ParseResult rStr      = parse("const msg = \"hello\";\n", opts);
        final ParseResult rTemplate = parse("const msg = `world`;\n", opts);
        assertEquals(rStr.get(0), rTemplate.get(0));
    }

    // -------------------------------------------------------------------------
    // 6. IGNORE_MODIFIERS
    //    Strips words in the MODIFIERS set: abstract, accessor, async, class, const,
    //    declare, enum, export, extends, function, implements, interface, namespace,
    //    module, override, private, protected, public, readonly, satisfies, static,
    //    type, var, let.
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
    public void ignoreModifiersStripsVisibilityKeywords() throws IOException {
        // 'public', 'private', 'protected' are all MODIFIERS - stripped together.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult pub  = parse("public name: string;\n", opts);
        final ParseResult priv = parse("private name: string;\n", opts);
        final ParseResult prot = parse("protected name: string;\n", opts);
        assertEquals(pub.get(0), priv.get(0));
        assertEquals(pub.get(0), prot.get(0));
    }

    @Test
    public void ignoreModifiersNormalisesConstAndLet() throws IOException {
        // Both 'const' and 'let' are MODIFIERS; after stripping they match.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rConst = parse("const x = 5;\n", opts);
        final ParseResult rLet   = parse("let x = 5;\n", opts);
        assertEquals(rConst.get(0), rLet.get(0));
    }

    @Test
    public void ignoreModifiersStripsInterfaceKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rInterface = parse("interface Foo {\n", opts);
        final ParseResult rPlain     = parse("Foo {\n", bare());
        assertEquals(rPlain.get(0), rInterface.get(0));
    }

    @Test
    public void ignoreModifiersStripsAbstractKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rAbstract = parse("abstract class Foo {\n", opts);
        final ParseResult rPlain    = parse("class Foo {\n", opts);
        assertEquals(rPlain.get(0), rAbstract.get(0));
    }

    @Test
    public void ignoreModifiersStripsEnumKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rEnum  = parse("enum Color {\n", opts);
        final ParseResult rPlain = parse("Color {\n", bare());
        assertEquals(rPlain.get(0), rEnum.get(0));
    }

    @Test
    public void ignoreModifiersStripsExportKeyword() throws IOException {
        // 'export' does not suppress the line (see exportDoesNotSuppressLine); this test
        // confirms it is separately stripped as a MODIFIER when the option is active.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rExport = parse("export const PI = 3;\n", opts);
        final ParseResult rPlain  = parse("const PI = 3;\n", opts);
        assertEquals(rPlain.get(0), rExport.get(0));
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
    public void ignoreModifiersStripsImplementsKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rImplements = parse("class Foo implements Bar {\n", opts);
        final ParseResult rPlain      = parse("class Foo Bar {\n", opts);
        assertEquals(rPlain.get(0), rImplements.get(0));
    }

    @Test
    public void ignoreModifiersStripsNamespaceAndModuleKeywords() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rNamespace = parse("namespace Foo {\n", opts);
        final ParseResult rModule    = parse("module Foo {\n", opts);
        final ParseResult rPlain     = parse("Foo {\n", bare());
        assertEquals(rPlain.get(0), rNamespace.get(0));
        assertEquals(rPlain.get(0), rModule.get(0));
    }

    @Test
    public void ignoreModifiersStripsOverrideKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rOverride = parse("override method() {\n", opts);
        final ParseResult rPlain    = parse("method() {\n", opts);
        assertEquals(rPlain.get(0), rOverride.get(0));
    }

    @Test
    public void ignoreModifiersStripsSatisfiesKeyword() throws IOException {
        // Only the 'satisfies' word itself is a MODIFIER - the type expression that follows
        // it ('Foo') is an ordinary identifier and is not removed, so the plain comparison
        // target keeps 'Foo' too.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rSatisfies = parse("x = obj satisfies Foo;\n", opts);
        final ParseResult rPlain     = parse("x = obj Foo;\n", bare());
        assertEquals(rPlain.get(0), rSatisfies.get(0));
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
    public void ignoreModifiersStripsVarKeyword() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rVar   = parse("var x = 5;\n", opts);
        final ParseResult rConst = parse("const x = 5;\n", opts);
        assertEquals(rConst.get(0), rVar.get(0));
    }

    @Test
    public void ignoreModifiersStripsTypeKeywordInAliasDeclaration() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rType  = parse("type Foo = string;\n", opts);
        final ParseResult rPlain = parse("Foo = string;\n", bare());
        assertEquals(rPlain.get(0), rType.get(0));
    }

    @Test
    public void ignoreModifiersStripsAccessorKeyword() throws IOException {
        // 'accessor' is the TC39 auto-accessor class field modifier (e.g. 'accessor count = 0;').
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rAccessor = parse("accessor count = 0;\n", opts);
        final ParseResult rPlain    = parse("count = 0;\n", bare());
        assertEquals(rPlain.get(0), rAccessor.get(0));
    }

    // -------------------------------------------------------------------------
    // 7. TypeScript-specific modifiers
    //    'async', 'readonly', and 'declare' are in MODIFIERS and are stripped when
    //    IGNORE_MODIFIERS is active. 'await' is intentionally absent from MODIFIERS —
    //    it is an expression operator ('await fetch()'), not a declaration modifier,
    //    and must produce different fingerprints from non-awaited calls.
    // -------------------------------------------------------------------------

    @Test
    public void asyncFunctionMatchesPlainFunctionWithIgnoreModifiers() throws IOException {
        // 'async' is now in MODIFIERS. Both 'async' and 'function' are stripped, so an
        // async function and its sync counterpart produce identical fingerprints.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rAsync = parse("async function fetch(): Promise<void> {\n", opts);
        final ParseResult rPlain = parse("function fetch(): Promise<void> {\n", opts);
        assertEquals(rAsync.get(0), rPlain.get(0));
    }

    @Test
    public void readonlyIsStrippedByIgnoreModifiers() throws IOException {
        // 'readonly' is now in MODIFIERS and is stripped when IGNORE_MODIFIERS is active.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rReadonly = parse("readonly name: string;\n", opts);
        final ParseResult rPlain    = parse("name: string;\n", opts);
        assertEquals(rReadonly.get(0), rPlain.get(0));
    }

    @Test
    public void declareIsStrippedByIgnoreModifiers() throws IOException {
        // 'declare' and 'const' are both MODIFIERS; both are stripped, leaving only the
        // identifier and its type annotation.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rDeclare = parse("declare const VERSION: string;\n", opts);
        final ParseResult rPlain   = parse("const VERSION: string;\n", opts);
        assertEquals(rDeclare.get(0), rPlain.get(0));
    }

    @Test
    public void awaitExpressionIsPreservedByIgnoreModifiers() throws IOException {
        // 'await' is not a MODIFIER — it is an expression operator. Stripping it would
        // make 'await fetch(url)' and 'fetch(url)' fingerprint-identical, masking a
        // structural difference in async call sites.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rAwait = parse("return await fetch(url);\n", opts);
        final ParseResult rPlain = parse("return fetch(url);\n", opts);
        assertNotEquals(rAwait.get(0), rPlain.get(0));
    }

    @Test
    public void getSetAccessorKeywordsAreStrippedByIgnoreModifiers() throws IOException {
        // 'get' and 'set' are now in MODIFIERS, so an accessor and a plain method of the
        // same name produce identical fingerprints when IGNORE_MODIFIERS is active.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rGetter = parse("get celsius() {\n", opts);
        final ParseResult rSetter = parse("set celsius(v) {\n", opts);
        final ParseResult rPlain  = parse("celsius() {\n", opts);
        assertEquals(rPlain.get(0), rGetter.get(0));
        assertNotEquals(rPlain.get(0), rSetter.get(0)); // setter still has the 'v' parameter
    }

    @Test
    public void outVarianceAnnotationIsStrippedByIgnoreModifiers() throws IOException {
        // 'out' is now in MODIFIERS, so a covariant type parameter and a plain one
        // produce identical fingerprints when IGNORE_MODIFIERS is active.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rOut   = parse("interface Producer<out T> {\n", opts);
        final ParseResult rPlain = parse("interface Producer<T> {\n", opts);
        assertEquals(rPlain.get(0), rOut.get(0));
    }

    @Test
    public void functionKeywordAloneIsStrippedByIgnoreModifiers() throws IOException {
        // Isolates 'function' from 'async' (asyncFunctionMatchesPlainFunctionWithIgnoreModifiers
        // strips both together, so it can't tell them apart). Comparing against a fingerprint
        // built with no 'function' keyword at all confirms 'function' itself is stripped.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rFunction = parse("function fetch(): void {\n", opts);
        final ParseResult rPlain    = parse("fetch(): void {\n", bare());
        assertEquals(rPlain.get(0), rFunction.get(0));
    }

    // -------------------------------------------------------------------------
    // 8. IGNORE_IDENTIFIERS
    //    RecogniseIdentifiersTokenVisitor now classifies TypeScript/JavaScript keywords
    //    as KEYWORD, so they survive IGNORE_IDENTIFIERS. Only user-defined names
    //    (VARIABLE, METHOD, TYPE, CONSTANT) are collapsed to '_'.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreIdentifiersPreservesControlFlowKeywords() throws IOException {
        // 'if' and 'while' are now KEYWORD — they survive IGNORE_IDENTIFIERS.
        // Structurally different headers (different keywords) produce different fingerprints.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rIf    = parse("if (x) {\n", opts);
        final ParseResult rWhile = parse("while (x) {\n", opts);
        assertNotEquals(rIf.get(0), rWhile.get(0));
    }

    @Test
    public void ignoreIdentifiersMakesReturnStatementsMatch() throws IOException {
        // 'return' survives as KEYWORD; only the returned value (VARIABLE) becomes '_'.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rA = parse("return someValue;\n", opts);
        final ParseResult rB = parse("return otherValue;\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void ignoreIdentifiersNormalisesTypeAnnotationsNotDeclarationKeywords() throws IOException {
        // Declaration keywords (const, let) are KEYWORD and survive. Only user-defined
        // names and type annotation names (TYPE) become '_'. Two declarations that differ
        // only in their type annotation produce the same fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rNum = parse("const x: number = 5;\n", opts);
        final ParseResult rStr = parse("const x: string = 5;\n", opts);
        assertEquals(rNum.get(0), rStr.get(0));
    }

    @Test
    public void constAndLetProduceDifferentFingerprintsUnderIgnoreIdentifiers() throws IOException {
        // 'const' and 'let' are both KEYWORD — they survive IGNORE_IDENTIFIERS.
        // Use IGNORE_MODIFIERS to strip them if declaration-keyword equivalence is needed.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rConst = parse("const x: number = 5;\n", opts);
        final ParseResult rLet   = parse("let x: number = 5;\n", opts);
        assertNotEquals(rConst.get(0), rLet.get(0));
    }

    @Test
    public void asTypeAssertionKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'as' (type assertion, e.g. 'x as string') is KEYWORD and must survive
        // IGNORE_IDENTIFIERS. It is one of the most common TypeScript-only keywords.
        // (The TYPE-classified name that follows, e.g. 'string', is erased to '_' the same
        // as any other type annotation - see ignoreIdentifiersNormalisesTypeAnnotationsNotDeclarationKeywords -
        // so this only checks that 'as' itself is preserved, not the asserted type.)
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult r = parse("return value as string;\n", opts);
        assertTrue(r.get(0).contains("as"));
    }

    @Test
    public void declarationAndVisibilityKeywordsSurviveIgnoreIdentifiers() throws IOException {
        // 'enum', 'namespace', 'module', 'abstract', 'implements', and 'override' are all
        // KEYWORD - they survive IGNORE_IDENTIFIERS even though they are also MODIFIERS
        // (a different option, IgnoreWordsTokenVisitor, governs stripping them).
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        assertTrue(parse("enum Color {\n", opts).get(0).startsWith("enum"));
        assertTrue(parse("namespace Foo {\n", opts).get(0).startsWith("namespace"));
        assertTrue(parse("module Foo {\n", opts).get(0).startsWith("module"));
        assertTrue(parse("abstract class Foo {\n", opts).get(0).startsWith("abstract"));
        assertTrue(parse("class Foo implements Bar {\n", opts).get(0).contains("implements"));
        assertTrue(parse("override method() {\n", opts).get(0).startsWith("override"));
    }

    @Test
    public void typeOperatorKeywordsSurviveIgnoreIdentifiers() throws IOException {
        // 'asserts', 'global', 'infer', 'is', 'keyof', 'satisfies', 'unique', and 'type' are
        // all KEYWORD - none are user-defined names, so IGNORE_IDENTIFIERS must not erase
        // them. ('from' is omitted: it only appears on import/export lines, which
        // IgnoreLinesTokenVisitor suppresses entirely regardless of this option.)
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        assertTrue(parse("function f(x: unknown): asserts x is string {\n", opts).get(0).contains("asserts"));
        assertTrue(parse("declare global {\n", opts).get(0).contains("global"));
        assertTrue(parse("type Elem<T> = T extends (infer U)[] ? U : never;\n", opts).get(0).contains("infer"));
        assertTrue(parse("function f(x: unknown): x is string {\n", opts).get(0).contains("is"));
        assertTrue(parse("type Keys = keyof Foo;\n", opts).get(0).contains("keyof"));
        assertTrue(parse("const x = obj satisfies Foo;\n", opts).get(0).contains("satisfies"));
        assertTrue(parse("type Id<T> = T & unique symbol;\n", opts).get(0).contains("unique"));
        assertTrue(parse("type Foo = string;\n", opts).get(0).startsWith("type"));
    }

    @Test
    public void remainingControlFlowKeywordsSurviveIgnoreIdentifiers() throws IOException {
        // Covers the control-flow KEYWORDS not already exercised by
        // ignoreIdentifiersPreservesControlFlowKeywords / ignoreIdentifiersMakesReturnStatementsMatch:
        // break, case, catch, continue, debugger, default, delete, do, else, finally,
        // instanceof, new, of, super, switch, this, throw, try, typeof, with, yield.
        // Each snippet is written on a single source line so it produces exactly one
        // fingerprint entry (ParseResult.get(0)), matching this file's existing convention.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);

        final String tryFingerprint = parse(
                "try { doSomething(); } catch (e) { handle(e); } finally { cleanup(); }\n", opts).get(0);
        assertTrue(tryFingerprint.contains("try"));
        assertTrue(tryFingerprint.contains("catch"));
        assertTrue(tryFingerprint.contains("finally"));

        final String switchFingerprint = parse(
                "switch (x) { case 1: break; default: continue; }\n", opts).get(0);
        assertTrue(switchFingerprint.contains("switch"));
        assertTrue(switchFingerprint.contains("case"));
        assertTrue(switchFingerprint.contains("break"));
        assertTrue(switchFingerprint.contains("default"));
        assertTrue(switchFingerprint.contains("continue"));

        assertTrue(parse("for (const x of items) { doStuff(); }\n", opts).get(0).contains("of"));
        assertTrue(parse("do { x(); } while (cond);\n", opts).get(0).contains("do"));
        assertTrue(parse("if (x) { y(); } else { z(); }\n", opts).get(0).contains("else"));
        assertTrue(parse("delete obj.prop;\n", opts).get(0).contains("delete"));

        final String newFingerprint = parse("this.value = new Foo();\n", opts).get(0);
        assertTrue(newFingerprint.contains("this"));
        assertTrue(newFingerprint.contains("new"));

        final String instanceofFingerprint = parse("if (x instanceof Foo) { throw x; }\n", opts).get(0);
        assertTrue(instanceofFingerprint.contains("instanceof"));
        assertTrue(instanceofFingerprint.contains("throw"));

        assertTrue(parse("yield x;\n", opts).get(0).contains("yield"));
        assertTrue(parse("debugger;\n", opts).get(0).contains("debugger"));
        assertTrue(parse("with (obj) { x(); }\n", opts).get(0).contains("with"));
        assertTrue(parse("typeof x;\n", opts).get(0).contains("typeof"));
        assertTrue(parse("super();\n", opts).get(0).contains("super"));
    }

    @Test
    public void literalKeywordsSurviveIgnoreIdentifiers() throws IOException {
        // 'false', 'null', 'true', and 'undefined' are KEYWORD (literal keywords) - they
        // must survive IGNORE_IDENTIFIERS unchanged, unlike a real identifier.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final String fingerprint = parse("const a = true, b = false, c = null, d = undefined;\n", opts).get(0);
        assertTrue(fingerprint.contains("true"));
        assertTrue(fingerprint.contains("false"));
        assertTrue(fingerprint.contains("null"));
        assertTrue(fingerprint.contains("undefined"));
    }

    @Test
    public void inKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'in' (the 'in' operator, e.g. 'key in obj', and for-in loops) is KEYWORD and must
        // survive IGNORE_IDENTIFIERS while the surrounding variable names are erased.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rWithIn    = parse("if (key in obj) {\n", opts);
        final ParseResult rWithoutIn = parse("if (key obj) {\n", opts);
        assertNotEquals(rWithoutIn.get(0), rWithIn.get(0));
    }

    @Test
    public void modifierKeywordsAlsoSurviveIgnoreIdentifiers() throws IOException {
        // 'declare', 'export', 'extends', 'function', 'interface', 'static', 'var', 'out',
        // 'readonly', 'get', 'set', 'async', and 'await' are all KEYWORD, and are already
        // verified to be stripped by IGNORE_MODIFIERS elsewhere in this file. This confirms
        // the separate, unrelated claim that they also survive IGNORE_IDENTIFIERS - a
        // different option governed by a different visitor (IgnoreIdentifiersTokenVisitor,
        // which checks classified type, vs IgnoreWordsTokenVisitor, which matches by name).
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        assertTrue(parse("declare const VERSION: string;\n", opts).get(0).contains("declare"));
        assertTrue(parse("export const PI = 3;\n", opts).get(0).contains("export"));
        assertTrue(parse("class Foo extends Base {\n", opts).get(0).contains("extends"));
        assertTrue(parse("function fetch() {\n", opts).get(0).startsWith("function"));
        assertTrue(parse("interface Foo {\n", opts).get(0).startsWith("interface"));
        assertTrue(parse("static count() {\n", opts).get(0).startsWith("static"));
        assertTrue(parse("var x = 5;\n", opts).get(0).startsWith("var"));
        assertTrue(parse("interface Producer<out T> {\n", opts).get(0).contains("out"));
        assertTrue(parse("readonly name: string;\n", opts).get(0).startsWith("readonly"));
        assertTrue(parse("get celsius() {\n", opts).get(0).startsWith("get"));
        assertTrue(parse("set celsius(v) {\n", opts).get(0).startsWith("set"));
        assertTrue(parse("async function fetch() {\n", opts).get(0).contains("async"));
        assertTrue(parse("return await fetch(url);\n", opts).get(0).contains("await"));
    }

    // -------------------------------------------------------------------------
    // 9. IGNORE_VARIABLE_NAMES
    //    RecogniseIdentifiersTokenVisitor now classifies user-defined identifiers as
    //    VARIABLE or METHOD, so IGNORE_VARIABLE_NAMES correctly strips them.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreVariableNamesStripsUserDefinedIdentifiers() throws IOException {
        // With RecogniseIdentifiersTokenVisitor, user-defined names get type=VARIABLE.
        // IGNORE_VARIABLE_NAMES strips VARIABLE-typed tokens, normalising names.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rA = parse("x + y\n", opts);
        final ParseResult rB = parse("alpha + beta\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void getSetAndOutAreKeywordsNotVariableNames() throws IOException {
        // 'get', 'set', and 'out' are contextual keywords, not user-defined names.  Before
        // being added to KEYWORDS they were classified as VARIABLE by
        // RecogniseIdentifiersTokenVisitor (the catch-all branch), so IGNORE_VARIABLE_NAMES
        // would have erased them the same way it erases a real variable name. As KEYWORD
        // tokens they must now survive IGNORE_VARIABLE_NAMES untouched.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rGetter = parse("get celsius() {\n", opts);
        final ParseResult rOut    = parse("interface Producer<out T> {\n", opts);
        assertTrue(rGetter.get(0).startsWith("get "));
        assertTrue(rOut.get(0).contains("out"));
    }

    // -------------------------------------------------------------------------
    // 10. Generic type syntax
    //     '<' and '>' are ordinary punctuation; the type argument identifier
    //     appears in the fingerprint between them.
    // -------------------------------------------------------------------------

    @Test
    public void genericTypeArgumentAppearsInFingerprint() throws IOException {
        final ParseResult rStr = parse("const arr: Array<string> = [];\n", bare());
        final ParseResult rNum = parse("const arr: Array<number> = [];\n", bare());
        assertNotEquals(rStr.get(0), rNum.get(0));
    }

    @Test
    public void genericTypesNormalisedByIgnoreIdentifiers() throws IOException {
        // With IGNORE_IDENTIFIERS, 'Array<string>' and 'Array<number>' collapse to
        // the same punctuation skeleton since both 'Array' and the type argument become '_'.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rStr = parse("const arr: Array<string> = [];\n", opts);
        final ParseResult rNum = parse("const arr: Array<number> = [];\n", opts);
        assertEquals(rStr.get(0), rNum.get(0));
    }

    // -------------------------------------------------------------------------
    // 11. BALANCE_PARENTHESES
    // -------------------------------------------------------------------------

    @Test
    public void balanceParenthesesMergesMultiLineCallIntoOneLine() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.BALANCE_PARENTHESES, Boolean.TRUE);

        final String multiLine = "foo(\n    a,\n    b\n);\n";
        final ParseResult rWith    = parse(multiLine, opts);
        final ParseResult rWithout = parse(multiLine, bare());

        assertEquals(4, rWithout.rawLineCount);
        assertEquals(4, rWithout.count());
        assertEquals(1, rWith.count());
    }

    @Test
    public void balanceParenthesesMergesMultiLineFunctionSignature() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.BALANCE_PARENTHESES, Boolean.TRUE);

        final String sig = "function greet(\n    name: string,\n    age: number\n): void {\n";
        final ParseResult rWith    = parse(sig, opts);
        final ParseResult rWithout = parse(sig, bare());

        assertEquals(4, rWithout.rawLineCount);
        assertEquals(4, rWithout.count());
        // With balance: the 4 lines collapse to fewer fingerprints.
        assertTrue(rWith.count() < rWithout.count());
    }

    // -------------------------------------------------------------------------
    // 12. BALANCE_SQUARE_BRACKETS
    // -------------------------------------------------------------------------

    @Test
    public void balanceSquareBracketsMergesMultiLineArrayLiteral() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.BALANCE_SQUARE_BRACKETS, Boolean.TRUE);

        final String multiLine = "const arr = [\n    1,\n    2,\n    3\n];\n";
        final ParseResult rWith    = parse(multiLine, opts);
        final ParseResult rWithout = parse(multiLine, bare());

        assertEquals(5, rWithout.rawLineCount);
        assertTrue(rWithout.count() > 1);
        assertEquals(1, rWith.count());
    }

    // -------------------------------------------------------------------------
    // 13. Private class fields ('#' as word character)
    //     CFamilyParser configures '#' as a word char, so '#name' is one identifier.
    // -------------------------------------------------------------------------

    @Test
    public void privateFieldHashPrefixIsOneToken() throws IOException {
        // '#name' is a single identifier token; it differs from 'name'.
        final ParseResult rHash  = parse("this.#count = 0;\n", bare());
        final ParseResult rPlain = parse("this.count = 0;\n", bare());
        assertNotEquals(rHash.get(0), rPlain.get(0));
    }

    @Test
    public void twoPrivateFieldAccessesWithSameNameMatch() throws IOException {
        final ParseResult rA = parse("this.#count = 0;\n", bare());
        final ParseResult rB = parse("that.#count = 0;\n", bare());
        // Only the object ('this' vs 'that') differs - fingerprints differ too.
        assertNotEquals(rA.get(0), rB.get(0));
    }

    // -------------------------------------------------------------------------
    // 14. Structural equivalence with IGNORE_IDENTIFIERS
    // -------------------------------------------------------------------------

    @Test
    public void functionsIdenticalExceptParameterTypesMatchWithIgnoreIdentifiers() throws IOException {
        // Two functions that differ only in TypeScript type annotations match after
        // identifier erasure - the strongest test of duplicate detection across typed variants.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);

        final ParseResult rNum = parse("function add(a: number, b: number): number {\n", opts);
        final ParseResult rStr = parse("function add(a: string, b: string): string {\n", opts);
        assertEquals(rNum.get(0), rStr.get(0));
    }

    @Test
    public void classesWithDifferentNamesMatchWithIgnoreIdentifiers() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);

        final ParseResult rFoo = parse("class Foo extends Base {\n", opts);
        final ParseResult rBar = parse("class Bar extends Base {\n", opts);
        assertEquals(rFoo.get(0), rBar.get(0));
    }

    // -------------------------------------------------------------------------
    // 15. IGNORE_TYPE_ANNOTATIONS
    //     IgnoreTypeAnnotationsTokenVisitor buffers each ':' and, when the token
    //     immediately following is a TYPE-classified identifier, suppresses both
    //     the ':' and the complete type expression (including generics, arrays,
    //     union/intersection operators). When ':' is followed by a non-TYPE token
    //     (e.g. a number in an object literal or a variable in a ternary), the ':'
    //     is emitted unchanged.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreTypeAnnotationsNormalisesVariableDeclarations() throws IOException {
        // 'let x: string = 5' and 'let x: number = 5' both collapse to 'let x = 5.0'.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rStr = parse("let x: string = 5;\n", opts);
        final ParseResult rNum = parse("let x: number = 5;\n", opts);
        assertEquals(rStr.get(0), rNum.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsNormalisesFunctionParameters() throws IOException {
        // Type annotations on parameters are suppressed; only the parameter names remain.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rStr = parse("function f(x: string) {\n", opts);
        final ParseResult rNum = parse("function f(x: number) {\n", opts);
        assertEquals(rStr.get(0), rNum.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsStripsReturnType() throws IOException {
        // The return type annotation ('): Type') is also suppressed.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rVoid = parse("function f(): void {\n", opts);
        final ParseResult rStr  = parse("function f(): string {\n", opts);
        assertEquals(rVoid.get(0), rStr.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsHandlesUnionTypes() throws IOException {
        // Union operators ('|') and all member types are suppressed when in a type
        // annotation context — 'string | number' and 'boolean' both become absent.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rUnion  = parse("let x: string | number = 5;\n", opts);
        final ParseResult rSimple = parse("let x: boolean = 5;\n", opts);
        assertEquals(rUnion.get(0), rSimple.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsHandlesGenericTypes() throws IOException {
        // The generic argument list ('<string>', '<number>') is suppressed along with
        // the outer type name ('Array'), collapsing 'Array<string>' and 'Array<number>'
        // to the same fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rStr = parse("const arr: Array<string> = [];\n", opts);
        final ParseResult rNum = parse("const arr: Array<number> = [];\n", opts);
        assertEquals(rStr.get(0), rNum.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsPreservesObjectLiteralColons() throws IOException {
        // In an object literal the ':' is followed by a value (number, string, variable),
        // not a type name. The ':' is flushed immediately and appears in the fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rWith    = parse("const obj = {a: 1, b: 2};\n", opts);
        final ParseResult rWithout = parse("const obj = {a: 1, b: 2};\n", bare());
        assertEquals(rWithout.get(0), rWith.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsCombinedWithIgnoreVariableNames() throws IOException {
        // Combining IGNORE_TYPE_ANNOTATIONS and IGNORE_VARIABLE_NAMES normalises two
        // functions that differ only in parameter names and types.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rA = parse("function process(x: string, y: number): void {\n", opts);
        final ParseResult rB = parse("function process(a: number, b: string): boolean {\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsSuppressesSingleCharGenericTypeParam() throws IOException {
        // Single uppercase letters (T, K, V, …) must be classified as TYPE so that
        // ': T' annotations are suppressed — previously they were misclassified as CONSTANT.
        // The '<T>' declaration is NOT a type annotation, so it still appears in the fingerprint;
        // only the ': T' and ': U' annotation usages are suppressed and become identical.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rT = parse("function process(x: T): T {\n", opts);
        final ParseResult rU = parse("function process(x: U): U {\n", opts);
        assertEquals(rT.get(0), rU.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsSuppressesTupleTypeAnnotations() throws IOException {
        // Tuple types ': [string, number]' are suppressed — PENDING_COLON state now
        // handles '[' by entering IN_ARRAY instead of flushing the buffered ':'.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rTuple  = parse("const pair: [string, number] = [1, 2];\n", opts);
        final ParseResult rSimple = parse("const pair: string = [1, 2];\n", opts);
        assertEquals(rTuple.get(0), rSimple.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsSuppressesNestedArrayTypeInTuple() throws IOException {
        // Tuple elements can themselves be array types, e.g. '[string[], number]'.
        // The inner ']' of 'string[]' must NOT exit suppression — the _arrayDepth
        // counter tracks nesting so only the outer ']' terminates the tuple.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rNested = parse("const x: [string[], number] = [];\n", opts);
        final ParseResult rFlat   = parse("const x: [string, number] = [];\n", opts);
        assertEquals(rNested.get(0), rFlat.get(0));
    }

    @Test
    public void ignoreTypeAnnotationsSuppressesDeepNestedArrayInTuple() throws IOException {
        // Two levels of nesting: '[string[][], number]' — '[][]' generates two '[' tokens
        // (depth goes to 1 then 2) and two ']' tokens before the outer ']' closes the tuple.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_TYPE_ANNOTATIONS, Boolean.TRUE);
        final ParseResult rDeep  = parse("const x: [string[][], number] = [];\n", opts);
        final ParseResult rFlat  = parse("const x: [string, number] = [];\n", opts);
        assertEquals(rDeep.get(0), rFlat.get(0));
    }

    // -------------------------------------------------------------------------
    // 16. Remaining TYPES entries
    //     'string'/'number'/'void'/'boolean' are already exercised above; this covers the
    //     rest of the TYPES set: any, bigint, never, object, symbol, unknown.
    // -------------------------------------------------------------------------

    @Test
    public void remainingBuiltInTypesProduceDifferentFingerprints() throws IOException {
        final ParseResult rAny     = parse("let x: any = 0;\n", bare());
        final ParseResult rBigint  = parse("let x: bigint = 0;\n", bare());
        final ParseResult rNever   = parse("let x: never = 0;\n", bare());
        final ParseResult rObject  = parse("let x: object = 0;\n", bare());
        final ParseResult rSymbol  = parse("let x: symbol = 0;\n", bare());
        final ParseResult rUnknown = parse("let x: unknown = 0;\n", bare());
        assertNotEquals(rAny.get(0), rBigint.get(0));
        assertNotEquals(rNever.get(0), rObject.get(0));
        assertNotEquals(rSymbol.get(0), rUnknown.get(0));
    }

    @Test
    public void remainingBuiltInTypesAreNormalisedByIgnoreIdentifiers() throws IOException {
        // TYPE-classified identifiers are erased to '_' under IGNORE_IDENTIFIERS, the same
        // as 'string'/'number' in ignoreIdentifiersNormalisesTypeAnnotationsNotDeclarationKeywords
        // (only KEYWORD tokens survive that option - see RecogniseIdentifiersTokenVisitor /
        // IgnoreIdentifiersTokenVisitor). Two declarations differing only in one of these
        // built-in type annotations therefore produce the same fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rAny    = parse("let x: any = 0;\n", opts);
        final ParseResult rBigint = parse("let x: bigint = 0;\n", opts);
        final ParseResult rNever  = parse("let x: never = 0;\n", opts);
        assertEquals(rAny.get(0), rBigint.get(0));
        assertEquals(rAny.get(0), rNever.get(0));
    }

    // -------------------------------------------------------------------------
    // 17. IGNORE_SUBTYPE_NAMES
    //     IgnoreSubtypeNamesTokenVisitor reduces a TYPE-classified compound PascalCase
    //     name to its last capitalised segment (e.g. 'StringBuilder' -> 'Builder').
    //     It only acts on type == TYPE; VARIABLE-classified identifiers are untouched.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreSubtypeNamesReducesCompoundTypeNameToLastCapitalisedSegment() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_SUBTYPE_NAMES, Boolean.TRUE);
        final ParseResult rCompound = parse("const x: StringBuilder = y;\n", opts);
        final ParseResult rShort    = parse("const x: Builder = y;\n", bare());
        assertEquals(rShort.get(0), rCompound.get(0));
    }

    @Test
    public void ignoreSubtypeNamesDoesNotAffectVariableNames() throws IOException {
        // 'stringBuilder' here is a lowercase identifier (VARIABLE), not a type annotation,
        // so IgnoreSubtypeNamesTokenVisitor must leave it - and other variable names -
        // untouched; two different variable names still produce different fingerprints.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_SUBTYPE_NAMES, Boolean.TRUE);
        final ParseResult rA = parse("stringBuilder + 1\n", opts);
        final ParseResult rB = parse("otherBuilder + 1\n", opts);
        assertNotEquals(rA.get(0), rB.get(0));
    }

    // -------------------------------------------------------------------------
    // 18. IGNORE_CURLY_BRACES
    //     IgnoreCurlyBracesTokenVisitor drops '{' and '}' from the fingerprint entirely.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreCurlyBracesRemovesBracesFromFingerprint() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_CURLY_BRACES, Boolean.TRUE);
        final ParseResult rBraces   = parse("if (x) { y(); }\n", opts);
        final ParseResult rNoBraces = parse("if (x) y();\n", bare());
        assertEquals(rNoBraces.get(0), rBraces.get(0));
    }

    @Test
    public void curlyBracesAppearInFingerprintByDefault() throws IOException {
        // Without IGNORE_CURLY_BRACES, braces are ordinary punctuation and differentiate
        // a braced block from an equivalent unbraced statement.
        final ParseResult rBraces   = parse("if (x) { y(); }\n", bare());
        final ParseResult rNoBraces = parse("if (x) y();\n", bare());
        assertNotEquals(rNoBraces.get(0), rBraces.get(0));
    }

    // -------------------------------------------------------------------------
    // 19. Sample file
    // -------------------------------------------------------------------------

    @Test
    public void sampleFileParsesSuccessfully() throws IOException {
        final ParseResult r = parseResource("/data/typeScript/test.ts", bare());
        assertEquals(97, r.rawLineCount);
    }
}
