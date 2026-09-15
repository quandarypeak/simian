/*
 * Copyright 2022-2026 Quandary Peak Research, Inc.
 * Original authorship by Simon Harris.
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Pre-processes TypeScript source to convert backtick template literals into double-quoted
 * string equivalents before the StreamTokenizer runs.
 *
 * <p>StreamTokenizer does not recognise the backtick as a string delimiter, so without this
 * reader a template literal like {@code `Hello, ${name}!`} is tokenised as a series of
 * punctuation and identifier tokens rather than a single string token. This causes two
 * template literals that differ only in the interpolated expression to produce different
 * fingerprints, and prevents {@code IGNORE_STRINGS} from suppressing template-literal values.
 *
 * <p>This reader replaces each template literal with a double-quoted string:
 * <ul>
 *   <li>The opening and closing backticks are replaced with {@code "}</li>
 *   <li>Each {@code ${...}} interpolation expression is replaced with a single space;
 *       the entire expression body (including nested strings) is discarded</li>
 *   <li>Any {@code "} character in the literal text is replaced with a space to avoid
 *       prematurely closing the synthetic double-quoted string</li>
 *   <li>Embedded newlines within a template literal are replaced with spaces and blank
 *       lines are re-inserted after the closing quote so the total line count is
 *       preserved, keeping Simian's line-number attribution correct</li>
 * </ul>
 *
 * <p>The reader reads the entire source upfront and is therefore suited only for source
 * files that fit comfortably in memory, which is always the case for Simian.
 *
 * <p><b>Known limitation:</b> tagged template literals (e.g. {@code html`<div/>`}) are
 * normalised identically to ordinary template literals. The tag identifier appears in the
 * fingerprint as a normal identifier immediately before the now-string token.
 */
final class TypeScriptTemplateLiteralNormalisingReader extends Reader {

    private final StringReader _normalised;

    TypeScriptTemplateLiteralNormalisingReader(final Reader source) throws IOException {
        _normalised = new StringReader(normalise(source));
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        return _normalised.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        _normalised.close();
    }

    // -------------------------------------------------------------------------

    private static String normalise(final Reader source) throws IOException {
        final StringBuilder src = readAll(source);
        final StringBuilder out = new StringBuilder(src.length());
        final int len = src.length();
        int i = 0;

        while (i < len) {
            final char c = src.charAt(i);

            if (c == '/' && i + 1 < len && src.charAt(i + 1) == '/') {
                // Single-line comment: pass through unchanged so backticks inside comments
                // are not misidentified as template literal delimiters.
                while (i < len && src.charAt(i) != '\n') {
                    out.append(src.charAt(i++));
                }
            } else if (c == '/' && i + 1 < len && src.charAt(i + 1) == '*') {
                // Block comment: pass through unchanged.
                out.append(src.charAt(i++)); // '/'
                out.append(src.charAt(i++)); // '*'
                while (i < len) {
                    if (src.charAt(i) == '*' && i + 1 < len && src.charAt(i + 1) == '/') {
                        out.append(src.charAt(i++)); // '*'
                        out.append(src.charAt(i++)); // '/'
                        break;
                    }
                    out.append(src.charAt(i++));
                }
            } else if (c == '"' || c == '\'') {
                // Regular string: pass through unchanged (handles backslash escapes).
                i = passString(src, i, c, len, out);
            } else if (c == '`') {
                // Template literal: convert to a double-quoted string.
                i = processTemplateLiteral(src, i + 1, len, out);
            } else {
                out.append(c);
                i++;
            }
        }

        return out.toString();
    }

