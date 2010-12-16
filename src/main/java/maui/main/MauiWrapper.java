package maui.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import maui.filters.MauiFilter;
import maui.stemmers.PorterStemmer;
import maui.stemmers.Stemmer;
import maui.stopwords.Stopwords;
import maui.stopwords.StopwordsEnglish;
import maui.vocab.Vocabulary;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


/**
 * This class shows how to use Maui on a single document
 * or just a string of text.
 * @author alyona
 *
 */
public class MauiWrapper {

	/** Maui filter object */
	private MauiFilter extractionModel = null;
	
	private Vocabulary vocabulary = null;
	private Stemmer stemmer;
	private Stopwords stopwords;
	private String language = "en";
	
	/**
	 * Constructor, which loads the data
	 * @param dataDirectory - e.g. Maui's main directory (should has "data" dir in it)
	 * @param vocabularyName - name of the rdf vocabulary
	 * @param modelName - name of the model
	 */
	public MauiWrapper(String dataDirectory, String vocabularyName, String modelName) {
	
		stemmer = new PorterStemmer();
		String englishStopwords = dataDirectory + "data/stopwords/stopwords_en.txt";
		stopwords = new StopwordsEnglish(englishStopwords);
		String vocabularyDirectory = dataDirectory +  "data/vocabularies/";
		String modelDirectory = dataDirectory +  "data/models";
		loadVocabulary(vocabularyDirectory, vocabularyName);
		loadModel(modelDirectory, modelName, vocabularyName);
	}

	/**
	 * Loads a vocabulary from a given directory
	 * @param vocabularyDirectory
	 * @param vocabularyName
	 */
	public void loadVocabulary(String vocabularyDirectory, String vocabularyName) {
		if (vocabulary != null)
			return;
		try {
			vocabulary = new Vocabulary(vocabularyName, "skos", vocabularyDirectory);
			vocabulary.setStemmer(stemmer);
			vocabulary.setStopwords(stopwords);
			vocabulary.setLanguage(language);
			vocabulary.initialize();
		} catch (Exception e) {
			System.err.println("Failed to load vocabulary!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the model
	 * @param modelDirectory
	 * @param modelName
	 * @param vocabularyName
	 */
	public void loadModel(String modelDirectory, String modelName, String vocabularyName) {

		try {
			BufferedInputStream inStream = new BufferedInputStream(
					new FileInputStream(modelDirectory + "/" + modelName));
			ObjectInputStream in = new ObjectInputStream(inStream);
			extractionModel = (MauiFilter) in.readObject();
			in.close();
		} catch (Exception e) {
			System.err.println("Failed to load model!");
			e.printStackTrace();
		}

		extractionModel.setVocabularyName(vocabularyName);
		extractionModel.setVocabularyFormat("skos");
		extractionModel.setDocumentLanguage(language);
		extractionModel.setStemmer(stemmer);
		extractionModel.setStopwords(stopwords);

		extractionModel.setVocabulary(vocabulary);
	}

	/**
	 * Main method to extract the main topics from a given text
	 * @param text
	 * @param topicsPerDocument
	 * @return
	 * @throws Exception
	 */
	public ArrayList<String> extractTopicsFromText(String text, int topicsPerDocument) throws Exception {

		if (text.length() < 5) {
			throw new Exception("Text is too short!");
		}

		extractionModel.setWikipedia(null);

		FastVector atts = new FastVector(3);
		atts.addElement(new Attribute("filename", (FastVector) null));
		atts.addElement(new Attribute("doc", (FastVector) null));
		atts.addElement(new Attribute("keyphrases", (FastVector) null));
		Instances data = new Instances("keyphrase_training_data", atts, 0);

		double[] newInst = new double[3];

		newInst[0] = (double) data.attribute(0).addStringValue("inputFile");
		newInst[1] = (double) data.attribute(1).addStringValue(text);
		newInst[2] = Instance.missingValue();
		data.add(new Instance(1.0, newInst));

		extractionModel.input(data.instance(0));

		data = data.stringFreeStructure();
		Instance[] topRankedInstances = new Instance[topicsPerDocument];
		Instance inst;

		// Iterating over all extracted keyphrases (inst)
		while ((inst = extractionModel.output()) != null) {

			int index = (int) inst.value(extractionModel.getRankIndex()) - 1;

			if (index < topicsPerDocument) {
				topRankedInstances[index] = inst;
			}
		}

		ArrayList<String> topics = new ArrayList<String>();

		for (int i = 0; i < topicsPerDocument; i++) {
			if (topRankedInstances[i] != null) {
				String topic = topRankedInstances[i].stringValue(extractionModel
						.getOutputFormIndex());
			
				topics.add(topic);
			}
		}
		extractionModel.batchFinished();
		return topics;
	}

	/**
	 * Triggers topic extraction from a text file
	 * @param filePath
	 * @param numberOfTopics
	 * @return
	 * @throws Exception
	 */
	public ArrayList<String> extractTopicsFromFile(String filePath, int numberOfTopics) throws Exception {
		File documentTextFile = new File(filePath);
		String documentText = FileUtils.readFileToString(documentTextFile);
		return extractTopicsFromText(documentText, numberOfTopics);
	}
	
	/**
	 * Main method for testing MauiWrapper
	 * Add the path to a text file as command line argument
	 * @param args
	 */
	public static void main(String[] args) {
		
		String vocabularyName = "agrovoc_en";
		String modelName = "fao30";
		String dataDirectory = "../Maui1.2/";
		
		MauiWrapper wrapper = new MauiWrapper(dataDirectory, vocabularyName, modelName);
		
		String filePath = args[0];
		
		try {
			
			ArrayList<String> keywords = wrapper.extractTopicsFromFile(filePath, 15);
			for (String keyword : keywords) {
				System.out.println("Keyword: " + keyword);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
