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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Command line interface.
 */
public class SimianMain {
    private static final Option FORMATTER = new Option("formatter", Option.PARAMETER_SEP + "TYPE[:FNAME]", "Uses the specified output format when reporting");

    private static final Option INCLUDES = new Option("includes", Option.PARAMETER_SEP + "SPEC", "Including files matching the specified pattern");

    private static final Option EXCLUDES = new Option("excludes", Option.PARAMETER_SEP + "SPEC", "Excludes files matching the specified pattern");

    private static final Option CONFIG = new Option("config", Option.PARAMETER_SEP + "FNAME", "Reads the configuration from the specified file");

    private static final PrintStream LOG = new PrintStream(new IgnoreCloseOutputStream(System.out));

    private final Options options = new Options();
    private final List<IncludesFileFilter> includes = new LinkedList<>();
    private final List<ExcludesFileFilter> excludes = new LinkedList<>();
    private final List<AuditListener> formatters = new LinkedList<>();
    private final String[] args;

    /**
     * Constructor marked private to prevent instantiation.
     */
    public SimianMain(final String[] args) {
        this.args = Objects.requireNonNull(args);
        options.setOption(Option.FAIL_ON_DUPLICATION, true);
    }

    /**
     * Entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(final String[] args) {
        new SimianMain(args).run();
    }

    public void run() {
        LOG.println(Version.BANNER);

        for (final String arg : args) {
            processArg(arg);
        }

        if (includes.isEmpty()) {
            usage();
        }

        if (formatters.isEmpty()) {
            processFormatter(Option.PARAMETER_SEP + FormatterFactory.PLAIN);
        }

        //print the whole commandline
        LOG.println("Input arguments:");
        LOG.println(String.join(" ",args));

        final Checker checker = new Checker(formatters.size() == 1 ? formatters.get(0) : new CompositeAuditListener(formatters), options);

        final CompositeFileFilter excludes = new CompositeFileFilter(this.excludes);

        final FileLoader fileLoader = new FileLoader(new StreamLoader(checker));
        for (final IncludesFileFilter include : includes) {
            final FilterFileLoader loader = new FilterFileLoader(include.getBaseDirectory(), new FileSetFileFilter(include, excludes), fileLoader);
            loader.load();
        }

        exit(checker.check());
    }

    private void processArg(final String arg) {
        Objects.requireNonNull(arg);

        if (arg.startsWith("-")) {
            processOption(arg.substring(1));
        } else {
            processImplicitIncludes(arg);
        }
    }

    private void processOption(final String arg) {
        if (arg.startsWith(INCLUDES.getName())) {
            processIncludes(arg.substring(INCLUDES.getName().length()));
        } else if (arg.startsWith(EXCLUDES.getName())) {
            processExcludes(arg.substring(EXCLUDES.getName().length()));
        } else if (arg.startsWith(Option.LANGUAGE.getName())) {
            processLanguage(Option.LANGUAGE, arg.substring(Option.LANGUAGE.getName().length()));
        } else if (arg.startsWith(Option.DEFAULT_LANGUAGE.getName())) {
            processLanguage(Option.DEFAULT_LANGUAGE, arg.substring(Option.DEFAULT_LANGUAGE.getName().length()));
        } else if (arg.startsWith(Option.THRESHOLD.getName())) {
            processThreshold(arg.substring(Option.THRESHOLD.getName().length()));
        } else if (arg.startsWith(FORMATTER.getName())) {
            processFormatter(arg.substring(FORMATTER.getName().length()));
        } else if (arg.startsWith(CONFIG.getName())) {
            processConfig(arg.substring(CONFIG.getName().length()));
        } else if (arg.startsWith(Option.IGNORE_BLOCKS.getName())) {
            processIgnoreBlocks(arg.substring(Option.IGNORE_BLOCKS.getName().length()));
        } else if (arg.startsWith("balance") || arg.startsWith("ignore") || arg.startsWith("report") || arg.startsWith(Option.FAIL_ON_DUPLICATION.getName())) {
            processBoolean(arg);
        } else {
            usage("Invalid option : '" + arg + "'");
        }
    }

    private void processIgnoreBlocks(final String arg) {
        if (!arg.startsWith(Option.PARAMETER_SEP)) {
            usage("Missing block markers");
        }

        BlockMarkers markers = null;

        try {
            markers = BlockMarkers.valueOf(arg.substring(1));
        } catch (final IllegalStateException e) {
            usage(e.getMessage());
        }

        options.setOption(Option.IGNORE_BLOCKS, markers);
    }

    private void processBoolean(final String arg) {
        if (arg.endsWith("-")) {
            processBoolean(arg.substring(0, arg.length() - 1), false);
        } else if (arg.endsWith("+")) {
            processBoolean(arg.substring(0, arg.length() - 1), true);
        } else {
            processBoolean(arg, true);
        }
    }

    private void processBoolean(final String arg, final boolean enable) {
        if (!Option.isValidOption(arg)) {
            usage("Invalid option : '" + arg + "'");
        }

        options.setOption(Option.valueOf(arg), enable);
    }

    private void processConfig(final String arg) {
        if (!arg.startsWith(Option.PARAMETER_SEP)) {
            usage("Missing config filename");
        }

        final String fname = arg.substring(1).trim();
        if (fname.isEmpty()) {
            usage("Missing config filename");
        }

        try {
            processConfigFile(fname);
        } catch (final IOException e) {
            exitWithErrorMessage(e.getMessage());
        }
    }

    private void processConfigFile(final String filename) throws IOException {

        try (final BufferedReader reader = new BufferedReader(new UnicodeBOMAwareReader(filename))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    processArg(line);
                }
            }
        }
    }

    private void processFormatter(final String arg) {
        if (!arg.startsWith(Option.PARAMETER_SEP)) {
            usage("Missing formatter type");
        }

        String type = arg.substring(1).trim();

        String fname = "";
        final int i = type.indexOf(':');
        if (i != -1) {
            fname = type.substring(i + 1).trim();
            type = type.substring(0, i).trim();
        }

        if (!FormatterFactory.isValidType(type)) {
            usage("Missing/invalid formatter type");
        }

        OutputStream out = LOG;
        if (!fname.isEmpty()) {
            try {
                out = new BufferedOutputStream(new FileOutputStream(fname));
            } catch (final FileNotFoundException e) {
                exitWithErrorMessage(e.getMessage());
            }
        }

        formatters.add(FormatterFactory.createFormatter(type, out, out != LOG));
    }

    private void processThreshold(final String arg) {
        if (!arg.startsWith(Option.PARAMETER_SEP)) {
            usage("Missing threshold");
        }

        int threshold = 0;

        try {
            threshold = Integer.parseInt(arg.substring(1).trim());
        } catch (final NumberFormatException e) {
            usage("Missing/invalid threshold");
        }

        if (threshold < Options.MINIMUM_THRESHOLD) {
            usage("Threshold can't be less that " + Options.MINIMUM_THRESHOLD);
        }

        options.setThreshold(threshold);
    }

    private void processLanguage(final Option option, final String arg) {
        if (!arg.startsWith(Option.PARAMETER_SEP)) {
            usage("Missing language");
        }

        final String language = arg.substring(1).trim();
        if (language.isEmpty()) {
            usage("Missing language");
        }

        if (!Language.isValidLanguage(language)) {
            usage("Invalid language : '" + language + "'");
        }

        options.setOption(option, Language.valueOf(language));
    }

    private void processImplicitIncludes(final String arg) {
        includes.add(new IncludesFileFilter(processGlob(arg)));
    }

    private void processIncludes(final String arg) {
        includes.add(new IncludesFileFilter(processXGlob(arg)));
    }

    private void processExcludes(final String arg) {
        excludes.add(new ExcludesFileFilter(processXGlob(arg)));
    }

    private Glob processXGlob(final String arg) {
        if (!arg.startsWith(Option.PARAMETER_SEP)) {
            usage("Missing filespec");
        }
        return processGlob(arg.substring(Option.PARAMETER_SEP.length()));
    }

    private Glob processGlob(final String arg) {
        if (arg.trim().isEmpty()) {
            usage("Missing filespec");
        }
        return new Glob(new File(arg).getAbsolutePath());
    }

    private void usage(final String message) {
        LOG.println("Error: " + message);
        usage();
    }

    private void usage() {
        final StringBuilder message = new StringBuilder();

        message.append("Usage: [options] [files]");
        message.append(System.lineSeparator());

        for (final Option option : Option.values()) {
            final String name = option.getName();
            final String parameters = option.getParameters();

            message.append("    -").append(name).append(parameters);

            for (int j = 32 - (name.length() + parameters.length()); j > 0; --j) {
                message.append(' ');
            }

            message.append(option.getDescription()).append(System.lineSeparator());
        }

        exitWithMessage(message.toString());
    }

    private void exitWithErrorMessage(final String message) {
        exitWithMessage("Error: " + message);
    }

    private void exitWithMessage(final String message) {
        LOG.println(message);
        exit(-1);
    }

    private void exit(final boolean success) {
        exit(success ? 0 : 1);
    }

    private void exit(final int status) {
        System.exit(status);
    }
}
