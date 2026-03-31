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
import java.util.Set;

/**
 * Ignores all lines containing a trigger word.
 */
final class IgnoreLinesTokenVisitor extends DecoratorTokenVisitor {
    /**
     * The words that trigger a line to be ignored
     */
    private final Set<String> _triggerWords;

    /**
     * Should we ignore the current line?
     */
    private boolean _ignoreLine;

    /**
     * Constructor.
     *
     * @param decorated    The token visitor being decorated
     * @param triggerWords The words that trigger a line to be ignored
     */
    IgnoreLinesTokenVisitor(final TokenVisitor decorated, final Set<String> triggerWords) {
        super(decorated);
        _triggerWords = Objects.requireNonNull(triggerWords, "triggerWords");
    }

    @Override
    public void visit(final int lineNumber) {
        _ignoreLine = false;
        super.visit(lineNumber);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        if (_triggerWords.contains(name)) {
            _ignoreLine = true;
        } else {
            super.visitIdentifier(name, type);
        }
    }

    @Override
    public void visitEnd() {
        if (!_ignoreLine) {
            super.visitEnd();
        }
    }
}
