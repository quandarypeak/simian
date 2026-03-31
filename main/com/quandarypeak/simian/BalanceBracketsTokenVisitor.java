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

class BalanceBracketsTokenVisitor extends DecoratorTokenVisitor {
    private final char openBracket;
    private final char closeBracket;
    private int outstanding;

    BalanceBracketsTokenVisitor(final char openBracket, final char closeBracket, final TokenVisitor decorated) {
        super(decorated);
        this.openBracket = openBracket;
        this.closeBracket = closeBracket;
    }

    @Override
    public void visitFile() {
        outstanding = 0;
        super.visitFile();
    }

    @Override
    public void visit(final int lineNumber) {
        if (!hasOutstanding()) {
            super.visit(lineNumber);
        }
    }

    @Override
    public void visitPunctuation(final char c) {
        if (c == openBracket) {
            ++outstanding;
        } else if (c == closeBracket) {
            --outstanding;
            if (outstanding < 0) {
                outstanding = 0;
            }
        }

        super.visitPunctuation(c);
    }

    @Override
    public void visitEnd() {
        if (!hasOutstanding()) {
            super.visitEnd();
        }
    }

    private boolean hasOutstanding() {
        return outstanding > 0;
    }
}
