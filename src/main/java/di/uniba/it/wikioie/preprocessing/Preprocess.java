package di.uniba.it.wikioie.preprocessing;

/**
* This class preprocesses a PDF.
* 
* @author angelica
*/


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class Preprocess {	
	
	private static BlockingQueue<PreFile> in = new ArrayBlockingQueue(10000);
	private int id = 0;
	private static int inputDocCount = 0;
	private static int outputDocCount;
	private static final Logger LOG = Logger.getLogger(Preprocess.class.getName());
		
	public Preprocess() { }
	
	public void preprocess(File file, String output) throws IOException, TikaException, SAXException {	
		if(file.isDirectory()) { 	
			File[] listFiles = file.listFiles();
			for(File f: listFiles) {
				preprocess(f, output);
			}
		} else {
			if(file.isFile()) {
				inputDocCount++;
				if(file.length() != 0) {					
					PreFile prefile = new PreFile(file, id);
					in.add(prefile);
					id++;
				} else {
					LOG.log(Level.INFO, file.getName() + " is empty");
				}
			}				
		}	
	} 
	
	private void poison(int nt) {
		PreFile poison = new PreFile();
		for(int i=0; i<nt; i++)
			in.add(poison);
	}
	
	public static void main(String[] args) throws IOException, TikaException, SAXException {
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"%4$s: %5$s [%1$tc]%n");
		Options options = new Options();
		options = options.addOption(new Option("i", true, "Input directory"))
				.addOption(new Option("o", true, "Output directory"))
				.addOption(new Option("t", true, "Number of threads (optional, default 4)"));
		try {
			DefaultParser cmdParser = new DefaultParser();
			CommandLine cmd = cmdParser.parse(options, args);
			if(cmd.hasOption("i") && cmd.hasOption("o")) {
				File inputPath = new File(cmd.getOptionValue("i"));
				String outputPath = cmd.getOptionValue("o");
				int nt = Integer.parseInt(cmd.getOptionValue("t", "4"));
				LOG.log(Level.INFO, "Input dir: {0}", cmd.getOptionValue("i"));
				LOG.log(Level.INFO, "Output dir: {0}", cmd.getOptionValue("o"));
				LOG.log(Level.INFO, "Threads: {0}", nt);				
				List<PreprocessThread> list = new ArrayList<>();
				for(int i=0; i<nt; i++) {
					list.add(new PreprocessThread(in, outputPath));
				}
				for(Thread t: list) {
					t.start();
				}
				LOG.info("Starting preprocessing...");
				Preprocess pre = new Preprocess();
				pre.preprocess(inputPath, outputPath);
				pre.poison(nt);
				for(Thread t: list) {
					t.join();
				}
				LOG.info("Closing...");
				for(PreprocessThread t: list) {
					outputDocCount = outputDocCount + t.getDocCount();
				}
				LOG.log(Level.INFO, "Input file count: " + inputDocCount);
				LOG.log(Level.INFO, "Processed file count: " + outputDocCount);			
			}			
		} catch (ParseException e) {
			 HelpFormatter formatter = new HelpFormatter();
	         formatter.printHelp("Preprocess", options);
		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "An error occurred: ", e);
		}
		
	}

}
