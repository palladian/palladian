package ws.palladian.extraction.location.experimental;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wikipedia.MultiStreamBZip2InputStream;
import ws.palladian.retrieval.wikipedia.WikipediaPage;
import ws.palladian.retrieval.wikipedia.WikipediaPageCallback;
import ws.palladian.retrieval.wikipedia.WikipediaPageContentHandler;
import ws.palladian.retrieval.wikipedia.WikipediaUtil;

class WikipediaCaseDictionaryCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaCaseDictionaryCreator.class);

    private static final CountMap<String> wordCounts = CountMap.create();

    private static final CountMap<String> uppercaseCounts = CountMap.create();

    /**
     * @param wikipediaDump Path to the Wikipedia dump file (in .bz2 format).
     * @param outputFile The CSV to which to write (existing files will be overwritten).
     * @param limit Number of pages to read.
     */
    public static void mineCaseDictionary(File wikipediaDump, File outputFile, final int limit) {
        if (!wikipediaDump.isFile()) {
            throw new IllegalArgumentException(wikipediaDump + " is not a file or could not be accessed.");
        }
        Validate.isTrue(limit > 0, "limit must be greater zero");
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser parser = saxParserFactory.newSAXParser();
            InputStream inputStream = new MultiStreamBZip2InputStream(new BufferedInputStream(new FileInputStream(
                    wikipediaDump)));
            final int[] counter = new int[] {0};
            parser.parse(inputStream, new WikipediaPageContentHandler(new WikipediaPageCallback() {
                @Override
                public void callback(WikipediaPage page) {
                    if (page.getNamespaceId() != WikipediaPage.MAIN_NAMESPACE) {
                        return;
                    }
                    if (counter[0]++ == limit) {
                        throw new StopException();
                    }
                    if (ProcessHelper.getFreeMemory() < SizeUnit.MEGABYTES.toBytes(128)) {
                        LOGGER.info("Memory nearly exhausted, stopping. Make sure to assign lots of heap memory before running!");
                        throw new StopException();
                    }
                    System.out.println(counter[0]);
                    String pageText = WikipediaUtil.stripMediaWikiMarkup(page.getText());
                    pageText = StringHelper.normalizeQuotes(pageText);
                    pageText = WikipediaUtil.extractSentences(pageText);
                    addCounts(pageText);
                }
            }));
        } catch (StopException e) {
            // finished.
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        writeCaseDictionary(outputFile);
    }

    private static void addCounts(String pageText) {
        List<String> sentences = Tokenizer.getSentences(pageText, true);
        for (String sentence : sentences) {
            List<String> tokens = Tokenizer.tokenize(sentence);
            for (int i = 1; i < tokens.size(); i++) {
                String token = tokens.get(i);
                wordCounts.add(token.toLowerCase());
                if (StringHelper.startsUppercase(token)) {
                    uppercaseCounts.add(token.toLowerCase());
                }
            }
        }
    }

    private static void writeCaseDictionary(File outputFile) {
        Writer writer = null;
        try {
            Set<String> words = wordCounts.keySet();
            writer = new BufferedWriter(new FileWriter(outputFile));
            for (String word : words) {
                int totalCount = wordCounts.getCount(word);
                int uppercaseCount = uppercaseCounts.getCount(word);
                writer.write(String.format("%s\t%s\t%s\n", word, totalCount, uppercaseCount));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(writer);
        }
    }

    /** Used to break the callback. */
    private static final class StopException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static void clean(File caseDictionaryInput, File caseDictionaryOutput) {
        final Writer[] writer = new Writer[1];
        final int[] counter = new int[] {0};
        try {
            writer[0] = new BufferedWriter(new FileWriter(caseDictionaryOutput));
            int lines = FileHelper.performActionOnEveryLine(caseDictionaryInput.getPath(), new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    try {
                        String[] split = line.split("\t");
                        String value = split[0];
                        int count = Integer.valueOf(split[1]);
                        if (count >= 10 && value.matches("[A-Za-z\\-]+")) {
                            writer[0].write(line);
                            writer[0].write('\n');
                            counter[0]++;
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
            System.out.println("Reduced from " + lines + " to " + counter[0]);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(writer[0]);
        }
    }

    public static void main(String[] args) {
        // File wikipediaDump = new File("/Users/pk/Downloads/enwiki-latest-pages-articles.xml.bz2");
        File caseDictionaryFile = new File("wikipediaCaseDictionary.csv");
        // mineCaseDictionary(wikipediaDump, caseDictionaryFile, 50000);
        File caseDictionaryCleaned = new File("wikipediaCaseDictionaryClean.csv");
        clean(caseDictionaryFile, caseDictionaryCleaned);
    }

}
