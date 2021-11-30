package di.uniba.it.wikioie.preprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class PreprocessThread extends Thread {
	
	private final BlockingQueue<PreFile> in;
	private boolean run = true;	
	private String outputPath;	
	private int threadDocCount = 0;
	private static final Logger LOG = Logger.getLogger(PreprocessThread.class.getName());
	
	public PreprocessThread(BlockingQueue<PreFile> in, String outputPath) {
		this.in = in;
		this.outputPath = outputPath;
	}
	
	@Override
	public void run() {
		while(run) {
			try {
				PreFile prefile = in.take();
				if(!prefile.isPoison()) {
					File file = prefile.getFile();
					int id = prefile.getId();
					String title = file.getAbsolutePath();
					String text = parse(file);
					if(!text.isEmpty()) {
						String folderName = file.getParentFile().getName();
						File outputDir = new File(outputPath + "/" + folderName);
						outputDir.mkdirs();
						writePlainText(id, title, text, outputDir.getAbsolutePath());	
					}						
					else {
						LOG.log(Level.WARNING, "Unable to correctly parse " + title);
					}
						
				} else {
					setRun(false);
				}
			} catch (InterruptedException | IOException | SAXException | TikaException e) {
				LOG.log(Level.SEVERE, "An error occurred: ", e);
			}
		}
	}
	
	public String parse(File file) throws IOException, SAXException, TikaException {		
		FileInputStream stream = new FileInputStream(file);	
		AutoDetectParser autoParser = new AutoDetectParser();
		Metadata metadata = new Metadata();
		BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);		
		PDFParserConfig pdfConfig = new PDFParserConfig();
		pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);
		pdfConfig.setExtractInlineImages(true);		
		TesseractOCRConfig ocrConfig = new TesseractOCRConfig();		
		ParseContext context = new ParseContext();				
		context.set(TesseractOCRConfig.class, ocrConfig);
		context.set(PDFParserConfig.class, pdfConfig);	
		context.set(AutoDetectParser.class, autoParser);
		autoParser.parse(stream, handler, metadata, context);
		
		String text = handler.toString();
		stream.close();
		return text;
	}
	
	public void writePlainText(int id, String title, String text, String outputPath) throws IOException {
		try {
			FileWriter writer = new FileWriter(new File(outputPath, "plain_"+id)); 
			writer.write("<doc id=\"" + id + "\" url=\"?curid="+ id + "\" title=\"" + title + "\" >");
			writer.write(text);
			writer.write("</doc>");
			writer.close();
			threadDocCount++;
    	} catch (IOException e) {
    		LOG.log(Level.SEVERE, "An error occurred", e);
    	}
	}

	public void setRun(boolean set) {
		run = set;
	}

	public int getDocCount() {
		return threadDocCount;
	}	
}
