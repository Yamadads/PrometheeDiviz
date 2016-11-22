package org.put.promethee.xmcda;

public class InputFile {	
	public String loadTagV3;
	public String filename;
	public Boolean mandatory;
	
	public InputFile(String loadTagV3, String filename, Boolean mandatory){		
		this.loadTagV3 = loadTagV3;
		this.filename = filename;
		this.mandatory = mandatory;
	}
}