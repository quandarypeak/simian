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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Objects;

final class IbmSystem390AssemblerParser implements Parser {
    private final TokenVisitor _visitor;

    IbmSystem390AssemblerParser(final TokenVisitor visitor) {
        _visitor = Objects.requireNonNull(visitor, "visitor");
    }

    @Override
    public int parse(final Reader reader) throws IOException {
        _visitor.visitFile();
        return parse(new IbmSystem390AssemblerLineReader(reader));
    }

    public int parse(final IbmSystem390AssemblerLineReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            _visitor.visit(reader.getLineNumber());
            parse(line);
            _visitor.visitEnd();
        }

        return reader.getLineNumber();
    }

    private void parse(final String line) {
        if (isCommentLine(line)) {
            return;
        }

        final CharacterIterator iter = new StringCharacterIterator(line);
        iter.first();
        parseLabel(iter);
        parseOperator(iter);
        parseOperands(iter);
        parseRemarks(line, iter.getIndex());
    }

    private void parseLabel(final CharacterIterator iter) {
        parseIdentifier(iter);
    }

    private void parseOperator(final CharacterIterator iter) {
        final String operator = parseIdentifier(iter);
        if (operator != null) {
            _visitor.visitIdentifier(operator, TokenVisitor.KEYWORD);
        }
    }

    private void parseOperands(final CharacterIterator iter) {
        final StringBuilder buffer = new StringBuilder();
        boolean insideQuotes = false;
        int parenCount = 0;

        loop:
        for (char c = iter.current(), last = ' '; c != CharacterIterator.DONE; c = iter.next()) {
            switch (c) {
                case ' ':
                    if (!insideQuotes) {
                        if (parenCount == 0) {
                            break loop;
                        } else {
                            appendOperand(buffer, iter);
                            iter.previous();
                            last = c;
                            continue;
                        }
                    }
                    break;
                case '(':
                    if (!insideQuotes) {
                        appendOperand(buffer, iter);
                        _visitor.visitPunctuation(c);
                        ++parenCount;
                        last = c;
                        continue;
                    }
                    break;
                case ')':
                    if (!insideQuotes) {
                        appendOperand(buffer, iter);
                        _visitor.visitPunctuation(c);
                        --parenCount;
                        if (parenCount < 0) {
                            parenCount = 0;
                        }
                        last = c;
                        continue;
                    }
                    break;
                case '\'':
                    if (insideQuotes || last != 'L') {
                        insideQuotes = !insideQuotes;
                    }
                    break;
                case ',':
                    if (!insideQuotes) {
                        appendOperand(buffer, iter);
                        _visitor.visitPunctuation(c);
                        last = c;
                        continue;
                    }
                    break;
                default:
                    // Do nothing
            }
            buffer.append(c);
            last = c;
        }
        appendOperand(buffer, iter);
    }

    private void appendOperand(final StringBuilder buffer, final CharacterIterator iter) {
        if (buffer.length() > 0) {
            _visitor.visitOther(buffer.toString());
            buffer.setLength(0);
        }
        skipBlanks(iter);
    }

    private void parseRemarks(final String line, final int index) {
        if (index < line.length()) {
            _visitor.visitComment(line.substring(index));
        }
    }

    private String parseIdentifier(final CharacterIterator iter) {
        final StringBuilder buffer = new StringBuilder();
        for (char c = iter.current(); c != CharacterIterator.DONE && c != ' '; c = iter.next()) {
            buffer.append(c);
        }
        skipBlanks(iter);
        return buffer.length() > 0 ? buffer.toString() : null;
    }

    private void skipBlanks(final CharacterIterator iter) {
        for (char c = iter.current(); c == ' '; c = iter.next()) {
        }
    }

    private boolean isCommentLine(final String line) {
        return isAssemblerCommentLine(line) || isJclCommentLine(line);
    }

    private boolean isJclCommentLine(final String line) {
        if (line.startsWith("//")) {
            _visitor.visitComment(line.substring(2));
            return true;
        }
        return false;
    }

    private boolean isAssemblerCommentLine(final String line) {
        if (line.startsWith("*")) {
            _visitor.visitComment(line.substring(1));
            return true;
        }
        return false;
    }
}
