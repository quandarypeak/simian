package com.quandarypeak.simian.java.io;

import java.io.IOException;
import java.io.Reader;

/**
 * Derivative of the OpenJDK class to:
 *
 * 1. Fix a line counting bug; and
 * 2. Add support for dash-dash ("--") comments.
 *
 * The original license states that
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
public final class StreamTokenizer {
    private final Reader _reader;

    private char[] _buf = new char[20];

    private int _peekc = NEED_CHAR;

    private static final int NEED_CHAR = Integer.MAX_VALUE;
    private static final int SKIP_LF = Integer.MAX_VALUE - 1;

    private boolean _forceLower;

    private boolean _skipLF;

    private int _lineno = 1;

    private boolean _eolIsSignificantP;
    private boolean _slashSlashCommentsP;
    private boolean _slashStarCommentsP;
    private boolean _dashDashCommentsP;

    private final byte[] _ctype = new byte[256];
    private static final byte CT_WHITESPACE = 1;
    private static final byte CT_DIGIT = 2;
    private static final byte CT_ALPHA = 4;
    private static final byte CT_QUOTE = 8;
    private static final byte CT_COMMENT = 16;

    public int _ttype = TT_NOTHING;

    public static final int TT_EOF = -1;

    public static final int TT_EOL = '\n';

    public static final int TT_NUMBER = -2;

    public static final int TT_WORD = -3;

    public static final int TT_COMMENT = -4;

    private static final int TT_NOTHING = -4;

    private boolean _adjustLineCount;

    public String sval;

    public double nval;

    public StreamTokenizer(final Reader r) {
        if (r == null) {
            throw new NullPointerException();
        }
        _reader = r;

        wordChars('a', 'z');
        wordChars('A', 'Z');
        wordChars(128 + 32, 255);
        whitespaceChars(0, ' ');
        commentChar('/');
        quoteChar('"');
        quoteChar('\'');
        parseNumbers();
    }

    public void resetSyntax() {
        for (int i = _ctype.length; --i >= 0; ) {
            _ctype[i] = 0;
        }
    }

    public void wordChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi >= _ctype.length) {
            hi = _ctype.length - 1;
        }
        while (low <= hi) {
            _ctype[low++] |= CT_ALPHA;
        }
    }

    public void whitespaceChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi >= _ctype.length) {
            hi = _ctype.length - 1;
        }
        while (low <= hi) {
            _ctype[low++] = CT_WHITESPACE;
        }
    }

    public void ordinaryChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi >= _ctype.length) {
            hi = _ctype.length - 1;
        }
        while (low <= hi) {
            _ctype[low++] = 0;
        }
    }

    public void ordinaryChar(final int ch) {
        if (ch >= 0 && ch < _ctype.length) {
            _ctype[ch] = 0;
        }
    }

    public void commentChar(final int ch) {
        if (ch >= 0 && ch < _ctype.length) {
            _ctype[ch] = CT_COMMENT;
        }
    }

    public void quoteChar(final int ch) {
        if (ch >= 0 && ch < _ctype.length) {
            _ctype[ch] = CT_QUOTE;
        }
    }

    public void parseNumbers() {
        for (int i = '0'; i <= '9'; i++) {
            _ctype[i] |= CT_DIGIT;
        }
        _ctype['.'] |= CT_DIGIT;
        _ctype['-'] |= CT_DIGIT;
    }

    public void eolIsSignificant(final boolean flag) {
        _eolIsSignificantP = flag;
    }

    public void slashStarComments(final boolean flag) {
        _slashStarCommentsP = flag;
    }

    public void slashSlashComments(final boolean flag) {
        _slashSlashCommentsP = flag;
    }

    public void dashDashComments(final boolean flag) {
        _dashDashCommentsP = flag;
    }

    public void lowerCaseMode(final boolean fl) {
        _forceLower = fl;
    }

    private int read() throws IOException {
        int c = _reader.read();
        if (_skipLF) {
            if (c == '\n') {
                c = _reader.read();
            }
            _skipLF = false;
        }
        switch (c) {
            case '\r':
                _skipLF = true;
            case '\n':		/* Fall through */
                _lineno++;
                return '\n';
        }
        return c;
    }

    public int nextToken() throws IOException {
        final byte[] ct = _ctype;
        sval = null;

        int c = _peekc;
        if (c < 0) {
            c = NEED_CHAR;
        }
        if (c == SKIP_LF) {
            c = read();
            if (c < 0) {
                return endOfFile();
            }
            if (c == '\n') {
                c = NEED_CHAR;
            }
        }
        if (c == NEED_CHAR) {
            c = read();
            if (c < 0) {
                return endOfFile();
            }
        }
        _ttype = c;		/* Just to be safe */

        /* Set _peekc so that the next invocation of nextToken will read
        * another character unless _peekc is reset in this invocation
        */
        _peekc = NEED_CHAR;

        int ctype = c < 256 ? ct[c] : CT_ALPHA;
        while ((ctype & CT_WHITESPACE) != 0) {
            if (c == '\r') {
                if (_eolIsSignificantP) {
                    _peekc = SKIP_LF;
                    return _ttype = TT_EOL;
                }
                c = read();
                if (c == '\n') {
                    c = read();
                }
            } else {
                if (c == '\n') {
                    if (_eolIsSignificantP) {
                        return _ttype = TT_EOL;
                    }
                }
                c = read();
            }
            if (c < 0) {
                return endOfFile();
            }
            ctype = c < 256 ? ct[c] : CT_ALPHA;
        }

        if ((ctype & CT_DIGIT) != 0) {
            boolean neg = false;
            if (c == '-') {
                c = read();
                if (c != '.' && (c < '0' || c > '9')) {
                    if (c == '-' && _dashDashCommentsP) {
                        return readSingleLineComment();
                    }
                    _peekc = c;
                    return _ttype = '-';
                }
                neg = true;
            }
            double v = 0;
            int decexp = 0;
            int seendot = 0;
            while (true) {
                if (c == '.' && seendot == 0) {
                    seendot = 1;
                } else if ('0' <= c && c <= '9') {
                    v = v * 10 + (c - '0');
                    decexp += seendot;
                } else {
                    break;
                }
                c = read();
            }
            _peekc = c;
            if (decexp != 0) {
                double denom = 10;
                decexp--;
                while (decexp > 0) {
                    denom *= 10;
                    decexp--;
                }
                /* Do one division of a likely-to-be-more-accurate number */
                v = v / denom;
            }
            nval = neg ? -v : v;
            return _ttype = TT_NUMBER;
        }

        if ((ctype & CT_ALPHA) != 0) {
            int i = 0;
            do {
                i = addToBuf(c, i);
                c = read();
                ctype = c < 0 ? CT_WHITESPACE : c < 256 ? ct[c] : CT_ALPHA;
            } while ((ctype & (CT_ALPHA | CT_DIGIT)) != 0);
            _peekc = c;
            sval = String.copyValueOf(_buf, 0, i);
            if (_forceLower) {
                sval = sval.toLowerCase();
            }
            return _ttype = TT_WORD;
        }

        if ((ctype & CT_QUOTE) != 0) {
            _ttype = c;
            int i = 0;
            /* Invariants (because \Octal needs a lookahead):
            *   (i)  c contains char value
            *   (ii) d contains the lookahead
            */
            int d = read();
            while (d >= 0 && d != _ttype && d != '\n' && d != '\r') {
                if (d == '\\') {
                    c = read();
                    final int first = c;   /* To allow \377, but not \477 */
                    if (c >= '0' && c <= '7') {
                        c = c - '0';
                        int c2 = read();
                        if ('0' <= c2 && c2 <= '7') {
                            c = (c << 3) + c2 - '0';
                            c2 = read();
                            if ('0' <= c2 && c2 <= '7' && first <= '3') {
                                c = (c << 3) + c2 - '0';
                                d = read();
                            } else {
                                d = c2;
                            }
                        } else {
                            d = c2;
                        }
                    } else {
                        switch (c) {
                            case 'a':
                                c = 0x7;
                                break;
                            case 'b':
                                c = '\b';
                                break;
                            case 'f':
                                c = 0xC;
                                break;
                            case 'n':
                                c = '\n';
                                break;
                            case 'r':
                                c = '\r';
                                break;
                            case 't':
                                c = '\t';
                                break;
                            case 'v':
                                c = 0xB;
                                break;
                        }
                        d = read();
                    }
                } else {
                    c = d;
                    d = read();
                }
                i = addToBuf(c, i);
            }

            /* If we broke out of the loop because we found a matching quote
            * character then arrange to read a new character next time
            * around; otherwise, save the character.
            */
            _peekc = d == _ttype ? NEED_CHAR : d;

            sval = String.copyValueOf(_buf, 0, i);
            return _ttype;
        }

        if (c == '/' && (_slashSlashCommentsP || _slashStarCommentsP)) {
            c = read();
            if (c == '*' && _slashStarCommentsP) {
                int prevc = 0;
                while ((c = read()) != '/' || prevc != '*') {
                    if (c == '\r') {
                        c = read();
                        if (c == '\n') {
                            c = read();
                        }
                    } else {
                        if (c == '\n') {
                            c = read();
                        }
                    }
                    if (c < 0) {
                        return endOfFile();
                    }
                    prevc = c;
                }
                return nextToken();
            } else if (c == '/' && _slashSlashCommentsP) {
                return readSingleLineComment();
            } else if ((ct['/'] & CT_COMMENT) != 0) {
                /* Arbitrary single line comment */
                return readSingleLineComment();
            } else {
                _peekc = c;
                return _ttype = '/';
            }
        }

        if ((ctype & CT_COMMENT) != 0) {
            return readSingleLineComment();
        }

        return _ttype = c;
    }

    private int endOfFile() {
        _adjustLineCount = _ttype == TT_EOL;
        return _ttype = TT_EOF;
    }

    public int lineno() {
        return _lineno;
    }

    public int lineCount() {
        if (_adjustLineCount) {
            return _lineno - 1;
        }
        return _lineno;
    }

    private int readSingleLineComment() throws IOException {
        int c;
        int i = 0;
        while ((c = read()) != '\n' && c != '\r' && c >= 0) {
            i = addToBuf(c, i);
        }
        _peekc = c;
        sval = String.copyValueOf(_buf, 0, i);
        return _ttype = TT_COMMENT;
    }

    private int addToBuf(final int c, int i) {
        if (i >= _buf.length) {
            final char[] nb = new char[_buf.length * 2];
            System.arraycopy(_buf, 0, nb, 0, _buf.length);
            _buf = nb;
        }
        _buf[i++] = (char) c;
        return i;
    }
}
