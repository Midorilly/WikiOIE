package di.uniba.it.wikioie.preprocessing;

import java.io.File;

/**
 * 
 * @author angelica
 *
 */
public class PreFile {
	
	private File file;
	private boolean poison;
	private int id;
	
	public PreFile(File file, int id) {
		this.file = file;
		poison = false;
		this.id = id;
	}
	
	public PreFile() {
		file = new File("poison");
		poison = true;
	}
	
	public boolean isPoison() {
		return poison;
	}
	
	public File getFile() {
		return file;
	}
	
	public int getId() {
		return id;
	}

}
