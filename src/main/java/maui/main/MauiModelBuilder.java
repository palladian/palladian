package maui.main;

/*
 *    MauiModelBuilder.java
 *    Copyright (C) 2001-2009 Eibe Frank, Olena Medelyan
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import org.wikipedia.miner.model.Wikipedia;

import org.apache.commons.io.FileUtils;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import maui.filters.MauiFilter;
import maui.stemmers.*;
import maui.stopwords.*;
import maui.vocab.Vocabulary;

/**
 * Builds a topic indexing model from the documents in a given
 * directory.  Assumes that the file names for the documents end with
 * ".txt".  Assumes that files containing corresponding
 * author-assigned keyphrases end with ".key". Optionally an encoding
 * for the documents/keyphrases can be defined (e.g. for Chinese
 * text).
 *
 * Valid options are:<p>
 *
 * -l "directory name"<br>
 * Specifies name of directory.<p>
 *
 * -m "model name"<br>
 * Specifies name of model.<p>
 *
 * -e "encoding"<br>
 * Specifies encoding.<p>
 * 
 *  -w "WikipediaDatabase@WikipediaServer" <br>
 * Specifies wikipedia data.<p>
 * 
 * -v "vocabulary name" <br>
 * Specifies vocabulary name (e.g. agrovoc or none).<p>
 * 
 * -f "vocabulary format" <br>
 * Specifies vocabulary format (txt or skos).<p>
 *
 * -i "document language" <br>
 * Specifies document language (en, es, de, fr).<p>
 *
 * -d<br>
 * Turns debugging mode on.<p>
 * 
 * -x "length"<br>
 * Sets maximum phrase length (default: 3).<p>
 *
 * -y "length"<br>
 * Sets minimum phrase length (default: 1).<p>
 *
 * -o "number"<br>
 * Sets the minimum number of times a phrase needs to occur (default: 2). <p>
 *
 * -s "stopwords class"<br>
 * Sets the name of the class implementing the stop words (default: StopwordsEnglish).<p>
 *
 * -t "stemmer class "<br>
 * Sets stemmer to use (default: PorterStemmer). <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz), Olena Medelyan (olena@cs.waikato.ac.nz)
 * @version 1.0
 */
public class MauiModelBuilder implements OptionHandler {

	/** Name of directory */
	public String inputDirectoryName = null;

	/** Name of model */
	public String modelName = null;

	/** Vocabulary name */
	public String vocabularyName = "none";

	/** Format of the vocabulary {skos,text} */
	public String vocabularyFormat = null;
	
	/** Directory where vocabularies are stored **/
	public String vocabularyDirectory = "data/vocabularies";

	/** Document language {en,es,de,fr,...} */
	public String documentLanguage = "en";

	/** Document encoding */
	public String documentEncoding = "default";

	/** Debugging mode? */
	public boolean debugMode = false;

	/** Maximum length of phrases */
	public int maxPhraseLength = 5;

	/** Minimum length of phrases */
	public int minPhraseLength = 1;

	/** Minimum number of occurences of a phrase */
	public int minNumOccur = 1;
	
	/** Wikipedia object */
	public Wikipedia wikipedia = null;
	
	/** Classifier */
	private Classifier classifier = null;
	
	/** Name of the server with the mysql Wikipedia data */ 
	private String wikipediaServer = "localhost"; 
	
	/** Name of the database with Wikipedia data */
	private String wikipediaDatabase = "database";
	
	/** Name of the directory with Wikipedia data in files */
	private String wikipediaDataDirectory = null;
	
	/** Should Wikipedia data be cached first? */
	private boolean cacheWikipediaData = false;
	
	/** Minimum keyphraseness of a string */
	private double minKeyphraseness = 0.01;

	/** Minimum sense probability or commonness */
	private double minSenseProbability = 0.005;

	/** Minimum number of the context articles */
	private int contextSize = 5;

	/** Use basic features  
	 * TFxIDF & First Occurrence */
	boolean useBasicFeatures = true;

	/** Use domain keyphraseness feature */
	boolean useKeyphrasenessFeature = true;

	/** Use frequency features
	 * TF & IDF additionally */
	boolean useFrequencyFeatures = true;

	/** Use occurrence position features
	 * LastOccurrence & Spread */
	boolean usePositionsFeatures = true;

	/** Use thesaurus features
	 * Node degree  */
	boolean useNodeDegreeFeature = true;

