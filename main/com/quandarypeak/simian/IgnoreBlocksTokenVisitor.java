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

final class IgnoreBlocksTokenVisitor extends DecoratorTokenVisitor {
    private final BlockMarkerState _state;

    IgnoreBlocksTokenVisitor(final TokenVisitor decorated, final BlockMarkers markers) {
        super(decorated);
        _state = new BlockMarkerState(markers);
    }

    @Override
    public void visit(final int lineNumber) {
        if (!_state.isInBlock()) {
            super.visit(lineNumber);
        }
    }

    @Override
    public void visitNumber(final double value) {
        if (!_state.isInBlock()) {
            super.visitNumber(value);
        }
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        if (!_state.isInBlock()) {
            super.visitIdentifier(name, type);
        }
    }

    @Override
    public void visitString(final String text, final char type) {
        if (!_state.isInBlock()) {
            super.visitString(text, type);
        }
    }

    @Override
    public void visitComment(final String text) {
        if (!_state.isInBlock(text)) {
            super.visitComment(text);
        }
    }

    @Override
    public void visitPunctuation(final char c) {
        if (!_state.isInBlock()) {
            super.visitPunctuation(c);
        }
    }

    @Override
    public void visitEnd() {
        if (!_state.isInBlock()) {
            super.visitEnd();
        }
    }
}
