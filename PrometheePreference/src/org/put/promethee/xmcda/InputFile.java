package org.put.promethee.xmcda;

public class InputFile {
	public String loadTag;
	public String filename;
	public Boolean mandatory;
	
	public InputFile(String loadTag, String filename, Boolean mandatory){
		this.loadTag = loadTag;
		this.filename = filename;
		this.mandatory = mandatory;
	}
}
