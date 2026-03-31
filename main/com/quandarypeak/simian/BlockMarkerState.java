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

final class BlockMarkerState {
    private final BlockMarkers _markers;
    private int _count;

    BlockMarkerState(final BlockMarkers markers) {
        _markers = Objects.requireNonNull(markers, "markers");
    }

    public boolean isInBlock(final CharSequence text) {
        if (_markers.isStart(text)) {
            ++_count;
        } else if (_markers.isEnd(text)) {
            --_count;
            if (_count < 1) {
                _count = 0;
            }
        } else {
            return isInBlock();
        }
        return true;
    }

    public boolean isInBlock() {
        return _count > 0;
    }

    public void reset() {
        _count = 0;
    }
}
