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
