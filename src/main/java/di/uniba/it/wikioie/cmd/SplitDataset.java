package di.uniba.it.wikioie.cmd;

import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SplitDataset class split a starting dataset in training and test set, respecting given sampling and relevant
 * triples percentage.
 */
public class SplitDataset {

    private final Set<String> relevant;
    private final Set<String> notRelevant;
    private double relevantPercentage = 0.0;
    private String outputPath = "";
    private int triplesCount = 0;
    private static final Logger LOG = Logger.getLogger(CreateDataset.class.getName());

    public SplitDataset(double percentage, String path) {
        relevant = new HashSet<String>(10000);
        notRelevant = new HashSet<String>(10000);
        relevantPercentage = percentage;
        outputPath = path;
    }

    /**
     * Splits evaluated triples in sets of relevant and non-relevant triples.
     * HashSet structure assures the absence of duplicate triples.
     * @param triples .tsv file containing evaluated triples.
     * @throws IOException
     */
    private void splitTriples(File triples) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(triples));
        String r = "1";
        String nr = "0";
        String line;
        while(reader.ready()) {
            line = reader.readLine();
            if(line.endsWith(r)) {
                relevant.add(line);
            } else if(line.endsWith(nr)) {
                notRelevant.add(line);
            }
        }
        reader.close();
        triplesCount = relevant.size() + notRelevant.size();
        LOG.log(Level.INFO, "Starting with " + triplesCount + " triples");
        LOG.log(Level.INFO, "Starting with " + relevant.size() + " relevant triples");
        LOG.log(Level.INFO, "Starting with " + notRelevant.size() + " non-relevant triples");
        removeDuplicates(triples);
    }

    /**
     * Removes possible duplicate triples from the initial dataset.
     * @param triples
     * @throws IOException
     */
    private void removeDuplicates(File triples) throws IOException {
        File triples_dd = new File(triples.getAbsolutePath() + "/l_triples_dd.tsv");
        FileWriter writer = new FileWriter(triples_dd);
        BufferedWriter buff = new BufferedWriter(writer);
        CSVPrinter csv = CSVFormat.TDF.withHeader("title", "text", "subject", "predicate", "object", "ann.").print(buff);
        for(String line: relevant) {
            buff.write(line);
            buff.newLine();
        }
        for(String line: notRelevant) {
            buff.write(line);
            buff.newLine();
        }
        csv.close();
        buff.close();
        writer.close();
    }
    /**
     * Creates a training set with given sampling percentage. Every taken triple is removed from its set.
     * @param sampling
     * @throws IOException
     */
    private void createTrainingSet(double sampling) throws IOException {
        double trSize = triplesCount * sampling; //size of entire training set
        int rSize = (int) (trSize * relevantPercentage); //number of relevant triples in training set
        int nrSize = (int) trSize - rSize; //number of non-relevant triples in training set
        LOG.log(Level.INFO, "TRAINING SET has " + rSize + " relevant triples and " + nrSize + " non-relevant triples");

        File training = new File(outputPath + "/training");
        if(training.mkdirs()) {
            File r = new File(training.getAbsolutePath() + "/training_set.tsv");
            FileWriter writer = new FileWriter(r);
            BufferedWriter buff = new BufferedWriter(writer);
            CSVPrinter csv = CSVFormat.TDF.withHeader("title", "text", "subject", "predicate", "object", "ann.").print(buff);

            Iterator<String> rIter = relevant.iterator();
            for(int i=0; i<rSize; i++) {
                if(!relevant.isEmpty()) {
                    String line = rIter.next().toString();
                    buff.write(line);
                    buff.newLine();
                    rIter.remove();
                } else {
                    LOG.log(Level.WARNING, "Empty relevant set");
                }
            }

            Iterator<String> nrIter = notRelevant.iterator();
            for(int i=0; i<nrSize; i++) {
                if(!notRelevant.isEmpty()) {
                    String line = nrIter.next().toString();
                    buff.write(line);
                    buff.newLine();
                    nrIter.remove();
                } else {
                    LOG.log(Level.WARNING, "Empty not relevant set");
                }
            }
            csv.close();
            buff.close();
            writer.close();
            LOG.log(Level.INFO, relevant.size() + " relevant triples remaining");
            LOG.log(Level.INFO, notRelevant.size() + " non-relevant triples remaining");
        }
    }

    /**
     * Creates a test set with given sampling percentage. Every taken triple is removed from its set.
     * @param sampling
     * @throws IOException
     */
    private void createTestSet(double sampling) throws IOException {
        double teSize = triplesCount * sampling; //size of entire test set
        int rSize = (int) (teSize * relevantPercentage); //number of relevant triples in test set
        int nrSize = (int) teSize - rSize; //number of non-relevant triples in test set
        LOG.log(Level.INFO, "TEST SET has " + rSize + " relevant triples and " + nrSize + " non-relevant triples");

        File test = new File(outputPath + "/test");
        if(test.mkdirs()) {
            File r = new File(test.getAbsolutePath() + "/test_set.tsv");
            FileWriter writer = new FileWriter(r);
            BufferedWriter buff = new BufferedWriter(writer);
            CSVPrinter csv = CSVFormat.TDF.withHeader("title", "text", "subject", "predicate", "object", "ann.").print(buff);

            Iterator<String> rIter = relevant.iterator();
            for(int i=0; i<rSize; i++) {
                if(rIter.hasNext()) {
                    String line = rIter.next().toString();
                    buff.write(line);
                    buff.newLine();
                    rIter.remove();
                } else {
                    LOG.log(Level.WARNING, "Empty relevant set");
                }
            }

            Iterator<String> nrIter = notRelevant.iterator();
            for(int i=0; i<nrSize; i++) {
                if (nrIter.hasNext()) {
                    String line = nrIter.next().toString();
                    buff.write(line);
                    buff.newLine();
                    nrIter.remove();
                } else {
                    LOG.log(Level.WARNING, "Empty not relevant set");
                }
            }
            csv.close();
            buff.close();
            writer.close();
            LOG.log(Level.INFO, relevant.size() + " relevant triples remaining");
            LOG.log(Level.INFO, notRelevant.size() + " non-relevant triples remaining");
        }

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
                .addOption(new Option("r", true, "Relevant triples percentage"));
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("i") && cmd.hasOption("o")) {
                LOG.log(Level.INFO, "Input file: {0}", cmd.getOptionValue("i"));
                LOG.log(Level.INFO, "Output dir: {0}", cmd.getOptionValue("o"));
                File input = new File(cmd.getOptionValue("i"));
                String outputPath = cmd.getOptionValue("o");
                double trainingSampling = Double.parseDouble(cmd.getOptionValue("a"));
                double testSampling = Double.parseDouble(cmd.getOptionValue("e"));
                double relevantPercentage = Double.parseDouble(cmd.getOptionValue("r"));

                SplitDataset splitter = new SplitDataset(relevantPercentage, outputPath);
                LOG.log(Level.INFO, "Splitting dataset...");
                splitter.splitTriples(input);
                LOG.log(Level.INFO, "Creating training set...");
                splitter.createTrainingSet(trainingSampling);
                LOG.log(Level.INFO, "Creating test set...");
                splitter.createTestSet(testSampling);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

}
