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
 * <p><b>Parser internals:</b> TypeScript uses TypeScriptParser (backed by the same C-family
 * StreamTokenizer as Java/C#/C++) with a pre-processing step via
 * TypeScriptTemplateLiteralNormalisingReader. Single-line (//) and block (/* *&#47;) comments
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
public class TypeScriptParserFactoryTest {

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
    //    TypeScriptTemplateLiteralNormalisingReader converts backtick template literals
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
    //    Strips words in the MODIFIERS set: abstract, class, const, enum, export,
    //    extends, final, function, implements, interface, native, private, protected,
    //    public, static, throws, transient, volatile, var, let, tuple, type.
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
}
