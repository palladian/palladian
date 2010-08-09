package tud.iir.extraction.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import tud.iir.classification.FeatureObject;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;
import tud.iir.knowledge.Entity;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ChunkFactory;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.chunk.NBestChunker;
import com.aliasi.coref.EnglishMentionFactory;
import com.aliasi.coref.Mention;
import com.aliasi.coref.MentionFactory;
import com.aliasi.coref.WithinDocCoref;
import com.aliasi.dict.ApproxDictionaryChunker;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.TrieDictionary;
import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.spell.FixedWeightEditDistance;
import com.aliasi.spell.WeightedEditDistance;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.FastCache;
import com.aliasi.util.ScoredObject;
import com.representqueens.lingua.en.Fathom;
import com.representqueens.lingua.en.Readability;
import com.representqueens.lingua.en.Fathom.Stats;

/**
 * EventFeatureExtractor to extract Features from Events
 * 
 * @author Martin Wunderwald
 */
public class EventFeatureExtractor {

	/** model file for lingpipe rescoring chunker from muc6 */
	static final File modelFile = new File("data/models/ne-en-news-muc6.AbstractCharLmRescoringChunker");

	/** brown hidden markov model */
	static final String BROWN_HMM = "pos-en-general-brown.HiddenMarkovModel";

	static final Pattern MALE_PRONOUNS = Pattern
			.compile("\\b(He|he|Him|him|His|his)\\b");
	static final Pattern FEMALE_PRONOUNS = Pattern
			.compile("\\b(She|she|Her|her|Hers|hers)\\b");

	/** the logger for this class */
	private static final Logger LOGGER = Logger.getLogger(EventExtractor.class);

	/**
	 * sets the features of an event
	 * @param event
	 */
	public static void setFeatures(Event event) {

		setTextFeatures(event);
		setEntityFeatures(event);
	}

	
	/**
	 * sets the entityFeatures for a whole Map of Events
	 * @param eventMap
	 */
	private static void setEntityFeatures(HashMap<String, Event> eventMap) {

		for (Entry<String, Event> entry : eventMap.entrySet()) {
			Event event = entry.getValue();
			if (event != null && event.getText() != null) {
				setEntityFeatures(event);
			}
		}

	}

	/**
	 * sets text features for event
	 * @param event
	 */
	private static void setTextFeatures(Event event) {

		Stats fs = Fathom.analyze(event.getText());

		HashMap<String, Double> features = new HashMap<String, Double>();

		Map<String, Integer> uniqueWords = fs.getUniqueWords();

		features.put("UniqueWords", (double) uniqueWords.size());
		features.put("WordCount", (double) fs.getNumWords());
		features.put("SentenceCount", (double) fs.getNumSentences());
		features.put("SyllablesCount", (double) fs.getNumSyllables());

		features.put("ComplexWordPercentage", (double) Readability
				.percentComplexWords(fs));
		// Gunning-Fog Score
		features.put("FogIndex", (double) Readability.calcFog(fs));
		// Flesch-Kincaid Reading Ease
		features.put("Flesh", (double) Readability.calcFlesch(fs));
		features.put("WordsPerSentence", (double) Readability
				.wordsPerSentence(fs));

		event.setFeatures(new FeatureObject(features));

	}
	

