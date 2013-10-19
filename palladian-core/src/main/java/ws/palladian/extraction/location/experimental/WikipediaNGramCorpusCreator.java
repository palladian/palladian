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
import java.util.Map;
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
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wikipedia.MultiStreamBZip2InputStream;
import ws.palladian.retrieval.wikipedia.WikipediaPage;
import ws.palladian.retrieval.wikipedia.WikipediaPageCallback;
import ws.palladian.retrieval.wikipedia.WikipediaPageContentHandler;
import ws.palladian.retrieval.wikipedia.WikipediaUtil;

public class WikipediaNGramCorpusCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaNGramCorpusCreator.class);

    private static final Map<Integer, CountMap<String>> nGrams = LazyMap.create(new Factory<CountMap<String>>() {
        // create CountMap, if key with null value is requested
        @Override
        public CountMap<String> create() {
            return CountMap.create();
        }
    });

    /**
     * @param wikipediaDump Path to the Wikipedia dump file (in .bz2 format).
     * @param outputPath Path where to put the result files.
     * @param maxNGramSize Maximum lenght for nGrams.
     * @param limit Number of pages to read.
     */
    public static void mineNGrams(File wikipediaDump, File outputPath, final int maxNGramSize, final int limit) {
        if (!wikipediaDump.isFile()) {
            throw new IllegalArgumentException(wikipediaDump + " is not a file or could not be accessed.");
        }
        Validate.isTrue(maxNGramSize > 0, "limit must be greater zero");
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
                    addCounts(pageText, maxNGramSize);
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
        writeNGrams(outputPath);
    }

    private static void addCounts(String pageText, int maxNGramSize) {
        for (int n = 1; n <= maxNGramSize; n++) {
            CountMap<String> counts = nGrams.get(n);
            Set<String> nGrams = Tokenizer.calculateWordNGrams(pageText, n);
            for (String nGram : nGrams) {
                nGram = new String(nGram);
                counts.add(nGram);
            }
        }
    }

    private static void writeNGrams(File outputPath) {
        for (Integer n : nGrams.keySet()) {
            Writer writer = null;
            try {
                CountMap<String> currentNGrams = nGrams.get(n);
                File file = new File(outputPath, "ngrams_" + n + "_" + System.currentTimeMillis() + ".tsv");
                writer = new BufferedWriter(new FileWriter(file));
                for (String nGram : currentNGrams.uniqueItems()) {
                    writer.write(nGram);
                    writer.write('\t');
                    writer.write(currentNGrams.getCount(nGram));
                    writer.write('\n');
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                FileHelper.close(writer);
            }
        }
    }

    public static void main(String[] args) {
        File wikipediaDump = new File("");
        File outputPath = new File("/Users/pk/Desktop/WikipediaNGrams");
        mineNGrams(wikipediaDump, outputPath, 4, 1000);
    }

}
