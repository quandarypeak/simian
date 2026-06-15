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
 * Pre-processes Python source to convert triple-quoted strings into single-quoted equivalents
 * before the StreamTokenizer runs.
 *
 * <p>StreamTokenizer's string-reading loop terminates on newline, so multi-line
 * {@code """..."""} and {@code '''...'''} strings cause it to desynchronise: the tokenizer
 * exits string mode at the first embedded newline, then interprets subsequent docstring lines
 * as code. This reader collapses each triple-quoted span into a single-line double- or
 * single-quoted string, replacing embedded newlines with spaces, before any tokenization
 * occurs. Blank lines are re-inserted after the collapsed string to keep the raw line count
 * identical to the original source.
 *
 * <p>The reader reads the entire source upfront; it is therefore suited only for source files
 * that fit comfortably in memory (which is always the case for code Simian is asked to analyse).
 */
final class PythonTripleQuoteNormalisingReader extends Reader {

    private final StringReader _normalised;

    PythonTripleQuoteNormalisingReader(final Reader source) throws IOException {
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
            if (c == '#') {
                // Line comment: pass through to (but not including) the newline.
                // The tokenizer will consume the '#' and everything to EOL as a comment token;
                // we just need to ensure any """ inside the comment is not misidentified.
                while (i < len && src.charAt(i) != '\n') {
                    out.append(src.charAt(i++));
                }
            } else if (c == '"' || c == '\'') {
                if (i + 2 < len && src.charAt(i + 1) == c && src.charAt(i + 2) == c) {
                    i = processTripleQuote(src, i, c, len, out);
                } else if (i + 1 < len && src.charAt(i + 1) == c) {
                    // Empty string "" or '' — pass through as-is.
                    out.append(c).append(c);
                    i += 2;
                } else {
                    i = passRegularString(src, i, c, len, out);
                }
            } else {
                out.append(c);
                i++;
            }
        }

        return out.toString();
    }

    /**
     * Collapses a triple-quoted string starting at {@code i} into a single-line quoted token.
     * Embedded newlines become spaces; the delimiter character inside the content becomes a space.
     * No trailing newline is emitted — the newline that followed the closing triple-quote is left
     * in the stream for the main loop to emit normally, preserving the total newline count.
     *
     * @return the index of the first character after the closing triple-quote
     */
    private static int processTripleQuote(
            final StringBuilder src, int i, final char delim, final int len, final StringBuilder out) {

        i += 3; // skip opening triple-quote
        int newlines = 0;
        final StringBuilder content = new StringBuilder();

        while (i < len) {
            // Handle backslash escape sequences before the closing-quote check.
            // Without this, \""" would be misread as a closing delimiter: the backslash
            // would be consumed as content, then the three quotes would match as a closer.
            // With this, \ + next-char are consumed together: the next char is never the
            // start of a triple-quote test, so \""" leaves only "" (two quotes) which
            // cannot form a closing triple-quote on their own.
            if (src.charAt(i) == '\\' && i + 1 < len) {
                final char next = src.charAt(i + 1);
                if (next == '\n') {
                    newlines++;
                    content.append(' ');
                } else if (next != '\r') {
                    // Replace escaped delimiter with space; pass other escaped chars through.
                    content.append(next == delim ? ' ' : next);
                }
                i += 2;
                continue;
            }
            if (i + 2 < len && src.charAt(i) == delim && src.charAt(i + 1) == delim && src.charAt(i + 2) == delim) {
                i += 3; // skip closing triple-quote
                break;
            }
            final char c = src.charAt(i++);
            if (c == '\n') {
                newlines++;
                content.append(' ');
            } else if (c == '\r') {
                // skip; the \n that follows (if any) will be counted on the next iteration
            } else if (c == delim) {
                // replace the delimiter character inside content to avoid confusing the tokenizer
                content.append(' ');
            } else {
                content.append(c);
            }
        }

        // Emit the collapsed string as a single-line token (no trailing newline).
        out.append(delim).append(content).append(delim);

        // Re-emit blank lines to compensate for the newlines consumed from the content.
        // The newline after the closing triple-quote (if any) is NOT counted here; it remains
        // in the stream and will be emitted by the outer loop, keeping totals correct.
        for (int k = 0; k < newlines; k++) {
            out.append('\n');
        }

        return i;
    }

    /**
     * Passes a regular single- or double-quoted string through unchanged,
     * including escape sequences, up to (and including) the closing quote or EOL.
     *
     * @return the index of the first character after the closing quote (or EOL)
     */
    private static int passRegularString(
            final StringBuilder src, int i, final char delim, final int len, final StringBuilder out) {

        out.append(delim); // opening quote
        i++;

        while (i < len && src.charAt(i) != delim && src.charAt(i) != '\n' && src.charAt(i) != '\r') {
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
