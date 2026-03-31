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

final class IgnoreLiteralsTokenVisitor extends DecoratorTokenVisitor {
    private static final char REPLACEMENT = '_';

    IgnoreLiteralsTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitNumber(final double value) {
        super.visitPunctuation(REPLACEMENT);
    }

    @Override
    public void visitString(final String text, final char type) {
        super.visitPunctuation(REPLACEMENT);
    }
}
