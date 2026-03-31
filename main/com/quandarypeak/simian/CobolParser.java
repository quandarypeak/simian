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

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

public final class CobolParser implements Parser {
    private final TokenVisitor _visitor;

    public CobolParser(final TokenVisitor visitor) {
        _visitor = Objects.requireNonNull(visitor, "visitor");
    }

    @Override
    public int parse(final Reader reader) throws IOException {
        _visitor.visitFile();
        return parse(new CobolLineReader(reader));
    }

    public int parse(final CobolLineReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            _visitor.visit(reader.getLineNumber());
            _visitor.visitOther(line);
            _visitor.visitEnd();
        }

        return reader.getLineNumber();
    }
}
