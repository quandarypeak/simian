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

public final class CobolLineReader {
    private final LineNumberReader _reader;
    private int _lineNumber;

    public CobolLineReader(final Reader reader) {
        _reader = new LineNumberReader(reader);
    }

    public final String readLine() throws IOException {
        String line = _reader.readLine();
        if (line == null) {
            return null;
        }

        _lineNumber = _reader.getLineNumber();

        do {
            if (line.length() > 6) {
                final char column7 = line.charAt(6);
                if (column7 == '*' || column7 == '/') {
                    continue;
                }

                if (line.length() > 7) {
                    line = line.substring(7, Math.min(72, line.length()));
                }

                if (line.endsWith(".")) {
                    line = line.substring(0, line.length() - 1);
                }

                line = line.toUpperCase();
                line = line.trim();
            } else {
                line = "";
            }

            break;
        } while ((line = _reader.readLine()) != null);

        return line;
    }

    public final int getLineNumber() {
        return _lineNumber;
    }
}
