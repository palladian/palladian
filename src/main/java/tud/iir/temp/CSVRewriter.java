package tud.iir.temp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;

public class CSVRewriter {

	private static final Logger logger = Logger.getLogger(CSVRewriter.class);
	
	private static String[] categories = { "Lebensmittel", "Drogerie", "Elektro & Technik", "Tierbedarf & Zoo", "Mode & Schmuck", "Hobby & Freizeit",
			"Wohnen & Haushalt", "Sonstiges" };

	private static String[] categoriesNum = { "###1", "###2", "###3", "###4", "###5", "###6", "###7", "###8" };

	public void rewriteInput() {

		final Object[] obj = new Object[2];
		obj[0] = new StringBuilder();
		obj[1] = categoriesNum;

		LineAction la = new LineAction(obj) {

			@Override
			public void performAction(String line, int lineNumber) {

				if (lineNumber == 1) return;
				
				String[] parts = line.split(";");

				String newLine = "";

				for (int i = 0; i < 6; i++) {
					newLine += "\"" + parts[i].replaceAll(";", "") + "\"" + ";";
				}
				System.out.println(newLine);
				String cn = "";
				String cn2 = "";
				for (int i = 6; i < 14; i++) {
					String num = parts[i];
					num = num.replaceAll("\"", "");
					// System.out.println(num);
					if (num.equals("1")) {
						// System.out.println(obj[1].toString());
						if (cn.length() > 0) {
							cn2 = ((String[]) obj[1])[i - 6]; // category 2
//							cn2 = cn2.substring(3, cn2.length()); // remove second #-sign
						} else {
							cn = ((String[]) obj[1])[i - 6];
						}
					} 
				}

				((StringBuilder) obj[0]).append(newLine + "\"" + cn + "\"");
				// category 2
				if (cn2.length() > 0) {
					((StringBuilder) obj[0]).append("\"" + cn2 + "\"");
				}
				((StringBuilder) obj[0]).append("\n");
						

			}
		};

		// FileHelper.performActionOnEveryLine("res/analytix_daten_ohne_spalten_5_6.csv", la);
		FileHelper.performActionOnEveryLine("data/temp/analytix_daten_temp.csv", la);
		FileHelper.writeToFile("data/temp/dataRewritten.csv", (StringBuilder) obj[0]);

		//if (true) return;

		final Object[] obj2 = new Object[1];
		obj[0] = new StringBuilder();

		// combine fields
		la = new LineAction(obj2) {

			@Override
			public void performAction(String line, int lineNumber) {

				String newLine = line.replaceAll("\";\"", " ").replaceAll("\";;\"", " ").replaceAll("\"", "");

				((StringBuilder) obj[0]).append(newLine).append("\n");

			}
		};

		FileHelper.performActionOnEveryLine("data/temp/dataRewritten.csv", la);
		FileHelper.writeToFile("data/temp/dataRewrittenCombined.csv", (StringBuilder) obj[0]);
	}
	
