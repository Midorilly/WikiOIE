package di.uniba.it.wikioie.cmd;

import di.uniba.it.wikioie.Utils;
import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SplitDataset {

    private static final Logger LOG = Logger.getLogger(CreateDataset.class.getName());

    private void splitTriples(File input, String output) throws IOException {
        File triples = input;
        BufferedReader reader = new BufferedReader(new FileReader(triples));
        File relevant = new File(output + "/l_relevant.tsv");
        File notRelevant = new File(output + "/l_not_relevant.tsv");
        FileWriter writerR = new FileWriter(relevant);
        FileWriter writerNR = new FileWriter(notRelevant);
        BufferedWriter bufferedR = new BufferedWriter(writerR);
        BufferedWriter bufferedNR = new BufferedWriter(writerNR);
        CSVPrinter csvRelevant = CSVFormat.TDF.withHeader("title", "text", "subject", "predicate", "object", "relevance").print(bufferedR);
        CSVPrinter csvNotRelevant = CSVFormat.TDF.withHeader("title", "text", "subject", "predicate", "object", "relevance").print(bufferedNR);

        String r = "1";
        String nr = "0";
        while(reader.ready()) {
            String line = reader.readLine();
            if(line.endsWith(r)) {
                bufferedR.write(line);
                bufferedR.newLine();
            } else if(line.endsWith(nr)) {
                bufferedNR.write(line);
                bufferedNR.newLine();
            }
        }
        csvRelevant.close();
        csvNotRelevant.close();
        reader.close();
        bufferedR.close();
        bufferedNR.close();
        writerNR.close();
        writerR.close();
    }

    private void createTrainingSet(long linesCount, double sampling, double percentage, String inputFile, String outputFile) throws IOException {
        double v = linesCount*sampling;
        double p = v/100.0;
        double x = p*percentage;
        int f = (int) (x/100.0);
        String size = String.valueOf(f);
        List<String> arguments = new ArrayList<>();
        arguments.add(inputFile);
        arguments.add(outputFile);
        arguments.add(size);
        ShuffleCatDataset.main(arguments.toArray(new String[arguments.size()]));
    }

    private void createTestSet(String duplicateFile, String inputFile, String outputFile) throws IOException {
        Utils.removeDuplicate(new File(duplicateFile), new File(inputFile), new File(outputFile));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Options options = new Options();
        options = options.addOption(new Option("i", true, "Input file"))
                .addOption(new Option("o", true, "Output directory"))
                .addOption(new Option("a", true, "Training sampling"))
                .addOption(new Option("e", true, "Test sampling"))
                .addOption(new Option("r", true, "Relevant triples percentage"))
                .addOption(new Option("n", true, "Not relevant triples percentage"));
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("i") && cmd.hasOption("o")) {
                LOG.log(Level.INFO, "Input file: {0}", cmd.getOptionValue("i"));
                LOG.log(Level.INFO, "Output dir: {0}", cmd.getOptionValue("o"));
                File input = new File(cmd.getOptionValue("i"));
                BufferedReader inputReader = new BufferedReader(new FileReader(input));
                Stream<String> lines = inputReader.lines();
                long linesCount = lines.count() - 1;
                inputReader.close();
                String output = cmd.getOptionValue("o");
                SplitDataset splitter = new SplitDataset();
                LOG.log(Level.INFO, "Splitting dataset...");
                splitter.splitTriples(input, output);
                double trainingSampling = Double.parseDouble(cmd.getOptionValue("a"));
                double testSampling = Double.parseDouble(cmd.getOptionValue("e"));
                File training = new File(output + "/training");
                training.mkdirs();
                File test = new File(output + "/test");
                test.mkdirs();
                double relevantPercentage = Double.parseDouble(cmd.getOptionValue("r"));
                double notRelevantPercentage = Double.parseDouble(cmd.getOptionValue("n"));

                //CREATE TRAINING SET
                LOG.log(Level.INFO, "Creating training set...");
                String inputFile = output + "/l_relevant.tsv";
                String outputFile = training.getAbsolutePath() + "/tr_relevant.tsv";
                splitter.createTrainingSet(linesCount, trainingSampling, relevantPercentage, inputFile, outputFile);

                inputFile = output + "/l_not_relevant.tsv";
                outputFile = training.getAbsolutePath() + "/tr_not_relevant.tsv";
                splitter.createTrainingSet(linesCount, trainingSampling, notRelevantPercentage, inputFile, outputFile);

                //CREATE TEST SET FROM L REMOVING TRAINING DUPLICATES
                LOG.log(Level.INFO, "Creating test set...");
                String duplicateFile = training.getAbsolutePath() + "/tr_relevant.tsv";
                inputFile = output + "/l_relevant.tsv";
                outputFile = test.getAbsolutePath() + "/te_relevant.tsv";
                splitter.createTestSet(duplicateFile, inputFile, outputFile);

                duplicateFile = training.getAbsolutePath() + "/tr_not_relevant.tsv";
                inputFile = output + "/l_not_relevant.tsv";
                outputFile = test.getAbsolutePath() + "/te_not_relevant.tsv";
                splitter.createTestSet(duplicateFile, inputFile, outputFile);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

}
