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
