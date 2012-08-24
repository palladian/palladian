package ws.palladian.extraction.keyphrase.extractors;

/*
 * MauiTopicExtractor.java
 * Copyright (C) 2001-2009 Eibe Frank, Olena Medelyan
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import ws.palladian.external.maui.filters.MauiFilter;
import ws.palladian.external.maui.main.MauiModelBuilder;
import ws.palladian.external.maui.main.MauiTopicExtractor;
import ws.palladian.external.maui.stemmers.PorterStemmer;
import ws.palladian.external.maui.stemmers.Stemmer;
import ws.palladian.external.maui.stopwords.Stopwords;
import ws.palladian.external.maui.vocab.Vocabulary;
import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;

/**
 * <p>
 * Maui based keyphrase extractor. This class merges code from {@link MauiModelBuilder} and {@link MauiTopicExtractor}
 * and mapping it to our common interface.
 * </p>
 * 
 * TODO removed Wikipedia stuff for now, to fix build problems on Hudson.
 * 
 * @see <a href="http://code.google.com/p/maui-indexer/">Maui - Multi-purpose automatic topic indexing</a>
 * @author Philipp Katz
 */
public final class MauiKeyphraseExtractor extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MauiKeyphraseExtractor.class);

    /** Maui filter object */
    private MauiFilter mauiFilter = null;

    /** Name of model */
    private String modelName = "MauiKeyphraseExtractorModel";

    /** Document language */
    private String documentLanguage = "en";

    /** Directory where vocabularies are stored */
    private String vocabularyDirectory = "data/vocabularies";

    /** List of stopwords to be used */
    private Stopwords stopwords = new Stopwords("/maui/stopwords/stopwords_en.txt");

    private Vocabulary vocabulary = null;

    /** Build global dictionaries from the test set. */
    private boolean buildGlobalDictionary = false;

    /** Debugging mode? */
    private boolean debugMode = false;

    /** Stemmer to be used */
    private Stemmer stemmer = new PorterStemmer();

    private Instances data;

    /** Vocabulary name */
    private String vocabularyName = "none";

    /** Format of the vocabulary {skos,text} */
    private String vocabularyFormat = null;

    /** Number of documents which have been used for training. */
    private int numTrainDocs = 0;

    /** The maximum number of documents to use for training. Used to avoid out of memory errors. */
    // private static final int TRAIN_DOCUMENTS_LIMIT = 100;
    private static final int TRAIN_DOCUMENTS_LIMIT = Integer.MAX_VALUE;

    public MauiKeyphraseExtractor() {
        reset();
    }

    @Override
    public void reset() {
        FastVector atts = new FastVector(3);
        atts.addElement(new Attribute("filename", (FastVector)null));
        atts.addElement(new Attribute("document", (FastVector)null));
        atts.addElement(new Attribute("keyphrases", (FastVector)null));
        data = new Instances("keyphrase_training_data", atts, 0);
        mauiFilter = new MauiFilter();
        setBasicWikipediaFeatures(false);
        setAllWikipediaFeatures(false);
        numTrainDocs = 0;
    }

    @Override
    public void startTraining() {
        mauiFilter.setDebug(debugMode);
        mauiFilter.setVocabularyName(vocabularyName);
        mauiFilter.setVocabularyFormat(vocabularyFormat);
        try {
            mauiFilter.setInputFormat(data);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    @Override
    public void train(String inputText, Set<String> keyphrases) {
        if (numTrainDocs == TRAIN_DOCUMENTS_LIMIT) {
            LOGGER.info("Train limit reached");
            return;
        }

        double[] newInst = new double[3];

        newInst[0] = data.attribute(0).addStringValue("inputFile"); // just a dummy
        newInst[1] = data.attribute(1).addStringValue(inputText);
        newInst[2] = data.attribute(2).addStringValue(StringUtils.join(keyphrases, "\n"));

        data.add(new Instance(1.0, newInst));
        try {
            mauiFilter.input(data.instance(0));
        } catch (Exception e) {
            LOGGER.error(e);
        }
        data = data.stringFreeStructure();
        numTrainDocs++;
    }

    @Override
    public void endTraining() {
        try {
            mauiFilter.batchFinished();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        while ((mauiFilter.output()) != null) {
            // noop
        }
        try {
            saveModel();
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    @Override
    public boolean needsTraining() {
        return true;
    }

    @Override
    public String getExtractorName() {
        return "Maui";
    }

    /**
     * Saves the extraction model to the file.
     */
    private void saveModel() throws Exception {
        BufferedOutputStream bufferedOut = new BufferedOutputStream(new FileOutputStream(modelName));
        ObjectOutputStream out = new ObjectOutputStream(bufferedOut);
        out.writeObject(mauiFilter);
        out.flush();
        out.close();
    }

    /**
     * Loads the extraction model from the file.
     */
    private void loadModel() {
        // mauiFilter = null;

        try {

            BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(modelName));
            ObjectInputStream in = new ObjectInputStream(inStream);
            mauiFilter = (MauiFilter)in.readObject();

            // If TFxIDF values are to be computed from the test corpus
            if (buildGlobalDictionary == true) {
                mauiFilter.globalDictionary = null;
            }
            in.close();

        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } catch (ClassNotFoundException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public void startExtraction() {
        loadModel();
    }

    @Override
    public List<Keyphrase> extract(String inputText) {

        List<Keyphrase> result = new ArrayList<Keyphrase>();

        double[] newInst = new double[3];
        newInst[0] = data.attribute(0).addStringValue("inputFile"); // just a dummy
        newInst[1] = data.attribute(1).addStringValue(inputText);
        newInst[2] = Instance.missingValue();
        data.add(new Instance(1.0, newInst));

        try {
            mauiFilter.input(data.instance(0));
        } catch (Exception e) {
            LOGGER.error(e);
        }

        data = data.stringFreeStructure();

        int topicsPerDocument = getKeyphraseCount();
        Instance[] topRankedInstances = new Instance[topicsPerDocument];
        Instance inst;

        // Iterating over all extracted keyphrases (inst)
        while ((inst = mauiFilter.output()) != null) {

            int index = (int)inst.value(mauiFilter.getRankIndex()) - 1;
            if (index < topicsPerDocument) {
                topRankedInstances[index] = inst;
            }
        }

        for (int i = 0; i < topicsPerDocument; i++) {
            if (topRankedInstances[i] != null) {

                // Candidate_original is at position 1
                // String text = topRankedInstances[i].stringValue(1);
                String text = topRankedInstances[i].stringValue(mauiFilter.getOutputFormIndex());
                double probability = topRankedInstances[i].value(mauiFilter.getProbabilityIndex());

                result.add(new Keyphrase(text, probability));
            }
        }

        try {
            mauiFilter.batchFinished();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return result;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////
    // Setters for various Maui properties; these just delegate to the MauiFilter instance
    // ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Use basic features
     * TFxIDF & First Occurrence
     * 
     * @param useBasicFeatures
     * @see maui.filters.MauiFilter#setBasicFeatures(boolean)
     */
    public void setBasicFeatures(boolean useBasicFeatures) {
        mauiFilter.setBasicFeatures(useBasicFeatures);
    }

    /**
     * @param classifier
     * @see maui.filters.MauiFilter#setClassifier(weka.classifiers.Classifier)
     */
    public void setClassifier(Classifier classifier) {
        mauiFilter.setClassifier(classifier);
    }

    /**
     * Use domain keyphraseness feature
     * 
     * @param useKeyphrasenessFeature
     * @see maui.filters.MauiFilter#setKeyphrasenessFeature(boolean)
     */
    public void setKeyphrasenessFeature(boolean useKeyphrasenessFeature) {
        mauiFilter.setKeyphrasenessFeature(useKeyphrasenessFeature);
    }

    /**
     * Use frequency features
     * TF & IDF additionally
     * 
     * @param useFrequencyFeatures
     * @see maui.filters.MauiFilter#setFrequencyFeatures(boolean)
     */
    public void setFrequencyFeatures(boolean useFrequencyFeatures) {
        mauiFilter.setFrequencyFeatures(useFrequencyFeatures);
    }

    /**
     * Use occurrence position features
     * LastOccurrence & Spread
     * 
     * @param usePositionsFeatures
     * @see maui.filters.MauiFilter#setPositionsFeatures(boolean)
     */
    public void setPositionsFeatures(boolean usePositionsFeatures) {
        mauiFilter.setPositionsFeatures(usePositionsFeatures);
    }

    /**
     * Use thesaurus features
     * 
     * @param useThesaurusFeatures
     * @see maui.filters.MauiFilter#setThesaurusFeatures(boolean)
     */
    public void setThesaurusFeatures(boolean useThesaurusFeatures) {
        mauiFilter.setThesaurusFeatures(useThesaurusFeatures);
    }

    /**
     * Use length feature
     * 
     * @param useLengthFeature
     * @see maui.filters.MauiFilter#setLengthFeature(boolean)
     */
    public void setLengthFeature(boolean useLengthFeature) {
        mauiFilter.setLengthFeature(useLengthFeature);
    }

    /**
     * Use basic Wikipedia features
     * Wikipedia keyphraseness & Total Wikipedia keyphraseness
     * 
     * @param useBasicWikipediaFeatures
     * @see maui.filters.MauiFilter#setBasicWikipediaFeatures(boolean)
     */
    public void setBasicWikipediaFeatures(boolean useBasicWikipediaFeatures) {
        mauiFilter.setBasicWikipediaFeatures(useBasicWikipediaFeatures);
    }

    /**
     * Use all Wikipedia features
     * Inverse Wikipedia frequency & Semantic relatedness
     * 
     * @param useAllWikipediaFeatures
     * @see maui.filters.MauiFilter#setAllWikipediaFeatures(boolean)
     */
    public void setAllWikipediaFeatures(boolean useAllWikipediaFeatures) {
        mauiFilter.setAllWikipediaFeatures(useAllWikipediaFeatures);
    }

    /**
     * Minimum number of the context articles
     * 
     * @param contextSize
     * @see maui.filters.MauiFilter#setContextSize(int)
     */
    public void setContextSize(int contextSize) {
        mauiFilter.setContextSize(contextSize);
    }

    /**
     * Minimum sense probability or commonness
     * 
     * @param minSenseProbability
     * @see maui.filters.MauiFilter#setMinSenseProbability(double)
     */
    public void setMinSenseProbability(double minSenseProbability) {
        mauiFilter.setMinSenseProbability(minSenseProbability);
    }

    /**
     * Minimum keyphraseness of a string
     * 
     * @param minKeyphraseness
     * @see maui.filters.MauiFilter#setMinKeyphraseness(double)
     */
    public void setMinKeyphraseness(double minKeyphraseness) {
        mauiFilter.setMinKeyphraseness(minKeyphraseness);
    }

    /**
     * @param stopwords
     * @see maui.filters.MauiFilter#setStopwords(maui.stopwords.Stopwords)
     */
    public void setStopwords(Stopwords stopwords) {
        mauiFilter.setStopwords(stopwords);
    }

    /**
     * @param stemmer
     * @see maui.filters.MauiFilter#setStemmer(maui.stemmers.Stemmer)
     */
    public void setStemmer(Stemmer stemmer) {
        this.stemmer = stemmer;
        mauiFilter.setStemmer(stemmer);
    }

    /**
     * Minimum number of occurences of a phrase
     * 
     * @param minNumOccur
     * @see maui.filters.MauiFilter#setMinNumOccur(int)
     */
    public void setMinNumOccur(int minNumOccur) {
        mauiFilter.setMinNumOccur(minNumOccur);
    }

    /**
     * Maximum length of phrases
     * 
     * @param maxPhraseLength
     * @see maui.filters.MauiFilter#setMaxPhraseLength(int)
     */
    public void setMaxPhraseLength(int maxPhraseLength) {
        mauiFilter.setMaxPhraseLength(maxPhraseLength);
    }

    /**
     * Minimum length of phrases
     * 
     * @param minPhraseLength
     * @see maui.filters.MauiFilter#setMinPhraseLength(int)
     */
    public void setMinPhraseLength(int minPhraseLength) {
        mauiFilter.setMinPhraseLength(minPhraseLength);
    }

    /**
     * Specifies document language (en, es, de, fr).
     * 
     * @param documentLanguage
     * @see maui.filters.MauiFilter#setDocumentLanguage(java.lang.String)
     */
    public void setDocumentLanguage(String documentLanguage) {
        mauiFilter.setDocumentLanguage(documentLanguage);
    }

    /**
     * @param debugMode
     * @see maui.filters.MauiFilter#setDebug(boolean)
     */
    public void setDebug(boolean debugMode) {
        this.debugMode = debugMode;
        mauiFilter.setDebug(debugMode);
    }

    /**
     * Specifies vocabulary name (e.g. agrovoc or none).
     * 
     * @param vocabularyName
     * @see maui.filters.MauiFilter#setVocabularyName(java.lang.String)
     */
    public void setVocabularyName(String vocabularyName) {
        this.vocabularyName = vocabularyName;
        mauiFilter.setVocabularyName(vocabularyName);
        if (!vocabularyName.equals("none") && !vocabularyName.equals("wikipedia")) {
            loadThesaurus(stemmer, stopwords, vocabularyDirectory);
            mauiFilter.setVocabulary(vocabulary);
        }
    }

    /**
     * Specifies format of vocabulary (text or skos).
     * 
     * @param vocabularyFormat
     * @see maui.filters.MauiFilter#setVocabularyFormat(java.lang.String)
     */
    public void setVocabularyFormat(String vocabularyFormat) {
        this.vocabularyFormat = vocabularyFormat;
        mauiFilter.setVocabularyFormat(vocabularyFormat);
    }

    // /**
    // * @param wikipedia
    // * @see maui.filters.MauiFilter#setWikipedia(org.wikipedia.miner.model.Wikipedia)
    // */
    // public void setWikipedia(Wikipedia wikipedia) {
    // mauiFilter.setWikipedia(wikipedia);
    // }

    // /**
    // * @param wikipediaServer
    // * @param wikipediaDatabase
    // * @param cacheData
    // * @param wikipediaDataDirectory
    // * @see maui.filters.MauiFilter#setWikipedia(java.lang.String, java.lang.String, boolean, java.lang.String)
    // */
    // public void setWikipedia(String wikipediaServer, String wikipediaDatabase, boolean cacheData,
    // String wikipediaDataDirectory) {
    // mauiFilter.setWikipedia(wikipediaServer, wikipediaDatabase, cacheData, wikipediaDataDirectory);
    // }

    /**
     * Build global dictionaries from the test set.
     * 
     * @param buildGlobalDictionary
     */
    public void setBuildGlobalDictionary(boolean buildGlobalDictionary) {
        this.buildGlobalDictionary = buildGlobalDictionary;
    }

    /**
     * Specifies name of model.
     * 
     * @param modelName
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    private void loadThesaurus(Stemmer st, Stopwords sw, String vocabularyDirectory) {
        if (vocabulary != null) {
            return;
        }

        try {

            LOGGER.debug("--- Loading the vocabulary...");
            vocabulary = new Vocabulary(vocabularyName, vocabularyFormat, vocabularyDirectory);
            vocabulary.setStemmer(st);
            vocabulary.setStopwords(sw);
            vocabulary.setDebug(debugMode);
            vocabulary.setLanguage(documentLanguage);
            vocabulary.initialize();

        } catch (Exception e) {
            // System.err.println("Failed to load thesaurus!");
            // e.printStackTrace();
            LOGGER.error("Failed to load thesaurus!", e);
        }

    }

}
