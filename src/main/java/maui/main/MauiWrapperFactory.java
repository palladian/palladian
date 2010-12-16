package maui.main;

import java.util.ArrayList;

/**
 * This class loads several vocabularies and models simultaneously.
 * Then all of them can be applied to generate topics from a given text. 
 * @author alyona
 *
 */
public class MauiWrapperFactory {

	MauiWrapper agriculture;
	MauiWrapper physics;
	MauiWrapper medicine;
	MauiWrapper general;
	
	/**
	 * Constructor - directory, where data is stored (should be similar structure as
	 * the main directory of Maui1.2, i.e. contain data/vocabularies, data/models,
	 * and data/stopwords).
	 * @param dataDirectory
	 */
	public MauiWrapperFactory(String dataDirectory) {
		agriculture = new MauiWrapper(dataDirectory, "agrovoc_en", "fao30");
		physics = new MauiWrapper(dataDirectory, "hep", "cern290");
		medicine = new MauiWrapper(dataDirectory, "mesh", "nlm500");
		general = new MauiWrapper(dataDirectory, "lcsh", "theses80");
	}
	
	
	/**
	 * Extracts topics given specified input and parameters
	 * @param input - input text or a path to a file with text input
	 * @param isaFile - whether the input is a file or a text string
	 * @param numberOfTopics - how many topics should be extracted
	 * @param domain - what is the domain (agriculture, physics, medicine, general)
	 * @return
	 * @throws Exception
	 */
	private ArrayList<String> extractTopics(String input, boolean isaFile, int numberOfTopics, String domain) throws Exception {
		ArrayList<String> topics = null;
		if (domain.equals("agriculture") && isaFile)
			topics = agriculture.extractTopicsFromFile(input, numberOfTopics);
		else if (domain.equals("agriculture") && !isaFile)
			topics = agriculture.extractTopicsFromText(input, numberOfTopics);
		if (domain.equals("physics") && isaFile)
			topics = physics.extractTopicsFromFile(input, numberOfTopics);
		else if (domain.equals("physics") && !isaFile)
			topics = physics.extractTopicsFromText(input, numberOfTopics);
		if (domain.equals("medicine") && isaFile)
			topics = medicine.extractTopicsFromFile(input, numberOfTopics);
		else if (domain.equals("medicine") && !isaFile)
			topics = medicine.extractTopicsFromText(input, numberOfTopics);
		if (domain.equals("general") && isaFile)
			topics = general.extractTopicsFromFile(input, numberOfTopics);
		else if (domain.equals("general") && !isaFile)
			topics = general.extractTopicsFromText(input, numberOfTopics);
		
		return topics;
	}

	
	/**
	 * Main method for testing MauiWrapperFactory
	 * Add the path to a text file as command line argument
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		MauiWrapperFactory factory = new MauiWrapperFactory("../Maui1.2/");
		String filePath = args[0];
		
		try {
			
			ArrayList<String> keywords = factory.extractTopics(filePath, true, 10, "agriculture");
			for (String keyword : keywords) {
				System.out.println("Agrovoc keyword: " + keyword);
			}
			
			keywords = factory.extractTopics(filePath, true, 15, "physics");
			for (String keyword : keywords) {
				System.out.println("Physics keyword: " + keyword);
			}
			
			keywords = factory.extractTopics(filePath, true, 15, "medicine");
			for (String keyword : keywords) {
				System.out.println("Medical keyword: " + keyword);
			}
			
			keywords = factory.extractTopics(filePath, true, 15, "general");
			for (String keyword : keywords) {
				System.out.println("General keyword: " + keyword);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}	

	}

	
}
