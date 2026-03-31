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

final class IgnoreBlocksLineListener extends DecoratorLineListener {
    private final BlockMarkerState _state;

    IgnoreBlocksLineListener(final LineListener decorated, final BlockMarkers markers) {
        super(decorated);
        _state = new BlockMarkerState(markers);
    }

    @Override
    public void file() {
        _state.reset();
    }

    @Override
    public void line(final int lineNumber, final LineBuffer line) {
        if (!_state.isInBlock(line)) {
            super.line(lineNumber, line);
        }
    }
}
