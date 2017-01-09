package pl.poznan.put.weights.xmcda;

public class InputFile {
	public String loadTagV2;
	public String loadTagV3;
	public String filename;
	public Boolean mandatory;
	
	public InputFile(String loadTagV2, String loadTagV3, String filename, Boolean mandatory){
		this.loadTagV2 = loadTagV2;
		this.loadTagV3 = loadTagV3;
		this.filename = filename;
		this.mandatory = mandatory;
	}
}
