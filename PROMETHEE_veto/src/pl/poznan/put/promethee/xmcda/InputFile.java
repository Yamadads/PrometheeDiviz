package pl.poznan.put.promethee.xmcda;

public class InputFile {
	public String loadTagV2;
	public String loadTagV3;
	public String filenameV2;
	public String filenameV3;
	public Boolean mandatory;
	
	public InputFile(String loadTagV2, String loadTagV3, String filenameV2, String filenameV3, Boolean mandatory){
		this.loadTagV2 = loadTagV2;
		this.loadTagV3 = loadTagV3;
		this.filenameV2 = filenameV2;
		this.filenameV3 = filenameV3;
		this.mandatory = mandatory;
	}
}
