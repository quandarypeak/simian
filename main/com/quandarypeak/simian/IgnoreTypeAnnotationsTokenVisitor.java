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

/**
 * Suppresses TypeScript type annotations from fingerprints when
 * {@link Option#IGNORE_TYPE_ANNOTATIONS} is active.
 *
 * <p>A type annotation is a {@code :} followed by a type expression. This visitor buffers
 * each {@code :} and, when the immediately-following token is a TYPE-classified identifier
 * (e.g. {@code string}, {@code number}, a PascalCase user type), suppresses both the colon
 * and the type expression. When the token after {@code :} is not a type (e.g. an object
 * literal value or the else-branch of a ternary), the {@code :} is emitted normally.
 *
 * <p>Full type expressions are suppressed, including:
 * <ul>
 *   <li>Simple names: {@code : string}, {@code : MyClass}</li>
 *   <li>Generic types: {@code : Array<string>}, {@code : Map<string, number>}</li>
 *   <li>Array types: {@code : string[]}</li>
 *   <li>Tuple types: {@code : [string, number]}, {@code : [A, B, C]}, {@code : [string[], number]}</li>
 *   <li>Union and intersection types: {@code : string | number}, {@code : A & B}</li>
 *   <li>Combinations: {@code : Array<string> | null} (up to the first non-TYPE token)</li>
 * </ul>
 *
 * <p><b>Known limitations:</b>
 * <ul>
 *   <li>Literal keyword types ({@code null}, {@code undefined}, {@code true}, {@code false})
 *       are classified as KEYWORD rather than TYPE and therefore survive suppression.
 *       {@code string | null} will suppress {@code string} but leave {@code | null} visible.
 *       Use {@link Option#IGNORE_IDENTIFIERS} to fully erase all identifiers including
 *       keyword types.</li>
 *   <li>A {@code :} that opens a ternary else-branch followed by an array literal
 *       ({@code cond ? val : [a, b]}) is indistinguishable from a tuple type annotation
 *       and the array literal will be incorrectly suppressed. This is an inherent ambiguity
 *       in the token-stream approach and cannot be resolved without a full expression
 *       parser.</li>
 * </ul>
 *
 * <p>This visitor must sit between the base visitor chain and
 * {@link RecogniseIdentifiersTokenVisitor} so that it receives TYPE-classified identifiers.
 * It depends on identifier classification being performed upstream before tokens arrive here.
 */
final class IgnoreTypeAnnotationsTokenVisitor extends DecoratorTokenVisitor {

    private enum State { NORMAL, PENDING_COLON, IN_TYPE, IN_GENERIC, IN_ARRAY }

    private State _state = State.NORMAL;
    private int _genericDepth;
    // Depth counter for nested '[' inside an array/tuple type — needed because tuple
    // elements can themselves be array types, e.g.: [string[], number].
    private int _arrayDepth;

    IgnoreTypeAnnotationsTokenVisitor(final TokenVisitor decorated) {
        super(decorated);
    }

    @Override
    public void visitFile() {
        _state = State.NORMAL;
        _genericDepth = 0;
        _arrayDepth = 0;
        super.visitFile();
    }

    @Override
    public void visitPunctuation(final char c) {
        switch (_state) {
            case NORMAL:
                if (c == ':') {
                    _state = State.PENDING_COLON;
                    // Buffer the ':' — emit only if not followed by a TYPE identifier.
                } else {
                    super.visitPunctuation(c);
                }
                break;

            case PENDING_COLON:
                if (c == '[') {
                    // Tuple type annotation: ': [string, number]' — suppress ':' + entire tuple.
                    _arrayDepth = 0;
                    _state = State.IN_ARRAY;
                } else {
                    // Next token is punctuation (not a TYPE identifier) — flush the buffered ':'.
                    super.visitPunctuation(':');
                    _state = State.NORMAL;
                    super.visitPunctuation(c);
                }
                break;

            case IN_TYPE:
                if (c == '|' || c == '&') {
                    // Union / intersection operator — suppress and stay in type context.
                } else if (c == '<') {
                    _genericDepth = 1;
                    _state = State.IN_GENERIC;
                } else if (c == '[') {
                    _arrayDepth = 0;
                    _state = State.IN_ARRAY;
                } else {
                    // End of type annotation (e.g. '=', ',', ')', '{', '}').
                    _state = State.NORMAL;
                    super.visitPunctuation(c);
                }
                break;

            case IN_GENERIC:
                if (c == '<') {
                    _genericDepth++;
                } else if (c == '>' && --_genericDepth == 0) {
                    _state = State.IN_TYPE;
                }
                // All punctuation inside generic argument lists is suppressed.
                break;

            case IN_ARRAY:
                if (c == '[') {
                    _arrayDepth++;
                } else if (c == ']') {
                    if (_arrayDepth > 0) {
                        _arrayDepth--;
                    } else {
                        _state = State.IN_TYPE;
                    }
                }
                // Suppress all other punctuation inside the array/tuple type.
                break;
        }
    }

    @Override
    public void visitIdentifier(final String name, final int type) {
        switch (_state) {
            case PENDING_COLON:
                if (type == TYPE) {
                    // The ':' and this type name are both part of the annotation — suppress both.
                    _state = State.IN_TYPE;
                } else {
                    // Not a type annotation: flush the buffered ':' and pass this identifier through.
                    super.visitPunctuation(':');
                    _state = State.NORMAL;
                    super.visitIdentifier(name, type);
                }
                break;

            case IN_TYPE:
                if (type == TYPE) {
                    // Continuation of the type expression (e.g. second type in a union) — suppress.
                } else {
                    // First non-TYPE identifier ends the type annotation.
                    _state = State.NORMAL;
                    super.visitIdentifier(name, type);
                }
                break;

            case IN_GENERIC:
            case IN_ARRAY:
                // Inside a generic or array type — suppress all identifier tokens.
                break;

            default: // NORMAL
                super.visitIdentifier(name, type);
                break;
        }
    }

    @Override
    public void visitNumber(final double value) {
        flushAndReset();
        super.visitNumber(value);
    }

    @Override
    public void visitString(final String text, final char type) {
        flushAndReset();
        super.visitString(text, type);
    }

    @Override
    public void visitEnd() {
        flushAndReset();
        super.visitEnd();
    }

    @Override
    public void visitOther(final String s) {
        flushAndReset();
        super.visitOther(s);
    }

    private void flushAndReset() {
        if (_state == State.PENDING_COLON) {
            super.visitPunctuation(':');
        }
        _state = State.NORMAL;
        _genericDepth = 0;
        _arrayDepth = 0;
    }
}
