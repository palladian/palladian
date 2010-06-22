package tud.iir.extraction.snippet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.control.Controller;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Snippet;
import tud.iir.knowledge.Source;
import tud.iir.web.AggregatedResult;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.WebResult;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.representqueens.lingua.en.Fathom;
import com.representqueens.lingua.en.Readability;
import com.representqueens.lingua.en.Fathom.Stats;

/**
 * Given an extracted snippet, a feature vector is generated.
 * 
 * This class is described in detail in "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis,
 * Technische Universität Dresden, April 2010".
 * 
 * @author Christopher Friedrich
 */
public class SnippetFeatureExtractor {

    public static void setFeatures(Snippet snippet) {
        setIndexFeatures(snippet);
        setWebpageFeatures(snippet);
        setSnippetReadabilityFeatures(snippet);
        setSyntacticSnippetFeatures(snippet);
        setSemanticSnippetFeatures(snippet);
    }

    private static void setIndexFeatures(Snippet snippet) {

        snippet.setFeature("AggregatedRank", snippet.getAggregatedResult().getAggregatedRank());

        for (int se : SourceRetrieverManager.getSearchEngines()) {
            if (snippet.getAggregatedResult().getSearchEngines().contains(se)) {
                snippet.setFeature("SearchEngine" + se, 1.0);
            } else {
                snippet.setFeature("SearchEngine" + se, 0.0);
            }

            // TODO: query-type
            // snippet.setFeature("SearchEngineQueryType", 0);

            // TODO: (rank-)change-frequency
            // snippet.setFeature("SearchEngineChangeFrequency", 0);

            // TODO: index-type
            // snippet.setFeature("SearchEngineType", classifySearchEngine(se));
        }
    }

    private static void setWebpageFeatures(Snippet snippet) {

        // retrieve pagerank from google toolbar service
        snippet.setFeature("PageRank", snippet.getAggregatedResult().getSource().getPageRank());

        // 1st level domain
        snippet.setFeature("TopLevelDomain", classifyTLD(snippet.getAggregatedResult().getSource()));

        // TODO: pubdate -> Gerhard Weikum
        // snippet.setFeature("Age", 0);

        // some document statistics
        snippet.setFeature("MainContentCharCount", snippet.getAggregatedResult().getSource().getMainContent().length());

        // TODO: content-to-noise
        // downloadUrl, remove tags, -> (len - MainContentCharCount)
        // snippet.setFeature("ContentToNoiseRatio", 0);

        // TODO: avg. sentence length
        // snippet.setFeature("AverageSentenceLength", 0);

        // TODO: snippets-per-entity-per-page count
        // snippet.setFeature("SnippetsPerPageCount", 0);

        // TODO: more from SEOfox
        // Google Cache Date
        // Traffic Value
        // Age
        // del.icio.us
        // del.icio.us Page Bookmarks
        // Diggs
        // Digg's Popular Stories
        // Stumbleupon
        // Twitter
        // Y! Links
        // Y! .edu Links
        // Y! .gov Links
        // Y! Page Links
        // Y! .edu Page Links
        // Technorati
        // Alexa
        // Compete.com Rank
        // Compete.com Uniques
        // Trends
        // Cached
        // dmoz
        // Bloglines
        // Page blog links
        // dir.yahoo.com
        // Botw
        // Business
        // Whois
        // Sktool
        // Google position
        // Yahoo position
        // Majestic SEO linkdomain
    }