	/**
	 * sets the EntityFeatures for a given event
	 * 
	 * @param event
	 */
	private static void setEntityFeatures(Event event) {

		
		HashMap<Integer, Set<Chunk>> corefChunkSet = getCoreferenceChunks(event);
		
		//setting coreferenceChunkSet
		event.setEntityChunks(corefChunkSet);

		HashMap<Integer, FeatureObject> featureObjects = new HashMap<Integer, FeatureObject>();

		for (Entry<Integer, Set<Chunk>> entry : corefChunkSet.entrySet()) {
			featureObjects.put(entry.getKey(), calculateEntityChunkSetFeatures(
					event, entry.getValue()));
		}

		//setting entity features for the chunks
		event.setEntityFeatures(featureObjects);

	}
	

	
	/**
	 * performs coreference chunking on an Event
	 * 
	 * @param event
	 * @return
	 */
	private static HashMap<Integer, Set<Chunk>> getCoreferenceChunks(Event event){
	
		
		LOGGER.info("performing coreference: " + event.getUrl());
		
		MentionFactory mf = new EnglishMentionFactory();
		WithinDocCoref coref = new WithinDocCoref(mf);

		Set<Chunk> chunkSet = getEntityChunks(event);

		addPronouns(MALE_PRONOUNS, "MALE_PRONOUN", event.getText(), chunkSet);
		addPronouns(FEMALE_PRONOUNS, "FEMALE_PRONOUN", event.getText(),
				chunkSet);

		HashMap<Integer, Set<Chunk>> corefChunkMap = new HashMap<Integer, Set<Chunk>>();

		Iterator<Chunk> it = chunkSet.iterator();

		while (it.hasNext()) {

			Chunk chunk = it.next();

			String phrase = event.getText().substring(chunk.start(),
					chunk.end()).toLowerCase();

			Mention mention = mf.create(phrase, chunk.type());
			int mentionId = coref.resolveMention(mention, 1);

			if (corefChunkMap.containsKey(mentionId)) {
				Set<Chunk> tmpChunkSet = corefChunkMap.get(mentionId);
				tmpChunkSet.add(chunk);
				corefChunkMap.put(mentionId, tmpChunkSet);

			} else {
				Set<Chunk> tmpChunkSet = new HashSet<Chunk>();
				tmpChunkSet.add(chunk);
				corefChunkMap.put(mentionId, tmpChunkSet);

			}

		}
		
		return corefChunkMap;
	}
	
	
	

	/**
	 * calculates features for a Set of Chunks
	 * 
	 * @param event
	 * @param chunkSet
	 * @return
	 */
	private static FeatureObject calculateEntityChunkSetFeatures(Event event,
			Set<Chunk> chunkSet) {

		HashMap<String, Double> featureMap = new HashMap<String, Double>();

		double textEntityCount = 0.0;
		double titleEntityCount = 0.0;
		double density = 0.0;
		double type = 0.0;
		double start = 0.0;
		double end = 0.0;

		for (Chunk chunk : chunkSet) {
			String phrase = event.getText().substring(chunk.start(),
					chunk.end()).toLowerCase();

			if (phrase.length() > 3) {

				textEntityCount += 1.0;

				if (titleEntityCount == 0.0) {
					titleEntityCount = (double) countEntityOccurrences(
							new Entity(phrase), event.getTitle(), false);
				}

				if (chunk.type().equals("PERSON")) {
					type += 1.0;
				} else if (chunk.type().equals("ORGANIZATION")) {
					type += 2.0;

				} else if (chunk.type().equals("LOCATION")) {
					type += 3.0;
				}
				start += chunk.start();
				end += chunk.end();

			}

		}

		featureMap.put("titleEntityCount", titleEntityCount);
		featureMap.put("textEntityCount", textEntityCount);
		featureMap.put("density", density);
		featureMap.put("type", type / chunkSet.size());
		featureMap.put("start", start / chunkSet.size());
		featureMap.put("end", end / chunkSet.size());

		return new FeatureObject(featureMap);

	}