	/** Use length feature */
	boolean useLengthFeature = true;

	/** Use basic Wikipedia features 
	 *  Wikipedia keyphraseness & Total Wikipedia keyphraseness */
	boolean useBasicWikipediaFeatures = false;

	/** Use all Wikipedia features 
	 * Inverse Wikipedia frequency & Semantic relatedness*/
	boolean useAllWikipediaFeatures = false;

	/** Maui filter object */
	private MauiFilter mauiFilter = null;

	/** Stemmer to be used */
	Stemmer stemmer = new PorterStemmer();

	/** Llist of stopwords to be used */
	public  Stopwords stopwords = new StopwordsEnglish("data/stopwords/stopwords_en.txt");
	
	private Vocabulary vocabulary = null;
	
	public void loadThesaurus(Stemmer st, Stopwords sw, String vocabularyDirectory) {
		if (vocabulary != null)
			return;

		try {

			if (debugMode) {
				System.err.println("--- Loading the vocabulary...");
			}
			vocabulary = new Vocabulary(vocabularyName, vocabularyFormat, vocabularyDirectory);
			vocabulary.setStemmer(st);
			vocabulary.setStopwords(sw);
			vocabulary.setDebug(debugMode);
			vocabulary.setLanguage(documentLanguage);
			vocabulary.initialize();
			
		} catch (Exception e) {
			System.err.println("Failed to load thesaurus!");
			e.printStackTrace();
		}

	}

	public boolean getDebug()	{
		return debugMode;
	}
	
	public void setBasicFeatures(boolean useBasicFeatures) {
		this.useBasicFeatures = useBasicFeatures;
	}

	public void setKeyphrasenessFeature(boolean useKeyphrasenessFeature) {
		this.useKeyphrasenessFeature = useKeyphrasenessFeature;
	}

	public void setFrequencyFeatures(boolean useFrequencyFeatures) {
		this.useFrequencyFeatures = useFrequencyFeatures;
	}

	public void setPositionsFeatures(boolean usePositionsFeatures) {
		this.usePositionsFeatures = usePositionsFeatures;
	}

	public void setNodeDegreeFeature(boolean useNodeDegreeFeature) {
		this.useNodeDegreeFeature = useNodeDegreeFeature;
	}

	public void setLengthFeature(boolean useLengthFeature) {
		this.useLengthFeature = useLengthFeature;
	}

	public void setBasicWikipediaFeatures(boolean useBasicWikipediaFeatures) {
		this.useBasicWikipediaFeatures = useBasicWikipediaFeatures;
	}

	public void setAllWikipediaFeatures(boolean useAllWikipediaFeatures) {
		this.useAllWikipediaFeatures = useAllWikipediaFeatures;
	}
	
	public void setContextSize(int contextSize) {
		this.contextSize = contextSize;
	}
	
	public void setMinSenseProbability(double minSenseProbability) {
		this.minSenseProbability = minSenseProbability;
	}
	
	public void setMinKeyphraseness(double minKeyphraseness) {
		this.minKeyphraseness = minKeyphraseness;
	}
	