	/**
	 * TUDIIR output to evaluate input 
	 *
	 */
	public void rewriteOutput() {

		final Object[] obj = new Object[1];
		obj[0] = new StringBuilder();

		LineAction la = new LineAction(obj) {

			@Override
			public void performAction(String line, int lineNumber) {

				try {
					String[] parts = line.split("###");
//					int num = Integer.valueOf(parts[1].replaceAll("\"", ""));

					for (int i = 0; i < 8; i++) {
						String newLine = "0";
						if (line.indexOf("#"+(i+1)) > -1)
							newLine = "1";
						((StringBuilder) obj[0]).append(newLine).append(";");
					}

					((StringBuilder) obj[0]).append("\n");
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		};

		FileHelper.performActionOnEveryLine("data/temp/dataRewrittenCombinedClassified.csv", la);
		FileHelper.writeToFile("data/temp/dataRewrittenCombinedClassifiedNormalized.csv", (StringBuilder) obj[0]);
		System.out.println("finished rewriting output");
	}

	
	/**
	 * TUDIIR output to evaluate input 
	 *
	 */
	public void rewriteOutputGoldstandard() {

		final Object[] obj = new Object[1];
		obj[0] = new StringBuilder();

		LineAction la = new LineAction(obj) {

			@Override
			public void performAction(String line, int lineNumber) {

				try {
//					String[] parts = line.split("###");
//					int num = Integer.valueOf(parts[1].replaceAll("\"", ""));

					for (int i = 0; i < 8; i++) {
						String newLine = "0";
						if (line.indexOf("#"+(i+1)) > -1)
							newLine = "1";
						((StringBuilder) obj[0]).append(newLine).append(";");
					}
					// ugly: remove last ";"
					((StringBuilder) obj[0]).replace(((StringBuilder) obj[0]).length()-1, ((StringBuilder) obj[0]).length(), "");
					((StringBuilder) obj[0]).append("\n");
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		};

		FileHelper.performActionOnEveryLine("data/temp/dataRewrittenCombined_Testing.csv", la);
		FileHelper.writeToFile("data/temp/dataRewrittenCombinedClassifiedNormalized_Testing.csv", (StringBuilder) obj[0]);
		System.out.println("finished rewriting output");
	}
	
	
	
	/**
	 * Input files are 2 files: 
	 * real c1;...c8 
	 * predicted c1;...;c8
	 * Output file is input for evaluate(): a line-by-line concatenation real c1;...c8; predicted c1;...;c8  
	 */
	public void combineGoldstandardAndPredictedCategories(){
		
		String goldstandardFile = "data/temp/dataRewrittenCombinedClassifiedNormalized_Testing.csv";
		String classifiedFile = "data/temp/dataRewrittenCombinedClassifiedNormalized.csv";
		StringBuilder outputFile = new StringBuilder();
		
		try {
			FileReader frGS = new FileReader(goldstandardFile);
			BufferedReader brGS = new BufferedReader(frGS);
			FileReader frCF = new FileReader(classifiedFile);
			BufferedReader brCF = new BufferedReader(frCF);

			String lineGS = "";
			String lineCF = "";
			

			
			do {
				lineGS = brGS.readLine();
				lineCF = brCF.readLine();
				
				if (lineGS == null || lineCF == null)
					break;

				
				try {
					outputFile.append(lineGS).append(";").append(lineCF).append("\n");
					
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}

			} while (lineGS != null && lineCF != null);
						
			frGS.close();
			brGS.close();
			frCF.close();
			brCF.close();

		} catch (FileNotFoundException e) {
			logger.error(goldstandardFile + " or " +classifiedFile+ ", " + e.getMessage());
		} catch (IOException e) {
			logger.error(goldstandardFile + " or " +classifiedFile+ ", " + e.getMessage());
		} catch (OutOfMemoryError e) {
			logger.error(goldstandardFile + " or " +classifiedFile+ ", " + e.getMessage());
		}
		
//		outputFile.append(";;;;;;;;;;;;;;;;").append(maxTotalError).append(";").append(ourTotalError);
//		double performance = 1;
//		performance -= (double)ourTotalError/(double)maxTotalError;
//		System.out.println(performance);
		
		FileHelper.writeToFile("data/temp/dataTestAndClassified.csv", outputFile);
		System.out.println("finished combining gold standard and predicted output");
		
		
	}
	
	
	
	
	/**
	 * Input file must be: real c1;...c8; predicted c1;...;c8
	 * @param outputFilePath Path and filename of the file the 
	 * output should be written to
	 */
	public double evaluate(String outputFilePath) {

		final Object[] obj = new Object[3];
		obj[0] = new StringBuilder();
		obj[1] = 0; // total errors
		obj[2] = 0; // total possible errors

		
		((StringBuilder) obj[0]).append("real1;real2;real3;real4;real5;real6;real7;real8;our1;our2;our3;our4;our5;our6;our7;our8;ourError\n");
		
		LineAction la = new LineAction(obj) {

			@Override
			public void performAction(String line, int lineNumber) {

				int errors = 0;
				boolean errorCounted = false;
				String[] parts = line.split(";");
				for (int i = 0; i < 8; i++) {
					((StringBuilder) obj[0]).append(parts[i]).append(";");
					if (!parts[i].equalsIgnoreCase(parts[i + 8])) {
						int c = ((Integer) obj[1]);
						c++;
						obj[1] = c;
						errors++;
					}
					if (parts[i].equalsIgnoreCase("1") && !errorCounted) {
						int c2 = ((Integer) obj[2]);
						c2 += 3;
						obj[2] = c2;
						errorCounted = true;
					} else if (parts[i].equalsIgnoreCase("1")) {
						int c2 = ((Integer) obj[2]);
						c2 += 1;
						obj[2] = c2;
					}
				}
				for (int i = 8; i < 16; i++) {
					((StringBuilder) obj[0]).append(parts[i]).append(";");
				}

				((StringBuilder) obj[0]).append(errors).append("\n");
			}
		};
		
		FileHelper.performActionOnEveryLine("data/temp/dataTestAndClassified.csv", la);
		
		double performance = 1 - (double)((Integer)obj[1])/(double)((Integer)obj[2]);
		StringBuilder performanceSB = new StringBuilder();
		performanceSB.append("open analytix performance: ").append(performance).append("\n");
		
		((StringBuilder) obj[0]).append(";;;;;;;;;;;;;;;;").append((Integer) obj[1]).append(";").append((Integer) obj[2]).append("\n");
		((StringBuilder) obj[0]).append(";;;;;;;;;;;;;;;;").append("performance:;").append(performance);
		FileHelper.writeToFile(outputFilePath, (StringBuilder) obj[0]);
		FileHelper.appendToFile("data/temp/thresholds.txt", performanceSB, false);
		System.out.println("finished evaluation");
		return performance;
	}

	public void addToFolders() {

		// combine fields
		LineAction la = new LineAction() {

			@Override
			public void performAction(String line, int lineNumber) {

				if (lineNumber <= 1)
					return;

				String[] parts = line.split("#");

				if (parts.length < 2)
					return;

				FileHelper.writeToFile("res" + File.separator + parts[1] + File.separator + lineNumber + ".txt", parts[0]);
			}
		};

		FileHelper.performActionOnEveryLine("data/temp/dataRewrittenCombined.csv", la);
	}

	public static void main(String[] args) {
//		new CSVRewriter().rewriteInput();
//		new CSVRewriter().rewriteOutput();
	//		new CSVRewriter().evaluate();
		// new CSVRewriter().addToFolders();
	}

}