    private static void setSnippetReadabilityFeatures(Snippet snippet) {

        if (snippet.getText() == null) {
            return;
        }

        // Readability and general measurements of English text.

        Stats fs = Fathom.analyze(snippet.getText());

        // int textLineCount = fs.getNumTextLines();
        // snippet.setFeature("TextLineCount", textLineCount);

        // int blankLinesCount = fs.getNumBlankLines();
        // snippet.setFeature("BlankLinesCount", blankLinesCount);

        int characterCount = snippet.getText().length();
        snippet.setFeature("CharacterCount", characterCount);

        int letterNumberCount = StringHelper.letterNumberCount(snippet.getText());
        // snippet.setFeature("LetterNumberCount", letterNumberCount);

        float letterNumberPercentage = (100 / (float) characterCount) * letterNumberCount;
        snippet.setFeature("LetterNumberPercentage", letterNumberPercentage);

        // int syllableCount = fs.getNumSyllables();
        // snippet.setFeature("SyllableCount", syllableCount);

        float syllablesPerWordCount = Readability.syllablesPerWords(fs);
        snippet.setFeature("SyllablesPerWordCount", syllablesPerWordCount);

        int wordCount = fs.getNumWords();
        snippet.setFeature("WordCount", wordCount);

        int uniqueWordCount = fs.getUniqueWords().size();
        snippet.setFeature("UniqueWordCount", uniqueWordCount);

        int complexCount = fs.getNumComplexWords();
        // snippet.setFeature("ComplexWordCount", complexCount);

        float complexWordPercentage = Readability.percentComplexWords(fs);
        snippet.setFeature("ComplexWordPercentage", complexWordPercentage);

        int sentenceCount = fs.getNumSentences();
        if (sentenceCount == 0)
            sentenceCount++; // FIXME
        snippet.setFeature("SentenceCount", sentenceCount);

        float wordsPerSentenceCount = Readability.wordsPerSentence(fs);
        snippet.setFeature("WordsPerSentenceCount", wordsPerSentenceCount);

        // Flesch-Kincaid Reading Ease
        // double fleschKincaidReadingEase = 206.835 - (1.015 * wordCount) / sentenceCount - (84.6 * syllableCount) / wordCount;
        double fleschKincaidReadingEase = Readability.calcFlesch(fs);
        snippet.setFeature("FleschKincaidReadingEase", (float) fleschKincaidReadingEase);

        // Gunning-Fog Score
        // double gunningFogScore = 0.4 * ( (double)wordCount / sentenceCount + (100.0 * complexCount) / wordCount );
        double gunningFogScore = Readability.calcFog(fs);
        snippet.setFeature("GunningFogScore", (float) gunningFogScore);

        // Flesch-Kincaid Grade Level
        // double fleschKincaidGradeLevel = (0.39 * wordCount) / sentenceCount + (11.8 * syllableCount) / wordCount - 15.59;
        double fleschKincaidGradeLevel = Readability.calcKincaid(fs);
        snippet.setFeature("FleschKincaidGradeLevel", (float) fleschKincaidGradeLevel);

        // Automated Readability Index
        double automatedReadabilityIndex = (4.71 * letterNumberCount) / wordCount + (0.5 * wordCount) / sentenceCount - 21.43;
        snippet.setFeature("AutomatedReadabilityIndex", (float) automatedReadabilityIndex);

        // Coleman-Liau Index
        double colemanLiauIndex = (5.89 * letterNumberCount) / wordCount - (30.0 * sentenceCount) / wordCount - 15.8;
        snippet.setFeature("ColemanLiauIndex", (float) colemanLiauIndex);

        // SMOG Index
        double smogIndex = Math.sqrt(complexCount * 30.0 / sentenceCount) + 3.0;
        snippet.setFeature("SmogIndex", (float) smogIndex);
    }

    private static void setSyntacticSnippetFeatures(Snippet snippet) {

        if (snippet.getText() == null) {
            return;
        }

        // NOTICE: some features are already implemented in readability function, because
        // they are needed there and it is more performant to not calculate them twice.

        // TODO: POS: nouns

        snippet.setFeature("ContainsProperNoun", StringHelper.containsProperNoun(snippet.getText()) ? 1 : 0);

        // TODO: POS: verbs

        snippet.setFeature("CapitalizedWordCount", StringHelper.capitalizedWordCount(snippet.getText()));

        // TODO: misspellings
        // TODO: datesCount
        // TODO: special chars
        // TODO: hellips or ... count
        // TODO: <entity> is/was/isn't count
    }

