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
