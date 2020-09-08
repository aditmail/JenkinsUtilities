package com.jenkins.util.test;

import org.apache.commons.cli.*;

import java.util.Random;

public class RunningTest {

    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String RANDOM_CLASS = "c";
    private static final String SEED = "s";
    private static final String RESULTS = "n";

    //for TestNG -> using -ea -Dproperty="asdf" (enabled assertion)
    public static void main(String[] args) {

        //Using -Dproperties --->
        String a = System.getProperty("value");
        String aProp = System.getProperty("value.prop");
        String dFileEncode = System.getProperty("file.encoding");

        System.out.println("Encode:: " + dFileEncode);
        System.out.println("Hello Running Test with value:: " + a + "and Prop:: " + aProp);

        /* Using Args --->
        If using Options.. maybe its not good if using arguments too
        Since the Options is reading from Arguments and parse it to parser
        */
        //String firstArg = args[0];
        //System.out.println("and First Args value:: " + firstArg);

        //Using Options
        CommandLineParser parser = new DefaultParser();
        Options options = prepareOptions();

        try {
            CommandLine cmdLine = parser.parse(prepareOptions(), args);

            //** Getting required args **
            int min = ((Number) cmdLine.getParsedOptionValue(MIN)).intValue();
            int max = ((Number) cmdLine.getParsedOptionValue(MAX)).intValue();

            //** Getting optional args **
            Random random = ((Random) cmdLine.getParsedOptionValue(RANDOM_CLASS));
            if (random == null) {
                random = new Random();
            }

            if (cmdLine.hasOption(SEED)) {
                long seed = ((Number) cmdLine.getParsedOptionValue(SEED)).longValue();
                random.setSeed(seed);
            }

            long numbers = 1;
            if (cmdLine.hasOption(RESULTS)) {
                numbers = ((Number) cmdLine.getParsedOptionValue(RESULTS)).longValue();
            }

            for (int i = 0; i < numbers; i++) {
                System.out.print("Value is:: " + Math.round(random.nextInt(max)) + min);
                if (i != numbers - 1) {
                    System.out.println();
                }
            }
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            new HelpFormatter().printHelp("cli-random", options);
        }

        //Using Options Model 1
        /*CommandLine cmdLine;
        Option option_T = Option.builder("t")
                .required(true)
                .desc("Target Name")
                .longOpt("target")
                .build();

        Option option_Opt = Option.builder("opt")
                .required(false)
                .desc("Optionals Input")
                .longOpt("optional")
                .build();

        Options options = new Options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        String[] testArgs = {"-t", "target", "-opt", "optional"};
        options.addOption(option_Opt);
        options.addOption(option_T);

        try {
            cmdLine = parser.parse(options, testArgs);
            if (cmdLine.hasOption("t")) {
                System.out.println("option Target:: " + cmdLine.getOptionValue("t"));
            }

            if (cmdLine.hasOption("opt")) {
                System.out.println("option Optionals:: " + cmdLine.getOptionValue("opt"));
            }

        } catch (ParseException exception) {
            System.out.println(exception.getMessage());
            helpFormatter.printHelp("utility-name", options);
        }*/

        //Using Options Model 2
        /*Options options = new Options();
        Option input = new Option("t", "target", true, "Target name");
        input.setRequired(true);
        options.addOption(input);

        Option inputOpt = new Option("opt", "optional", true, "Optionals..");
        inputOpt.setRequired(false);
        options.addOption(inputOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helpFormatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String target = cmd.getOptionValue("target");
        String optional = cmd.getOptionValue("optional");
        System.out.println("and Using Options:: target-> " + target);
        System.out.println("and Using Options:: optional-> " + optional);*/
    }

    private static Options prepareOptions() {
        Options options = new Options();
        options.addOption(getMinOpt())
                .addOption(getMaxOpt())
                .addOption(getRandomClassOpt())
                .addOption(getSeedOpt())
                .addOption(getNumberOfResultsOpt());

        return options;
    }

    private static Option getMinOpt() {
        return Option.builder().required().desc("minimum num (inclusive)")
                .longOpt(MIN)
                .type(Number.class)
                .hasArg()
                .build();
    }

    private static Option getMaxOpt() {
        return Option.builder().required().desc("maximum num (inclusive)")
                .longOpt(MAX)
                .type(Number.class)
                .hasArg()
                .build();
    }

    private static Option getRandomClassOpt() {
        return Option.builder(RANDOM_CLASS).desc("class extending Random, that will provide " +
                "random numbers,\nfor example: java.security.SecureRandom")
                .longOpt("class")
                .type(Object.class)
                .hasArg()
                .build();
    }

    private static Option getSeedOpt() {
        return Option.builder(RESULTS).desc("Number of Results, 1 by Default")
                .longOpt("numbers")
                .type(Object.class)
                .hasArg()
                .build();
    }

    private static Option getNumberOfResultsOpt() {
        return Option.builder(SEED).desc("seed, by default determined by " +
                "Random implementation")
                .longOpt("numbers")
                .type(Object.class)
                .hasArg()
                .build();
    }


}
