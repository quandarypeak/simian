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

interface TokenVisitor {
    int UNKNOWN = 0;

    int METHOD = 1;

    int VARIABLE = 2;

    int CONSTANT = 3;

    int TYPE = 4;

    int KEYWORD = 5;

    void visitFile();

    void visit(int lineNumber);

    void visitNumber(double value);

    void visitIdentifier(String name, int type);

    void visitString(String text, char type);

    void visitComment(String text);

    void visitPunctuation(char c);

    void visitOther(String s);

    void visitEnd();
}
