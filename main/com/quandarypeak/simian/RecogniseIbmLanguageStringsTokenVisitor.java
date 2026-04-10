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

final class RecogniseIbmLanguageStringsTokenVisitor extends DecoratorTokenVisitor {
    RecogniseIbmLanguageStringsTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitOther(final String s) {
        if (s.endsWith("'")) {
            if (s.length() == 1) {
                super.visitPunctuation('\'');
            } else {
                final int from = s.startsWith("=") ? 1 : 0;
                if (isString(s, from)) {
                    super.visitString(s.substring(s.indexOf('\'') + 1, s.length() - 1), '"');
                    return;
                }
            }
        }

        super.visitOther(s);
    }

    private boolean isString(final String s, final int from) {
        return isSelfDefiningTerm(s, from) || isLiteral(s, from);
    }

    private boolean isLiteral(final String s, final int from) {
        return s.startsWith("'", from);
    }

    private boolean isSelfDefiningTerm(final String s, final int from) {
        return s.startsWith("C", from);
    }

}