    /**
     * Processes a template literal whose opening backtick has already been consumed.
     * Emits a synthetic {@code "..."} string and returns the index after the closing backtick.
     */
    private static int processTemplateLiteral(
            final StringBuilder src, int i, final int len, final StringBuilder out) {

        out.append('"'); // opening synthetic double-quote
        int newlines = 0;

        while (i < len) {
            final char c = src.charAt(i);

            if (c == '`') {
                // Closing backtick — end of template literal.
                i++;
                break;
            } else if (c == '\\' && i + 1 < len) {
                // Escape sequence in the static text part.
                final char next = src.charAt(i + 1);
                if (next == '\n') {
                    newlines++;
                    out.append(' ');
                } else if (next == '`' || next == '"') {
                    // Escaped backtick or quote: emit a space (quote would close the synthetic string).
                    out.append(' ');
                } else if (next != '\r') {
                    out.append(next);
                }
                i += 2;
            } else if (c == '$' && i + 1 < len && src.charAt(i + 1) == '{') {
                // ${...} interpolation: replace the entire expression with a space.
                out.append(' ');
                i += 2; // skip '${'
                final int[] result = skipExpression(src, i, len);
                newlines += result[1];
                i = result[0];
            } else if (c == '\n') {
                // Embedded newline in the static text — will be compensated by blank lines below.
                newlines++;
                out.append(' ');
                i++;
            } else if (c == '\r') {
                i++;
            } else if (c == '"') {
                // Embedded double-quote in static text: replace with space to avoid closing
                // the synthetic string prematurely.
                out.append(' ');
                i++;
            } else {
                out.append(c);
                i++;
            }
        }

        out.append('"'); // closing synthetic double-quote

        // Re-emit blank lines to compensate for newlines consumed from the template literal.
        for (int k = 0; k < newlines; k++) {
            out.append('\n');
        }

        return i;
    }

    /**
     * Skips a {@code ${...}} expression body; the opening '{' has already been consumed.
     * Handles nested strings and template literals so that braces inside them do not
     * affect the depth count. Returns {@code {indexAfterClosingBrace, newlinesConsumed}}.
     */
    private static int[] skipExpression(final StringBuilder src, int i, final int len) {
        int depth = 1;
        int newlines = 0;

        while (i < len && depth > 0) {
            final char c = src.charAt(i);

            if (c == '{') {
                depth++;
                i++;
            } else if (c == '}') {
                depth--;
                i++;
            } else if (c == '"' || c == '\'') {
                // String literal inside expression: skip to closing delimiter.
                final char delim = c;
                i++;
                while (i < len && src.charAt(i) != delim && src.charAt(i) != '\n') {
                    if (src.charAt(i) == '\\' && i + 1 < len) {
                        i += 2;
                    } else {
                        i++;
                    }
                }
                if (i < len && src.charAt(i) == delim) i++;
            } else if (c == '`') {
                // Nested template literal inside expression: skip to closing backtick,
                // handling escape sequences and counting any embedded newlines.
                i++;
                while (i < len && src.charAt(i) != '`') {
                    if (src.charAt(i) == '\\' && i + 1 < len) {
                        i += 2;
                    } else if (src.charAt(i) == '\n') {
                        newlines++;
                        i++;
                    } else {
                        i++;
                    }
                }
                if (i < len) i++; // closing backtick
            } else if (c == '/') {
                if (i + 1 < len && src.charAt(i + 1) == '/') {
                    // Line comment inside expression: skip to end of line.
                    while (i < len && src.charAt(i) != '\n') i++;
                } else if (i + 1 < len && src.charAt(i + 1) == '*') {
                    // Block comment inside expression: skip to closing */.
                    i += 2;
                    while (i + 1 < len && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) {
                        if (src.charAt(i) == '\n') newlines++;
                        i++;
                    }
                    if (i + 1 < len) i += 2;
                } else {
                    i++;
                }
            } else if (c == '\n') {
                newlines++;
                i++;
            } else {
                i++;
            }
        }

        return new int[]{i, newlines};
    }

    /**
     * Passes a regular single- or double-quoted string through to {@code out} unchanged,
     * handling escape sequences. Stops at the closing delimiter or end of line.
     *
     * @return index of the first character after the closing delimiter (or after EOL)
     */
    private static int passString(
            final StringBuilder src, int i, final char delim, final int len,
            final StringBuilder out) {

        out.append(delim); // opening quote
        i++;
        while (i < len && src.charAt(i) != delim && src.charAt(i) != '\n') {
            if (src.charAt(i) == '\\' && i + 1 < len) {
                out.append(src.charAt(i)).append(src.charAt(i + 1));
                i += 2;
            } else {
                out.append(src.charAt(i++));
            }
        }
        if (i < len && src.charAt(i) == delim) {
            out.append(delim); // closing quote
            i++;
        }
        return i;
    }

    private static StringBuilder readAll(final Reader source) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[4096];
        int n;
        while ((n = source.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb;
    }
}
