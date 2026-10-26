/*
 * Copyright 2022-2026 Quandary Peak Research, Inc.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LanguageTest {

    // -------------------------------------------------------------------------
    // JavaScript extensions: js, javascript, jsx, mjs, cjs all map to JAVA_SCRIPT.
    // jsx is JavaScript with embedded JSX markup; mjs/cjs are plain JavaScript explicitly
    // marked ES module / CommonJS module - all lexically identical, same parser.
    // -------------------------------------------------------------------------

    @Test
    public void jsxExtensionResolvesToJavaScript() {
        assertTrue(Language.isValidLanguage("jsx"));
        assertEquals(Language.JAVA_SCRIPT, Language.valueOf("jsx"));
    }

    @Test
    public void mjsExtensionResolvesToJavaScript() {
        assertTrue(Language.isValidLanguage("mjs"));
        assertEquals(Language.JAVA_SCRIPT, Language.valueOf("mjs"));
    }

    @Test
    public void cjsExtensionResolvesToJavaScript() {
        assertTrue(Language.isValidLanguage("cjs"));
        assertEquals(Language.JAVA_SCRIPT, Language.valueOf("cjs"));
    }

    // -------------------------------------------------------------------------
    // TypeScript extensions: ts, typescript, tsx, mts, cts all map to TYPESCRIPT.
    // tsx is TypeScript with embedded JSX markup; mts/cts are plain TypeScript explicitly
    // marked ES module / CommonJS module - all lexically identical, same parser.
    // -------------------------------------------------------------------------

    @Test
    public void tsxExtensionResolvesToTypeScript() {
        assertTrue(Language.isValidLanguage("tsx"));
        assertEquals(Language.TYPESCRIPT, Language.valueOf("tsx"));
    }

    @Test
    public void mtsExtensionResolvesToTypeScript() {
        assertTrue(Language.isValidLanguage("mts"));
        assertEquals(Language.TYPESCRIPT, Language.valueOf("mts"));
    }

    @Test
    public void ctsExtensionResolvesToTypeScript() {
        assertTrue(Language.isValidLanguage("cts"));
        assertEquals(Language.TYPESCRIPT, Language.valueOf("cts"));
    }

    @Test
    public void extensionLookupIsCaseInsensitive() {
        assertEquals(Language.TYPESCRIPT, Language.valueOf("TSX"));
        assertEquals(Language.JAVA_SCRIPT, Language.valueOf("JSX"));
    }
}
