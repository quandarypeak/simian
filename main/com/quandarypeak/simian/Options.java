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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Encapsulates all checking options.
 */
public final class Options {
    /**
     * The smallest legal value for threshold.
     */
    public static final int MINIMUM_THRESHOLD = 1;

    /**
     * The default minimum number of lines that are considered significant.
     */
    public static final int DEFAULT_THRESHOLD = 6;

    private final Map<Option, Object> options = new TreeMap<>();

    /**
     * Default constructor.
     */
    public Options() {
        setThreshold(DEFAULT_THRESHOLD);
        setOption(Option.IGNORE_MODIFIERS, true);
        setOption(Option.IGNORE_CURLY_BRACES, true);
        setOption(Option.IGNORE_CHARACTER_CASE, true);
        setOption(Option.IGNORE_STRING_CASE, true);
        setOption(Option.IGNORE_IDENTIFIER_CASE, true);
    }

    /**
     * Clears the options.
     */
    public void clear() {
        options.clear();
        setThreshold(DEFAULT_THRESHOLD);
    }

    /**
     * Obtain the value of the {@link Option#THRESHOLD} option.
     *
     * @return The minimum number of lines considered a match.
     */
    public int getThreshold() {
        return getOption(Option.THRESHOLD);
    }

    /**
     * Sets the value of the {@link Option#THRESHOLD} option.
     *
     * @param threshold The minimum number of lines considered a match.
     */
    public void setThreshold(final int threshold) {
        if (threshold < MINIMUM_THRESHOLD) {
            throw new IllegalArgumentException(String.format("threshold can't be less than %d", MINIMUM_THRESHOLD));
        }
        setOption(Option.THRESHOLD, threshold);
    }

    /**
     * Obtains the value of a specified option.
     *
     * @param option The option to get.
     * @return value The value; or {@code null} if the option should be cleared.
     */
    public <T> T getOption(final Option option) {
        Objects.requireNonNull(option, "option can't be null");
        return (T) options.get(option);
    }

    /**
     * Sets the value of a specified option.
     *
     * @param option The option to set.
     * @param value  The value; or {@code null} if the option should be cleared.
     */
    public void setOption(final Option option, final Object value) {
        if (option.isMultiValued()) {
            setMultiValuedOption(option, value);
        } else {
            setSingleValuedOption(option, value);
        }
    }

    private void setSingleValuedOption(final Option option, final Object value) {
        if (Objects.isNull(value) || Objects.equals(value, Boolean.FALSE)) {
            options.remove(option);
        } else {
            options.put(option, value);
        }
    }

    private void setMultiValuedOption(final Option option, final Object value) {
        if (Objects.isNull(value)) {
            options.remove(option);
        } else {
            final Set<Object> values = (Set<Object>) options.computeIfAbsent(option, o -> new HashSet<>());
            values.add(value);
        }
    }

    /**
     * Determines if an option has been set or enabled.
     *
     * @param option The option to check for.
     * @return <code>true</code> if the option has been set or enabled; otherwise <code>false</code>.
     */
    public boolean hasOption(final Option option) {
        Objects.requireNonNull(option, "option");
        return options.containsKey(option);
    }

    /**
     * Obtains the value of all options that have been set/enabled.
     *
     * @return Values keyed by option.
     */
    public Map<Option, Object> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    public int hashCode() {
        return options.hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        final Options other = (Options) object;
        return options.equals(other.options);
    }

    public String toString() {
        return options.toString();
    }
}