	/**
	 * simply extracts chunks
	 * @param event
	 * @return
	 */
	private static Set<Chunk> getEntityChunks(Event event) {

		Chunker mEntityChunker;

		try {
			mEntityChunker = (Chunker) AbstractExternalizable
					.readObject(modelFile);

			Chunking mentionChunking = mEntityChunker.chunk(event.getText());

			Set<Chunk> chunkSet = new TreeSet<Chunk>(
					Chunk.LONGEST_MATCH_ORDER_COMPARATOR);
			chunkSet.addAll(mentionChunking.chunkSet());

			return chunkSet;
		} catch (IOException e) {
			LOGGER.error(e);
		} catch (ClassNotFoundException e) {
			LOGGER.error(e);
		}
		return null;
	}
	
	
	/**
	 * Extract a list of part-of-speech tags from a sentence.
	 * 
	 * @param sentence
	 *            - The sentence
	 * @return The part of speach tags.
	 */
	private static Set<Chunk> getPhraseChunks(String sentence) {

		ObjectInputStream oi = null;

		try {
			HiddenMarkovModel hmm = null;

			if (DataHolder.getInstance().containsDataObject(BROWN_HMM)) {
				hmm = (HiddenMarkovModel) DataHolder.getInstance()
						.getDataObject(BROWN_HMM);
			} else {
				oi = new ObjectInputStream(new FileInputStream("data"
						+ File.separator + "models" + File.separator
						+ BROWN_HMM));
				hmm = (HiddenMarkovModel) oi.readObject();
				DataHolder.getInstance().putDataObject(BROWN_HMM, hmm);
			}

			int cacheSize = Integer.valueOf(100);
			FastCache<String, double[]> cache = new FastCache<String, double[]>(
					cacheSize);

			// read HMM for pos tagging

			// construct chunker
			HmmDecoder posTagger = new HmmDecoder(hmm, null, cache);
			TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
			PhraseChunker chunker = new PhraseChunker(posTagger,
					tokenizerFactory);

			// apply chunker and pos tagger
			String[] tokens = tokenizerFactory.tokenizer(
					sentence.toCharArray(), 0, sentence.length()).tokenize();
			List<String> tokenList = Arrays.asList(tokens);
			Tagging<String> tagging = posTagger.tag(tokenList);

			for (int j = 0; j < tokenList.size(); ++j) {
				LOGGER.trace(tokens[j] + "/" + tagging.tag(j) + " ");
			}
			LOGGER.trace("");

			return chunker.chunk(sentence).chunkSet();

			/*
			 * // first get the tokens char[] cs = sentence.toCharArray();
			 * Tokenizer tokenizer = tokenizer_Factory.tokenizer(cs, 0,
			 * cs.length); String[] tokens = tokenizer.tokenize();
			 * 
			 * // then get the tags
			 * 
			 * firstBest(Arrays.asList(tokens),decoder);
			 */
		} catch (IOException ie) {
			LOGGER.error("IO Error: " + ie.getMessage());
		} catch (ClassNotFoundException ce) {
			LOGGER.error("Class error: " + ce.getMessage());
		} finally {
			if (oi != null) {
				try {
					oi.close();
				} catch (IOException ie) {
					LOGGER.error(ie.getMessage());
				}
			}
		}
		return null;
	}


	/**
	 * Return the set of occurrences of a certain entity in a provided string,
	 * including different spellings of the entity.
	 * 
	 * An optional parameter allows to specify whether the entity might be
	 * prefixed by "the", "an" or "a".
	 *
	 * @param entity
	 * @param text
	 * @param includePrefixes
	 * @return
	 */
	private static Set<Chunk> getDictionaryChunksForEntity(Entity entity,
			String text, boolean includePrefixes) {

		// lowercase everything

		String entityName = entity.getName().toLowerCase();

		ArrayList<String> prefixes = new ArrayList<String>();
		prefixes.add("the");
		prefixes.add("an");
		prefixes.add("a");

		ArrayList<String> synonyms = new ArrayList<String>();
		// synonyms.add(entity.getName().toLowerCase());

		// Approximate Dictionary-Based Chunking

		double maxDistance = 2.0;

		TrieDictionary<String> dict = new TrieDictionary<String>();

		// matches
		dict
				.addEntry(new DictionaryEntry<String>(entityName, entity
						.getName()));
		if (includePrefixes) {
			for (String prefix : prefixes) {
				dict.addEntry(new DictionaryEntry<String>(prefix + " "
						+ entityName, entity.getName()));
			}
		}

		// synonyms
		for (String synonym : synonyms) {
			dict
					.addEntry(new DictionaryEntry<String>(synonym, entity
							.getName()));
			if (includePrefixes) {
				for (String prefix : prefixes) {
					dict.addEntry(new DictionaryEntry<String>(prefix + " "
							+ synonym, entity.getName()));
				}
			}
		}

		WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -1,
				-1, -1, Double.NaN);

