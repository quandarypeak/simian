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
 * Ignores instances of specified words (eg 'public', 'private', 'protected', 'static', 'final').
 */
final class IgnoreWordsTokenVisitor extends DecoratorTokenVisitor {
    /**
     * Words to ignore
     */
    private final Set<String> words;

    /**
     * Constructor.
     *
     * @param words     Words to ignore
     * @param decorated The token visitor being decorated
     */
    IgnoreWordsTokenVisitor(final Set<String> words, final TokenVisitor decorated) {
        super(decorated);
        this.words = Objects.requireNonNull(words);
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        if (!words.contains(name)) {
            super.visitIdentifier(name, type);
        }
    }
}