	/**
	 * Parses a given list of options controlling the behaviour of this object.
	 * Valid options are:<p>
	 *
	 * -l "directory name" <br>
	 * Specifies name of directory.<p>
	 *
	 * -m "model name" <br>
	 * Specifies name of model.<p>
	 *
	 * -v "vocabulary name" <br>
	 * Specifies vocabulary name.<p>
	 * 
	 * -f "vocabulary format" <br>
	 * Specifies vocabulary format.<p>
	 *    
	 * -i "document language" <br>
	 * Specifies document language.<p>
	 * 
	 * -e "encoding" <br>
	 * Specifies encoding.<p>
	 * 
	 *  -w "WikipediaDatabase@WikipediaServer" <br>
	 * Specifies wikipedia data.<p>
	 * 
	 * -d<br>
	 * Turns debugging mode on.<p>
	 *
	 * -x "length"<br>
	 * Sets maximum phrase length (default: 3).<p>
	 *
	 * -y "length"<br>
	 * Sets minimum phrase length (default: 3).<p>
	 *
	 * -o "number"<br>
	 * The minimum number of times a phrase needs to occur (default: 2). <p>
	 *
	 * -s "name of class implementing list of stop words"<br>
	 * Sets list of stop words to used (default: StopwordsEnglish).<p>
	 *
	 * -t "name of class implementing stemmer"<br>
	 * Sets stemmer to use (default: IteratedLovinsStemmer). <p>
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {

		String dirName = Utils.getOption('l', options);
		if (dirName.length() > 0) {
			inputDirectoryName = dirName;
		} else {
			inputDirectoryName = null;
			throw new Exception("Name of directory required argument.");
		}

		String modelName = Utils.getOption('m', options);
		if (modelName.length() > 0) {
			this.modelName = modelName;
		} else {
			this.modelName = null;
			throw new Exception("Name of model required argument.");
		}

		String vocabularyName = Utils.getOption('v', options);
		if (vocabularyName.length() > 0) {
			this.vocabularyName = vocabularyName;
		} 

		String vocabularyFormat = Utils.getOption('f', options);

		if (!vocabularyName.equals("none") && !vocabularyName.equals("wikipedia")) {
			if (vocabularyFormat.length() > 0) {
				if (vocabularyFormat.equals("skos")
						|| vocabularyFormat.equals("text")) {
					this.vocabularyFormat = vocabularyFormat;
				} else {
					throw new Exception(
							"Unsupported format of vocabulary. It should be either \"skos\" or \"text\".");
				}
			} else {
				throw new Exception(
						"If a controlled vocabulary is used, format of vocabulary required argument (skos or text).");
			}
		}
		
		String encoding = Utils.getOption('e', options);
		if (encoding.length() > 0) 
			this.documentEncoding = encoding;
		
		String wikipediaConnection = Utils.getOption('w', options);
		if (wikipediaConnection.length() > 0) {
			int at = wikipediaConnection.indexOf("@");
			wikipediaDatabase = wikipediaConnection.substring(0,at);
			wikipediaServer = wikipediaConnection.substring(at+1);
		} 

		String documentLanguage = Utils.getOption('i', options);
		if (documentLanguage.length() > 0) 
			this.documentLanguage = documentLanguage;
	

		String maxPhraseLengthString = Utils.getOption('x', options);
		if (maxPhraseLengthString.length() > 0) 
			this.maxPhraseLength = Integer.parseInt(maxPhraseLengthString);
		
		String minPhraseLengthString = Utils.getOption('y', options);
		if (minPhraseLengthString.length() > 0) 
			this.minPhraseLength = Integer.parseInt(minPhraseLengthString);
			
		String minNumOccurString = Utils.getOption('o', options);
		if (minNumOccurString.length() > 0) 
			this.minNumOccur = Integer.parseInt(minNumOccurString);
		

		String stopwordsString = Utils.getOption('s', options);
		if (stopwordsString.length() > 0) {
			stopwordsString = "maui.stopwords.".concat(stopwordsString);
			this.stopwords = (Stopwords) Class.forName(stopwordsString)
					.newInstance();
		}

		String stemmerString = Utils.getOption('t', options);
		if (stemmerString.length() > 0) {
			stemmerString = "maui.stemmers.".concat(stemmerString);
			this.stemmer = (Stemmer) Class.forName(stemmerString).newInstance();
		}
		debugMode = Utils.getFlag('d', options);
		Utils.checkForRemainingOptions(options);
	}

	/**
	 * Gets the current option settings.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {

		String[] options = new String[23];
		int current = 0;

		options[current++] = "-l";
		options[current++] = "" + (this.inputDirectoryName);
		options[current++] = "-m";
		options[current++] = "" + (this.modelName);
		options[current++] = "-v";
		options[current++] = "" + (this.vocabularyName);
		options[current++] = "-f";
		options[current++] = "" + (this.vocabularyFormat);
		options[current++] = "-e";
		options[current++] = "" + (this.documentEncoding);
		options[current++] = "-i";
		options[current++] = "" + (this.documentLanguage);

		if (this.debugMode) {
			options[current++] = "-d";
		}
		options[current++] = "-x";
		options[current++] = "" + (this.maxPhraseLength);
		options[current++] = "-y";
		options[current++] = "" + (this.minPhraseLength);
		options[current++] = "-o";
		options[current++] = "" + (this.minNumOccur);
		options[current++] = "-s";
		options[current++] = "" + (stopwords.getClass().getName());
		options[current++] = "-t";
		options[current++] = "" + (stemmer.getClass().getName());

		while (current < options.length) {
			options[current++] = "";
		}
		return options;
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options
	 */
	public Enumeration<Option> listOptions() {

		Vector<Option> newVector = new Vector<Option>(12);

		newVector.addElement(new Option("\tSpecifies name of directory.", "l",
				1, "-l <directory name>"));
		newVector.addElement(new Option("\tSpecifies name of model.", "m", 1,
				"-m <model name>"));
		newVector.addElement(new Option("\tSpecifies vocabulary name.", "v", 1,
				"-v <vocabulary name>"));
		newVector.addElement(new Option(
				"\tSpecifies vocabulary format (text or skos or none).", "f",
				1, "-f <vocabulary format>"));
		newVector.addElement(new Option(
				"\tSpecifies document language (en (default), es, de, fr).",
				"i", 1, "-i <document language>"));
		newVector.addElement(new Option("\tSpecifies encoding.", "e", 1,
				"-e <encoding>"));
		newVector.addElement(new Option("\tSpecifies wikipedia database and server.", "w", 1,
		"-w <wikipediaDatabase@wikipediaServer>"));
		newVector.addElement(new Option("\tTurns debugging mode on.", "d", 0,
				"-d"));
		newVector.addElement(new Option(
				"\tSets the maximum phrase length (default: 5).", "x", 1,
				"-x <length>"));
		newVector.addElement(new Option(
				"\tSets the minimum phrase length (default: 1).", "y", 1,
				"-y <length>"));
		newVector.addElement(new Option(
				"\tSet the minimum number of occurences (default: 2).", "o", 1,
				"-o"));
		newVector
				.addElement(new Option(
						"\tSets the list of stopwords to use (default: StopwordsEnglish).",
						"s", 1, "-s <name of stopwords class>"));
		newVector.addElement(new Option(
				"\tSet the stemmer to use (default: SremovalStemmer).", "t", 1,
				"-t <name of stemmer class>"));

		return newVector.elements();
	}

