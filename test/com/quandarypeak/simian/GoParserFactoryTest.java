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
 * Tests for GoParserFactory: fingerprint quality, option behaviours, and Go idiom coverage.
 *
 * <p><b>Parser internals:</b> Go uses CFamilyParser (the standard C-family StreamTokenizer)
 * with RecogniseIdentifiersTokenVisitor wired to classify Go's 25 reserved keywords, built-in
 * functions, and built-in types. Single-line ({@code //}) and block ({@code /* *}{@code /})
 * comments are stripped; semicolons are whitespace; {@code package} and {@code import} lines
 * are unconditionally suppressed by IgnoreLinesTokenVisitor.
 *
 * <p><b>Fingerprint format:</b>
 * <ul>
 *   <li>Numbers are doubles: {@code 42} becomes {@code "42.0"}</li>
 *   <li>Space appears only between two consecutive identifiers</li>
 *   <li>Punctuation carries no space</li>
 *   <li>Semicolons are whitespace and do not appear in fingerprints</li>
 *   <li>Indentation is ignored</li>
 * </ul>
 *
 * <p><b>Key Go classification decisions:</b>
 * <ul>
 *   <li>Built-in lowercase types ({@code int}, {@code string}, {@code error}, …) → TYPE</li>
 *   <li>All 25 reserved keywords + built-in functions → KEYWORD (survive IGNORE_IDENTIFIERS)</li>
 *   <li>PascalCase user types ({@code Foo}, {@code HttpHandler}) → TYPE via isTypeName heuristic</li>
 *   <li>Short declaration {@code :=} tokenises as {@code :} then {@code =}; differs from {@code =}</li>
 * </ul>
 */
public class GoParserFactoryTest {

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

    /** All options cleared — only unconditional parser behaviours apply. */
    private static Options bare() {
        final Options opts = new Options();
        opts.clear();
        return opts;
    }

    private static ParseResult parse(final String code, final Options opts) throws IOException {
        final CapturingLineListener listener = new CapturingLineListener();
        final Parser parser = new GoParserFactory().createParser(listener, opts);
        final int rawLines = parser.parse(new StringReader(code));
        return new ParseResult(rawLines, listener.lines);
    }

    // -------------------------------------------------------------------------
    // 1. Comment stripping
    // -------------------------------------------------------------------------

    @Test
    public void singleLineCommentProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("// a Go comment\n", bare());
        assertEquals(1, r.rawLineCount);
        assertEquals(0, r.count());
    }

    @Test
    public void blockCommentProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("/* block comment */\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void multiLineBlockCommentProducesNoFingerprints() throws IOException {
        final ParseResult r = parse("/*\n * Package doc\n * comment\n */\n", bare());
        assertEquals(4, r.rawLineCount);
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
    // 2. Package and import suppression
    //    IgnoreLinesTokenVisitor is always active — any line containing 'package'
    //    or 'import' as an identifier token is unconditionally suppressed.
    // -------------------------------------------------------------------------

    @Test
    public void packageLineIsSuppressed() throws IOException {
        final ParseResult r = parse("package main\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void singleLineImportIsSuppressed() throws IOException {
        final ParseResult r = parse("import \"fmt\"\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void importWithParenthesisLineIsSuppressed() throws IOException {
        // The 'import (' line is suppressed by IgnoreLinesTokenVisitor. An import block
        // with no body (just the opening line) produces no fingerprints.
        final ParseResult r = parse("import (\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void blankLineProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("x = 1\n\ny = 2\n", bare());
        assertEquals(3, r.rawLineCount);
        assertEquals(2, r.count());
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
    // 4. Keyword fingerprinting
    //    All 25 Go reserved keywords are classified as KEYWORD and survive
    //    IGNORE_IDENTIFIERS — control-flow structure must remain visible when
    //    user-defined names are erased.
    // -------------------------------------------------------------------------

    @Test
    public void ifAndForProduceDifferentFingerprintsUnderIgnoreIdentifiers() throws IOException {
        // 'if' and 'for' are KEYWORD — they are preserved by IGNORE_IDENTIFIERS.
        // Structurally different control-flow headers must produce different fingerprints.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rIf  = parse("if x {\n", opts);
        final ParseResult rFor = parse("for x {\n", opts);
        assertNotEquals(rIf.get(0), rFor.get(0));
    }

    @Test
    public void returnStatementKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'return' is KEYWORD — it is preserved; only the returned variable is erased.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rA = parse("return someValue\n", opts);
        final ParseResult rB = parse("return otherValue\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void rangeKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'range' is a KEYWORD — preserved even when user-defined names are erased.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rRange = parse("for i, v := range s {\n", opts);
        final ParseResult rPlain = parse("for i := 0; i < n; i++ {\n", opts);
        assertNotEquals(rRange.get(0), rPlain.get(0));
    }

    // -------------------------------------------------------------------------
    // 5. Built-in type classification
    //    Go's built-in types are all lowercase (int, string, bool, error, …).
    //    They must be in the explicit TYPES set to receive TYPE classification;
    //    otherwise the isTypeName heuristic (uppercase start) would miss them.
    //    TYPE-classified identifiers are erased by IGNORE_IDENTIFIERS but preserved
    //    by IGNORE_VARIABLE_NAMES (which only erases VARIABLE-typed identifiers).
    // -------------------------------------------------------------------------

    @Test
    public void differentBuiltInTypesProduceDifferentFingerprints() throws IOException {
        // Without any options, 'int' and 'string' appear literally in fingerprints.
        final ParseResult rInt = parse("func f(x int) {\n", bare());
        final ParseResult rStr = parse("func f(x string) {\n", bare());
        assertNotEquals(rInt.get(0), rStr.get(0));
    }

    @Test
    public void builtInTypesErasedByIgnoreIdentifiers() throws IOException {
        // With IGNORE_IDENTIFIERS, built-in types (TYPE) are erased along with
        // user-defined names — two functions differing only in parameter type match.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rInt = parse("func f(x int) {\n", opts);
        final ParseResult rStr = parse("func f(x string) {\n", opts);
        assertEquals(rInt.get(0), rStr.get(0));
    }

    @Test
    public void builtInTypesPreservedByIgnoreVariableNames() throws IOException {
        // IGNORE_VARIABLE_NAMES erases VARIABLE-typed tokens only. Built-in types
        // (TYPE) and keywords (KEYWORD) are preserved, so two functions with the
        // same parameter types but different parameter names match.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rX = parse("func f(x int) {\n", opts);
        final ParseResult rY = parse("func f(y int) {\n", opts);
        assertEquals(rX.get(0), rY.get(0));
    }

    @Test
    public void builtInTypesPreservedByIgnoreVariableNamesDistinguishTypes() throws IOException {
        // With IGNORE_VARIABLE_NAMES, parameter names are erased but types remain.
        // Two functions with different parameter types still produce different fingerprints.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rInt = parse("func f(x int) {\n", opts);
        final ParseResult rStr = parse("func f(x string) {\n", opts);
        assertNotEquals(rInt.get(0), rStr.get(0));
    }

    @Test
    public void errorTypeIsClassifiedAsType() throws IOException {
        // 'error' is Go's fundamental built-in type for error handling. It must be
        // classified as TYPE so that two functions differing only in whether they
        // return 'error' or 'int' produce different fingerprints, and so that
        // IGNORE_IDENTIFIERS treats them equivalently.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rErr = parse("func f() (int, error) {\n", opts);
        final ParseResult rInt = parse("func f() (int, int) {\n", opts);
        assertEquals(rErr.get(0), rInt.get(0));
    }

    // -------------------------------------------------------------------------
    // 6. Error handling idiom
    //    'if err != nil' is the most common Go pattern. 'err' is a VARIABLE,
    //    'nil' is a KEYWORD, '!=' is two punctuation characters '!' and '='.
    // -------------------------------------------------------------------------

    @Test
    public void errorCheckFingerprintIsCorrect() throws IOException {
        // Verify the exact fingerprint for Go's canonical error check pattern.
        final ParseResult r = parse("if err != nil {\n", bare());
        assertEquals(1, r.count());
        assertEquals("if err!=nil{", r.get(0));
    }

    @Test
    public void errorCheckMatchesAcrossDifferentErrVariableNames() throws IOException {
        // With IGNORE_VARIABLE_NAMES, 'err', 'err2', 'parseErr' are all VARIABLE
        // and are erased — the error check pattern matches regardless of variable name.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rErr  = parse("if err != nil {\n", opts);
        final ParseResult rErr2 = parse("if err2 != nil {\n", opts);
        assertEquals(rErr.get(0), rErr2.get(0));
    }

    @Test
    public void nilKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'nil' is a KEYWORD — it is preserved by IGNORE_IDENTIFIERS.
        // The error check pattern remains distinguishable from other if-statements.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rNilCheck = parse("if err != nil {\n", opts);
        final ParseResult rOther    = parse("if x > 0 {\n", opts);
        assertNotEquals(rNilCheck.get(0), rOther.get(0));
    }

    // -------------------------------------------------------------------------
    // 7. IGNORE_MODIFIERS
    //    MODIFIERS = {const, defer, func, type, var}.
    //    'const'/'var' normalise declaration style; 'defer' normalises scheduling;
    //    'func'/'type' normalise declaration vs usage.
    //    'go' is intentionally NOT a modifier — goroutine launches change execution
    //    semantics (analogous to 'await' in TypeScript, also excluded from MODIFIERS).
    // -------------------------------------------------------------------------

    @Test
    public void ignoreModifiersNormalisesConstAndVar() throws IOException {
        // 'const' and 'var' are both MODIFIERS — stripped together, so a const
        // declaration and a var declaration with the same name and value match.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rConst = parse("const x = 5\n", opts);
        final ParseResult rVar   = parse("var x = 5\n", opts);
        assertEquals(rConst.get(0), rVar.get(0));
    }

    @Test
    public void ignoreModifiersStripsDeferKeyword() throws IOException {
        // 'defer f()' and 'f()' are structurally the same call site when
        // IGNORE_MODIFIERS is active — the scheduling qualifier is stripped.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rDefer  = parse("defer cleanup()\n", opts);
        final ParseResult rDirect = parse("cleanup()\n", bare());
        assertEquals(rDirect.get(0), rDefer.get(0));
    }

    @Test
    public void goroutineLaunchIsPreservedByIgnoreModifiers() throws IOException {
        // 'go' is NOT a modifier — goroutine launches and direct calls remain
        // structurally different even when IGNORE_MODIFIERS is active.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rGo     = parse("go process(item)\n", opts);
        final ParseResult rDirect = parse("process(item)\n", opts);
        assertNotEquals(rDirect.get(0), rGo.get(0));
    }

    @Test
    public void goroutineAndDirectCallDifferWithoutIgnoreModifiers() throws IOException {
        // Without IGNORE_MODIFIERS, 'go' is a KEYWORD that appears in the fingerprint,
        // so goroutine launch and direct call are structurally different.
        final ParseResult rGo     = parse("go process(item)\n", bare());
        final ParseResult rDirect = parse("process(item)\n", bare());
        assertNotEquals(rGo.get(0), rDirect.get(0));
    }

    @Test
    public void ignoreModifiersFuncStripsDeclarationKeyword() throws IOException {
        // 'func' is a MODIFIER — stripped by IGNORE_MODIFIERS, making the function
        // declaration header look like a plain call site.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rFunc = parse("func greet(name string) {\n", opts);
        final ParseResult rCall = parse("greet(name string) {\n", bare());
        assertEquals(rCall.get(0), rFunc.get(0));
    }

    // -------------------------------------------------------------------------
    // 8. IGNORE_IDENTIFIERS
    //    User-defined names (VARIABLE, METHOD, TYPE, CONSTANT) are erased; keywords
    //    (KEYWORD) are preserved. All built-in types are added to KEYWORDS so they
    //    survive classification, but they are classified as TYPE and therefore erased.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreIdentifiersErasesUserDefinedNames() throws IOException {
        // Two return statements differing only in the returned variable name produce
        // the same fingerprint: 'return' (KEYWORD) survives, the name (VARIABLE) is erased.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rA = parse("return result\n", opts);
        final ParseResult rB = parse("return response\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void ignoreIdentifiersPreservesStructuralKeywords() throws IOException {
        // 'if', 'for', 'switch', 'return' are all KEYWORD — they survive IGNORE_IDENTIFIERS.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rIf     = parse("if x {\n", opts);
        final ParseResult rSwitch = parse("switch x {\n", opts);
        assertNotEquals(rIf.get(0), rSwitch.get(0));
    }

    @Test
    public void ignoreIdentifiersMakesFunctionsWithDifferentTypesMatch() throws IOException {
        // Two functions that differ only in their parameter type (and parameter name)
        // produce the same fingerprint under IGNORE_IDENTIFIERS.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rInt = parse("func add(a int, b int) int {\n", opts);
        final ParseResult rStr = parse("func add(a string, b string) string {\n", opts);
        assertEquals(rInt.get(0), rStr.get(0));
    }

    // -------------------------------------------------------------------------
    // 9. IGNORE_VARIABLE_NAMES
    //    Erases VARIABLE-typed tokens only. METHOD, TYPE, KEYWORD, and CONSTANT
    //    identifiers are preserved, so function names and type names remain visible.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreVariableNamesStripsLocalVariables() throws IOException {
        // User-defined variable names become VARIABLE and are erased.
        // Two expressions with different variable names produce the same fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rA = parse("x + y\n", opts);
        final ParseResult rB = parse("alpha + beta\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void ignoreVariableNamesPreservesFunctionNames() throws IOException {
        // Function names receive METHOD classification (not VARIABLE), so they are
        // preserved by IGNORE_VARIABLE_NAMES. Two calls to different functions differ.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rFoo = parse("foo(x)\n", opts);
        final ParseResult rBar = parse("bar(x)\n", opts);
        assertNotEquals(rFoo.get(0), rBar.get(0));
    }

    // -------------------------------------------------------------------------
    // 10. Multiple return values
    //     Go functions frequently return (value, error). The parenthesised return
    //     type list is ordinary punctuation — each type name appears in the fingerprint.
    // -------------------------------------------------------------------------

    @Test
    public void multipleReturnValuesFingerprintIsCorrect() throws IOException {
        // Verify the exact fingerprint for a common Go function signature with
        // a named parameter and a multi-value return type.
        final ParseResult r = parse("func divide(a, b int) (int, error) {\n", bare());
        assertEquals(1, r.count());
        assertEquals("func divide(a,b int)(int,error){", r.get(0));
    }

    @Test
    public void functionsWithDifferentReturnTypesProduceDifferentFingerprints() throws IOException {
        final ParseResult rIntErr = parse("func f() (int, error) {\n", bare());
        final ParseResult rStrErr = parse("func f() (string, error) {\n", bare());
        assertNotEquals(rIntErr.get(0), rStrErr.get(0));
    }

    @Test
    public void functionsWithDifferentReturnTypesMatchUnderIgnoreIdentifiers() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rIntErr = parse("func f() (int, error) {\n", opts);
        final ParseResult rStrErr = parse("func f() (string, error) {\n", opts);
        assertEquals(rIntErr.get(0), rStrErr.get(0));
    }

    // -------------------------------------------------------------------------
    // 11. Struct type definitions
    //     'type Foo struct {' — 'type' is MODIFIER+KEYWORD, 'Foo' is TYPE via
    //     isTypeName (PascalCase), 'struct' is KEYWORD.
    // -------------------------------------------------------------------------

    @Test
    public void structDefinitionFingerprintIsCorrect() throws IOException {
        final ParseResult r = parse("type Foo struct {\n", bare());
        assertEquals(1, r.count());
        assertEquals("type Foo struct{", r.get(0));
    }

    @Test
    public void differentStructNamesProduceDifferentFingerprints() throws IOException {
        final ParseResult rFoo = parse("type Foo struct {\n", bare());
        final ParseResult rBar = parse("type Bar struct {\n", bare());
        assertNotEquals(rFoo.get(0), rBar.get(0));
    }

    @Test
    public void structsWithDifferentNamesMatchUnderIgnoreIdentifiersAndIgnoreModifiers() throws IOException {
        // With both options: 'type' (MODIFIER) is stripped; 'Foo'/'Bar' (TYPE) are erased;
        // 'struct' (KEYWORD, not a MODIFIER) is preserved. Both collapse to '_ struct{'.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rFoo = parse("type Foo struct {\n", opts);
        final ParseResult rBar = parse("type Bar struct {\n", opts);
        assertEquals(rFoo.get(0), rBar.get(0));
    }

    // -------------------------------------------------------------------------
    // 12. Short variable declaration ':='
    //     In Go, ':=' is the short variable declaration operator. The StreamTokenizer
    //     produces ':' and '=' as two separate ordinary punctuation characters.
    //     This differs from '=' (assignment) and should produce a different fingerprint.
    // -------------------------------------------------------------------------

    @Test
    public void shortDeclarationDiffersFromAssignment() throws IOException {
        // ':=' (declare + assign) must not fingerprint the same as '=' (assign only).
        final ParseResult rDecl   = parse("x := foo()\n", bare());
        final ParseResult rAssign = parse("x = foo()\n", bare());
        assertNotEquals(rDecl.get(0), rAssign.get(0));
    }

    @Test
    public void shortDeclarationFingerprintIsCorrect() throws IOException {
        // Verify the exact fingerprint: ':' and '=' are adjacent (no space),
        // producing ':=' in the fingerprint.
        final ParseResult r = parse("x := 5\n", bare());
        assertEquals(1, r.count());
        assertEquals("x:=5.0", r.get(0));
    }

    @Test
    public void shortDeclarationsWithDifferentNamesMatchUnderIgnoreVariableNames() throws IOException {
        // Variable names in ':=' declarations are VARIABLE-typed and erased by
        // IGNORE_VARIABLE_NAMES, so 'x := 5' and 'y := 5' match.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rX = parse("x := 5\n", opts);
        final ParseResult rY = parse("y := 5\n", opts);
        assertEquals(rX.get(0), rY.get(0));
    }

    // -------------------------------------------------------------------------
    // 13. Goroutine launch vs direct call
    //     'go' is a reserved KEYWORD but NOT a MODIFIER.  A goroutine launch always
    //     differs from a direct call, including when IGNORE_MODIFIERS is active.
    // -------------------------------------------------------------------------

    @Test
    public void goroutineAndDirectCallDifferEvenUnderIgnoreModifiers() throws IOException {
        // 'go' is not stripped by IGNORE_MODIFIERS — goroutine semantics are preserved.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        final ParseResult rGo     = parse("go handle(req)\n", opts);
        final ParseResult rDirect = parse("handle(req)\n", opts);
        assertNotEquals(rGo.get(0), rDirect.get(0));
    }

    @Test
    public void goroutineKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'go' is KEYWORD — it is not erased by IGNORE_IDENTIFIERS.
        // A goroutine launch and a direct call remain structurally different.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rGo     = parse("go handle(req)\n", opts);
        final ParseResult rDirect = parse("handle(req)\n", opts);
        assertNotEquals(rGo.get(0), rDirect.get(0));
    }

    // -------------------------------------------------------------------------
    // 14. BALANCE_PARENTHESES
    //     Multi-line function calls and signatures are merged into a single
    //     logical line when BALANCE_PARENTHESES is active.
    // -------------------------------------------------------------------------

    @Test
    public void balanceParenthesesMergesMultiLineCall() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.BALANCE_PARENTHESES, Boolean.TRUE);

        final String multiLine = "fmt.Println(\n\tx,\n\ty,\n)\n";
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

        final String sig = "func greet(\n\tname string,\n\tage int,\n) error {\n";
        final ParseResult rWith    = parse(sig, opts);
        final ParseResult rWithout = parse(sig, bare());

        assertEquals(4, rWithout.rawLineCount);
        assertTrue(rWith.count() < rWithout.count());
    }

    // -------------------------------------------------------------------------
    // 15. User-defined PascalCase types
    //     Go user types follow PascalCase convention (exported) or camelCase
    //     (unexported). PascalCase names are classified as TYPE by the isTypeName
    //     heuristic; camelCase names are classified as VARIABLE/METHOD.
    // -------------------------------------------------------------------------

    @Test
    public void pascalCaseTypeNamesAreClassifiedAsType() throws IOException {
        // With IGNORE_VARIABLE_NAMES, PascalCase type names (TYPE) are preserved
        // while camelCase variable names (VARIABLE) are erased. Two variables of
        // the same type but different names produce the same fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rA = parse("var conn HttpHandler\n", opts);
        final ParseResult rB = parse("var srv HttpHandler\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void pascalCaseTypeNamesWithDifferentTypesProduceDifferentFingerprints() throws IOException {
        // With IGNORE_VARIABLE_NAMES, the type name (TYPE) remains in the fingerprint.
        // Two variables of different types still differ.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rHandler = parse("var srv HttpHandler\n", opts);
        final ParseResult rWriter  = parse("var srv ResponseWriter\n", opts);
        assertNotEquals(rHandler.get(0), rWriter.get(0));
    }

    // -------------------------------------------------------------------------
    // 16. IGNORE_SUBTYPE_NAMES
    //     PascalCase type names classified as TYPE participate in subtype matching:
    //     'HttpHandler', 'Handler', and 'BaseHandler' all share the suffix 'Handler'.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreSubtypeNamesMatchesSimilarTypeNames() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_SUBTYPE_NAMES, Boolean.TRUE);
        final ParseResult rHttp = parse("var h HttpHandler\n", opts);
        final ParseResult rBase = parse("var h BaseHandler\n", opts);
        assertEquals(rHttp.get(0), rBase.get(0));
    }

    // -------------------------------------------------------------------------
    // 17. Raw string literals (backtick strings)
    //     GoRawStringNormalisingReader converts `...` to "..." before tokenisation,
    //     so raw strings are handled identically to regular double-quoted strings:
    //     their content does not appear as code tokens, IGNORE_STRINGS suppresses
    //     them, and two raw strings that differ only in content produce the same
    //     structural fingerprint.
    // -------------------------------------------------------------------------

    @Test
    public void rawStringAndRegularStringProduceSameFingerprint() throws IOException {
        // Both `hello` and "hello" are string tokens — same structural fingerprint.
        final ParseResult rRaw = parse("x = `hello`\n", bare());
        final ParseResult rReg = parse("x = \"hello\"\n", bare());
        assertEquals(rReg.get(0), rRaw.get(0));
    }

    @Test
    public void rawStringsWithDifferentContentProduceSameFingerprintUnderIgnoreStrings() throws IOException {
        // With IGNORE_STRINGS, raw strings that differ only in content produce the same
        // structural fingerprint — the string token is suppressed entirely.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_STRINGS, Boolean.TRUE);
        final ParseResult rSql  = parse("q := `SELECT * FROM users`\n", opts);
        final ParseResult rHtml = parse("q := `<html><body></body></html>`\n", opts);
        assertEquals(rSql.get(0), rHtml.get(0));
    }

    @Test
    public void rawStringContentIsNotTokenizedAsCode() throws IOException {
        // The content of a raw string must not appear as code tokens.
        // `if err != nil {` inside a raw string must not produce an if-fingerprint.
        final ParseResult r = parse("s := `if err != nil {`\n", bare());
        assertEquals(1, r.count());
        // The line fingerprint should be a short-declaration with a string, not an if-block.
        assertTrue("raw string content must not appear as code tokens", r.get(0).startsWith("s:="));
    }

    @Test
    public void ignoreStringsSuppressesRawStringValue() throws IOException {
        // IGNORE_STRINGS suppresses string tokens. A raw string should be suppressed
        // exactly like a regular string — two assignments with different raw string
        // values produce the same fingerprint under IGNORE_STRINGS.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_STRINGS, Boolean.TRUE);
        final ParseResult rA = parse("q := `SELECT * FROM users`\n", opts);
        final ParseResult rB = parse("q := `SELECT * FROM orders WHERE id = ?`\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void ignoreStringsSuppressesRawStringAndRegularStringEqually() throws IOException {
        // With IGNORE_STRINGS, a raw string and a regular string in equivalent
        // positions produce the same fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_STRINGS, Boolean.TRUE);
        final ParseResult rRaw = parse("msg := `hello world`\n", opts);
        final ParseResult rReg = parse("msg := \"hello world\"\n", opts);
        assertEquals(rReg.get(0), rRaw.get(0));
    }

    @Test
    public void multiLineRawStringPreservesLineCount() throws IOException {
        // A multi-line raw string spans several physical lines. GoRawStringNormalisingReader
        // compresses the raw string body onto one line but re-emits compensating blank lines
        // so the total line count of the file is preserved.
        // Input has 3 newlines: end-of-line-1 (inside backtick), end-of-line-2 (inside backtick),
        // and the trailing newline after the closing backtick → rawLineCount = 3.
        final String code = "s := `line one\nline two\nline three`\n";
        final ParseResult r = parse(code, bare());
        assertEquals(3, r.rawLineCount);
    }

    @Test
    public void embeddedDoubleQuoteInRawStringDoesNotBreakTokenisation() throws IOException {
        // A raw string can contain double-quote characters. The normaliser replaces them
        // with spaces so the synthetic "..." string is not prematurely closed.
        // The surrounding code must still tokenise correctly.
        final ParseResult r = parse("s := `say \"hello\"`\n", bare());
        assertEquals(1, r.count());
        assertTrue("assignment with raw string containing \" must tokenise cleanly",
                r.get(0).startsWith("s:="));
    }

    @Test
    public void structTagIsTokenisedAsRawString() throws IOException {
        // Go struct field tags are raw string literals and must be treated as string
        // tokens (not code) — their JSON-like content must not pollute code fingerprints.
        // With IGNORE_STRINGS, both lines (same structure, different tag content) match.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_STRINGS, Boolean.TRUE);
        final ParseResult rA = parse("Name string `json:\"name\"`\n", opts);
        final ParseResult rB = parse("Name string `json:\"name\" db:\"full_name\"`\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void backtickInLineCommentIsIgnored() throws IOException {
        // A backtick inside a // comment must not start a raw string. The comment
        // is stripped before tokenisation reaches any string handling.
        final ParseResult rComment = parse("x = 5 // use `backtick` here\n", bare());
        final ParseResult rPlain   = parse("x = 5\n", bare());
        assertEquals(rPlain.get(0), rComment.get(0));
    }

    // -------------------------------------------------------------------------
    // 18. Channel operations
    //     '<-' is two separate punctuation characters ('<' and '-') in the
    //     StreamTokenizer. The channel send and receive operators therefore appear
    //     literally in fingerprints, making sends, receives, and plain assignments
    //     structurally distinct.
    // -------------------------------------------------------------------------

    @Test
    public void channelSendFingerprintIsCorrect() throws IOException {
        // 'ch <- val': '<' and '-' are separate ordinary characters.
        final ParseResult r = parse("ch <- val\n", bare());
        assertEquals(1, r.count());
        assertEquals("ch<-val", r.get(0));
    }

    @Test
    public void channelReceiveFingerprintIsCorrect() throws IOException {
        // 'v := <-ch': the receive operator '<-' is two punctuation characters
        // preceding the channel name.
        final ParseResult r = parse("v := <-ch\n", bare());
        assertEquals(1, r.count());
        assertEquals("v:=<-ch", r.get(0));
    }

    @Test
    public void channelSendAndDirectAssignmentAreDistinct() throws IOException {
        // 'ch <- val' (channel send) must not fingerprint the same as 'ch = val'
        // (assignment) — the extra '<' changes the structural fingerprint.
        final ParseResult rSend   = parse("ch <- val\n", bare());
        final ParseResult rAssign = parse("ch = val\n", bare());
        assertNotEquals(rSend.get(0), rAssign.get(0));
    }

    @Test
    public void channelSendMatchesUnderIgnoreVariableNames() throws IOException {
        // Channel variable name and value are both VARIABLE-typed — erased by
        // IGNORE_VARIABLE_NAMES, leaving only the structural '<-' operator.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rA = parse("ch <- val\n", opts);
        final ParseResult rB = parse("queue <- msg\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void chanKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'chan' is a KEYWORD — it survives IGNORE_IDENTIFIERS, so a channel
        // variable declaration is distinguishable from a plain variable declaration.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rChan  = parse("var ch chan int\n", opts);
        final ParseResult rPlain = parse("var x int\n", opts);
        assertNotEquals(rChan.get(0), rPlain.get(0));
    }

    // -------------------------------------------------------------------------
    // 19. Type assertions
    //     'x.(SomeType)' — '.' is configured as an ordinary character so it
    //     appears as punctuation in the fingerprint. The asserted type name (TYPE
    //     via isTypeName for PascalCase, or TYPES set for built-ins) participates
    //     in the fingerprint and can be erased by IGNORE_IDENTIFIERS.
    // -------------------------------------------------------------------------

    @Test
    public void typeAssertionFingerprintContainsOperator() throws IOException {
        // The '.(' sequence must appear in the fingerprint — it distinguishes a
        // type assertion from a plain variable or field access.
        final ParseResult r = parse("v, ok := x.(SomeType)\n", bare());
        assertEquals(1, r.count());
        assertTrue("type assertion '.(Type)' must appear in fingerprint", r.get(0).contains(".("));
    }

    @Test
    public void typeAssertionsWithDifferentTypesProduceDifferentFingerprints() throws IOException {
        // Two type assertions on the same value but to different concrete types
        // must produce different fingerprints so they are not treated as duplicates.
        final ParseResult rA = parse("v, ok := x.(SomeType)\n", bare());
        final ParseResult rB = parse("v, ok := x.(OtherType)\n", bare());
        assertNotEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void typeAssertionsMatchUnderIgnoreIdentifiers() throws IOException {
        // With IGNORE_IDENTIFIERS, both the asserted type (TYPE) and the variable
        // names (VARIABLE) are erased — two type assertions that differ only in
        // target type and variable name produce the same structural fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rA = parse("v, ok := x.(SomeType)\n", opts);
        final ParseResult rB = parse("w, found := y.(OtherType)\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    @Test
    public void typeSwitchIsDistinctFromOrdinarySwitch() throws IOException {
        // 'switch v := x.(type) {' contains the type-switch expression '.(type)'.
        // Without IGNORE_IDENTIFIERS, the extra '.(type)' token sequence makes it
        // structurally different from 'switch x {'.
        final ParseResult rTypeSwitch    = parse("switch v := x.(type) {\n", bare());
        final ParseResult rOrdinarySwitch = parse("switch x {\n", bare());
        assertNotEquals(rTypeSwitch.get(0), rOrdinarySwitch.get(0));
    }

    // -------------------------------------------------------------------------
    // 20. Blank identifier '_'
    //     Go's blank identifier '_' is configured as a word character (via
    //     wordChars('_', '_')). It is classified as VARIABLE: it is not in TYPES
    //     or KEYWORDS, not all-uppercase (length == 1), and not isTypeName ('_'
    //     is not an uppercase letter). IGNORE_VARIABLE_NAMES therefore erases it.
    // -------------------------------------------------------------------------

    @Test
    public void blankIdentifierIsErasedByIgnoreVariableNames() throws IOException {
        // '_ = someFunc()' and 'result = someFunc()' differ only in the left-hand
        // side name. With IGNORE_VARIABLE_NAMES both names are VARIABLE and erased.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rBlank = parse("_ = someFunc()\n", opts);
        final ParseResult rNamed = parse("result = someFunc()\n", opts);
        assertEquals(rBlank.get(0), rNamed.get(0));
    }

    @Test
    public void blankIdentifierInMultiReturnIsErasedByIgnoreVariableNames() throws IOException {
        // '_, err := f()' and 'val, err := f()' differ only in the first variable.
        // With IGNORE_VARIABLE_NAMES, both '_' and 'err' (both VARIABLE) are erased.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rBlank  = parse("_, err := someFunc()\n", opts);
        final ParseResult rNormal = parse("val, err := someFunc()\n", opts);
        assertEquals(rBlank.get(0), rNormal.get(0));
    }

    @Test
    public void blankIdentifierIsDistinctFromAssignmentWithoutIgnoreVariableNames() throws IOException {
        // Without options, '_ = f()' and 'result = f()' differ because
        // '_' and 'result' are different VARIABLE tokens in the fingerprint.
        final ParseResult rBlank = parse("_ = someFunc()\n", bare());
        final ParseResult rNamed = parse("result = someFunc()\n", bare());
        assertNotEquals(rBlank.get(0), rNamed.get(0));
    }

    // -------------------------------------------------------------------------
    // 21. Named struct literals
    //     'Point{X: 1, Y: 2}' — the struct type name is TYPE (PascalCase);
    //     single-character uppercase field names (X, Y) are also TYPE via
    //     isTypeName (single char → not isAllUpperCase → falls through to
    //     isTypeName which checks charAt(0) is uppercase). Numbers appear as
    //     doubles in the fingerprint.
    // -------------------------------------------------------------------------

    @Test
    public void namedStructLiteralFingerprintIsCorrect() throws IOException {
        // Verify the exact fingerprint for a two-field struct literal.
        final ParseResult r = parse("p := Point{X: 1, Y: 2}\n", bare());
        assertEquals(1, r.count());
        assertEquals("p:=Point{X:1.0,Y:2.0}", r.get(0));
    }

    @Test
    public void namedStructLiteralsWithSameTypeMatchUnderIgnoreVariableNames() throws IOException {
        // The local variable name is VARIABLE — erased by IGNORE_VARIABLE_NAMES.
        // Type name and field names are TYPE — preserved. Two variables of the same
        // struct type with the same field values but different variable names match.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rP = parse("p := Point{X: 1, Y: 2}\n", opts);
        final ParseResult rQ = parse("q := Point{X: 1, Y: 2}\n", opts);
        assertEquals(rP.get(0), rQ.get(0));
    }

    @Test
    public void namedStructLiteralsOfDifferentTypesRemainDistinct() throws IOException {
        // Even with IGNORE_VARIABLE_NAMES, the struct type name (TYPE) is preserved,
        // so 'Point{…}' and 'Coord{…}' produce different fingerprints.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rPoint = parse("p := Point{X: 1, Y: 2}\n", opts);
        final ParseResult rCoord = parse("p := Coord{X: 1, Y: 2}\n", opts);
        assertNotEquals(rPoint.get(0), rCoord.get(0));
    }

    // -------------------------------------------------------------------------
    // 22. Interface definitions
    //     'type Reader interface {' — 'type' is MODIFIER+KEYWORD, 'Reader' is TYPE,
    //     'interface' is KEYWORD (not a MODIFIER). Because 'interface' and 'struct'
    //     are different keywords, interface and struct definitions are structurally
    //     distinguishable even after both IGNORE_MODIFIERS and IGNORE_IDENTIFIERS
    //     erase the declaration keyword and the type name.
    // -------------------------------------------------------------------------

    @Test
    public void interfaceDefinitionFingerprintIsCorrect() throws IOException {
        final ParseResult r = parse("type Reader interface {\n", bare());
        assertEquals(1, r.count());
        assertEquals("type Reader interface{", r.get(0));
    }

    @Test
    public void interfaceAndStructDefinitionsAreDistinctAfterErasing() throws IOException {
        // With IGNORE_MODIFIERS + IGNORE_IDENTIFIERS:
        //   - 'type' (MODIFIER) is stripped
        //   - 'Foo'/'Reader' (TYPE) are erased
        //   - 'struct' vs 'interface' (KEYWORD, not MODIFIER) survive both options
        // The two definition kinds still produce different fingerprints.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, Boolean.TRUE);
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rStruct    = parse("type Foo struct {\n", opts);
        final ParseResult rInterface = parse("type Foo interface {\n", opts);
        assertNotEquals(rStruct.get(0), rInterface.get(0));
    }

    // -------------------------------------------------------------------------
    // 23. Map comma-ok idiom
    //     'v, ok := m[key]' — the map lookup and the channel receive both use
    //     comma-ok but differ structurally: map uses '[...]' while channel receive
    //     uses '<-'. Both are patterns common enough to warrant explicit coverage.
    // -------------------------------------------------------------------------

    @Test
    public void mapLookupFingerprintIsCorrect() throws IOException {
        final ParseResult r = parse("v, ok := m[key]\n", bare());
        assertEquals(1, r.count());
        assertEquals("v,ok:=m[key]", r.get(0));
    }

    @Test
    public void mapLookupAndChannelReceiveAreStructurallyDistinct() throws IOException {
        // 'm[key]' (map lookup) uses bracket syntax while '<-ch' (channel receive)
        // uses the arrow operator — different fingerprints even under all erase options.
        final ParseResult rMap  = parse("v, ok := m[key]\n", bare());
        final ParseResult rChan = parse("v, ok := <-ch\n", bare());
        assertNotEquals(rMap.get(0), rChan.get(0));
    }

    @Test
    public void mapLookupsMatchUnderIgnoreVariableNames() throws IOException {
        // Map name, key name, and value name are all VARIABLE-typed. With
        // IGNORE_VARIABLE_NAMES they are erased, so two equivalent map lookups
        // with different variable names produce the same structural fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, Boolean.TRUE);
        final ParseResult rA = parse("v, ok := m[key]\n", opts);
        final ParseResult rB = parse("val, found := cache[id]\n", opts);
        assertEquals(rA.get(0), rB.get(0));
    }

    // -------------------------------------------------------------------------
    // 24. select keyword
    //     Go's 'select' statement multiplexes on channel operations. It is one of
    //     the 25 reserved keywords and must be classified as KEYWORD so that:
    //     (a) it survives IGNORE_IDENTIFIERS, and
    //     (b) channel-select blocks fingerprint differently from switch blocks.
    // -------------------------------------------------------------------------

    @Test
    public void selectKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'select' and 'switch' are both KEYWORD — they produce different fingerprints
        // under IGNORE_IDENTIFIERS even though all variable names are erased.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rSelect = parse("select {\n", opts);
        final ParseResult rSwitch = parse("switch x {\n", opts);
        assertNotEquals(rSelect.get(0), rSwitch.get(0));
    }

    @Test
    public void selectFingerprintIsCorrect() throws IOException {
        final ParseResult r = parse("select {\n", bare());
        assertEquals(1, r.count());
        assertEquals("select{", r.get(0));
    }

    // -------------------------------------------------------------------------
    // 25. iota constant
    //     'iota' is a KEYWORD — it is incremented at each const spec in a const
    //     block. Under IGNORE_IDENTIFIERS it must survive so that 'A = iota'
    //     is distinguishable from 'A = 1'.
    // -------------------------------------------------------------------------

    @Test
    public void iotaKeywordSurvivesIgnoreIdentifiers() throws IOException {
        // 'iota' is KEYWORD — erasing all identifiers still leaves 'iota' visible.
        // 'A = iota' and 'A = 1.0' are structurally different even under IGNORE_IDENTIFIERS.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rIota = parse("A = iota\n", opts);
        final ParseResult rNum  = parse("A = 1\n", opts);
        assertNotEquals(rIota.get(0), rNum.get(0));
    }

    @Test
    public void iotaFingerprintIsCorrect() throws IOException {
        final ParseResult r = parse("A = iota\n", bare());
        assertEquals(1, r.count());
        // 'A' is single uppercase letter → TYPE (isTypeName), 'iota' is KEYWORD.
        assertEquals("A=iota", r.get(0));
    }

    // -------------------------------------------------------------------------
    // 26. Variadic parameters
    //     In Go, '...' is written as three consecutive '.' characters. Since '.'
    //     is configured as ordinaryChar in GoParser, each '.' becomes a separate
    //     punctuation token. The fingerprint must contain '...' and a variadic
    //     signature must differ from a non-variadic one.
    // -------------------------------------------------------------------------

    @Test
    public void variadicParameterFingerprintIsCorrect() throws IOException {
        // 'func f(args ...int)' — '.' is ordinaryChar so '...' is three punctuation
        // tokens producing '...' in the fingerprint.
        final ParseResult r = parse("func f(args ...int) {\n", bare());
        assertEquals(1, r.count());
        assertEquals("func f(args...int){", r.get(0));
    }

    @Test
    public void variadicAndNonVariadicProduceDifferentFingerprints() throws IOException {
        // A variadic parameter '...int' contains the '...' punctuation sequence that
        // a non-variadic 'int' does not — the two signatures must differ.
        final ParseResult rVariadic    = parse("func f(args ...int) {\n", bare());
        final ParseResult rNonVariadic = parse("func f(args int) {\n", bare());
        assertNotEquals(rVariadic.get(0), rNonVariadic.get(0));
    }

    // -------------------------------------------------------------------------
    // 28. Generic types and functions (Go 1.18+)
    //     Type parameters use '[T any]' or '[K comparable, V any]' syntax.
    //     Single-char uppercase type params (T, K, V) → TYPE via isTypeName.
    //     Built-in constraints (any, comparable) → TYPE via the _types set.
    //     Both are therefore erased by IGNORE_IDENTIFIERS, and passed through
    //     unchanged by IGNORE_SUBTYPE_NAMES (single-char: substring(0) = same char).
    // -------------------------------------------------------------------------

    @Test
    public void genericFunctionFingerprintIsCorrect() throws IOException {
        // Type-parameter brackets and constraints must appear in the fingerprint.
        // Space is inserted only between consecutive identifier tokens.
        final ParseResult r = parse("func Filter[T any](items []T) []T {\n", bare());
        assertEquals(1, r.count());
        assertEquals("func Filter[T any](items[]T)[]T{", r.get(0));
    }

    @Test
    public void genericTypeDeclarationFingerprintIsCorrect() throws IOException {
        // Generic type declaration: 'type Stack[T any] struct {'.
        final ParseResult r = parse("type Stack[T any] struct {\n", bare());
        assertEquals(1, r.count());
        assertEquals("type Stack[T any]struct{", r.get(0));
    }

    @Test
    public void genericTypeParametersReplacedByIgnoreIdentifiers() throws IOException {
        // IGNORE_IDENTIFIERS replaces every non-KEYWORD identifier with '_' punctuation.
        // TYPE identifiers (F, T, any) and VARIABLE identifiers (x) all become '_'.
        // KEYWORD identifiers (func) pass through unchanged.
        // Structural punctuation ([, ], (, ), {) is unaffected.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult r = parse("func F[T any](x T) T {\n", opts);
        assertEquals(1, r.count());
        assertEquals("func_[__](__)_{", r.get(0));
    }

    @Test
    public void genericFunctionsWithSameShapeMatchUnderIgnoreIdentifiers() throws IOException {
        // Two generic functions identical in structure but with different type param
        // names produce the same fingerprint under IGNORE_IDENTIFIERS.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, Boolean.TRUE);
        final ParseResult rT = parse("func F[T any](x T) T {\n", opts);
        final ParseResult rK = parse("func G[K any](x K) K {\n", opts);
        assertEquals(rT.get(0), rK.get(0));
    }

    @Test
    public void singleCharTypeParamIsUnchangedByIgnoreSubtypeNames() throws IOException {
        // IgnoreSubtypeNamesTokenVisitor truncates CamelCase types to their last
        // PascalCase component. For a single-char type param like 'T', truncation
        // produces substring(0) = "T" — the same value. The fingerprint is unchanged.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_SUBTYPE_NAMES, Boolean.TRUE);
        final ParseResult rWith    = parse("func F[T any](x T) T {\n", opts);
        final ParseResult rWithout = parse("func F[T any](x T) T {\n", bare());
        assertEquals(rWithout.get(0), rWith.get(0));
    }

    // -------------------------------------------------------------------------
    // 29. Rune literals
    //     Go rune literals ('a', '\n', '\t') are tokenized by the StreamTokenizer
    //     as character tokens (quote type '\'').  They are suppressed by
    //     IGNORE_CHARACTERS and by IGNORE_LITERALS, but not by IGNORE_STRINGS.
    // -------------------------------------------------------------------------

    @Test
    public void runeLiteralProducesFingerprint() throws IOException {
        // A rune literal produces a fingerprint; the line is not blank.
        final ParseResult r = parse("x := 'a'\n", bare());
        assertEquals(1, r.count());
    }

    @Test
    public void ignoreCharactersSuppressesRuneLiterals() throws IOException {
        // IGNORE_CHARACTERS erases rune literal tokens. Two assignments differing
        // only in the rune value produce the same fingerprint.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_CHARACTERS, Boolean.TRUE);
        final ParseResult rA = parse("x := 'a'\n", opts);
        final ParseResult rZ = parse("x := 'z'\n", opts);
        assertEquals(rA.get(0), rZ.get(0));
    }

    @Test
    public void escapeRuneLiteralProducesFingerprint() throws IOException {
        // Rune literals with backslash escape sequences are tokenized correctly
        // by the StreamTokenizer after GoRawStringNormalisingReader passes them through.
        final ParseResult r = parse("x := '\\t'\n", bare());
        assertEquals(1, r.count());
    }

    @Test
    public void ignoreLiteralsSuppressesRuneLiterals() throws IOException {
        // IGNORE_LITERALS is a superset of IGNORE_CHARACTERS — rune literals
        // are suppressed along with strings and numbers.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_LITERALS, Boolean.TRUE);
        final ParseResult rA = parse("x := 'a'\n", opts);
        final ParseResult rZ = parse("x := 'z'\n", opts);
        assertEquals(rA.get(0), rZ.get(0));
    }

    // -------------------------------------------------------------------------
    // 27. Grouped import block suppression
    //     GoRawStringNormalisingReader blanks the body of every 'import ( ... )'
    //     block so that individual import-path lines (which carry no 'import'
    //     keyword) are not fingerprinted as string tokens.  Newlines inside the
    //     block are preserved to keep line-count attribution correct.
    // -------------------------------------------------------------------------

    @Test
    public void groupedImportBodyProducesNoFingerprintTokens() throws IOException {
        // The full block is suppressed: 'import (' line by IgnoreLinesTokenVisitor,
        // interior path lines by GoRawStringNormalisingReader, closing ')' blanked.
        final ParseResult r = parse("import (\n    \"fmt\"\n    \"os\"\n)\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void groupedImportDoesNotAffectCodeAfterBlock() throws IOException {
        // Code on lines after the closing ')' must still be fingerprinted.
        final ParseResult r = parse("import (\n    \"fmt\"\n)\nx := 1\n", bare());
        assertEquals(1, r.count());
        assertEquals("x:=1.0", r.get(0));
    }

    @Test
    public void groupedImportPreservesLineCount() throws IOException {
        // Newlines inside the block are preserved so rawLineCount matches the
        // number of physical lines in the source.
        final String code = "import (\n    \"fmt\"\n    \"os\"\n)\nx := 1\n";
        final ParseResult r = parse(code, bare());
        assertEquals(5, r.rawLineCount);
        assertEquals(1, r.count());
    }

    @Test
    public void groupedImportWithAliasIsFullySuppressed() throws IOException {
        // Aliased imports ('alias "path"') and blank-identifier imports ('_ "path"')
        // must be suppressed along with the rest of the block body.
        final ParseResult r = parse(
                "import (\n    fmt \"fmt\"\n    _ \"os\"\n)\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void groupedImportWithCommentIsFullySuppressed() throws IOException {
        // Comments inside import blocks are also blanked; a ')' inside a comment
        // must not be mistaken for the closing delimiter of the block.
        final ParseResult r = parse(
                "import (\n    // stdlib\n    \"fmt\" // fmt.Println()\n)\n", bare());
        assertEquals(0, r.count());
    }

    @Test
    public void importKeywordInLineCommentDoesNotTriggerBlockSuppression() throws IOException {
        // 'import (' appearing inside a '//' comment is not at the start of a line
        // and must not trigger block suppression. The following code line must
        // still produce a fingerprint.
        final ParseResult r = parse("// import (something)\nx := 1\n", bare());
        assertEquals(1, r.count());
        assertEquals("x:=1.0", r.get(0));
    }
}
