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

/**
 * Base class for line listeners that wish to add some specific behaviour such as line filtering, keyword filtering,
 * etc. By default simply delegates all calls to the decorated line listener.
 */
class DecoratorLineListener implements LineListener {
    private final LineListener _decorated;

    DecoratorLineListener(final LineListener decorated) {
        _decorated = Objects.requireNonNull(decorated, "decorated");
    }

    @Override
    public void file() {
        _decorated.file();
    }

    @Override
    public void line(final int lineNumber, final LineBuffer line) {
        _decorated.line(lineNumber, line);
    }
}
