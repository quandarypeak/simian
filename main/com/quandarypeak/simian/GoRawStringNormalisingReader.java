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
 * Pre-processes Go source to convert backtick raw string literals into double-quoted
 * string equivalents before the StreamTokenizer runs.
 *
 * <p>StreamTokenizer only recognises {@code "} and {@code '} as string delimiters (hardcoded
 * in {@link com.quandarypeak.simian.AbstractStreamTokenizerParser}). Without this reader a Go
 * raw string literal like {@code `SELECT * FROM users`} is tokenised as a sequence of
 * identifier and punctuation tokens rather than a single string token. This causes:
 * <ul>
 *   <li>SQL, HTML, or other embedded languages inside raw strings to appear as code tokens
 *       in fingerprints, producing spurious differences between files</li>
 *   <li>{@code IGNORE_STRINGS} failing to suppress raw string values</li>
 *   <li>Two raw strings that differ only in content to produce different fingerprints even
 *       when the surrounding code structure is identical</li>
 * </ul>
 *
 * <p>This reader replaces each raw string literal with an equivalent double-quoted string:
 * <ul>
 *   <li>The opening and closing backticks are replaced with {@code "}</li>
 *   <li>Any {@code "} character inside the raw string is replaced with a space — raw strings
 *       cannot contain backticks but can contain double-quotes; replacing them avoids
 *       prematurely closing the synthetic double-quoted string</li>
 *   <li>Embedded newlines are replaced with spaces and blank lines are re-inserted after the
 *       closing {@code "} so the total line count is preserved, keeping Simian's line-number
 *       attribution correct</li>
 *   <li>Backslash is not an escape character in Go raw strings and is passed through as-is</li>
 * </ul>
 *
 * <p>Comments ({@code //} and {@code /* *}{@code /}) and regular strings ({@code "..."} and
 * rune literals {@code '...'}) are passed through unchanged so that backticks appearing
 * inside them are not misidentified as raw string delimiters.
 *
 * <p>This reader also suppresses grouped import blocks. Go's idiomatic import form places
 * multiple import paths inside {@code import ( ... )}, one per line. Only the {@code import}
 * line itself would be caught by {@link IgnoreLinesTokenVisitor}; the individual path lines
 * (e.g. {@code "fmt"}, {@code "os"}) have no trigger word and would otherwise fingerprint as
 * string tokens, creating false duplicate matches across files that share many imports. The
 * suppressor blanks every non-newline character between the opening {@code (} and the matching
 * {@code )}, preserving line count. The {@code import (} line is then caught and suppressed by
 * {@link IgnoreLinesTokenVisitor} as usual.
 *
 * <p>Known limitation: the opening {@code (} must appear on the same line as {@code import}.
 * The degenerate form {@code import\n(\n...)} is not suppressed (it is extremely rare and
 * non-idiomatic; {@code gofmt} always places the paren on the same line).
 *
 * <p>The reader reads the entire source upfront and is therefore suited only for source files
 * that fit comfortably in memory, which is always the case for Simian.
 */
final class GoRawStringNormalisingReader extends Reader {

    private final StringReader _normalised;

    GoRawStringNormalisingReader(final Reader source) throws IOException {
        final StringBuilder buf = normalise(source);
        suppressImportBlocks(buf);
        _normalised = new StringReader(buf.toString());
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

    private static StringBuilder normalise(final Reader source) throws IOException {
        final StringBuilder src = readAll(source);
        final StringBuilder out = new StringBuilder(src.length());
        final int len = src.length();
        int i = 0;

        while (i < len) {
            final char c = src.charAt(i);

            if (c == '/' && i + 1 < len && src.charAt(i + 1) == '/') {
                // Single-line comment: pass through unchanged so backticks inside comments
                // are not misidentified as raw string delimiters.
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
                // Regular string literal or rune literal: pass through unchanged,
                // handling escape sequences so that a backtick inside a string is not
                // misidentified as a raw string delimiter.
                i = passString(src, i, c, len, out);
            } else if (c == '`') {
                // Raw string literal: convert to a double-quoted string.
                i = processRawString(src, i + 1, len, out);
            } else {
                out.append(c);
                i++;
            }
        }

        return out;
    }

    /**
     * Processes a raw string literal whose opening backtick has already been consumed.
     * Emits a synthetic {@code "..."} string and returns the index after the closing backtick.
     *
     * <p>Go raw strings contain no escape sequences — every character is literal. The only
     * transformations needed are replacing embedded {@code "} with a space (to avoid closing
     * the synthetic string) and replacing embedded newlines with spaces (with compensating
     * blank lines to preserve total line count).
     */
    private static int processRawString(
            final StringBuilder src, int i, final int len, final StringBuilder out) {

        out.append('"'); // opening synthetic double-quote
        int newlines = 0;

        while (i < len) {
            final char c = src.charAt(i);

            if (c == '`') {
                // Closing backtick — end of raw string.
                i++;
                break;
            } else if (c == '\n') {
                // Embedded newline: replace with space; compensate with blank line below.
                newlines++;
                out.append(' ');
                i++;
            } else if (c == '\r') {
                // Lone CR (old Mac OS 9 line ending): treat as a newline for line-count purposes
                // unless followed by LF (which will be counted by the '\n' branch).
                if (i + 1 >= len || src.charAt(i + 1) != '\n') {
                    newlines++;
                    out.append(' ');
                }
                i++;
            } else if (c == '"') {
                // Embedded double-quote: replace with space to avoid closing the synthetic string.
                out.append(' ');
                i++;
            } else {
                out.append(c);
                i++;
            }
        }

        out.append('"'); // closing synthetic double-quote

        // Re-emit blank lines to compensate for newlines consumed from the raw string body.
        for (int k = 0; k < newlines; k++) {
            out.append('\n');
        }

        return i;
    }

    /**
     * Passes a regular string literal or rune literal through to {@code out} unchanged,
     * handling backslash escape sequences. Stops at the closing delimiter or end of line.
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

    /**
     * Blanks the body of every {@code import ( ... )} block in {@code src}, replacing
     * each non-newline character inside (and including) the {@code (} and {@code )} with
     * a space. Newlines are preserved so that the total line count of the file is unchanged.
     *
     * <p>Only {@code import} that appears as the first word on a line (possibly after
     * horizontal whitespace) and is followed by {@code (} on the same line is processed.
     * This avoids false matches for the word {@code import} appearing inside comments or
     * string literals on non-declaration lines.
     *
     * <p>Inside the block, {@code //} line comments and {@code /* *}{@code /} block comments
     * are tracked so that a {@code )} appearing inside a comment is not mistaken for the
     * closing delimiter of the import block.
     */
    private static void suppressImportBlocks(final StringBuilder src) {
        final int len = src.length();
        int i = 0;
        while (i < len) {
            // Skip optional leading horizontal whitespace — 'import' may be indented.
            while (i < len && (src.charAt(i) == ' ' || src.charAt(i) == '\t')) {
                i++;
            }
            // Check whether the first word on this line is 'import'.
            if (matchesWord(src, i, "import", len)) {
                int j = i + 6; // advance past "import"
                // Skip horizontal whitespace between 'import' and a possible '('.
                while (j < len && (src.charAt(j) == ' ' || src.charAt(j) == '\t')) {
                    j++;
                }
                if (j < len && src.charAt(j) == '(') {
                    i = blankImportBlock(src, j, len);
                    continue;
                }
            }
            // Not an import block — advance to the start of the next line.
            while (i < len && src.charAt(i) != '\n') {
                i++;
            }
            if (i < len) {
                i++; // consume the '\n'
            }
        }
    }

    /**
     * Returns true if {@code word} appears in {@code src} at {@code pos} and is followed
     * immediately by a non-word character (or end of input).
     */
    private static boolean matchesWord(
            final StringBuilder src, final int pos, final String word, final int len) {
        final int wlen = word.length();
        if (pos + wlen > len) {
            return false;
        }
        for (int k = 0; k < wlen; k++) {
            if (src.charAt(pos + k) != word.charAt(k)) {
                return false;
            }
        }
        return pos + wlen >= len || !isWordChar(src.charAt(pos + wlen));
    }

    /**
     * Blanks the content of a grouped import block whose opening {@code (} is at
     * {@code start}. Replaces every non-newline character from {@code (} up to and
     * including the matching {@code )} with a space. Returns the index immediately
     * after the {@code )}, or {@code len} if the block is unterminated.
     *
     * <p>Tracks {@code //} and {@code /* *}{@code /} comments so that a {@code )}
     * inside a comment is not mistaken for the closing delimiter.
     */
    private static int blankImportBlock(final StringBuilder src, int i, final int len) {
        src.setCharAt(i++, ' '); // blank the opening '('

        boolean inLineComment = false;
        boolean inBlockComment = false;

        while (i < len) {
            final char c = src.charAt(i);

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false; // preserve the newline
                } else {
                    src.setCharAt(i, ' ');
                }
            } else if (inBlockComment) {
                if (c == '*' && i + 1 < len && src.charAt(i + 1) == '/') {
                    src.setCharAt(i, ' ');
                    src.setCharAt(i + 1, ' ');
                    i += 2;
                    inBlockComment = false;
                    continue;
                } else if (c != '\n') {
                    src.setCharAt(i, ' ');
                }
            } else if (c == ')') {
                src.setCharAt(i, ' ');
                return i + 1;
            } else if (c == '/' && i + 1 < len && src.charAt(i + 1) == '/') {
                src.setCharAt(i, ' ');
                src.setCharAt(i + 1, ' ');
                i += 2;
                inLineComment = true;
                continue;
            } else if (c == '/' && i + 1 < len && src.charAt(i + 1) == '*') {
                src.setCharAt(i, ' ');
                src.setCharAt(i + 1, ' ');
                i += 2;
                inBlockComment = true;
                continue;
            } else if (c != '\n') {
                src.setCharAt(i, ' ');
            }

            i++;
        }

        return i; // unterminated block (malformed Go) — keep what was blanked
    }

    private static boolean isWordChar(final char c) {
        return Character.isLetterOrDigit(c) || c == '_';
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
