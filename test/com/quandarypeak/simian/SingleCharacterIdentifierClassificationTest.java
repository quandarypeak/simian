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
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Guards the behaviour of {@link RecogniseIdentifiersTokenVisitor#isAllUpperCase}'s
 * single-character carve-out: a bare uppercase letter (generic type parameter {@code T},
 * {@code K}, {@code V}, …) is classified as {@code TYPE} rather than {@code CONSTANT}, so
 * that it falls through to {@code isTypeName} instead of being treated as an all-caps
 * constant name.
 *
 * <p>This classification is shared by every language that wires
 * {@code RecogniseIdentifiersTokenVisitor(visitor, TYPES, KEYWORDS)} — confirmed (by
 * inspecting every *ParserFactory.java) to be Java, C, Python, TypeScript, and Go. Of the
 * visitors that read the resulting {@code type} value at all, only two ever branch on
 * {@code TYPE} specifically: {@link IgnoreSubtypeNamesTokenVisitor} (a no-op for a
 * single-character name — it strips everything before the last uppercase letter, which for
 * a 1-character name returns the name unchanged) and {@link IgnoreTypeAnnotationsTokenVisitor}
 * (wired only into TypeScript, to suppress {@code : Type} annotations).
 *
 * <p>Net effect, verified here: this classification is a complete no-op for Java, C, and
 * Python under every option that reads identifier type ({@code IGNORE_VARIABLE_NAMES},
 * {@code IGNORE_IDENTIFIERS}, {@code IGNORE_SUBTYPE_NAMES}) — it only has an observable
 * effect in TypeScript, via {@code IGNORE_TYPE_ANNOTATIONS}. JavaScript and VB are
 * unaffected for a different, more fundamental reason: JavaScript never wires
 * {@code RecogniseIdentifiersTokenVisitor} at all (no TYPE/CONSTANT classification step
 * exists), and VB's factory declares its own private, same-named, differently-signatured
 * nested classifier that has no {@code isAllUpperCase} call or {@code CONSTANT} path.
 *
 * <p>Go is covered by the equivalent reasoning but lives on a separate branch without this
 * test file; it should get a mirrored test there.
 */
public class SingleCharacterIdentifierClassificationTest {

    static class CapturingLineListener implements LineListener {
        final List<String> lines = new ArrayList<>();
        @Override public void file() { lines.clear(); }
        @Override public void line(final int n, final LineBuffer line) { lines.add(line.toString()); }
    }

    private static List<String> parse(final ParserFactory factory, final String code, final Options opts) throws IOException {
        final CapturingLineListener listener = new CapturingLineListener();
        final Parser parser = factory.createParser(listener, opts);
        parser.parse(new StringReader(code));
        return listener.lines;
    }

    private static Options bare() {
        final Options o = new Options();
        o.clear();
        return o;
    }

    private static Options with(final Option opt) {
        final Options o = bare();
        o.setOption(opt, Boolean.TRUE);
        return o;
    }

    // -------------------------------------------------------------------------
    // Java, C, Python: the reclassification must not change any fingerprint, under
    // any option that ever branches on identifier type.
    // -------------------------------------------------------------------------

    @Test
    public void javaSingleUpperCaseLetterUnaffectedByAnyTypeSensitiveOption() throws IOException {
        final String code = "N = someCall();\n";
        final ParserFactory f = new JavaParserFactory();
        assertEquals("N=someCall()", parse(f, code, bare()).get(0));
        assertEquals("N=someCall()", parse(f, code, with(Option.IGNORE_VARIABLE_NAMES)).get(0));
        assertEquals("_=_()", parse(f, code, with(Option.IGNORE_IDENTIFIERS)).get(0));
        assertEquals("N=someCall()", parse(f, code, with(Option.IGNORE_SUBTYPE_NAMES)).get(0));
    }

    @Test
    public void cSingleUpperCaseLetterUnaffectedByAnyTypeSensitiveOption() throws IOException {
        final String code = "N = someCall();\n";
        final ParserFactory f = new CParserFactory();
        assertEquals("N=someCall()", parse(f, code, bare()).get(0));
        assertEquals("N=someCall()", parse(f, code, with(Option.IGNORE_VARIABLE_NAMES)).get(0));
        assertEquals("_=_()", parse(f, code, with(Option.IGNORE_IDENTIFIERS)).get(0));
        assertEquals("N=someCall()", parse(f, code, with(Option.IGNORE_SUBTYPE_NAMES)).get(0));
    }

    @Test
    public void pythonSingleUpperCaseLetterUnaffectedByAnyTypeSensitiveOption() throws IOException {
        final String code = "N = some_call()\n";
        final ParserFactory f = new PythonParserFactory();
        assertEquals("N=some_call()", parse(f, code, bare()).get(0));
        assertEquals("N=some_call()", parse(f, code, with(Option.IGNORE_VARIABLE_NAMES)).get(0));
        assertEquals("_=_()", parse(f, code, with(Option.IGNORE_IDENTIFIERS)).get(0));
        assertEquals("N=some_call()", parse(f, code, with(Option.IGNORE_SUBTYPE_NAMES)).get(0));
    }

    // -------------------------------------------------------------------------
    // TypeScript: this is the one case where the reclassification has a real, intended
    // effect - a single-letter generic type parameter's annotation must be suppressed
    // under IGNORE_TYPE_ANNOTATIONS, the same as any other type name.
    // -------------------------------------------------------------------------

    @Test
    public void typeScriptGenericTypeParamAnnotationIsSuppressed() throws IOException {
        final String code = "function box<T>(x: T): T { return x; }\n";
        final ParserFactory f = new TypeScriptParserFactory();
        assertEquals("function box<T>(x:T):T{return x}", parse(f, code, bare()).get(0));
        assertEquals("function box<T>(x){return x}", parse(f, code, with(Option.IGNORE_TYPE_ANNOTATIONS)).get(0));
    }

    @Test
    public void typeScriptPlainSingleUpperCaseLetterUnaffectedByNonAnnotationOptions() throws IOException {
        // A single-letter identifier used as an ordinary value (not a type position) must
        // still behave like Java/C/Python under the non-TypeScript-specific options.
        final String code = "N = someCall();\n";
        final ParserFactory f = new TypeScriptParserFactory();
        assertEquals("N=someCall()", parse(f, code, bare()).get(0));
        assertEquals("N=someCall()", parse(f, code, with(Option.IGNORE_VARIABLE_NAMES)).get(0));
        assertEquals("_=_()", parse(f, code, with(Option.IGNORE_IDENTIFIERS)).get(0));
        assertEquals("N=someCall()", parse(f, code, with(Option.IGNORE_SUBTYPE_NAMES)).get(0));
    }

    // -------------------------------------------------------------------------
    // JavaScript and VB: categorically unaffected, for structural reasons rather than
    // because the classification happens to be a no-op for them.
    // -------------------------------------------------------------------------

    @Test
    public void javaScriptHasNoIdentifierClassificationStepAtAll() throws IOException {
        // JavaScriptParserFactory never wires RecogniseIdentifiersTokenVisitor - there is no
        // TYPE/CONSTANT classification step for this change to affect.
        final String code = "N = someCall();\n";
        final ParserFactory f = new JavaScriptParserFactory();
        assertEquals("N=someCall()", parse(f, code, bare()).get(0));
        assertEquals("N=someCall()", parse(f, code, with(Option.IGNORE_VARIABLE_NAMES)).get(0));
        assertEquals("_=_()", parse(f, code, with(Option.IGNORE_IDENTIFIERS)).get(0));
        assertEquals("N=someCall()", parse(f, code, with(Option.IGNORE_SUBTYPE_NAMES)).get(0));
    }

    @Test
    public void vbUsesItsOwnSeparateClassifierUnaffectedByThisChange() throws IOException {
        // VbParserFactory declares its own private, same-named RecogniseIdentifiersTokenVisitor
        // nested class (a different, 1-arg constructor) with no CONSTANT path and no call to
        // isAllUpperCase - it shadows the shared top-level class by name only.
        final String code = "N = SomeCall()\n";
        final ParserFactory f = new VbParserFactory();
        assertEquals("N=SomeCall()", parse(f, code, bare()).get(0));
        assertEquals("_=_()", parse(f, code, with(Option.IGNORE_VARIABLE_NAMES)).get(0));
        assertEquals("_=_()", parse(f, code, with(Option.IGNORE_IDENTIFIERS)).get(0));
        assertEquals("N=SomeCall()", parse(f, code, with(Option.IGNORE_SUBTYPE_NAMES)).get(0));
    }
}
