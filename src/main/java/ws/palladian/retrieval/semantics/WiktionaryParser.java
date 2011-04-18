package ws.palladian.retrieval.semantics;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * This class is a parser for the Wiktionary project dump files. The parser works for the German and English dumps which
 * can be found at <a href="http://dumps.wikimedia.org/dewiktionary/">German dumps</a> and <a
 * href="http://dumps.wikimedia.org/enwiktionary/">English dumps</a>.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class WiktionaryParser {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WiktionaryParser.class);

    /** The database where the dictionary is stored. */
    private WordDB wordDB;

    /** The supported languages which the parser can handle. */
    public enum Language {
        GERMAN, ENGLISH
    };

    /** The language to use for the parsing. */
    private Language corpusLanguage;

    public WiktionaryParser(String targetPath, Language language) {
        targetPath = FileHelper.addTrailingSlash(targetPath);
        this.corpusLanguage = language;
        wordDB = new WordDB(targetPath);
        wordDB.setInMemoryMode(true);
        wordDB.setup();
    }

    /**
     * 
     * @param wiktionaryXmlFilePath
     */
    public void parseAndCreateDB(String wiktionaryXmlFilePath) {

        final long bytesToProcess = new File(wiktionaryXmlFilePath).length();

        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {

                long bytesProcessed = 0;
                int elementsParsed = 0;
                boolean isTitle = false;
                boolean considerText = false;
                boolean isText = false;

                String currentWord = "";
                StringBuilder text = new StringBuilder();
                StopWatch sw = new StopWatch();

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {

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
                    String language = "";
                    String wordType = "";
                    List<String> synonyms = new ArrayList<String>();
                    List<String> hypernyms = new ArrayList<String>();

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
                        if (wordType.indexOf("}}") > -1) {
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
                        if (wordType.indexOf("Etymology==") > -1) {
                            wordType = StringHelper.getSubstringBetween(textString, "Etymology===", "# ");
                        }
                        if (wordType.indexOf("Pronunciation") > -1) {
                            wordType = StringHelper.getSubstringBetween(textString, "Pronunciation===", "# ");
                        }

                        if (wordType.length() > 0) {
                            wordType = StringHelper.getSubstringBetween(wordType, "===", "===");
                            wordType = StringHelper.trim(wordType);
                        }
                    }

                    String synonymString = "";

                    if (corpusLanguage.equals(Language.GERMAN)) {
                        synonymString = StringHelper.getSubstringBetween(textString, "{{Synonyme}}", "{{");
                        synonyms = StringHelper.getRegexpMatches("(?<=\\[\\[)(.+?)(?=\\]\\])", synonymString);
                    } else if (corpusLanguage.equals(Language.ENGLISH)) {
                        synonymString = StringHelper.getSubstringBetween(textString, "====Synonyms====", "===");
                        synonyms = StringHelper.getRegexpMatches("(?<=\\[\\[)(.+?)(?=\\]\\])", synonymString);
                    }

                    // hypernyms are only available in German, strange though...
                    if (corpusLanguage.equals(Language.GERMAN)) {
                        String hypernymString = StringHelper.getSubstringBetween(textString, "{{Oberbegriffe}}", "{{");
                        hypernyms = StringHelper.getRegexpMatches("(?<=\\[\\[)(.+?)(?=\\]\\])", hypernymString);
                    }
                    Word wordObject = wordDB.getWord(word);
                    if (wordObject == null) {
                        wordObject = new Word(-1, word, wordType, language);
                        wordDB.addWord(wordObject);

                        // get it from the db again to get the correct id
                        wordObject = wordDB.getWord(word);

                    } else {
                        wordObject.setType(wordType);
                        wordObject.setLanguage(language);
                        wordDB.updateWord(wordObject);
                    }

                    if (wordObject != null) {
                        wordDB.addSynonyms(wordObject, synonyms);
                        wordDB.addHypernyms(wordObject, hypernyms);
                    }

                    if (elementsParsed++ % 100 == 0) {
                        System.out.println(">" + MathHelper.round(100 * bytesProcessed / bytesToProcess, 2) + "%, +"
                                + sw.getElapsedTimeString());
                        sw.start();
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {

                    if (qName.equalsIgnoreCase("text")) {
                        if (considerText) {
                            LOGGER.debug("Word: " + currentWord);
                            LOGGER.debug("Text: " + text);
                            try {
                                postProcess(currentWord, text);
                            } catch (SQLException e) {
                                // TODO Auto-generated catch block
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
                public void characters(char ch[], int start, int length) throws SAXException {

                    if (isTitle) {
                        String titleText = new String(ch, start, length);

                        if (titleText.indexOf(":") == -1 && titleText.indexOf("Wiktionary") == -1) {
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

        } catch (Exception e) {
            e.printStackTrace();
        }

        wordDB.writeToDisk();
    }

    /**
     * The main function.
     * 
     * @param args
     */
    public static void main(String[] args) {
        StopWatch sw = new StopWatch();

        // German
        WiktionaryParser wpG = new WiktionaryParser("data/temp/wordDatabaseGerman/", Language.GERMAN);
        wpG.parseAndCreateDB("data/temp/dewiktionary-20110327-pages-meta-current.xml");

        // English
        WiktionaryParser wpE = new WiktionaryParser("data/temp/wordDatabaseEnglish/", Language.ENGLISH);
        wpE.parseAndCreateDB("data/temp/enwiktionary-20110402-pages-meta-current.xml");

        LOGGER.info("created wiktionary DB in " + sw.getElapsedTimeString());
    }

}