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

/**
 * Tests for SimianMain's CLI argument parsing, specifically the failOnDuplication option.
 *
 * <p>Only the happy paths are covered here. Every error path in SimianMain's parsing
 * (invalid threshold, invalid language, invalid failOnDuplication value, etc.) calls
 * {@code usage(...)}, which terminates the JVM via {@code System.exit(-1)} - that's true
 * for this class generally and isn't something these tests attempt to work around.
 * Those paths are verified manually by running the built jar (see the issue #23 fix notes).
 */
public class SimianMainTest {

    private static SimianMain newMain() {
        return new SimianMain(new String[0]);
    }

    @Test
    public void failOnDuplicationPercentageIsParsedAsInteger() {
        final SimianMain main = newMain();
        main.processArg("-failOnDuplication=60%");
        assertEquals(Integer.valueOf(60), main.getOptions().getOption(Option.FAIL_ON_DUPLICATION));
    }

    @Test
    public void failOnDuplicationPercentageAcceptsLowerBoundary() {
        final SimianMain main = newMain();
        main.processArg("-failOnDuplication=0%");
        assertEquals(Integer.valueOf(0), main.getOptions().getOption(Option.FAIL_ON_DUPLICATION));
    }

    @Test
    public void failOnDuplicationPercentageAcceptsUpperBoundary() {
        final SimianMain main = newMain();
        main.processArg("-failOnDuplication=100%");
        assertEquals(Integer.valueOf(100), main.getOptions().getOption(Option.FAIL_ON_DUPLICATION));
    }

    @Test
    public void failOnDuplicationPlusStillSetsBooleanTrue() {
        final SimianMain main = newMain();
        main.processArg("-failOnDuplication+");
        assertEquals(Boolean.TRUE, main.getOptions().getOption(Option.FAIL_ON_DUPLICATION));
    }

    @Test
    public void failOnDuplicationMinusStillClearsTheOption() {
        // Options.setOption treats Boolean.FALSE as "clear the option" (see
        // Options.setSingleValuedOption) - hasOption() is the real contract here, not
        // getOption() == false. '-failOnDuplication-' must still reach that same behavior.
        final SimianMain main = newMain();
        main.processArg("-failOnDuplication-");
        assertEquals(false, main.getOptions().hasOption(Option.FAIL_ON_DUPLICATION));
    }
}
