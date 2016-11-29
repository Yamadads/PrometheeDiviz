package pl.poznan.put.xmcda;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_2_1_v3_0.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_2_2_1.XMCDAParser;
import org.xml.sax.SAXException;

public class XMCDAFileConverter {

	public static void main(String[] args){
		final Arguments params = parseCmdLineArguments(args);
		if (params==null){
			return;
		}
		
		final ProgramExecutionResult executionResult = new ProgramExecutionResult();
		
		org.xmcda.v2_2_1.XMCDA xmcda_v2 = new org.xmcda.v2_2_1.XMCDA();
		loadXMCDAv2(xmcda_v2, new File(params.inputFile), true, executionResult, params.loadTag);
		
		if (!(executionResult.isOk() || executionResult.isWarning())) {
			System.out.println("Error in loading file");
			return;
		}
		XMCDA xmcda = null;	
		xmcda = convertToXMCDA_v3(xmcda_v2, executionResult);
		
		if (!(executionResult.isOk() || executionResult.isWarning() || xmcda==null)) {
			System.out.println("Error in loading file");
			return;
		}
		
		writeResultFile(params.outputFile, params.exportTag, xmcda);
		
	}
	
	private static void writeResultFile (String outputFilename, String exportTag, XMCDA xmcda){
		final org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser();
		File outputFile = new File(outputFilename);
		try {
			parser.writeXMCDA(xmcda, outputFile, exportTag);
		} catch (Throwable throwable) {
			System.out.println("Error in writing output file");			
			outputFile.delete();
		}
	}
	private static Arguments parseCmdLineArguments(String[] args){
		if (args.length != 8){
			System.out.println("Invalid number of arguments");
			return null;
		}
		Arguments arguments = new Arguments();
		for (int index = 0; index <= 6; index += 2) {
			String arg = args[index];
			if ("-i".equals(arg) || "--input-file".equals(arg))
				arguments.inputFile = args[index + 1];
			else if ("-l".equals(arg) || "--load-tag".equals(arg))
				arguments.loadTag = args[index + 1];
			else if ("-o".equals(arg) || "--output-file".equals(arg))
				arguments.outputFile = args[index + 1];
			else if ("-e".equals(arg) || "--export-tag".equals(arg))
				arguments.exportTag = args[index + 1];
		}
		if (arguments.inputFile == null || arguments.loadTag == null || arguments.outputFile == null
				|| arguments.exportTag == null){
			System.out.println("Missing parametrs");
			return null;
		}					
		return arguments;
	}

	public static class Arguments {
		public String inputFile;
		public String loadTag;
		public String outputFile;
		public String exportTag;
	}

	public static void loadXMCDAv2(org.xmcda.v2_2_1.XMCDA xmcda_v2, File file, boolean mandatory,
			ProgramExecutionResult x_execution_results, String... load_tags) {
		XMCDAParser parser = new XMCDAParser();
		final String baseFilename = file.getName();
		if (!file.exists()) {
			if (mandatory)
				x_execution_results.addError("Could not find the mandatory file " + baseFilename);
			return;
		}
		try {
			readXMCDAv2_and_update(xmcda_v2, parser, file, load_tags);
		} catch (Throwable throwable) {
			final String msg = String.format("Unable to read & parse the file %s, reason: ", baseFilename);
			x_execution_results.addError(getMessage(msg, throwable));
		}
	}

	@SuppressWarnings("unchecked")
	public static void readXMCDAv2_and_update(org.xmcda.v2_2_1.XMCDA xmcda_v2, XMCDAParser parser, File file,
			String[] load_tags) throws FileNotFoundException, JAXBException, SAXException {
		final org.xmcda.v2_2_1.XMCDA new_xmcda = parser.readXMCDA(file, load_tags);
		final List new_content = new_xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters();
		xmcda_v2.getProjectReferenceOrMethodMessagesOrMethodParameters().addAll(new_content);
	}

	static String getMessage(String message, Throwable throwable) {
		return message + getMessage(throwable);
	}
	
	static String getMessage(Throwable throwable)
	{
		if ( throwable.getMessage() != null )
			return throwable.getMessage();
		// when handling XMCDA v2 files, errors may be embedded in a JAXBException
		if ( throwable.getCause() != null && throwable.getCause().getMessage() != null )
			return throwable.getCause().getMessage();
		return "unknown";
	}
	
	private static XMCDA convertToXMCDA_v3(org.xmcda.v2_2_1.XMCDA xmcda_v2, ProgramExecutionResult executionResult) {
		XMCDA xmcda = null;
		try {
			xmcda = XMCDAConverter.convertTo_v3(xmcda_v2);
		} catch (Throwable t) {
			executionResult.addError(getMessage("Could not convert inputs to XMCDA v3, reason: ", t));
		}
		return xmcda;
	}
}