    private static void setSemanticSnippetFeatures(Snippet snippet) {

        // snippet.setFeature("EntityConcept", snippet.getEntity().getConcept().getID());

        if (snippet.startsWithEntity()) {
            snippet.setFeature("StartsWithEntity", 1);
        } else {
            snippet.setFeature("StartsWithEntity", 0);
        }

        HashSet<Attribute> attributes = snippet.getEntity().getConcept().getAttributes(false);
        int attributeCount = 0;
        for (Attribute attribute : attributes) {
            // attributeCount += StringHelper.countWordOccurrences(attribute.getName(), snippet.getText());
            // TODO: synonyms
        }
        // snippet.setFeature("AttributeCount", attributeCount);

        ArrayList<Entity> entities = snippet.getEntity().getConcept().getEntities();
        int entityCount = 0;
        SnippetBuilder sb = new SnippetBuilder();
        for (Entity entity : entities) {
            entityCount += sb.countEntityOccurrences(entity, snippet.getText());
            // TODO: synonyms
        }
        snippet.setFeature("RelatedEntityCount", entityCount);

        // TODO: number of entities extracted from this URL in DB
        // TODO: number of facts extracted from this URL in DB
        // TODO: number of snippets extracted from this URL in DB
    }

    /**
     * Assigns a classification to each source URL, to group by TLD.
     * 
     * @param source - The source object.
     * @return The TLD class.
     */
    private static int classifyTLD(Source source) {

        int result = -1;

        HashMap<String, Integer> map = new HashMap<String, Integer>();

        // mapping, TODO: move to some config file or so
        map.put("com", 1);
        map.put("net", 2);
        map.put("org", 3);
        map.put("edu", 4);
        map.put("gov", 5);
        map.put("biz", 6);
        map.put("info", 7);
        map.put("museum", 8);
        map.put("travel", 9);
        map.put("de", 10);

        String tld = source.getTLD();
        if (map.containsKey(tld)) {
            result = map.get(tld);
        }

        return result;
    }

    /**
     * Assigns a classification to each search engine, to group by search engine type.
     * 
     * @param se - The search engine.
     * @return The search engine type.
     */
    private static int classifySearchEngine(int se) {

        int result = -1;

        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

        // mapping, TODO: move to some config file or so
        map.put(SourceRetrieverManager.YAHOO, 1);
        map.put(SourceRetrieverManager.GOOGLE, 1);
        map.put(SourceRetrieverManager.MICROSOFT, 1);
        map.put(SourceRetrieverManager.HAKIA, 2);
        map.put(SourceRetrieverManager.YAHOO_BOSS, 3);
        map.put(SourceRetrieverManager.BING, 1);
        map.put(SourceRetrieverManager.TWITTER, 4);
        map.put(SourceRetrieverManager.GOOGLE_BLOGS, 5);
        map.put(SourceRetrieverManager.TEXTRUNNER, 6);

        if (map.containsKey(se)) {
            result = map.get(se);
        }

        return result;
    }

    /**
     * Extract a list of part-of-speech tags from a sentence.
     * 
     * @param sentence - The sentence
     * @return The part of speach tags.
     */
    public static List<String> extractPOSFromSentence(String sentence) {

        List<String> tags = null;
        ObjectInputStream oi = null;

        try {
            HiddenMarkovModel hmm = null;

            if (DataHolder.getInstance().containsDataObject("pos-en-general-brown.HiddenMarkovModel")) {
                hmm = (HiddenMarkovModel) DataHolder.getInstance().getDataObject("pos-en-general-brown.HiddenMarkovModel");
            } else {
                oi = new ObjectInputStream(new FileInputStream("data" + File.separator + "models" + File.separator + "pos-en-general-brown.HiddenMarkovModel"));
                hmm = (HiddenMarkovModel) oi.readObject();
                DataHolder.getInstance().putDataObject("pos-en-general-brown.HiddenMarkovModel", hmm);
            }

            HmmDecoder decoder = new HmmDecoder(hmm);

            TokenizerFactory tokenizer_Factory = IndoEuropeanTokenizerFactory.INSTANCE;

            // first get the tokens
            char[] cs = sentence.toCharArray();
            Tokenizer tokenizer = tokenizer_Factory.tokenizer(cs, 0, cs.length);
            String[] tokens = tokenizer.tokenize();

            // then get the tags
            tags = Arrays.asList(decoder.firstBest(tokens));
        } catch (IOException ie) {
            Logger.getRootLogger().error("IO Error: " + ie.getMessage());
        } catch (ClassNotFoundException ce) {
            Logger.getRootLogger().error("Class error: " + ce.getMessage());
        } finally {
            if (oi != null) {
                try {
                    oi.close();
                } catch (IOException ie) {
                    Logger.getRootLogger().error(ie.getMessage());
                }
            }
        }

        return tags;
    }

