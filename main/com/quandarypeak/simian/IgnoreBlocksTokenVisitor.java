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

final class IgnoreBlocksTokenVisitor extends DecoratorTokenVisitor {
    private final BlockMarkerState _state;

    IgnoreBlocksTokenVisitor(final TokenVisitor decorated, final BlockMarkers markers) {
        super(decorated);
        _state = new BlockMarkerState(markers);
    }

    @Override
    public void visit(final int lineNumber) {
        if (!_state.isInBlock()) {
            super.visit(lineNumber);
        }
    }

    @Override
    public void visitNumber(final double value) {
        if (!_state.isInBlock()) {
            super.visitNumber(value);
        }
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        if (!_state.isInBlock()) {
            super.visitIdentifier(name, type);
        }
    }

    @Override
    public void visitString(final String text, final char type) {
        if (!_state.isInBlock()) {
            super.visitString(text, type);
        }
    }

    @Override
    public void visitComment(final String text) {
        if (!_state.isInBlock(text)) {
            super.visitComment(text);
        }
    }

    @Override
    public void visitPunctuation(final char c) {
        if (!_state.isInBlock()) {
            super.visitPunctuation(c);
        }
    }

    @Override
    public void visitEnd() {
        if (!_state.isInBlock()) {
            super.visitEnd();
        }
    }
}
