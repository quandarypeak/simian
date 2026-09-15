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
import static org.junit.Assert.assertFalse;

/**
 * Tests for the Python parser, verifying tokenisation quality and fingerprint generation.
 *
 * Each test group targets a specific Python language structure or parser option and
 * documents both correct behaviour and known gaps in the current implementation.
 *
 * Fingerprint format notes (for reading expected values):
 *   - Numbers are formatted as doubles: integer 42 becomes "42.0"
 *   - Spaces appear between consecutive identifiers only; punctuation carries no space
 *   - Comments are stripped entirely; blank/comment-only lines emit no fingerprint
 *   - Indentation is stripped (leading whitespace is insignificant whitespace)
 */
public class PythonParserFactoryTest {

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

        String get(final int i) {
            return fingerprints.get(i);
        }

        int count() {
            return fingerprints.size();
        }

        boolean contains(final String fingerprint) {
            return fingerprints.contains(fingerprint);
        }
    }

    /** Options with all defaults cleared — only THRESHOLD is set. */
    private static Options bare() {
        final Options opts = new Options();
        opts.clear();
        return opts;
    }

    private static ParseResult parse(final String code, final Options opts) throws IOException {
        final CapturingLineListener listener = new CapturingLineListener();
        final Parser parser = new PythonParserFactory().createParser(listener, opts);
        final int rawLines = parser.parse(new StringReader(code));
        return new ParseResult(rawLines, listener.lines);
    }

    private static ParseResult parseResource(final String path, final Options opts) throws IOException {
        final CapturingLineListener listener = new CapturingLineListener();
        final Parser parser = new PythonParserFactory().createParser(listener, opts);
        try (InputStream is = PythonParserFactoryTest.class.getResourceAsStream(path);
             Reader reader = new InputStreamReader(is, "UTF-8")) {
            final int rawLines = parser.parse(reader);
            return new ParseResult(rawLines, listener.lines);
        }
    }

    // -------------------------------------------------------------------------
    // 1. Comment stripping
    // -------------------------------------------------------------------------

    @Test
    public void commentOnlyLineProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("# This is a comment\n", bare());
        assertEquals(1, r.rawLineCount);
        assertEquals(0, r.count());
    }

    @Test
    public void inlineCommentIsStrippedFromFingerprint() throws IOException {
        // The '#' marks a comment to EOL; only tokens before '#' are fingerprinted.
        final ParseResult r = parse("x = 42  # assign x\n", bare());
        assertEquals(1, r.rawLineCount);
        assertEquals(1, r.count());
        assertEquals("x=42.0", r.get(0));
    }

    @Test
    public void blankLineProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("x = 1\n\ny = 2\n", bare());
        assertEquals(3, r.rawLineCount);
        assertEquals(2, r.count());
    }

    @Test
    public void indentedCommentLineProducesNoFingerprint() throws IOException {
        final ParseResult r = parse("    # indented comment\n", bare());
        assertEquals(1, r.rawLineCount);
        assertEquals(0, r.count());
    }

    // -------------------------------------------------------------------------
    // 2. Number literals
    //    Numbers arrive via TT_NUMBER and are formatted as double via Double.toString().
    // -------------------------------------------------------------------------

    @Test
    public void integerLiteralFormattedAsDouble() throws IOException {
        // StreamTokenizer represents all numbers as double; 42 → "42.0"
        assertEquals("x=42.0", parse("x = 42\n", bare()).get(0));
    }

    @Test
    public void floatLiteralPreserved() throws IOException {
        assertEquals("x=3.14", parse("x = 3.14\n", bare()).get(0));
    }

    @Test
    public void negativeIntegerLiteralParsedAsNumber() throws IOException {
        // The '-' digit flag causes the tokenizer to parse '-5' as the number -5.0
        assertEquals("x=-5.0", parse("x = -5\n", bare()).get(0));
    }

    @Test
    public void ignoreNumbersReplacesIntegerWithHash() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_NUMBERS, true);
        assertEquals("x=#", parse("x = 42\n", opts).get(0));
    }

    // -------------------------------------------------------------------------
    // 3. String literals
    // -------------------------------------------------------------------------

    @Test
    public void doubleQuotedStringPreservedInFingerprint() throws IOException {
        // visitString appends: type + text + type (no surrounding spaces)
        assertEquals("x=\"hello\"", parse("x = \"hello\"\n", bare()).get(0));
    }

    @Test
    public void singleQuotedStringPreservedInFingerprint() throws IOException {
        assertEquals("x='world'", parse("x = 'world'\n", bare()).get(0));
    }

    @Test
    public void ignoreStringsReplacesDoubleQuotedContentWithSingleMarker() throws IOException {
        // IgnoreStringsTokenVisitor replaces double-quoted strings with a single '"' punctuation.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_STRINGS, true);
        assertEquals("x=\"", parse("x = \"hello\"\n", opts).get(0));
    }

    @Test
    public void ignoreStringsDoeNotAffectSingleQuotedStrings() throws IOException {
        // IGNORE_STRINGS only targets double-quoted strings; single-quoted are IGNORE_CHARACTERS.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_STRINGS, true);
        assertEquals("x='world'", parse("x = 'world'\n", opts).get(0));
    }

    @Test
    public void ignoreLiteralsReplacesAllStringAndNumberTokens() throws IOException {
        // Both strings and numbers become '_' (punctuation, no surrounding space).
        final Options opts = bare();
        opts.setOption(Option.IGNORE_LITERALS, true);
        assertEquals("x=_", parse("x = \"hello\"\n", opts).get(0));
        assertEquals("x=_", parse("x = 42\n", opts).get(0));
    }

    // -------------------------------------------------------------------------
    // 4. f-string and other string-prefix stripping (StripPythonStringPrefixTokenVisitor)
    //    Python string prefixes (f"", b"", r"", u"", rb"", fr"", etc.) tokenise as a
    //    separate word immediately before the string token.  StripPythonStringPrefixTokenVisitor
    //    buffers each prefix-sized identifier and discards it when the next token is a string,
    //    so f"text" and "text" produce identical fingerprints.
    // -------------------------------------------------------------------------

    @Test
    public void fStringPrefixIsStrippedFromFingerprint() throws IOException {
        // StripPythonStringPrefixTokenVisitor drops the 'f' word token that precedes the string.
        final ParseResult r = parse("x = f\"hello\"\n", bare());
        assertEquals(1, r.count());
        assertEquals("x=\"hello\"", r.get(0));
    }

    @Test
    public void fStringAndPlainStringProduceSameFingerprintAfterPrefixStripping() throws IOException {
        // After stripping, f"hello" and "hello" are fingerprint-identical.
        final ParseResult fStr  = parse("x = f\"hello\"\n", bare());
        final ParseResult plain = parse("x = \"hello\"\n",  bare());
        assertEquals("f-string and plain string should produce the same fingerprint after prefix stripping",
                fStr.get(0), plain.get(0));
    }

    @Test
    public void otherStringPrefixesAreAlsoStripped() throws IOException {
        // b"", r"", u"" prefixes are stripped by the same mechanism as f"".
        assertEquals("x=\"data\"", parse("x = b\"data\"\n", bare()).get(0));
        assertEquals("x='data'",   parse("x = r'data'\n",  bare()).get(0));
        assertEquals("x=\"data\"", parse("x = u\"data\"\n", bare()).get(0));
    }

    @Test
    public void twoCharacterStringPrefixesAreAlsoStripped() throws IOException {
        // Two-character combinations such as rb"" and fr"" are also stripped.
        assertEquals("x=\"data\"", parse("x = rb\"data\"\n", bare()).get(0));
        assertEquals("x=\"data\"", parse("x = fr\"data\"\n", bare()).get(0));
    }

    @Test
    public void prefixIdentifierFollowedByNonStringIsNotStripped() throws IOException {
        // If 'f' (or any other prefix letter) is used as a variable name (followed by '=',
        // not a string), it must survive as a normal identifier.
        final ParseResult r = parse("f = 42\n", bare());
        assertEquals(1, r.count());
        assertEquals("f=42.0", r.get(0));
    }

    // -------------------------------------------------------------------------
    // 5. Floor division (critical known bug)
    //    slashSlashComments(true) causes the tokenizer to treat '//' as a comment,
    //    consuming everything from '//' to EOL. Floor division loses its RHS operand.
    // -------------------------------------------------------------------------

    @Test
    public void floorDivisionProducesFullFingerprint() throws IOException {
        // slashSlashComments(false) — '//' is floor division, not a comment.
        // Both operands must appear in the fingerprint.
        final ParseResult r = parse("result = total // page_size\n", bare());
        assertEquals("result=total//page_size", r.get(0));
    }

    @Test
    public void twoIdenticalFloorDivisionLinesMatchEachOther() throws IOException {
        final ParseResult r1 = parse("result = total // page_size\n", bare());
        final ParseResult r2 = parse("result = total // page_size\n", bare());
        assertEquals(r1.get(0), r2.get(0));
    }

    @Test
    public void floorDivisionAndPlainAssignmentProduceDifferentFingerprints() throws IOException {
        // With the fix, 'a = x // y' and 'a = x' are no longer fingerprint-identical.
        final ParseResult withFloor    = parse("a = x // y\n", bare());
        final ParseResult withoutFloor = parse("a = x\n", bare());
        assertNotEquals("Floor division and plain assignment must have distinct fingerprints",
                withFloor.get(0), withoutFloor.get(0));
    }

    // -------------------------------------------------------------------------
    // 6. Python block structure and keywords
    //    Block headers are compound statements ending with ':'.
    //    Indentation that defines the block body is stripped (whitespace-insignificant).
    // -------------------------------------------------------------------------

    @Test
    public void functionDefinitionFingerprint() throws IOException {
        final ParseResult r = parse("def foo():\n", bare());
        assertEquals(1, r.rawLineCount);
        assertEquals(1, r.count());
        assertEquals("def foo():", r.get(0));
    }

    @Test
    public void functionWithParameterTypeAnnotationAndReturnType() throws IOException {
        // ':' and '->' are punctuation; no spaces before identifiers that follow punctuation.
        assertEquals("def greet(name:str)->str:", parse("def greet(name: str) -> str:\n", bare()).get(0));
    }

    @Test
    public void classDefinitionFingerprint() throws IOException {
        assertEquals("class Animal:", parse("class Animal:\n", bare()).get(0));
    }

    @Test
    public void classWithBaseClassFingerprint() throws IOException {
        assertEquals("class Dog(Animal):", parse("class Dog(Animal):\n", bare()).get(0));
    }

    @Test
    public void indentationIsStrippedFromBodyLines() throws IOException {
        // Both 0-indent and 4-indent versions of the same line produce the same fingerprint.
        final ParseResult flat     = parse("x = 1\n", bare());
        final ParseResult indented = parse("    x = 1\n", bare());
        assertEquals("Indented and non-indented identical lines fingerprint the same",
                flat.get(0), indented.get(0));
    }

    @Test
    public void allBlockHeaderEndWithColon() throws IOException {
        // Every compound-statement header ends with ':' — this is what signals a block open.
        final String[] headers = {
            "if x > 0:\n",
            "elif x == 0:\n",
            "else:\n",
            "for i in items:\n",
            "while True:\n",
            "try:\n",
            "except Exception:\n",
            "except ValueError as e:\n",
            "finally:\n",
            "with open(path) as f:\n",
            "def compute(x):\n",
            "class Node:\n",
        };
        for (final String header : headers) {
            final ParseResult r = parse(header, bare());
            assertEquals("Block header should produce exactly one fingerprint: " + header.trim(),
                    1, r.count());
            assertTrue("Block header fingerprint must end with ':': '" + r.get(0) + "'",
                    r.get(0).endsWith(":"));
        }
    }

    @Test
    public void functionBodyLinesAreIndependentFingerprints() throws IOException {
        final String code =
            "def foo():\n" +
            "    x = 1\n" +
            "    return x\n";
        final ParseResult r = parse(code, bare());
        assertEquals(3, r.rawLineCount);
        assertEquals(3, r.count());
        assertEquals("def foo():",  r.get(0));
        assertEquals("x=1.0",      r.get(1));
        assertEquals("return x",   r.get(2));
    }

    @Test
    public void decoratorLineFingerprint() throws IOException {
        // '@' is an ordinary punctuation character; the decorator name is a word token.
        assertEquals("@property",   parse("@property\n", bare()).get(0));
        assertEquals("@staticmethod", parse("@staticmethod\n", bare()).get(0));
        assertEquals("@classmethod", parse("@classmethod\n", bare()).get(0));
    }

    // -------------------------------------------------------------------------
    // 7. Exception handling structure
    // -------------------------------------------------------------------------

    @Test
    public void tryExceptFinallyBlockFingerprints() throws IOException {
        final String code =
            "try:\n" +
            "    result = risky()\n" +
            "except ValueError:\n" +
            "    handle()\n" +
            "finally:\n" +
            "    cleanup()\n";
        final ParseResult r = parse(code, bare());
        assertEquals(6, r.rawLineCount);
        assertEquals(6, r.count());
        assertEquals("try:",              r.get(0));
        assertEquals("result=risky()",   r.get(1));
        assertEquals("except ValueError:", r.get(2));
        assertEquals("handle()",          r.get(3));
        assertEquals("finally:",          r.get(4));
        assertEquals("cleanup()",         r.get(5));
    }

    @Test
    public void exceptWithAliasFingerprint() throws IOException {
        assertEquals("except TypeError as e:", parse("except TypeError as e:\n", bare()).get(0));
    }

    // -------------------------------------------------------------------------
    // 8. Control flow keywords
    //    Python keywords are currently typed UNKNOWN — they appear correctly in
    //    fingerprints when no normalization option is active, but are
    //    indistinguishable from user identifiers when IGNORE_IDENTIFIERS is used.
    // -------------------------------------------------------------------------

    @Test
    public void forLoopHeaderFingerprint() throws IOException {
        // 'in' is a word token (identifier type UNKNOWN), not an operator token.
        assertEquals("for i in range(10.0):", parse("for i in range(10):\n", bare()).get(0));
    }

    @Test
    public void whileLoopHeaderFingerprint() throws IOException {
        assertEquals("while True:", parse("while True:\n", bare()).get(0));
    }

    @Test
    public void returnStatementFingerprint() throws IOException {
        assertEquals("return x", parse("    return x\n", bare()).get(0));
    }

    @Test
    public void continueAndBreakFingerprints() throws IOException {
        assertEquals("continue", parse("        continue\n", bare()).get(0));
        assertEquals("break",    parse("        break\n", bare()).get(0));
    }

    @Test
    public void passStatementFingerprint() throws IOException {
        assertEquals("pass", parse("        pass\n", bare()).get(0));
    }

    @Test
    public void booleanKeywordsAreWordTokens() throws IOException {
        // 'and', 'or', 'not', 'in', 'is' are word tokens (appear as identifiers).
        final ParseResult r = parse("ok = a and b or not c\n", bare());
        final String fp = r.get(0);
        assertTrue("'and' should appear in fingerprint", fp.contains("and"));
        assertTrue("'or' should appear in fingerprint",  fp.contains("or"));
        assertTrue("'not' should appear in fingerprint", fp.contains("not"));
    }

    @Test
    public void noneAndBoolLiteralsAreWordTokens() throws IOException {
        // True, False, None are identifiers (type UNKNOWN), not special tokens.
        assertEquals("x=None",  parse("x = None\n", bare()).get(0));
        assertEquals("x=True",  parse("x = True\n", bare()).get(0));
        assertEquals("x=False", parse("x = False\n", bare()).get(0));
    }

    // -------------------------------------------------------------------------
    // 9. IGNORE_IDENTIFIERS option
    //    RecogniseIdentifiersTokenVisitor classifies all Python keywords (including
    //    the Python 3.10+ soft keywords 'match' and 'case') as KEYWORD before tokens
    //    reach IgnoreIdentifiersTokenVisitor.  Structural keywords survive with their
    //    literal names; user-defined identifiers collapse to '_'.
    // -------------------------------------------------------------------------

    @Test
    public void ignoreIdentifiersReplacesUserDefinedNames() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, true);
        assertEquals("_=_(_)", parse("result = compute(value)\n", opts).get(0));
    }

    @Test
    public void structurallyIdenticalBodyLinesMatchWithIgnoreIdentifiers() throws IOException {
        // The core duplicate detection use case: same structure, different variable names.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, true);
        final ParseResult r1 = parse("result = compute(value)\n", opts);
        final ParseResult r2 = parse("output = process(input)\n", opts);
        assertEquals("Same-structure lines with different names produce the same fingerprint",
                r1.get(0), r2.get(0));
    }

    @Test
    public void defAndClassProduceDifferentFingerprintsWithIgnoreIdentifiers() throws IOException {
        // 'def' and 'class' are tagged KEYWORD by RecogniseIdentifiersTokenVisitor and survive
        // IGNORE_IDENTIFIERS unchanged.  The user-defined names collapse to '_', but the
        // structural keywords remain, so the two forms are distinguishable.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, true);
        final ParseResult defLine   = parse("def foo():\n",   opts);
        final ParseResult classLine = parse("class Foo():\n", opts);
        // IgnoreIdentifiersTokenVisitor emits _ via visitPunctuation, which skips the
        // needSpace check, so there is no space between keyword and collapsed name.
        assertEquals("def_():",   defLine.get(0));
        assertEquals("class_():", classLine.get(0));
        assertNotEquals("def and class produce distinct fingerprints with keyword recognition",
                defLine.get(0), classLine.get(0));
    }

    @Test
    public void defKeywordSurvivesIgnoreIdentifiersWithItsName() throws IOException {
        // 'def' is a KEYWORD — it passes through IgnoreIdentifiersTokenVisitor unchanged.
        // The function name (METHOD) and parameter (VARIABLE) collapse to '_'.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, true);
        // _ is emitted via visitPunctuation (no space), so 'def' and '_' are not separated.
        assertEquals("def_(_):", parse("def my_func(arg):\n", opts).get(0));
    }

    @Test
    public void controlFlowKeywordsSurviveIgnoreIdentifiers() throws IOException {
        // All Python structural keywords survive IGNORE_IDENTIFIERS with their literal name.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, true);
        assertEquals("return_",  parse("    return result\n", opts).get(0));
        assertEquals("for_in_:", parse("for i in items:\n",  opts).get(0));
        assertEquals("while_:",  parse("while running:\n",   opts).get(0));
        assertEquals("if_>_:",   parse("if x > y:\n",        opts).get(0));
        // import lines are always suppressed by IgnoreLinesTokenVisitor — no fingerprint produced
    }

    @Test
    public void matchAndCaseSoftKeywordsSurviveIgnoreIdentifiers() throws IOException {
        // Python 3.10+ soft keywords 'match' and 'case' are included in KEYWORDS and
        // survive IGNORE_IDENTIFIERS with their literal names.  The subject expression
        // ('command') and numeric pattern ('42') behave as expected: identifiers collapse,
        // numbers are unchanged.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, true);
        assertEquals("match_:", parse("match command:\n", opts).get(0));
        assertEquals("case 42.0:", parse("case 42:\n", opts).get(0));
    }

    @Test
    public void underscoreThrowawayVariableIsClassifiedAsVariable() throws IOException {
        // '_' contains no uppercase letters so isAllUpperCase returns false — it is
        // classified as VARIABLE, not CONSTANT.  Under IGNORE_VARIABLE_NAMES, '_'
        // collapses identically to any other variable name.
        //
        // Without the fix, '_' was CONSTANT and survived IGNORE_VARIABLE_NAMES,
        // producing "if _ or_:" instead of "if_or_:" — a spurious mismatch.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_VARIABLE_NAMES, true);
        final ParseResult withUnderscore = parse("if _ or x:\n", opts);
        final ParseResult withNamedVar   = parse("if a or x:\n", opts);
        assertEquals("'_' and a named variable must collapse identically under IGNORE_VARIABLE_NAMES",
                withUnderscore.get(0), withNamedVar.get(0));
    }

    @Test
    public void duplicateFunctionBodiesMatchWithIgnoreIdentifiers() throws IOException {
        // Simulates the primary fingerprint-matching use case for Simian:
        // two functions with identical structure but different parameter names.
        final String funcA =
            "def process_order(order):\n" +
            "    validate(order)\n" +
            "    result = execute(order)\n" +
            "    return result\n";
        final String funcB =
            "def process_payment(payment):\n" +
            "    validate(payment)\n" +
            "    result = execute(payment)\n" +
            "    return result\n";

        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIERS, true);

        final ParseResult r1 = parse(funcA, opts);
        final ParseResult r2 = parse(funcB, opts);

        assertEquals(4, r1.count());
        assertEquals(4, r2.count());
        // Headers (index 0) both become "def_(_):" — 'def' survives as KEYWORD,
        // the function name collapses as METHOD and the parameter as VARIABLE.
        // Body lines (indices 1-3) match because all user-defined names collapse.
        assertEquals("validate lines match", r1.get(1), r2.get(1));
        assertEquals("execute lines match",  r1.get(2), r2.get(2));
        assertEquals("return lines match",   r1.get(3), r2.get(3));
    }

    // -------------------------------------------------------------------------
    // 10. IGNORE_IDENTIFIER_CASE option (set by default in new Options())
    // -------------------------------------------------------------------------

    @Test
    public void ignoreIdentifierCaseLowercasesAllIdentifiers() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIER_CASE, true);
        assertEquals("class myclass:", parse("class MyClass:\n", opts).get(0));
    }

    @Test
    public void ignoreIdentifierCaseMakesUpperAndLowerCaseIdentifierNamesMatch() throws IOException {
        final Options opts = bare();
        opts.setOption(Option.IGNORE_IDENTIFIER_CASE, true);
        final ParseResult r1 = parse("process(Item)\n", opts);
        final ParseResult r2 = parse("process(item)\n", opts);
        assertEquals("Same identifier in different cases should produce same fingerprint",
                r1.get(0), r2.get(0));
    }

    // -------------------------------------------------------------------------
    // 11. Comprehension structures
    // -------------------------------------------------------------------------

    @Test
    public void listComprehensionFingerprintContainsForAndIf() throws IOException {
        final ParseResult r = parse("evens = [x for x in items if x % 2 == 0]\n", bare());
        assertEquals(1, r.count());
        final String fp = r.get(0);
        assertTrue("List comprehension fingerprint should contain 'for'", fp.contains("for"));
        assertTrue("List comprehension fingerprint should contain 'if'",  fp.contains("if"));
    }

    @Test
    public void generatorExpressionProducesSingleFingerprint() throws IOException {
        final ParseResult r = parse("gen = (x * x for x in items)\n", bare());
        assertEquals(1, r.count());
    }

    // -------------------------------------------------------------------------
    // 12. Lambda expression
    // -------------------------------------------------------------------------

    @Test
    public void lambdaKeywordAppearsInFingerprint() throws IOException {
        final ParseResult r = parse("double = lambda x: x * 2\n", bare());
        assertEquals(1, r.count());
        assertTrue("'lambda' keyword should appear in fingerprint", r.get(0).contains("lambda"));
    }

    // -------------------------------------------------------------------------
    // 13. Semicolons treated as whitespace
    // -------------------------------------------------------------------------

    @Test
    public void semicolonTreatedAsWhitespaceCombinesTwoStatementsIntoOneFingerprint() throws IOException {
        // PythonParser maps ';' to whitespace, so "x = 1; y = 2" is one fingerprint line.
        final ParseResult r = parse("x = 1; y = 2\n", bare());
        assertEquals(1, r.rawLineCount);
        assertEquals(1, r.count());
        assertEquals("x=1.0y=2.0", r.get(0));
    }

    // -------------------------------------------------------------------------
    // 14. Options: import suppression, IGNORE_MODIFIERS, BALANCE_SQUARE_BRACKETS,
    //     IGNORE_CURLY_BRACES, BALANCE_PARENTHESES
    //     Import suppression and IGNORE_MODIFIERS/BALANCE_SQUARE_BRACKETS/IGNORE_CURLY_BRACES
    //     are wired in AbstractCFamilyParserFactory.  BALANCE_PARENTHESES is wired in the
    //     base AsbtractParserFactory and is therefore available to all language parsers.
    // -------------------------------------------------------------------------

    @Test
    public void importStatementProducesNoFingerprint() throws IOException {
        // IgnoreLinesTokenVisitor triggers on 'import', suppressing the entire line.
        assertEquals(0, parse("import os\n", bare()).count());
    }

    @Test
    public void fromImportStatementProducesNoFingerprint() throws IOException {
        // 'import' appears mid-line in "from X import Y" and still triggers suppression.
        assertEquals(0, parse("from typing import List\n", bare()).count());
    }

    @Test
    public void raiseFromIsNotSuppressedByImportTrigger() throws IOException {
        // 'from' is NOT in the trigger set; only 'import' is.
        // "raise X from e" must NOT be suppressed.
        final ParseResult r = parse("raise ValueError(\"bad\") from e\n", bare());
        assertEquals(1, r.count());
        assertTrue("raise...from fingerprint must contain 'raise'", r.get(0).contains("raise"));
    }

    @Test
    public void withoutIgnoreModifiersAsyncAppearsInFingerprint() throws IOException {
        // Without IGNORE_MODIFIERS, 'async' is a KEYWORD and appears in the fingerprint.
        final ParseResult r = parse("async def foo():\n", bare());
        assertEquals("async def foo():", r.get(0));
    }

    @Test
    public void ignoreModifiersSuppressesAsyncMakingAsyncAndSyncFunctionsMatch() throws IOException {
        // With IGNORE_MODIFIERS, 'async' is swallowed — async def and plain def
        // produce the same fingerprint, so structurally identical bodies are matched.
        final Options opts = bare();
        opts.setOption(Option.IGNORE_MODIFIERS, true);
        final ParseResult asyncDef = parse("async def foo():\n", opts);
        final ParseResult plainDef = parse("def foo():\n",       opts);
        assertEquals("def foo():", asyncDef.get(0));
        assertEquals("async def and def match with IGNORE_MODIFIERS",
                asyncDef.get(0), plainDef.get(0));
    }

    @Test
    public void balanceSquareBracketsMergesMultiLineBracketExpression() throws IOException {
        // Without BSB a multi-line list literal splits into one fingerprint per source line.
        // With BSB the entire bracket span is merged into the opening line's fingerprint.
        final String multiLine =
            "x = [\n" +
            "    1,\n" +
            "    2\n" +
            "]\n";
        final ParseResult withBsb    = parse(multiLine, withOption(Option.BALANCE_SQUARE_BRACKETS));
        final ParseResult withoutBsb = parse(multiLine, bare());
        assertEquals("Multi-line list collapses to one fingerprint with BSB", 1, withBsb.count());
        assertEquals("Multi-line list splits into four fingerprints without BSB", 4, withoutBsb.count());
        assertEquals("x=[1.0,2.0]", withBsb.get(0));
    }

    @Test
    public void ignoreCurlyBracesRemovesBracesFromDictLiteral() throws IOException {
        // With IGNORE_CURLY_BRACES the '{' and '}' tokens are suppressed, leaving the
        // content (key-value pairs) directly in the fingerprint.
        final ParseResult withIcb    = parse("d = {\"k\": v}\n", withOption(Option.IGNORE_CURLY_BRACES));
        final ParseResult withoutIcb = parse("d = {\"k\": v}\n", bare());
        assertEquals("d=\"k\":v", withIcb.get(0));
        assertEquals("d={\"k\":v}", withoutIcb.get(0));
    }

    @Test
    public void balanceParenthesesMergesMultiLineCallExpression() throws IOException {
        // Without BPT a multi-line function call splits into one fingerprint per source line.
        // With BPT the entire parenthesis span merges into the opening line's fingerprint.
        // BALANCE_PARENTHESES is wired in AsbtractParserFactory (the base of all parsers).
        final String multiLine =
            "result = foo(\n" +
            "    a,\n" +
            "    b\n" +
            ")\n";
        final ParseResult withBpt    = parse(multiLine, withOption(Option.BALANCE_PARENTHESES));
        final ParseResult withoutBpt = parse(multiLine, bare());
        assertEquals("Multi-line call collapses to one fingerprint with BPT", 1, withBpt.count());
        assertEquals("Multi-line call splits into four fingerprints without BPT", 4, withoutBpt.count());
        assertEquals("result=foo(a,b)", withBpt.get(0));
    }

    /** bare() with one additional option set. */
    private static Options withOption(final Option option) {
        final Options opts = bare();
        opts.setOption(option, true);
        return opts;
    }

    // -------------------------------------------------------------------------
    // 15. Triple-quoted strings (PythonTripleQuoteNormalisingReader)
    //     PythonTripleQuoteNormalisingReader pre-processes the source before the tokenizer
    //     runs, converting both """ and ''' spans into single-line equivalents with embedded
    //     newlines replaced by spaces. This fixes the tokenizer desynchronisation that
    //     previously caused multi-line docstrings to corrupt fingerprints on subsequent lines.
    //     Blank lines are re-inserted after the collapsed string so the raw line count is
    //     preserved. Single-line triple-quoted strings are normalised to plain quoted strings.
    // -------------------------------------------------------------------------

    @Test
    public void singleLineTripleQuotedStringProducesConsistentFingerprint() throws IOException {
        // After normalisation, """A docstring.""" becomes "A docstring." — a single string token.
        // Two identical triple-quoted strings produce the same fingerprint.
        final ParseResult r1 = parse("    \"\"\"A docstring.\"\"\"\n", bare());
        final ParseResult r2 = parse("    \"\"\"A docstring.\"\"\"\n", bare());
        assertEquals("Identical triple-quoted strings should produce the same fingerprint",
                r1.get(0), r2.get(0));
    }

    @Test
    public void singleLineTripleQuotedStringNormalisedToSingleQuotedEquivalent() throws IOException {
        // PythonTripleQuoteNormalisingReader converts """text""" to "text" before tokenisation,
        // so the resulting fingerprint is identical to that of a plain "text" string literal.
        final ParseResult tripleQuoted = parse("x = \"\"\"hello\"\"\"\n", bare());
        final ParseResult singleQuoted = parse("x = \"hello\"\n",      bare());
        assertEquals("Triple-quoted and single-quoted strings with identical content " +
                "produce the same fingerprint after normalisation",
                tripleQuoted.get(0), singleQuoted.get(0));
    }

    @Test
    public void singleQuoteTripleQuotedStringNormalised() throws IOException {
        // '''text''' is also handled: normalised to 'text' (single-quote delimiter preserved).
        final ParseResult r = parse("x = '''hello'''\n", bare());
        assertEquals(1, r.count());
        assertEquals("x='hello'", r.get(0));
    }

    @Test
    public void multiLineTripleQuotedStringCollapsedToSingleFingerprintAndLineCountPreserved()
            throws IOException {
        // A three-line triple-quoted string collapses to a single fingerprint on the first line;
        // blank lines are emitted for the consumed newlines so rawLineCount stays correct.
        //
        // Source (4 logical lines):
        //   """\n
        //   A\n
        //   """\n
        //   x = 1\n
        //
        // After normalisation the reader produces:
        //   " A "\n\n\nx = 1\n  (4 \n chars — same as original)
        final String code = "\"\"\"\nA\n\"\"\"\nx = 1\n";
        final ParseResult r = parse(code, bare());
        assertEquals(4, r.rawLineCount);
        assertEquals(2, r.count());
        assertEquals("\" A \"", r.get(0));   // collapsed docstring: newlines became spaces
        assertEquals("x=1.0",   r.get(1));   // line after the docstring is correct
    }

    @Test
    public void linesAfterMultiLineTripleQuotedStringFingerprintCorrectly() throws IOException {
        // Verifies that a function body following a multi-line docstring is not desynchronised.
        final String code =
            "def foo():\n" +
            "    \"\"\"\n" +
            "    Multi-line docstring.\n" +
            "    \"\"\"\n" +
            "    return 1\n";
        final ParseResult r = parse(code, bare());
        assertEquals(5, r.rawLineCount);
        // def header, the collapsed docstring, return — blank lines produce no fingerprint
        assertEquals(3, r.count());
        assertEquals("def foo():", r.get(0));
        assertEquals("return 1.0", r.get(2));
        // The collapsed docstring fingerprint contains the docstring text
        assertTrue("Collapsed docstring should contain the docstring text",
                r.get(1).contains("Multi-line docstring."));
    }

    @Test
    public void escapedDelimiterInTripleQuotedStringDoesNotTerminateEarly() throws IOException {
        // \""" inside a triple-quoted string: the backslash escapes the first quote so
        // the subsequent "" (two quotes) cannot form a closing triple-quote.
        // The string continues until the genuine closing """.
        //
        // Without the fix, the normaliser consumed \ as plain content and then saw
        // """ as a closing delimiter — cutting the string short and leaving "after"
        // as stray tokens on the same line.
        //
        // Java literal for:  x = """before \""" after"""
        final String code = "x = \"\"\"before \\\"\"\" after\"\"\"\n";
        final ParseResult r = parse(code, bare());
        assertEquals(1, r.rawLineCount);
        assertEquals("\\\"\"\" must not prematurely close the triple-quoted string", 1, r.count());
        assertTrue("'after' must appear inside the collapsed string fingerprint",
                r.get(0).contains("after"));
        assertTrue("fingerprint must be a string assignment starting with x=",
                r.get(0).startsWith("x="));
    }

    // -------------------------------------------------------------------------
    // 16. Comprehensive sample file (test/mockups/sample.py)
    //     Verifies the parser handles a realistic Python file without errors,
    //     and that key structural fingerprints are produced correctly.
    // -------------------------------------------------------------------------

    @Test
    public void sampleFileParsesSuccessfully() throws IOException {
        final ParseResult r = parseResource("/mockups/sample.py", bare());
        assertTrue("Sample file should produce at least one raw line", r.rawLineCount > 0);
        assertTrue("Sample file should produce at least one fingerprint", r.count() > 0);
        assertTrue("Blank and comment lines should reduce fingerprint count vs raw line count",
                r.count() < r.rawLineCount);
    }

    @Test
    public void sampleFileRawLineCount() throws IOException {
        // The sample file has exactly 137 lines (terminated by a final newline).
        final ParseResult r = parseResource("/mockups/sample.py", bare());
        assertEquals(137, r.rawLineCount);
    }

    @Test
    public void sampleFileImportLinesAreSuppressed() throws IOException {
        // IgnoreLinesTokenVisitor with trigger "import" suppresses all import statements.
        // This matches Simian's C/C++ behaviour where #include lines are always dropped.
        final ParseResult r = parseResource("/mockups/sample.py", bare());
        assertFalse("'import os' should be suppressed",  r.contains("import os"));
        assertFalse("'import sys' should be suppressed", r.contains("import sys"));
        // "from typing import ..." is also suppressed because 'import' appears mid-line.
        assertFalse("No from-import line should appear",
                r.fingerprints.stream().anyMatch(fp -> fp.startsWith("from")));
    }

    @Test
    public void sampleFileContainsExpectedFunctionHeaderFingerprints() throws IOException {
        final ParseResult r = parseResource("/mockups/sample.py", bare());
        assertTrue("paginate function header expected",
                r.contains("def paginate(total:int,page_size:int)->int:"));
        assertTrue("Animal class header expected",
                r.contains("class Animal:"));
        assertTrue("Dog subclass header expected",
                r.contains("class Dog(Animal):"));
    }

    @Test
    public void sampleFileFloorDivisionLineProducesFullFingerprint() throws IOException {
        // The 'paginate' function body contains 'return total // page_size'.
        // With slashSlashComments(false) the full fingerprint is produced.
        final ParseResult r = parseResource("/mockups/sample.py", bare());
        assertTrue("Full floor-division fingerprint must be present",
                r.contains("return total//page_size"));
        assertFalse("Truncated fingerprint must not be present",
                r.contains("return total") && !r.contains("return total//page_size"));
    }

    @Test
    public void sampleFileDecoratorFingerprintsPresent() throws IOException {
        final ParseResult r = parseResource("/mockups/sample.py", bare());
        assertTrue("@property decorator fingerprint expected",   r.contains("@property"));
        assertTrue("@staticmethod decorator fingerprint expected", r.contains("@staticmethod"));
        assertTrue("@classmethod decorator fingerprint expected", r.contains("@classmethod"));
    }

    @Test
    public void sampleFileParsesWithIgnoreStringsOption() throws IOException {
        final Options optsIgnoreStrings = bare();
        optsIgnoreStrings.setOption(Option.IGNORE_STRINGS, true);

        final ParseResult withStrings    = parseResource("/mockups/sample.py", bare());
        final ParseResult withoutStrings = parseResource("/mockups/sample.py", optsIgnoreStrings);

        assertEquals("IGNORE_STRINGS should not change raw line count",
                withStrings.rawLineCount, withoutStrings.rawLineCount);
        assertEquals("IGNORE_STRINGS should not change fingerprint line count",
                withStrings.count(), withoutStrings.count());
        assertFalse("With IGNORE_STRINGS, no fingerprint should contain the literal string 'hello'",
                withoutStrings.fingerprints.stream().anyMatch(fp -> fp.contains("\"hello\"")));
    }

    @Test
    public void sampleFileDocstringFingerprintIsNormalisedString() throws IOException {
        // sample.py line 15: """Return a personalised greeting string."""
        // After PythonTripleQuoteNormalisingReader this becomes a plain double-quoted string.
        final ParseResult r = parseResource("/mockups/sample.py", bare());
        assertTrue("Single-line docstring should appear as a plain double-quoted string",
                r.contains("\"Return a personalised greeting string.\""));
    }

    @Test
    public void sampleFileIgnoreIdentifiersProducesFewerDistinctFingerprints() throws IOException {
        final Options optsIgnore = bare();
        optsIgnore.setOption(Option.IGNORE_IDENTIFIERS, true);

        final ParseResult normal  = parseResource("/mockups/sample.py", bare());
        final ParseResult ignored = parseResource("/mockups/sample.py", optsIgnore);

        assertEquals("IGNORE_IDENTIFIERS should not change raw line count",
                normal.rawLineCount, ignored.rawLineCount);
        assertEquals("IGNORE_IDENTIFIERS should not change fingerprint line count",
                normal.count(), ignored.count());

        final long distinctNormal  = normal.fingerprints.stream().distinct().count();
        final long distinctIgnored = ignored.fingerprints.stream().distinct().count();
        assertTrue("IGNORE_IDENTIFIERS should collapse identifiers producing fewer distinct fingerprints",
                distinctIgnored <= distinctNormal);
    }
}
