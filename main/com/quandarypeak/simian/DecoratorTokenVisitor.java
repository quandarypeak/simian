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

import java.util.Objects;

class DecoratorTokenVisitor implements TokenVisitor {
    private final TokenVisitor _decorated;

    DecoratorTokenVisitor(final TokenVisitor decorated) {
        _decorated = Objects.requireNonNull(decorated, "decorated");;
    }

    @Override
    public void visitFile() {
        _decorated.visitFile();
    }

    @Override
    public void visit(final int lineNumber) {
        _decorated.visit(lineNumber);
    }

    @Override
    public void visitNumber(final double value) {
        _decorated.visitNumber(value);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        _decorated.visitIdentifier(name, type);
    }

    @Override
    public void visitString(final String text, final char type) {
        _decorated.visitString(text, type);
    }

    @Override
    public void visitComment(final String text) {
        _decorated.visitComment(text);
    }

    @Override
    public void visitPunctuation(final char c) {
        _decorated.visitPunctuation(c);
    }

    @Override
    public void visitOther(final String s) {
        _decorated.visitOther(s);
    }

    @Override
    public void visitEnd() {
        _decorated.visitEnd();
    }
}
