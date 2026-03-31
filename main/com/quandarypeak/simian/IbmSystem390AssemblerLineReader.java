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
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

final class IbmSystem390AssemblerLineReader {
    private final LineNumberReader _reader;
    private int _lineNumber;

    IbmSystem390AssemblerLineReader(final Reader reader) {
        _reader = new LineNumberReader(reader);
    }

    public final String readLine() throws IOException {
        final StringBuilder buffer = new StringBuilder();

        String line = _reader.readLine();
        if (line == null) {
            return null;
        }

        _lineNumber = _reader.getLineNumber();

        boolean continued = false;
        do {
            boolean continuationExpected = false;
            if (line.length() > 71) {
                continuationExpected = line.charAt(71) != ' ';
                line = line.substring(0, 71);
            }

            if (continued) {
                line = line.substring(Math.min(15, line.length()));
            }

            append(buffer, line);

            continued = continuationExpected;
        } while (continued && (line = _reader.readLine()) != null);

        return buffer.toString();
    }

    private void append(final StringBuilder buffer, final String s) {
        final CharacterIterator i = new StringCharacterIterator(s);
        for (char c = i.last(); c != CharacterIterator.DONE && c == ' '; c = i.previous()) {
            // Ignore space
        }

        if (i.current() != CharacterIterator.DONE) {
            int remaining = i.getIndex() + 1;

            for (char c = i.first(); remaining > 0; c = i.next(), --remaining) {
                buffer.append(Character.toUpperCase(c));
            }
        }
    }

    public final int getLineNumber() {
        return _lineNumber;
    }
}