    public static void main(String[] abc) {

        String s = "San Antonio, Texas (PRWEB) December 11, 2009 -- The leader in battery replacements for Apple\'s IPHONE and IPOD, milliamp.com, has just announced that they have the ability to change the battery in Apple\'s IPHONE 3G. \"Being able to replace the battery in Apple\'s IPHONE 3G is very exciting, and will prove to be more and more important for IPHONE users as these devices age and the batteries inside continue to lose their ability to retain a charge.\", said the owner of milliamp.com, Anthony Magnabosco. Unlike the first generation IPHONE (aka IPHONE 2G), the battery inside the IPHONE 3G and IPHONE 3GS is not soldered in. One would think this would make it easier for the average consumer to replace the battery, but in reality, the IPHONE 3G and 3GS takes special talent to safely open and service. \"We have been replacing batteries in IPODS and IPHONES for nearly five years now, and we are pleased to be able to perform this service for IPHONE 3GS owners.\", continued Magnabosco. IPHONE 3GS battery replacements are usually performed within a few days after arrival, and then go through a series of quality control tests before the device is quickly shipped back to the customer. Further, milliamp.com guarantees their IPHONE 3GS batteries for ten years, so should the battery fail to hold its\' charge or if you start to notice a decrease in playtime, you can have the battery replaced by milliamp.com and not have to buy a new one. Interestingly, the battery for Apple\'s IPHONE 3GS is a bit different in its\' configuration and circuitry than its\' IPHONE 3G predecessor, so the two batteries are not interchangeable. \"It is important for customers to know that, if they notice their IPHONE 3GS is not holding a charge as well as it did when it was brand new, they need to contact us for a solution.\" More information about this new battery repair offering can be found at milliamp.com. Milliamp LTD and their websites are in no way associated with Apple Computer, Inc. \'Apple\', \'iPod\', and \'iPhone\' are trademarks of Apple Computer, Inc., registered in the U.S. and other countries. Opening up your device may void or limit the scope of Apple\'s warranty that you may or may not have.";
        s = "The iphone 3GS is manufactered by Apple.";
        //		
        //		
        // // StringTokenizer st = new StringTokenizer(s);
        // // while (st.hasMoreTokens()) {
        // // System.out.println(st.nextToken());
        // // }
        // // System.out.println(st.countTokens());
        // // System.exit(0);
        //		
        // // net.sf.saxon.expr.Tokenizer
        // // com.hp.hpl.jena.util.Tokenizer
        // // weka.core.tokenizers.Tokenizer
        // // org.apache.lucene.analysis.Tokenizer
        //		
        // // test tokenizers
        // List<String> tokensList = new ArrayList<String>();
        // List<String> whitesList = new ArrayList<String>();
        // TokenizerFactory tokenizer_Factory;
        // // tokenizer_Factory = CharacterTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = EnglishStopTokenizerFactory.INSTANCE;
        // tokenizer_Factory = IndoEuropeanTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = LineTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = LowerCaseTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = ModifiedTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = ModifyTokenTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = NGramTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = PorterStemmerTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = RegExFilteredTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = RegExTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = SoundexTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = StopTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = TokenLengthTokenizerFactory.INSTANCE;
        // // tokenizer_Factory = WhitespaceNormTokenizerFactory.INSTANCE;
        //		
        // Tokenizer tokenizer = tokenizer_Factory.tokenizer(
        // s.toCharArray(), 0, s.length());
        // tokenizer.tokenize(tokensList, whitesList);
        // String[] tokens = new String[tokensList.size()];
        // tokensList.toArray(tokens);
        // String[] whites = new String[whitesList.size()];
        // whitesList.toArray(whites);
        //	    
        // // System.out.println(tokens.length);
        // //// for (String token : tokens) {
        // //// System.out.println(token);
        // //// }
        // // System.exit(0);
        //	    
        //	    
        //	    
        // // // test sentencers
        // // SentenceModel SENTENCE_MODEL = new IndoEuropeanSentenceModel();
        // // int[] sentenceBoundaries = SENTENCE_MODEL.boundaryIndices(tokens, whites);
        // //
        // // for (String x : extractPOSFromSentence(s)) {
        // // // *-- set the adjective tags
        // // if (x.startsWith("j") || x.equals("cd") || x.endsWith("od")) {
        // // System.out.println("<Adjective>");
        // // }
        // // // *-- next, to-be tags
        // // else if (x.startsWith("b")) {
        // // System.out.println("<ToBe>");
        // // }
        // // // *-- next, to-be tags
        // // else if (x.startsWith("h")) {
        // // System.out.println("<ToHave>");
        // // }
        // // // *-- next, to-be tags
        // // else if (x.startsWith("do")) {
        // // System.out.println("<ToDo>");
        // // }
        // // // *-- next, the noun tags
        // // else if (x.startsWith("n")) {
        // // System.out.println("<Noun>");
        // // }
        // // // *-- finally, the verb tags, skipping auxiliary verbs
        // // else if (x.startsWith("v")) {
        // // System.out.println("<Verb>");
        // // }
        // // else {
        // // System.out.println(x);
        // // }
        // // }
        // // System.exit(0);
        //		
        // // String url = "http://www.rotaract.org";
        // // System.out.println(getPageRank(url));
        // // System.out.println(classifyTLD(url));

        Controller.getInstance();

        Concept concept = new Concept("Mobile Phone");
        // Attribute a1 = new Attribute("width", Attribute.VALUE_NUMERIC, concept);
        // Attribute a2 = new Attribute("height", Attribute.VALUE_NUMERIC, concept);

        Entity entity = new Entity("iPhone 3GS", concept);
        concept.addEntity(entity);
        Entity e2 = new Entity("Palm Pre", concept);
        concept.addEntity(e2);
        Entity e3 = new Entity("Nokia 6700", concept);
        concept.addEntity(e3);

        Source source = new Source("http://www.apple.com/iphone/");
        source.setMainContent("iPhone 3GS is a GSM cell phone that's also an iPod, a video camera, and a mobile Internet device with email and GPS maps.");

        WebResult webresult = new WebResult(SourceRetrieverManager.GOOGLE, 1, source, "Apple - iPhone - Mobile phone, iPod, and Internet device.",
                "iPhone 3GS is a GSM cell phone that's also an iPod, a video camera, and a mobile Internet device with email and GPS maps.");

        List<WebResult> webresults = new ArrayList<WebResult>();
        webresults.add(webresult);

        AggregatedResult ar = new AggregatedResult(webresults, 1);

        // Snippet snippet = new Snippet(entity, ar, webresult.getSummary());
        Snippet snippet = new Snippet(
                entity,
                ar,
                "The iPhone 3GS doesn't make the same grand leap that the iPhone 3G made from the first-generation model, but the latest Apple handset is still a compelling upgrade for some users.");
        // Snippet snippet = new Snippet(entity, ar, "The iphone 3GS is manufactered by Apple.");

        SnippetFeatureExtractor.setFeatures(snippet);

        System.out.println("\n\t" + snippet.getText() + "\n");
        for (Entry<String, Double> feature : snippet.getFeatures().entrySet()) {
            System.out.println(feature.getKey() + ": " + feature.getValue());
        }

        HashSet<Attribute> attributes = snippet.getEntity().getConcept().getAttributes(false);
        for (Attribute a : attributes) {
            System.out.println(a.getName());
        }

        ArrayList<Entity> entities = snippet.getEntity().getConcept().getEntities();
        for (Entity e : entities) {
            System.out.println(e.getName());
        }
    }
}
