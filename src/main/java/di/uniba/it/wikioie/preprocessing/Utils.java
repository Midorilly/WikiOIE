package di.uniba.it.wikioie.preprocessing;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    private static final Logger LOG = Logger.getLogger(Preprocess.class.getName());

    public static class ExtractCallback implements IArchiveExtractCallback {
        private int hash = 0;
        private int size = 0;
        private int index;
        private String outputPath;
        private String fileName;
        private boolean skipExtraction;
        private IInArchive inArchive;

        public ExtractCallback(IInArchive inArchive, String outputPath, String fileName) {
            this.inArchive = inArchive;
            this.outputPath = outputPath;
            this.fileName = fileName;
        }

        public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
            this.index = index;
            skipExtraction = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
            if (skipExtraction || extractAskMode != ExtractAskMode.EXTRACT) {
                return null;
            }
            return new ISequentialOutStream() {
                public int write(byte[] data) throws SevenZipException {
                    try {
                        FileOutputStream file = new FileOutputStream(new File(outputPath, fileName));
                        file.write(data);
                        file.close();
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, e.getMessage());
                    }

                    hash ^= Arrays.hashCode(data);
                    size += data.length;
                    return data.length; // Return amount of proceed data
                }
            };
        }

        public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException { }

        public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
            if (skipExtraction) {
                return;
            }
            if (extractOperationResult != ExtractOperationResult.OK) {
                System.err.println("Extraction error");
            } else {
                System.out.println(String.format("%9X | %10s | %s", hash, size, inArchive.getProperty(index, PropID.PATH)));
                hash = 0;
                size = 0;
            }
        }

        public void setCompleted(long completeValue) throws SevenZipException { }
        public void setTotal(long total) throws SevenZipException { }
    }

    public void unzip(File file) throws FileNotFoundException, SevenZipException {
        if(file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for(File f: listFiles) {
                unzip(f);
            }
        } else {
            if (file.isFile()) {
                if (file.getName().contains(".zip") || file.getName().contains(".7z") || file.getName().contains(".rar") || file.getName().contains(".tar")) {
                    IInArchive archive;
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                    try {
                        archive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));
                        int count = archive.getNumberOfItems();
                        List<Integer> itemsToExtract = new ArrayList<Integer>();
                        for(int i = 0; i<count ; i++) {
                            if(!(Boolean) archive.getProperty(i, PropID.IS_FOLDER)) {
                                itemsToExtract.add(i);
                            }
                        }
                        int[] items = new int[itemsToExtract.size()];
                        int i = 0;
                        for(Integer integer : itemsToExtract) {
                            items[i++] = integer;
                        }
                        ExtractCallback callback = new ExtractCallback(archive);
                        archive.extract(items,false, callback);

                        //System.out.println(file.getAbsolutePath() + " has " + numberOfItems + " items");
                        archive.close();
                        randomAccessFile.close();
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, e.getMessage());
                    }
                }
            }
        }
    }

    public void convert() {

    }

    public static void main(String[] args) {
        Options options = new Options();
        options = options.addOption(new Option("i", true, "Input directory"))
                .addOption(new Option("o", true, "Output directory"));
        try {
            DefaultParser cmdParser = new DefaultParser();
            CommandLine cmd = cmdParser.parse(options, args);
            if(cmd.hasOption("i")) {
                File inputPath = new File(cmd.getOptionValue("i"));
                //String outputPath = cmd.getOptionValue("o");
                LOG.log(Level.INFO, "Input dir: {0}", cmd.getOptionValue("i"));
                //LOG.log(Level.INFO, "Output dir: {0}", cmd.getOptionValue("o"));

                LOG.info("Starting preprocessing...");
                Utils utils = new Utils();
                utils.unzip(inputPath);
                //utils.convert();

                LOG.info("Closing...");
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Preprocess", options);
        } catch (FileNotFoundException | SevenZipException e) {
            LOG.log(Level.WARNING, e.getMessage());
        }
    }

}
