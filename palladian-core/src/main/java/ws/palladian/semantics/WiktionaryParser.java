package ws.palladian.semantics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonDatabase;
import ws.palladian.persistence.json.JsonObject;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class is a parser for the Wiktionary project dump files. The parser works for the German and English dumps which
 * can be found at <a href="http://dumps.wikimedia.org/dewiktionary/">German dumps</a> and <a
 * href="http://dumps.wikimedia.org/enwiktionary/">English dumps</a>. Use pages-articles.xml.bz2.
 * </p>
 *
 * <p>
 * The German Word DB can be extended with data from openthesaurus.de. We need to download the SQL database
 * (http://www.openthesaurus.de/about/download) query the hypernyms (SELECT t1.word,t2.word FROM term t1, term t2,
 * synset s1, synset s2, synset_link sl WHERE t1.synset_id = s1.id AND t2.synset_id = s2.id AND sl.synset_id = s1.id AND
 * sl.target_synset_id = s2.id AND sl.link_type_id=1;), export this data to a csv file (word;hypernym), and tell the
 * parser to use this file for additional hypernyms.
 * </p>
 *
 * @author David Urbansky
 */
public class WiktionaryParser {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WiktionaryParser.class);
    public static final String HYPERNYMS = "hypernyms";
    public static final String HYPONYMS = "hyponyms";
    public static final String MERONYMS = "meronyms";
    public static final String HOLONYMS = "holonyms";
    public static final String SYNONYMS = "synonyms";

    /** The database where the dictionary is stored. */
    private final JsonDatabase wordDB;
    private static final String COLLECTION_NAME = "wiktionary";
    private static final int MAX_WORD_LENGTH = 30;
    /** The language to use for the parsing. */
    private final Language corpusLanguage;

    /**
     * The path to an additional hypernym file which should be used for parsing. The file has to have one
     * hyponym;hypernym tuple per line.
     */
    private String additionalHypernymFile = "";

    public WiktionaryParser(String targetPath, Language language) {
        targetPath = FileHelper.addTrailingSlash(targetPath);
        this.corpusLanguage = language;
        wordDB = new JsonDatabase(targetPath, 0);
    }

    public WiktionaryParser(Language language) {
        this.corpusLanguage = language;
        wordDB = null;
    }

    public void parseAndCreateDB(String wiktionaryXmlFilePath) {
        final long bytesToProcess = new File(wiktionaryXmlFilePath).length();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {
                private long bytesProcessed = 0;
                private int elementsParsed = 0;
                private boolean isTitle = false;
                private boolean considerText = false;
                private boolean isText = false;

                private String currentWord = "";
                private StringBuilder text = new StringBuilder();
                private final StopWatch sw = new StopWatch();

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    // System.out.println("Start Element :" + qName);
                    if (qName.equalsIgnoreCase("text")) {
                        isText = true;
                        text = new StringBuilder();
                    }

                    if (qName.equalsIgnoreCase("title")) {
                        isTitle = true;
                    }

                    bytesProcessed += qName.length();
                }

                private void postProcess(String word, StringBuilder text) throws SQLException {
                    if (word.equalsIgnoreCase("ewusersonly")) {
                        return;
                    }

                    String plural = "";
                    String wordRoot = "";
                    String language = "";
                    String wordType = "";
                    List<String> synonyms = new ArrayList<>();
                    List<String> hypernyms = new ArrayList<>();
                    List<String> hyponyms = new ArrayList<>();
                    List<String> meronyms = new ArrayList<>();
                    List<String> holonyms = new ArrayList<>();

                    String textString = text.toString();

                    // get the language
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        language = StringHelper.getSubstringBetween(textString, " ({{Sprache|", "}}");
                    } else if (corpusLanguage.equals(Language.ENGLISH)) {
                        language = StringHelper.getSubstringBetween(textString, "==", "==");
                    }

                    // get the word type
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        wordType = StringHelper.getSubstringBetween(textString, "=== {{Wortart|", "|");
                        if (wordType.contains("}}")) {
                            wordType = StringHelper.getSubstringBetween(textString, "=== {{Wortart|", "}}");
                        }
                    } else if (corpusLanguage.equals(Language.ENGLISH)) {
                        wordType = StringHelper.getSubstringBetween(textString, "Etymology 1===", "# ");
                        if (wordType.length() == 0) {
                            wordType = StringHelper.getSubstringBetween(textString, "Pronunciation===", "# ");
                        }
                        if (wordType.length() == 0) {
                            wordType = StringHelper.getSubstringBetween(textString, language + "==", "# ");
                        }
                        if (wordType.contains("Etymology==")) {
                            wordType = StringHelper.getSubstringBetween(textString, "Etymology===", "# ");
                        }
                        if (wordType.contains("Pronunciation")) {
                            wordType = StringHelper.getSubstringBetween(textString, "Pronunciation===", "# ");
                        }

                        if (wordType.length() > 0) {
                            wordType = StringHelper.getSubstringBetween(wordType, "===", "===");
                            wordType = StringHelper.trim(wordType);
                        }
                    }

                    // get the plural if noun
                    if (corpusLanguage.equals(Language.GERMAN) && wordType.equalsIgnoreCase("substantiv")) {
                        plural = StringHelper.getSubstringBetween(textString, "{{Silbentrennung}}\n", "\n");

                        if (plural.length() == 0) {
                            plural = StringHelper.getSubstringBetween(textString, "{{Silbentrennung}} \n", "\n");
                        }

                        int index = plural.indexOf("{{Pl.}}");
                        if (index > -1) {
                            plural = plural.substring(plural.indexOf("{{Pl.}}") + 7);
                        } else {
                            index = plural.indexOf("{{Pl.1}}");
                            if (index > -1) {
                                plural = plural.substring(plural.indexOf("{{Pl.1}}") + 8);
                                index = plural.indexOf(",");
                                if (index > -1) {
                                    plural = plural.substring(0, index);
                                } else {
                                    plural = "";
                                }
                            }
                        }
                        plural = StringHelper.trim(plural.replace("\n", "").replace("·", "").replaceAll("''.*?''", ""));
                    } else if (corpusLanguage.equals(Language.ENGLISH) && wordType.equalsIgnoreCase("noun")) {
                        plural = StringHelper.getSubstringBetween(textString, "{{en-noun|", "|+}");
                    }

                    if (plural.length() > MAX_WORD_LENGTH) {
                        plural = "";
                    }

                    // String tagGrabRegexp = "(?<=\\[\\[)([^\\]]+?)(?=\\]\\])";
                    String tagGrabRegexp = "(?<=(^ |  |, )\\[\\[)([^\\]]{1,30}?)(?=\\]\\]($|,|;))";

                    String synonymString = "";

                    if (corpusLanguage.equals(Language.GERMAN)) {
                        synonymString = StringHelper.getSubstringBetween(textString, "{{Synonyme}}", "}}\n");

                        // take only the line starting with [1] because it is the most relevant, the others are too far
                        // off
                        synonymString = StringHelper.getSubstringBetween(synonymString, ":[1]", "\n");
                        synonymString = synonymString.replaceAll("''.*?''", "");

                        synonyms = StringHelper.getRegexpMatches(tagGrabRegexp, synonymString);
                    } else if (corpusLanguage.equals(Language.ENGLISH)) {
                        synonymString = StringHelper.getSubstringBetween(textString, "====Synonyms====", "===");
                        synonyms = StringHelper.getRegexpMatches(tagGrabRegexp, synonymString);
                        if (synonyms.isEmpty()) {
                            synonyms = StringHelper.getRegexpMatches("(?<=en\\|)(.{1,30})(?=\\}\\})", synonymString);
                        }
                    }

                    // hypernyms are only available in German, strange though...
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        String hypernymString = StringHelper.getSubstringBetween(textString, "{{Oberbegriffe}}", "}}\n");
                        hypernymString = StringHelper.getSubstringBetween(hypernymString, ":[1]", "\n");
                        hypernymString = hypernymString.replaceAll("''.*?''", "");
                        hypernyms = StringHelper.getRegexpMatches(tagGrabRegexp, hypernymString);
                    }

                    // get descending words (words from which the current one is the hypernym)
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        String hyponymString = StringHelper.getSubstringBetween(textString, "{{Unterbegriffe}}", "}}\n");
                        hyponymString = StringHelper.getSubstringBetween(hyponymString, ":[1]", "\n");
                        hyponymString = hyponymString.replaceAll("''.*?''", "");
                        hyponyms = StringHelper.getRegexpMatches(tagGrabRegexp, hyponymString);
                    }

                    JsonObject wordObject = wordDB.getOne(COLLECTION_NAME, "_id", word);
                    if (wordObject == null) {
                        wordObject = new JsonObject();
                        wordObject.put("_id", word);
                    }
                    wordObject.put("root", wordRoot);
                    wordObject.put("plural", plural);
                    wordObject.put("type", wordType);
                    wordObject.put("language", language);

                    JsonArray existingSynonyms = wordObject.tryGetJsonArray(SYNONYMS);
                    if (existingSynonyms == null) {
                        existingSynonyms = new JsonArray();
                    }
                    existingSynonyms.addAll(synonyms);
                    wordObject.put(SYNONYMS, existingSynonyms);
                    updateWords(existingSynonyms, SYNONYMS, word);

                    JsonArray existingHypernyms = wordObject.tryGetJsonArray(HYPERNYMS);
                    if (existingHypernyms == null) {
                        existingHypernyms = new JsonArray();
                    }
                    existingHypernyms.addAll(hypernyms);
                    wordObject.put(HYPERNYMS, existingHypernyms);
                    updateWords(existingHypernyms, HYPERNYMS, word);

                    JsonArray existingHyponyms = wordObject.tryGetJsonArray(HYPONYMS);
                    if (existingHyponyms == null) {
                        existingHyponyms = new JsonArray();
                    }
                    existingHyponyms.addAll(hyponyms);
                    wordObject.put(HYPONYMS, existingHyponyms);

                    JsonArray existingHolonyms = wordObject.tryGetJsonArray(HOLONYMS);
                    if (existingHolonyms == null) {
                        existingHolonyms = new JsonArray();
                    }
                    existingHolonyms.addAll(holonyms);
                    wordObject.put(HOLONYMS, existingHolonyms);

                    JsonArray existingMeronyms = wordObject.tryGetJsonArray(MERONYMS);
                    if (existingMeronyms == null) {
                        existingMeronyms = new JsonArray();
                    }
                    existingMeronyms.addAll(meronyms);
                    wordObject.put(MERONYMS, existingMeronyms);

                    wordDB.add(COLLECTION_NAME, wordObject);

                    if (elementsParsed++ % 100 == 0) {
                        System.out.println(">" + MathHelper.round((double) (100 * bytesProcessed) / bytesToProcess, 2) + "%, +" + sw.getElapsedTimeString());
                        sw.start();
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (qName.equalsIgnoreCase("text")) {
                        if (considerText) {
                            LOGGER.debug("Word: " + currentWord);
                            LOGGER.debug("Text: " + text);
                            try {
                                postProcess(currentWord, text);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        isText = false;
                        considerText = false;
                    }

                    if (qName.equalsIgnoreCase("title")) {
                        isTitle = false;
                    }

                    bytesProcessed += qName.length();
                }

                @Override
                public void characters(char ch[], int start, int length) {
                    if (isTitle) {
                        String titleText = new String(ch, start, length);

                        if (!titleText.contains(":") && !titleText.contains("Wiktionary")) {
                            considerText = true;
                            currentWord = titleText;
                        }
                    }

                    if (isText && considerText) {
                        String textString = new String(ch, start, length);
                        text.append(textString);
                    }

                    bytesProcessed += length;
                }
            };

            saxParser.parse(wiktionaryXmlFilePath, handler);

            // if we have an additional hypernym file, parse it
            if (getAdditionalHypernymFile().length() > 0) {
                List<String> hypernymArray = FileHelper.readFileToArray(getAdditionalHypernymFile());

                String lastHyponym = "";
                List<String> hypernyms = new ArrayList<>();

                int c = 0;
                for (String wordPair : hypernymArray) {
                    String[] words = wordPair.split(";");

                    if (words.length < 2) {
                        continue;
                    }

                    String hyponym = StringHelper.trim(StringHelper.removeBrackets(words[0]));
                    String hypernym = StringHelper.trim(StringHelper.removeBrackets(words[1]));

                    if (!hyponym.equals(lastHyponym)) {
                        if (lastHyponym.length() > 0) {
                            JsonObject wordObject = wordDB.getById(COLLECTION_NAME, lastHyponym);
                            if (wordObject != null) {
                                JsonArray existingHypernyms = wordObject.tryGetJsonArray(HYPERNYMS);
                                if (existingHypernyms == null) {
                                    existingHypernyms = new JsonArray();
                                    wordObject.put(HYPERNYMS, existingHypernyms);
                                }
                                existingHypernyms.addAll(hypernyms);
                            }
                            hypernyms = new ArrayList<>();
                            hypernyms.add(hypernym);
                        } else {
                            hypernyms.add(hypernym);
                        }
                        lastHyponym = hyponym;
                    } else {
                        hypernyms.add(hypernym);
                    }

                    if (c++ % 100 == 0) {
                        LOGGER.info(MathHelper.round((double) (100 * c) / hypernymArray.size(), 2) + "% of additional hypernyms processed");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        wordDB.createIndex(COLLECTION_NAME, "_id");
    }

    /**
     * Update the words with the given relation.
     */
    private void updateWords(JsonArray existingWords, String relation, String sourceWord) {
        for (int i = 0; i < existingWords.size(); i++) {
            String relatedWord = existingWords.tryGetString(i);
            JsonObject relatedWordObject = wordDB.getOrCreateById(COLLECTION_NAME, relatedWord);
            switch (relation) {
                // NOTE: synonyms are not symmetric, so we don't add them here, e.g. "fuck" => mate but not vice versa
                //                case SYNONYMS:
                //                    JsonArray existingSynonyms = relatedWordObject.tryGetJsonArray(SYNONYMS);
                //                    if (existingSynonyms == null) {
                //                        existingSynonyms = new JsonArray();
                //                        relatedWordObject.put(SYNONYMS, existingSynonyms);
                //                    }
                //                    existingSynonyms.add(sourceWord);
                //                    break;
                case HYPERNYMS:
                    // if the related word is a hypernym, the source word is a hyponym of the related word
                    JsonArray existingHyponyms = relatedWordObject.tryGetJsonArray(HYPONYMS);
                    if (existingHyponyms == null) {
                        existingHyponyms = new JsonArray();
                        relatedWordObject.put(HYPONYMS, existingHyponyms);
                    }
                    existingHyponyms.add(sourceWord);
                    break;
                case HYPONYMS:
                    // if the related word is a hyponym, the source word is a hypernym of the related word
                    JsonArray existingHypernyms = relatedWordObject.tryGetJsonArray(HYPERNYMS);
                    if (existingHypernyms == null) {
                        existingHypernyms = new JsonArray();
                        relatedWordObject.put(HYPERNYMS, existingHypernyms);
                    }
                    existingHypernyms.add(sourceWord);
                    break;
                case HOLONYMS:
                    JsonArray existingMeronyms = relatedWordObject.tryGetJsonArray(MERONYMS);
                    if (existingMeronyms == null) {
                        existingMeronyms = new JsonArray();
                        relatedWordObject.put(MERONYMS, existingMeronyms);
                    }
                    existingMeronyms.add(sourceWord);
                    break;
                case MERONYMS:
                    JsonArray existingHolonyms = relatedWordObject.tryGetJsonArray(HOLONYMS);
                    if (existingHolonyms == null) {
                        existingHolonyms = new JsonArray();
                        relatedWordObject.put(HOLONYMS, existingHolonyms);
                    }
                    existingHolonyms.add(sourceWord);
                    break;
                default:
                    LOGGER.error("Unknown relation: " + relation);
                    break;
            }
            wordDB.add(COLLECTION_NAME, relatedWordObject);
        }
    }

    public void parseAndCreateSingularPluralFile(String wiktionaryXmlFilePath) {
        final long bytesToProcess = new File(wiktionaryXmlFilePath).length();

        final StringBuilder wordFile = new StringBuilder();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {
                private long bytesProcessed = 0;
                private int elementsParsed = 0;
                private boolean isTitle = false;
                private boolean considerText = false;
                private boolean isText = false;

                private String currentWord = "";
                private StringBuilder text = new StringBuilder();
                private final StopWatch sw = new StopWatch();

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if (qName.equalsIgnoreCase("text")) {
                        isText = true;
                        text = new StringBuilder();
                    }

                    if (qName.equalsIgnoreCase("title")) {
                        isTitle = true;
                    }

                    bytesProcessed += qName.length();
                }

                private void postProcess(String word, StringBuilder text) throws SQLException {
                    if (word.equalsIgnoreCase("ewusersonly")) {
                        return;
                    }

                    String singular = "";
                    String plural = "";
                    String wordType;

                    String textString = text.toString();

                    // get the word type
                    wordType = StringHelper.getSubstringBetween(textString, "=== {{Wortart|", "|");
                    if (wordType.contains("}}")) {
                        wordType = StringHelper.getSubstringBetween(textString, "=== {{Wortart|", "}}");
                    }

                    // get the plural if noun
                    if (corpusLanguage.equals(Language.GERMAN) && wordType.equalsIgnoreCase("substantiv")) {
                        singular = StringHelper.getSubstringBetween(textString, "|Nominativ Singular=", "\n");
                        plural = StringHelper.getSubstringBetween(textString, "|Nominativ Plural=", "\n");
                    }

                    if ((singular.startsWith("der ") || singular.startsWith("die ") || singular.startsWith("das ")) && (plural.startsWith("der ") || plural.startsWith("die ")
                            || plural.startsWith("das "))) {

                        wordFile.append(singular.replaceFirst("\\s", "\t")).append("\t");
                        wordFile.append(plural.replaceFirst("\\s", "\t")).append("\n");

                    }

                    if (elementsParsed++ % 100 == 0) {
                        System.out.println(">" + MathHelper.round((double) (100 * bytesProcessed) / bytesToProcess, 2) + "%, +" + sw.getElapsedTimeString());
                        sw.start();
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (qName.equalsIgnoreCase("text")) {
                        if (considerText) {
                            LOGGER.debug("Word: " + currentWord);
                            LOGGER.debug("Text: " + text);
                            try {
                                postProcess(currentWord, text);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        isText = false;
                        considerText = false;
                    }

                    if (qName.equalsIgnoreCase("title")) {
                        isTitle = false;
                    }

                    bytesProcessed += qName.length();
                }

                @Override
                public void characters(char ch[], int start, int length) {
                    if (isTitle) {
                        String titleText = new String(ch, start, length);

                        if (!titleText.contains(":") && !titleText.contains("Wiktionary")) {
                            considerText = true;
                            currentWord = titleText;
                        }
                    }

                    if (isText && considerText) {
                        String textString = new String(ch, start, length);
                        text.append(textString);
                    }

                    bytesProcessed += length;
                }
            };

            saxParser.parse(wiktionaryXmlFilePath, handler);

            FileHelper.writeToFile("singularPluralGermanNounsWiktionary.tsv", wordFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAdditionalHypernymFile() {
        return additionalHypernymFile;
    }

    public void setAdditionalHypernymFile(String additionalHypernymFile) {
        this.additionalHypernymFile = additionalHypernymFile;
    }

    public static void main(String[] args) {
        StopWatch sw = new StopWatch();

        //        String text = " [[alkoholisch]]es [[Getränk]], [[Getränk]] [[Bier]], [[Lebensmittel]]";
        //        //text = " [[alkoholisch]]es [[Getränk]]";
        //        //text = " [[Lebensmittel]]";
        //        text = " [[Getränk]], [[Lebensmittel]]";
        //        List<String> syns = StringHelper.getRegexpMatches("(?<=(^ |, )\\[\\[)([^\\]]+?)(?=\\]\\]($|,))", text);
        //        CollectionHelper.print(syns);
        //        System.exit(0);

        // German
        // WiktionaryParser wpG = new WiktionaryParser("data/temp/wdbg/", Language.GERMAN);
        // wpG.parseAndCreateDB("data/temp/dewiktionary-20110327-pages-meta-current.xml");
        // wpG.setAdditionalHypernymFile("data/temp/wdbg/openthesaurusadd.csv");
        // wpG.parseAndCreateDB("data/temp/wdbg/dewiktionary-latest-pages-articles.xml");
        // wpG.parseAndCreateDB("data/temp/disk1.xml");

        // English
        WiktionaryParser wpE = new WiktionaryParser(Language.ENGLISH);
        //        wpE.parseAndCreateSingularPluralFile("pages.xml");
        // WiktionaryParser wpE = new WiktionaryParser("data/temp/wordDatabaseEnglish/", Language.ENGLISH);
        wpE.parseAndCreateDB("C:\\Users\\User\\Downloads\\enwiktionary-latest-pages-articles.xml");

        LOGGER.info("created wiktionary DB in " + sw.getElapsedTimeString());
    }

}