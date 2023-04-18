/*
 * Copyright (c) 2003-08 RedHill Consulting, Pty. Ltd.  All rights reserved.
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
