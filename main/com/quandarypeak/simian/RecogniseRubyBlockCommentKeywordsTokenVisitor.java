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

final class RecogniseRubyBlockCommentKeywordsTokenVisitor extends DecoratorTokenVisitor {
    RecogniseRubyBlockCommentKeywordsTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        if (name.contains("=")) {
            visitEqualsPrefixedIdentifier(name);
        } else {
            super.visitIdentifier(name, type);
        }
    }

    private void visitEqualsPrefixedIdentifier(final String name) {
        if ("=begin".equals(name) || "=end".equals(name)) {
            super.visitIdentifier(name, TokenVisitor.KEYWORD);
        } else {
            visitEqualsSeparatedIdentifier(name);
        }
    }

    private void visitEqualsSeparatedIdentifier(final String name) {
        int lastIndex = 0;
        int index = name.indexOf('=');
        while (index != -1) {
            visitIdentifierIfPresent(name, lastIndex, index);
            visitPunctuation('=');
            lastIndex = index;
            index = name.indexOf('=', index + 1);
        }
        visitIdentifierIfPresent(name, lastIndex + 1, name.length());
    }

    private void visitIdentifierIfPresent(final String name, final int startIndex, final int endIndex) {
        if (startIndex < endIndex) {
            super.visitIdentifier(name.substring(startIndex, endIndex), TokenVisitor.UNKNOWN);
        }
    }
}