	/**
	 * Collects the file names
	 */
	public HashSet<String> collectStems() throws Exception {

		HashSet<String> stems = new HashSet<String>();

		try {
			File dir = new File(inputDirectoryName);

			for (String file : dir.list()) {
				if (file.endsWith(".txt")) {
					String stem = file.substring(0, file.length() - 4);

					File keys = new File(inputDirectoryName + "/" + stem
							+ ".key");
					if (keys.exists()) {
						stems.add(stem);
					}
				}
			}
		} catch (Exception e) {
			throw new Exception("Problem reading directory "
					+ inputDirectoryName);
		}
		return stems;
	}

	/**
	 * Builds the model from the training data
	 */
	public void buildModel(HashSet<String> fileNames) throws Exception {

		// Check whether there is actually any data
		if (fileNames.size() == 0) {
			throw new Exception("Couldn't find any data in "
					+ inputDirectoryName);
		}

		System.err.println("-- Building the model... ");
		
		FastVector atts = new FastVector(3);
		atts.addElement(new Attribute("filename", (FastVector) null));
		atts.addElement(new Attribute("document", (FastVector) null));
		atts.addElement(new Attribute("keyphrases", (FastVector) null));
		Instances data = new Instances("keyphrase_training_data", atts, 0);

		// Build model
		mauiFilter = new MauiFilter();

		mauiFilter.setDebug(debugMode);
		mauiFilter.setMaxPhraseLength(maxPhraseLength);
		mauiFilter.setMinPhraseLength(minPhraseLength);
		mauiFilter.setMinNumOccur(minNumOccur);
		mauiFilter.setStemmer(stemmer);
		mauiFilter.setDocumentLanguage(documentLanguage);
		mauiFilter.setVocabularyName(vocabularyName);
		mauiFilter.setVocabularyFormat(vocabularyFormat);
		mauiFilter.setStopwords(stopwords);
		
	
		if (wikipedia != null) {
			mauiFilter.setWikipedia(wikipedia);
		} else if (wikipediaServer.equals("localhost") && wikipediaDatabase.equals("database")) {
			mauiFilter.setWikipedia(wikipedia);		
		} else {
			mauiFilter.setWikipedia(wikipediaServer, wikipediaDatabase, cacheWikipediaData, wikipediaDataDirectory);
		}
		
		if (classifier != null) {
			mauiFilter.setClassifier(classifier);
		}
		
		mauiFilter.setInputFormat(data);
		
		// set features configurations
		mauiFilter.setBasicFeatures(useBasicFeatures);
		mauiFilter.setKeyphrasenessFeature(useKeyphrasenessFeature);
		mauiFilter.setFrequencyFeatures(useFrequencyFeatures);
		mauiFilter.setPositionsFeatures(usePositionsFeatures);
		mauiFilter.setLengthFeature(useLengthFeature);
		mauiFilter.setThesaurusFeatures(useNodeDegreeFeature);
		mauiFilter.setBasicWikipediaFeatures(useBasicWikipediaFeatures);
		mauiFilter.setAllWikipediaFeatures(useAllWikipediaFeatures);
		mauiFilter.setThesaurusFeatures(useNodeDegreeFeature);
		
		mauiFilter.setClassifier(classifier);
		
		mauiFilter.setContextSize(contextSize);
		mauiFilter.setMinKeyphraseness(minKeyphraseness);
		mauiFilter.setMinSenseProbability(minSenseProbability);
		
		if (!vocabularyName.equals("none") && !vocabularyName.equals("wikipedia") ) {
			loadThesaurus(stemmer, stopwords, vocabularyDirectory);
			mauiFilter.setVocabulary(vocabulary);
		}

		

		System.err.println("-- Reading the input documents... ");

		for (String fileName : fileNames) {

			double[] newInst = new double[3];

			newInst[0] = (double) data.attribute(0).addStringValue(fileName);

			File documentTextFile = new File(inputDirectoryName + "/"
					+ fileName + ".txt");
			File documentTopicsFile = new File(inputDirectoryName + "/"
					+ fileName + ".key");

			try {

				String documentText;
				if (!documentEncoding.equals("default")) {
					documentText = FileUtils.readFileToString(documentTextFile, documentEncoding);
				} else {
					documentText = FileUtils.readFileToString(documentTextFile);
				}
	
				// Adding the text of the document to the instance
				newInst[1] = (double) data.attribute(1).addStringValue(documentText);

			} catch (Exception e) {

				System.err.println("Problem with reading " + documentTextFile);
				e.printStackTrace();
				newInst[1] = Instance.missingValue();
			}

			try {

				String documentTopics;
				if (!documentEncoding.equals("default")) {
					documentTopics = FileUtils.readFileToString(documentTopicsFile, documentEncoding);
				} else {
					documentTopics = FileUtils.readFileToString(documentTopicsFile);
				}
				
				// Adding the topics to the file
				newInst[2] = (double) data.attribute(2).addStringValue(documentTopics);

			} catch (Exception e) {

				System.err
						.println("Problem with reading " + documentTopicsFile);
				e.printStackTrace();
				newInst[2] = Instance.missingValue();
			}

			data.add(new Instance(1.0, newInst));

			mauiFilter.input(data.instance(0));
			data = data.stringFreeStructure();
		}
		mauiFilter.batchFinished();

		while ((mauiFilter.output()) != null) {
		}
		;
	}

	
	/** 
	 * Saves the extraction model to the file.
	 */
	public void saveModel() throws Exception {

		BufferedOutputStream bufferedOut = new BufferedOutputStream(
				new FileOutputStream(modelName));
		ObjectOutputStream out = new ObjectOutputStream(bufferedOut);
		out.writeObject(mauiFilter);
		out.flush();
		out.close();
	}

	/**
	 * The main method.  
	 */
	public static void main(String[] ops) {

		MauiModelBuilder modelBuilder = new MauiModelBuilder();

		try {

			modelBuilder.setOptions(ops);

			// Output what options are used
			if (modelBuilder.getDebug() == true) {
				System.err.print("Building model with options: ");
				String[] optionSettings = modelBuilder.getOptions();
				for (String optionSetting : optionSettings) {
					System.err.print(optionSetting + " ");
				}
				System.err.println();
			}

			HashSet<String> fileNames = modelBuilder.collectStems();
			modelBuilder.buildModel(fileNames);

			if (modelBuilder.getDebug() == true) {
				System.err.print("Model built. Saving the model...");
			}

			modelBuilder.saveModel();

			System.err.print("Done!");
			
		} catch (Exception e) {

			// Output information on how to use this class
			e.printStackTrace();
			System.err.println(e.getMessage());
			System.err.println("\nOptions:\n");
			Enumeration<Option> en = modelBuilder.listOptions();
			while (en.hasMoreElements()) {
				Option option = (Option) en.nextElement();
				System.err.println(option.synopsis());
				System.err.println(option.description());
			}
		}
	}
}
