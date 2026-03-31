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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class VbParserFactory extends AbstractCFamilyParserFactory {
    private static final String BEGIN_REGION = "#Region";
    private static final String END_REGION = "#End";
    private static final BlockMarkers REGION_MARKERS = new BlockMarkers(BEGIN_REGION, END_REGION);

    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("Boolean", "Byte", "Char", "Date", "Decimal", "Double", "Integer", "Long", "Object", "Short", "Single", "String"));

    /**
     * These modifiers may be optionally ignored
     */
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("Ansi", "Alias", "Auto", "ByRef", "ByVal", "Class", "Const", "Delegate", "Default", "Dim", "Enum", "Event", "Friend", "Function", "Handles", "Inherits", "Interface", "Implements", "Let", "Lib", "Module", "MustInherit", "MustOverride", "NotInheritable", "NotOverridable", "Optional", "Overloads", "Overridable", "Overrides", "ParamArray", "Preserve", "Private", "Property", "Protected", "Public", "ReadOnly", "ReDim", "Shadows", "Shared", "Static", "Structure", "Sub", "SyncLock", "Unicode", "Variant", "WithEvents", "WriteOnly"));

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList("AddHandler", "AddressOf", "And", "AndAlso", "As", "Call", "Case", "Catch", "DirectCast", "Do", "Each", "Else", "ElseIf", "Erase", "Error", "Exit", "Finally", "For", "Get", "GetType", "GoSub", "GoTo", "If", "In", "Is", "Like", "Loop", "Mod", "New", "Next", "Not", "Or", "OrElse", "RaiseEvent", "RemoveHandler", "Resume", "Return", "Select", "Set", "Step", "Stop", "Throw", "To", "Try", "TypeOf", "Until", "While", "When", "With", "Xor"));

    private static final Set<String> IGNORE_WORDS = new HashSet<>(Arrays.asList("End", "Then"));

    /**
     * Any of these will cause the line on which it appears to be ignored
     */
    private static final Set<String> IGNORE_LINES_TRIGGERS_BASE = new HashSet<>(Arrays.asList("Assembly", "Declare", "Imports", "Namespace", "On", "Option", "REM"));

    /**
     * Any of these will cause the line on which it appears to be ignored
     */
    private static final Set<String> IGNORE_LINES_TRIGGERS_ALL = new HashSet<>();

    static {
        IGNORE_LINES_TRIGGERS_ALL.addAll(IGNORE_LINES_TRIGGERS_BASE);
        IGNORE_LINES_TRIGGERS_ALL.add(BEGIN_REGION);
        IGNORE_LINES_TRIGGERS_ALL.add(END_REGION);

        KEYWORDS.addAll(MODIFIERS);
        KEYWORDS.addAll(IGNORE_WORDS);
        KEYWORDS.addAll(IGNORE_LINES_TRIGGERS_ALL);
    }

    @Override
    public Parser createParser(final LineListener listener, final Options options) {

        Objects.requireNonNull(options, "options");

        LineListener l = listener;
        Set<String> t = IGNORE_LINES_TRIGGERS_ALL;

        if (options.hasOption(Option.IGNORE_REGIONS)) {
            l = new IgnoreBlocksLineListener(listener, REGION_MARKERS);
            t = IGNORE_LINES_TRIGGERS_BASE;
        }

        TokenVisitor visitor = createBaseLanguageTokenVisitor(l, options, MODIFIERS, t);

        visitor = new IgnoreWordsTokenVisitor(IGNORE_WORDS, visitor);

        visitor = new RecogniseIdentifiersTokenVisitor(visitor);

        visitor = new IgnoreCommentsTokenVisitor(visitor);

        return new CFamilyParser(visitor);
    }

    private static final class IgnoreCommentsTokenVisitor extends DecoratorTokenVisitor {
        public IgnoreCommentsTokenVisitor(final TokenVisitor decorated) {
            super(decorated);
        }

        @Override
        public void visitString(final String text, final char type) {
            if (type == '"') {
                super.visitString(text, '"');
            }
        }
    }

    private static final class RecogniseIdentifiersTokenVisitor extends DecoratorTokenVisitor {
        public RecogniseIdentifiersTokenVisitor(final TokenVisitor decorated) {
            super(decorated);
        }

        @Override
        public void visitIdentifier(final String name, final int type) {
            Objects.requireNonNull(name, "name");

            if (TYPES.contains(name)) {
                super.visitIdentifier(name, TYPE);
            } else if (KEYWORDS.contains(name)) {
                super.visitIdentifier(name, KEYWORD);
            } else {
                super.visitIdentifier(name, VARIABLE);
            }
        }
    }
}
