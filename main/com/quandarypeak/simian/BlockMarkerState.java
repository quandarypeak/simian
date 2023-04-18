/*
 * Copyright (c) 2003-0 RedHill Consulting, Pty. Ltd.  All rights reserved.
 *
 * Redistribution and use in source or binary forms IS NOT PERMITTED
 * without prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SIMON HARRIS OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
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
