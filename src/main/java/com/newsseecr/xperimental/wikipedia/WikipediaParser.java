package com.newsseecr.xperimental.wikipedia;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.helper.StopWatch;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.ProcessingPipeline;
import ws.palladian.preprocessing.featureextraction.ControlledVocabularyFilter;
import ws.palladian.preprocessing.featureextraction.DuplicateTokenRemover;
import ws.palladian.preprocessing.featureextraction.NGramCreator;
import ws.palladian.preprocessing.featureextraction.TermCorpusBuilder;
import ws.palladian.preprocessing.featureextraction.Tokenizer;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;

public class WikipediaParser {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WikipediaParser.class);

    private String inputFile;
    private String outputFile;
    private Set<String> pageTitles = new HashSet<String>();
    private MediaWikiParserFactory pf = new MediaWikiParserFactory();
    private MediaWikiParser parser = pf.createParser();
    private static final Pattern DISAMBIGUATION_PATTERN = Pattern.compile("\\s\\(.*\\)");
    private SAXParserFactory factory = SAXParserFactory.newInstance();
    private ProcessingPipeline processingPipeline;
    private Set<String> namespaces = new HashSet<String>();

    public WikipediaParser() {
        processingPipeline = new ProcessingPipeline();
    }

    public static void main(String[] args) throws Exception {

        WikipediaParser wikipediaParser = new WikipediaParser();
        wikipediaParser.setInputFile("/Users/pk/Desktop/WikipediaData/dewiki-20110410-pages-articles.xml");
        wikipediaParser.setOutputFile("data/wikipedia-de-corpus.ser");
        wikipediaParser.readArticleTitles();
        wikipediaParser.readTermFrequencies();

    }

    public void readArticleTitles() throws Exception {

        LOGGER.info("reading titles");

        final MutableInt counter = new MutableInt();
        StopWatch sw = new StopWatch();

        TextBufferHandler handler = new TextBufferHandler() {

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                if (qName.equals("namespace") || qName.equals("title")) {
                    startCatching();
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (qName.equals("namespace")) {
                    namespaces.add(getText());
                }
                if (qName.equals("title")) {
                    String title = getText();
                    if (!isInNamespace(title) && !isDisambiguated(title)) {
                        title = title.toLowerCase();
                        pageTitles.add(title);
                    }
                    counter.increment();
                    if (counter.intValue() % 10000 == 0) {
                        LOGGER.info("read " + counter.intValue());
                    }
                }
            }

        };

        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(inputFile, handler);
        LOGGER.info("finished reading " + pageTitles.size() + " titles in " + sw);


    }

    public void readTermFrequencies() throws Exception {

        LOGGER.info("calculating frequencies");
        
        TermCorpusBuilder corpusBuilder = new TermCorpusBuilder();
        
        // set up the processing pipeline
        processingPipeline.add(new Tokenizer());
        processingPipeline.add(new NGramCreator(4));
        processingPipeline.add(new DuplicateTokenRemover());
        processingPipeline.add(new ControlledVocabularyFilter(pageTitles));
        processingPipeline.add(corpusBuilder);


        final MutableInt counter = new MutableInt();

        TextBufferHandler handler = new TextBufferHandler() {

            private String pageTitle;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                if (qName.equals("title") || qName.equals("text")) {
                    startCatching();
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (qName.equals("title")) {
                    pageTitle = getText();
                } else if (qName.equals("text")) {
                    processPage(pageTitle, getText());
                    counter.increment();
                    if (counter.intValue() % 10000 == 0) {
                        LOGGER.info("read " + counter.intValue());
                    }
                }
            }

        };

        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputFile, handler);
            // LOGGER.info("finished calculating frequencies; titleCount=" + titleCorpus.getNumDocs());
            LOGGER.info("finished calculating frequencies; titleCount=" + corpusBuilder.getTermCorpus().getNumDocs());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // corpusBuilder.getTermCorpus().serialize(outputFile);
        corpusBuilder.getTermCorpus().save(outputFile);
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }
    
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    private boolean isInNamespace(String title) {
        boolean result = false;
        int colonIndex = title.indexOf(":");
        if (colonIndex > 0) {
            String prefix = title.substring(0, colonIndex);
            if (namespaces.contains(prefix)) {
                result = true;
            }
        }
        return result;
    }

    private boolean isDisambiguated(String title) {
        return DISAMBIGUATION_PATTERN.matcher(title).find();
    }

    private void processPage(String pageTitle, String text) {
        if (!isInNamespace(pageTitle) && !isDisambiguated(pageTitle)) {
            ParsedPage parsedPage = parser.parse(text);
            PipelineDocument document = new PipelineDocument(parsedPage.getText());
            processingPipeline.process(document);
        }
    }
    
    public void getNumItems() throws Exception {
        final MutableInt counter = new MutableInt();
        DefaultHandler handler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                if (qName.equals("title")) {
                    counter.increment();
                }
            }
        };
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(inputFile, handler);
        LOGGER.info("# items : " + counter);
    }


}
