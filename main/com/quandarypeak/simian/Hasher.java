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
 * Utility class for generating hashcodes.
 */
final class Hasher {
    /**
     * The magic multiplier. Notice that it's prime? This is VERY IMPORTANT!
     */
    private static final int MAGIC_NUMBER = 31;

    private Hasher() {
        throw new UnsupportedOperationException("Constructor should not be called");
    }

    public static long hash(final long hash1, final long hash2) {
        return MAGIC_NUMBER * hash1 + hash2;
    }
}
