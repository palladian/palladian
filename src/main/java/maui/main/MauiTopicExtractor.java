package maui.main;

/*
 *    MauiTopicExtractor.java
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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.text.CaseFolder;

import org.apache.commons.io.FileUtils;

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
 * Extracts topics from the documents in a given directory.
 * Assumes that the file names for the documents end with ".txt".
 * Puts extracted topics into corresponding files ending with
 * ".key" (if those are not already present). Optionally an encoding
 * for the documents/keyphrases can be defined (e.g. for Chinese
 * text). Documents for which ".key" exists are used for evaluation.
 *
 * Valid options are:<p>
 *
 * -l "directory name"<br>
 * Specifies name of directory.<p>
 *
 * -m "model name"<br>
 * Specifies name of model.<p>
 *
 * -v "vocabulary name"<br>
 * Specifies name of vocabulary.<p>
 *
 * -f "vocabulary format"<br>
 * Specifies format of vocabulary (text or skos).<p>
 *
 * -i "document language" <br>
 * Specifies document language (en, es, de, fr).<p>
 * 
 * -e "encoding"<br>
 * Specifies encoding.<p>
 * 
 *  -w "WikipediaDatabase@WikipediaServer" <br>
 * Specifies wikipedia data.<p>
 *
 * -n <br>
 * Specifies number of phrases to be output (default: 5).<p>
 *
 * -t "name of class implementing stemmer"<br>
 * Sets stemmer to use (default: SremovalStemmer). <p>
 *
 * -s "name of class implementing stopwords"<br>
 * Sets stemmer to use (default: StopwordsEnglish). <p>
 * 
 * -d<br>
 * Turns debugging mode on.<p>
 *
 * -g<br>
 * Build global dictionaries from the test set.<p>
 *
 * -p<br>
 * Prints plain-text graph description of the topics for visual representation of the results.<p>
 *
 * -a<br>
 * Also write stemmed phrase and score into ".key" file.<p>
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class MauiTopicExtractor implements OptionHandler {
	
	/** Name of directory */
	public String inputDirectoryName = null;
	
	/** Name of model */
	public String modelName = null;
	
	/** Vocabulary name */
	public String vocabularyName = "none";
	
	/** Format of the vocabulary */
	public String vocabularyFormat = null;
	
	/** Document language */
	public String documentLanguage = "en";
	
	/** Document encoding */
	public String documentEncoding = "default";
	
	/** Debugging mode? */
	public boolean debugMode = false;
	
	
	/** Maui filter object */
	private MauiFilter mauiFilter = null;
	
	/** Wikipedia object */
	public Wikipedia wikipedia = null;
	
	/** Name of the server with the mysql Wikipedia data */ 
	private String wikipediaServer = "localhost"; 
	
	/** Name of the database with Wikipedia data */
	private String wikipediaDatabase = "database";
	
	/** Name of the directory with Wikipedia data in files */
	private String wikipediaDataDirectory = null;
	
	/** Should Wikipedia data be cached first? */
	private boolean cacheWikipediaData = false;
	
	/** The number of phrases to extract. */
	int topicsPerDocument = 10;
	
	/** Directory where vocabularies are stored **/
	public String vocabularyDirectory = "data/vocabularies";
	
	/** Stemmer to be used */
	public Stemmer stemmer = new PorterStemmer();
	
	/** Llist of stopwords to be used */
	public Stopwords stopwords = new StopwordsEnglish("data/stopwords/stopwords_en.txt");
	
	
	private Vocabulary vocabulary = null;
	
	
	/** Also write stemmed phrase and score into .key file. */
	boolean additionalInfo = false;
	

	/** Prints plain-text graph description of the topics into a .gv file. */
	boolean printGraph = false;
	
	
	/** Build global dictionaries from the test set. */
	boolean buildGlobalDictionary = false;
	
	
	public boolean getDebug() {
		return debugMode;
	}
	
	/**
	 * Parses a given list of options controlling the behaviour of this object.
	 * Valid options are:<p>
	 *
	 * -l "directory name"<br>
	 * Specifies name of directory.<p>
	 *
	 * -m "model name"<br>
	 * Specifies name of model.<p>
	 *
	 * -v "vocabulary name"<br>
	 * Specifies vocabulary name.<p>
	 * 
	 * -f "vocabulary format"<br>
	 * Specifies vocabulary format.<p>
	 * 
	 * -i "document language" <br>
	 * Specifies document language.<p>
	 * 
	 * -e "encoding"<br>
	 * Specifies encoding.<p>
     *
     *  -w "WikipediaDatabase@WikipediaServer" <br>
     * Specifies wikipedia data.<p>
	 *
	 * -n<br>
	 * Specifies number of phrases to be output (default: 5).<p>
	 *
	 * -d<br>
	 * Turns debugging mode on.<p>
	 *
	 * -b<br>
	 * Builds global dictionaries for computing TFxIDF from the test collection.<p>
	 *
	 * -a<br>
	 * Also write stemmed phrase and score into ".key" file.<p>
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
		
		String numPhrases = Utils.getOption('n', options);
		if (numPhrases.length() > 0) {
			this.topicsPerDocument = Integer.parseInt(numPhrases);
		} 
		
		
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
		this.buildGlobalDictionary = Utils.getFlag('b', options);
		this.printGraph = Utils.getFlag('p', options);
		this.additionalInfo = Utils.getFlag('a', options);
		Utils.checkForRemainingOptions(options);
	}
	
	/**
	 * Gets the current option settings.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String [] getOptions() {
		
		String [] options = new String [22];
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
		options[current++] = "-n"; 
		options[current++] = "" + (this.topicsPerDocument);
		options[current++] = "-t"; 
		options[current++] = "" + (stemmer.getClass().getName());		
		options[current++] = "-s"; 
		options[current++] = "" + (stopwords.getClass().getName());

		if (getDebug()) {
			options[current++] = "-d";
		}
		

		if (printGraph) {
			options[current++] = "-p";
		}
		
		if (this.buildGlobalDictionary) {
			options[current++] = "-b";
		}
		
		if (additionalInfo) {
			options[current++] = "-a";
		}
		
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
		
		Vector<Option> newVector = new Vector<Option>(15);
		
		newVector.addElement(new Option(
				"\tSpecifies name of directory.",
				"l", 1, "-l <directory name>"));
		newVector.addElement(new Option(
				"\tSpecifies name of model.",
				"m", 1, "-m <model name>"));
		newVector.addElement(new Option(
				"\tSpecifies vocabulary name.",
				"v", 1, "-v <vocabulary name>"));
		newVector.addElement(new Option(
				"\tSpecifies vocabulary format.",
				"f", 1, "-f <vocabulary format>"));
		newVector.addElement(new Option(
				"\tSpecifies encoding.",
				"e", 1, "-e <encoding>"));		
		newVector.addElement(new Option("\tSpecifies wikipedia database and server.", "w", 1,
		"-w <wikipediaDatabase@wikipediaServer>"));
		newVector.addElement(new Option(
				"\tSpecifies document language (en (default), es, de, fr).",
				"i", 1, "-i <document language>"));
		newVector.addElement(new Option(
				"\tSpecifies number of phrases to be output (default: 5).",
				"n", 1, "-n"));
		newVector.addElement(new Option(
				"\tSet the stemmer to use (default: SremovalStemmer).",
				"t", 1, "-t <name of stemmer class>"));
		newVector.addElement(new Option(
				"\tSet the stopwords class to use (default: EnglishStopwords).",
				"s", 1, "-s <name of stopwords class>"));
		newVector.addElement(new Option(
				"\tTurns debugging mode on.",
				"d", 0, "-d"));
		newVector.addElement(new Option(
				"\tBuilds global dictionaries for computing TFIDF from the test collection.",
				"b", 0, "-b"));
		newVector.addElement(new Option(
				"\tPrints graph description into a \".gv\" file, in GraphViz format.",
				"p", 0, "-p"));
		newVector.addElement(new Option(
				"\tAlso write stemmed phrase and score into \".key\" file.",
				"a", 0, "-a"));
		
		return newVector.elements();
	}
	
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
					
					if (!stems.contains(stem)) {
						stems.add(stem);
					}
				}
			}
		} catch (Exception e) {
			throw new Exception("Problem reading directory " + inputDirectoryName);
		}
		return stems;
	}
	
	/**
	 * Builds the model from the files
	 */
	public void extractKeyphrases(HashSet<String> fileNames) throws Exception {
		
		// Check whether there is actually any data
		if (fileNames.size() == 0) {
			throw new Exception("Couldn't find any data in " + inputDirectoryName);
		}
		
		mauiFilter.setVocabularyName(vocabularyName);
		mauiFilter.setVocabularyFormat(vocabularyFormat);
		mauiFilter.setDocumentLanguage(documentLanguage);
		mauiFilter.setStemmer(stemmer);
		mauiFilter.setStopwords(stopwords);
		if (wikipedia != null) {
			mauiFilter.setWikipedia(wikipedia);
		} else if (wikipediaServer.equals("localhost") && wikipediaDatabase.equals("database")) {
			mauiFilter.setWikipedia(wikipedia);		
		} else {
			mauiFilter.setWikipedia(wikipediaServer, wikipediaDatabase, cacheWikipediaData, wikipediaDataDirectory);
		}
		if (!vocabularyName.equals("none") && !vocabularyName.equals("wikipedia") ) {
			loadThesaurus(stemmer, stopwords, vocabularyDirectory);
			mauiFilter.setVocabulary(vocabulary);
		}
		
		FastVector atts = new FastVector(3);
		atts.addElement(new Attribute("filename", (FastVector) null));
		atts.addElement(new Attribute("doc", (FastVector) null));
		atts.addElement(new Attribute("keyphrases", (FastVector) null));
		Instances data = new Instances("keyphrase_training_data", atts, 0);
		
		System.err.println("-- Extracting keyphrases... ");
		
		Vector<Double> correctStatistics = new Vector<Double>();
		Vector<Double> precisionStatistics = new Vector<Double>();
		Vector<Double> recallStatistics = new Vector<Double>();
		
		for (String fileName : fileNames) {
		
			double[] newInst = new double[3];
			
			newInst[0] = (double)data.attribute(0).addStringValue(fileName); ;
			
			File documentTextFile = new File(inputDirectoryName + "/" + fileName + ".txt");
			File documentTopicsFile = new File(inputDirectoryName + "/" + fileName + ".key");
			
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
				if (debugMode) {
					System.err.println("No existing topics for " + documentTextFile);
				}
				newInst[2] = Instance.missingValue();
			}
			
			
			data.add(new Instance(1.0, newInst));
			
			mauiFilter.input(data.instance(0));
			
			
			data = data.stringFreeStructure();
			if (debugMode) {
				System.err.println("-- Processing document: " + fileName);
			}
			Instance[] topRankedInstances = new Instance[topicsPerDocument];
			Instance inst;
			
			// Iterating over all extracted keyphrases (inst)
			while ((inst = mauiFilter.output()) != null) {
				
				int index = (int)inst.value(mauiFilter.getRankIndex()) - 1;
			
				if (index < topicsPerDocument) {
					topRankedInstances[index] = inst;
				}
			}
			
			if (debugMode) {
				System.err.println("-- Keyphrases and feature values:");
			}
			FileOutputStream out = null;
			PrintWriter printer = null; 

			if (!documentTopicsFile.exists()) {
				out = new FileOutputStream(documentTopicsFile);
				if (!documentEncoding.equals("default")) {
					printer = new PrintWriter(new OutputStreamWriter(out, documentEncoding));					
				} else {
					printer = new PrintWriter(out);
				}
			}
			
			double numExtracted = 0, numCorrect = 0;
			wikipedia = mauiFilter.getWikipedia();
			
			HashMap<Article, Integer> topics = null;
			
			if (printGraph) {
				topics = new HashMap<Article, Integer>();
			}
			
			int p = 0;
			String root = "";
			for (int i = 0; i < topicsPerDocument; i++) {
				if (topRankedInstances[i] != null) {
					if (!topRankedInstances[i].
							isMissing(topRankedInstances[i].numAttributes() - 1)) {
						numExtracted += 1.0;
					}
					if ((int)topRankedInstances[i].
							value(topRankedInstances[i].numAttributes() - 1) == 1) {
						numCorrect += 1.0;
					}
					if (printer != null) {
						String topic = topRankedInstances[i].
						stringValue(mauiFilter.getOutputFormIndex());
						printer.print(topic);
						
						if (printGraph) {
							
							Article article = wikipedia.getArticleByTitle(topic);
							if (article == null) {
								article = wikipedia.getMostLikelyArticle(topic,
										new CaseFolder());
							}
							if (article != null) {
								if (root == "") {
									root = article.getTitle();
								}
								topics.put(article, new Integer(p));
							} else {
								if (debugMode) {
									System.err.println("Couldn't find article for " + topic + " in " + documentTopicsFile);
								}
							}
							p++;
						}
						if (additionalInfo) {
							printer.print("\t");
							printer.print(topRankedInstances[i].
									stringValue(mauiFilter.getNormalizedFormIndex()));
							printer.print("\t");
							printer.print(Utils.
									doubleToString(topRankedInstances[i].
											value(mauiFilter.
													getProbabilityIndex()), 4));
						}
						printer.println();
					}
					if (debugMode) {
						System.err.println(topRankedInstances[i]);
					}
				}
			}
			
			if (printGraph) {
				String graphFile = documentTopicsFile.getAbsolutePath().replace(".key",".gv");
				computeGraph(topics, root, graphFile);
			}
			if (numExtracted > 0) {
				if (debugMode) {
					System.err.println("-- " + numCorrect + " correct");
				}
				double totalCorrect = mauiFilter.getTotalCorrect();
				correctStatistics.addElement(new Double(numCorrect));
				precisionStatistics.addElement(new Double(numCorrect/numExtracted));
				recallStatistics.addElement(new Double(numCorrect/totalCorrect));
				
			}
			if (printer != null) {
				printer.flush();
				printer.close();
				out.close();
			}
		}
		
		if (correctStatistics.size() != 0) {
			
		double[] st = new double[correctStatistics.size()];
		for (int i = 0; i < correctStatistics.size(); i++) {
			st[i] = correctStatistics.elementAt(i).doubleValue();
		}
		double avg = Utils.mean(st);
		double stdDev = Math.sqrt(Utils.variance(st));
		
		
		if (correctStatistics.size() == 1) {
			System.err.println("\n-- Evaluation results based on 1 document:");
				
		} else {
			System.err.println("\n-- Evaluation results based on " + correctStatistics.size() + " documents:");
		}
		System.err.println("Avg. number of correct keyphrases per document: " +
				Utils.doubleToString(avg, 2) + " +/- " + 
				Utils.doubleToString(stdDev, 2));
		
		
		st = new double[precisionStatistics.size()];
		for (int i = 0; i < precisionStatistics.size(); i++) {
			st[i] = precisionStatistics.elementAt(i).doubleValue();
		}
		double avgPrecision = Utils.mean(st);
		double stdDevPrecision = Math.sqrt(Utils.variance(st));
		
		System.err.println("Precision: " +
				Utils.doubleToString(avgPrecision*100, 2) + " +/- " + 
				Utils.doubleToString(stdDevPrecision*100, 2));
		
		st = new double[recallStatistics.size()];
		for (int i = 0; i < recallStatistics.size(); i++) {
			st[i] = recallStatistics.elementAt(i).doubleValue();
		}
		double avgRecall = Utils.mean(st);
		double stdDevRecall = Math.sqrt(Utils.variance(st));
		
		System.err.println("Recall: " +
				Utils.doubleToString(avgRecall*100, 2) + " +/- " + 
				Utils.doubleToString(stdDevRecall*100, 2));
		
		double fMeasure = 2*avgRecall*avgPrecision/(avgRecall + avgPrecision);
		System.err.println("F-Measure: " + Utils.doubleToString(fMeasure*100, 2));
		
		System.err.println("");
		}
		mauiFilter.batchFinished();
	}
	
	/**
	 * Prints out a plain-text representation of a graph representing the main topics of the document.
	 * The nodes are the topics and the edges are relations between them as computed using the Wikipedia Miner.
	 * Only possible if Wikipedia data is provided.
	 * 
	 * @param topics
	 * @param root
	 * @param outputFile
	 */
	public  void computeGraph(HashMap<Article, Integer> topics,
			String root, String outputFile) {
		FileOutputStream out;
		PrintWriter printer;
		try {
			
			if (debugMode) {
				System.err.println("Printing graph information into " + outputFile);
			}
			
			out = new FileOutputStream(outputFile);
			printer = new PrintWriter(out);

			printer.print("graph G {\n");

			printer.print("graph [root=\"" + root
					+ "\", outputorder=\"depthfirst\"];\n");

			HashSet<String> done = new HashSet<String>();
			double relatedness = 0;
			for (Article a : topics.keySet()) {
				int count = topics.get(a).intValue();
				if (count < 1) {
					printer.print("\"" + a.getTitle() + "\" [fontsize=22];\n");
				} else if (count < 3) {
					printer
							.print("\"" + a.getTitle()
									+ "\" [fontsize = 18];\n");
				} else if (count < 6) {
					printer
							.print("\"" + a.getTitle()
									+ "\" [fontsize = 14];\n");
				} else {
					printer
							.print("\"" + a.getTitle()
									+ "\" [fontsize = 12];\n");
				}

				for (Article c : topics.keySet()) {
					if (!c.equals(a)) {
						try {
							relatedness = a.getRelatednessTo(c);
							String relation = "\"" + a.getTitle() + "\" -- \""
									+ c.getTitle();
							String relation2 = "\"" + c.getTitle() + "\" -- \""
									+ a.getTitle();

							if (!done.contains(relation2)
									&& !done.contains(relation)) {
								done.add(relation2);
								done.add(relation);

								if (relatedness < 0.2) {
									printer.print(relation
											+ "\"[style=invis];\n");
								} else {
									printer.print(relation
											+ "\" [penwidth = \""
											+ (int) (relatedness * 10 - 0.2)
											+ "\"];\n");
								}
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
			printer.print("}\n");
			printer.close();
			out.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	
	/** 
	 * Loads the extraction model from the file.
	 */
	public void loadModel() throws Exception {
		
		BufferedInputStream inStream =
			new BufferedInputStream(new FileInputStream(modelName));
		ObjectInputStream in = new ObjectInputStream(inStream);
		mauiFilter = (MauiFilter)in.readObject();
		
		// If TFxIDF values are to be computed from the test corpus
		if (buildGlobalDictionary == true) {
			if (debugMode) {
				System.err.println("-- The global dictionaries will be built from this test collection..");
			}
			mauiFilter.globalDictionary = null;			
		}
		in.close();
	}
	
	/**
	 * The main method.  
	 */
	public static void main(String[] ops) {
		
		MauiTopicExtractor topicExtractor = new MauiTopicExtractor();
		try {
			// Checking and Setting Options selected by the user:
			topicExtractor.setOptions(ops);      
			System.err.print("Extracting keyphrases with options: ");
			
			// Reading Options, which were set above and output them:
			String[] optionSettings = topicExtractor.getOptions();
			for (int i = 0; i < optionSettings.length; i++) {
				System.err.print(optionSettings[i] + " ");
			}
			System.err.println();
			
			// Loading selected Model:
			System.err.println("-- Loading the model... ");
			topicExtractor.loadModel();
			
			// Extracting Keyphrases from all files in the selected directory
			topicExtractor.extractKeyphrases(topicExtractor.collectStems());
			
		} catch (Exception e) {
			
			// Output information on how to use this class
			e.printStackTrace();
			System.err.println(e.getMessage());
			System.err.println("\nOptions:\n");
			Enumeration<Option> en = topicExtractor.listOptions();
			while (en.hasMoreElements()) {
				Option option = (Option) en.nextElement();
				System.err.println(option.synopsis());
				System.err.println(option.description());
			}
		}
	}
}