		Chunker chunker = new ApproxDictionaryChunker(dict,
				IndoEuropeanTokenizerFactory.INSTANCE, editDistance,
				maxDistance);

		return chunker.chunk(text).chunkSet();
	}

	/**
	 * Split a provided string into sentences and return a set of sentence
	 * chunks.
	 */
	private static Set<Chunk> getSentenceChunks(String text) {

		if (text == null)
			return null;

		TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
		SentenceModel sentenceModel = new IndoEuropeanSentenceModel();

		SentenceChunker sentenceChunker = new SentenceChunker(tokenizerFactory,
				sentenceModel);
		Chunking chunking = sentenceChunker.chunk(text.toCharArray(), 0, text
				.length());

		return chunking.chunkSet();
	}

	
	
	private static void addPronouns(Pattern pattern, String tag,
			String sentenceText, Set<Chunk> chunkSet) {
		java.util.regex.Matcher matcher = pattern.matcher(sentenceText);
		int pos = 0;
		while (matcher.find(pos)) {
			Chunk proChunk = ChunkFactory.createChunk(matcher.start(), matcher
					.end(), tag);
			// incredibly inefficient quadratic algorithm here, but bounded by
			// sentence
			Iterator<Chunk> it = chunkSet.iterator();
			while (it.hasNext()) {
				Chunk chunk = it.next();
				if (overlap(chunk.start(), chunk.end(), proChunk.start(),
						proChunk.end()))
					it.remove();
			}
			chunkSet.add(proChunk);
			pos = matcher.end();
		}
	}

	@SuppressWarnings("unused")
	private static String resolveChunkSet(Set<Chunk> chunkSet, Event event) {
		Iterator<Chunk> it = chunkSet.iterator();
		StringBuilder sb = new StringBuilder();
		while (it.hasNext()) {

			Chunk chunk = it.next();

			String phrase = event.getText().substring(chunk.start(),
					chunk.end()).toLowerCase();

			sb.append(phrase + " ");
		}

		return sb.toString();
	}

	private static boolean overlap(int start1, int end1, int start2, int end2) {
		return java.lang.Math.max(start1, start2) < java.lang.Math.min(end1,
				end2);
	}

	@SuppressWarnings("unused")
	private static double calculateChunkDensity(Chunk chunk,
			Set<Chunk> chunkSet, int window) {

		double density = 0.0;

		for (Chunk cuk : chunkSet) {
			if ((cuk.start() > chunk.start() - window)
					&& (cuk.start() < chunk.start())
					&& cuk.type().equals("LOCATION")) {
				density++;
			}
			if ((cuk.end() < chunk.end() + window) && (cuk.end() > chunk.end())
					&& cuk.type().equals("LOCATION")) {
				density++;
			}
		}

		return density;
	}

	
	/**
	 * counts Entity occurences with the help of dictonaryChunker
	 * 
	 * @param entity
	 *            - the entity
	 * @param article
	 *            - the article
	 * @param includePrefixes
	 * @return count of occurrences
	 */
	private static int countEntityOccurrences(Entity entity, String article,
			boolean includePrefixes) {

		return getDictionaryChunksForEntity(entity, article, false).size();

	}

	/**
	 * first Best Namend Entity Chunking
	 * 
	 * @param text
	 *            - string to chunk
	 * @return
	 */
	@SuppressWarnings(value = { "unused" })
	private static Iterator<Chunk> firstBestNEChunking(String text) {

		Chunker chunker;
		try {
			chunker = (Chunker) AbstractExternalizable.readObject(modelFile);
			return chunker.chunk(text).chunkSet().iterator();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * confidence Namend Entity Chunking
	 * 
	 * @param text
	 *            - string to chunk
	 * @return
	 */
	@SuppressWarnings(value = { "unused" })
	private static Iterator<Chunk> confidenceNEChunking(String text) {

		ConfidenceChunker chunker;
		try {
			chunker = (ConfidenceChunker) AbstractExternalizable
					.readObject(modelFile);
			char[] cs = text.toCharArray();

			return chunker.nBestChunks(cs, 0, cs.length, 30);

		} catch (IOException e) {
			e.printStackTrace();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();

		}
		return null;

	}

	/**
	 * n-Best Namend Entity Chunking
	 * 
	 * @param text
	 *            - string to chunk
	 * @return
	 */
	@SuppressWarnings(value = { "unused" })
	private static Iterator<ScoredObject<Chunking>> nBestNEChunking(String text) {

		NBestChunker chunker;
		try {
			chunker = (NBestChunker) AbstractExternalizable
					.readObject(modelFile);
			char[] cs = text.toCharArray();

			return chunker.nBest(cs, 0, cs.length, 5);

		} catch (IOException e) {
			e.printStackTrace();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();

		}

		return null;
	}

	/**
	 * aggregates events from SearchEngines by a given query
	 * 
	 * @param query
	 *            - the query
	 * @return
	 */
	public static HashMap<String, Event> aggregateEvents(String query) {

		EventAggregator ea = new EventAggregator();
		ea.setMaxThreads(5);
		ea.setResultCount(10);
		ea.setQuery(query);
		ea.aggregate();

		return ea.getEventmap();
	}

	/**
	 * builds a searchengine query by given triple of whos,wheres,whats
	 * 
	 * @param whos
	 * @param wheres
	 * @param whats
	 * @return
	 */
	private static String buildQuery(ArrayList<String> whos,
			ArrayList<String> wheres, ArrayList<String> whats) {

		StringBuilder sb = new StringBuilder();

		sb.append("news ");

		for (String who : whos) {
			sb.append(who + " ");
		}
		for (String where : wheres) {
			sb.append(where + " ");
		}
		for (String what : whats) {
			sb.append(what + " ");
		}
		return sb.toString();
	}

	/**
	 * writes events to CSV file for training the classifier
	 * 
	 * @param eventMap
	 * @param whos
	 * @param wheres
	 * @param whats
	 * @param append
	 */
	public static void writeCSV(HashMap<String, Event> eventMap,
			ArrayList<String> whos, ArrayList<String> wheres,
			ArrayList<String> whats, boolean append) {

		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter("data/features/data.csv", append);

			String separator = ";";

			if (!append) {
				fileWriter.write("\"entity\"" + separator + "\"inTitle\""
						+ separator + "\"density\"" + separator + "\"start\""
						+ separator + "\"inText\"" + separator + "\"type\""
						+ separator + "\"end\"" + separator + "\"class\""
						+ separator + "\n");
				fileWriter.flush();
			}
			for (Entry<String, Event> eentry : eventMap.entrySet()) {
				Event ev = eentry.getValue();
				if (ev != null && ev.getText() != null) {

					HashMap<Integer, FeatureObject> featureMap = ev
							.getEntityFeatures();
					HashMap<Integer, Set<Chunk>> chunkMap = ev
							.getEntityChunks();
					// hm.put(url, e);

					if (chunkMap.size() > 0
							&& featureMap.size() == chunkMap.size()) {

						for (Entry<Integer, FeatureObject> eeentry : featureMap
								.entrySet()) {

							FeatureObject fo = eeentry.getValue();
							Integer id = eeentry.getKey();

							Set<Chunk> chunkSet = chunkMap.get(id);

							fileWriter.write(id + separator);

							for (Chunk entity : chunkSet) {

								if (wheres.contains(ev.getText().substring(
										entity.start(), entity.end()))) {
									fo.setClassAssociation(2);
								}
								if (whos.contains(ev.getText().substring(
										entity.start(), entity.end()))) {
									fo.setClassAssociation(1);
								}
								if (whats.contains(ev.getText().substring(
										entity.start(), entity.end()))) {
									fo.setClassAssociation(3);
								}
							}
							for (Double d : fo.getFeatures()) {
								fileWriter.write(d.toString() + separator);
							}
							if (fo.getClassAssociation() == 1) {
								fileWriter.write("WHO;\n");
							} else if (fo.getClassAssociation() == 2) {
								fileWriter.write("WHERE;\n");
							} else if (fo.getClassAssociation() == 3) {
								fileWriter.write("WHAT;\n");
							} else if (fo.getClassAssociation() == 4) {
								fileWriter.write("WHEN;\n");
							} else {
								fileWriter.write("ELSE;\n");
							}
							fileWriter.flush();
						}
					}

				}
				ev = null;
			}

			// fileWriter.write("\n");
			fileWriter.flush();
			fileWriter.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * featureObject 2 HashMap function
	 * 
	 * @param fo -  the featureObject
	 * @return hashMap
	 */
	@SuppressWarnings("unused")
	private static HashMap<String, Double> fo2map(FeatureObject fo) {

		List<String> fn = Arrays.asList(fo.getFeatureNames());
		List<Double> fv = Arrays.asList(fo.getFeatures());

		HashMap<String, Double> hm = new HashMap<String, Double>();

		for (int i = 0; i < fn.size(); i++) {
			hm.put(fn.get(i), fv.get(i));
		}
		return hm;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Event e =
		// ee.extractEventFromURL("http://www.bbc.co.uk/news/world-asia-pacific-10707945");

		// findWhat(e);
		// NER + Feature Extraction of each Entity
		// counting entityoccurence with dictChunker
		// setEntityFeatures(e);

		// findHotties(e);

		/*
		 * queries.add("smoke moscow wildfire");
		 * queries.add("mexican border prison dead 14");
		 */

		ArrayList<String> whos = new ArrayList<String>();
		whos.add("medics");
		whos.add("workers");
		// whos.add("foreign");
		whos.add("british");
		whos.add("taliban");
		whos.add("Karen Woo");
		whos.add("doctors");
		whos.add("Doctors");

		ArrayList<String> wheres = new ArrayList<String>();
		wheres.add("Afghanistan");
		wheres.add("afghanistan");
		wheres.add("Badakhshan");
		wheres.add("province");

		ArrayList<String> whats = new ArrayList<String>();
		whats.add("killed");
		whats.add("kill");
		whats.add("died");

		// Michelle Obama visits Spanish city of Ronda
		ArrayList<String> whos1 = new ArrayList<String>();
		whos1.add("Michelle Obama");
		whos1.add("Sasha");
		whos1.add("dautgher");
		whos1.add("first lady");

		ArrayList<String> wheres1 = new ArrayList<String>();
		wheres1.add("Ronda");
		wheres1.add("Spanish");
		wheres1.add("city");
		wheres1.add("spain");
		wheres1.add("Casa del Rey Moro");

		ArrayList<String> whats1 = new ArrayList<String>();
		whats1.add("visits");
		whats1.add("visit");

		// russia fire
		ArrayList<String> whos2 = new ArrayList<String>();
		whos2.add("Putin");

		ArrayList<String> wheres2 = new ArrayList<String>();
		wheres2.add("russia");
		wheres2.add("moscow");

		ArrayList<String> whats2 = new ArrayList<String>();
		whats2.add("fire");
		whats2.add("fires");
		whats2.add("wildfires");

		HashMap<String, Event> eventMap = aggregateEvents(buildQuery(whos,
				wheres, whats));

		HashMap<String, Event> eventMap1 = aggregateEvents(buildQuery(whos1,
				wheres1, whats1));
		HashMap<String, Event> eventMap2 = aggregateEvents(buildQuery(whos2,
				wheres2, whats2));

		StopWatch sw = new StopWatch();
		sw.start();
		setEntityFeatures(eventMap);
		setEntityFeatures(eventMap1);
		setEntityFeatures(eventMap2);

		writeCSV(eventMap, whos, wheres, whats, false);
		writeCSV(eventMap1, whos1, wheres1, whats1, true);
		writeCSV(eventMap2, whos2, wheres2, whats2, true);

		sw.stop();
		LOGGER.info("time elapsed: " + sw.getElapsedTimeString());

		
		getPhraseChunks("This is a test late at night.");
		getSentenceChunks("");
		
	}

}
