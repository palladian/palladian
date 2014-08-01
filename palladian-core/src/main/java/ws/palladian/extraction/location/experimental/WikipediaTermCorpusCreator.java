package ws.palladian.extraction.location.experimental;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ws.palladian.extraction.feature.MapTermCorpus;
import ws.palladian.extraction.feature.Stemmer;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.functional.Consumer;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wikipedia.WikipediaPage;
import ws.palladian.retrieval.wikipedia.WikipediaUtil;

class WikipediaTermCorpusCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaTermCorpusCreator.class);

//    private static final MapTermCorpus corpus = new MapTermCorpus();

    private static final MapTermCorpus bigramCorpus = new MapTermCorpus();

    private static final Stemmer stemmer = new Stemmer(Language.ENGLISH);

    /**
     * @param wikipediaDump Path to the Wikipedia dump file (in .bz2 format).
     * @param outputFile File name and path of the resulting corpus.
     * @param limit Number of pages to read.
     */
    public static void createCorpus(File wikipediaDump, File outputPath, final int limit) {
        if (!wikipediaDump.isFile()) {
            throw new IllegalArgumentException(wikipediaDump + " is not a file or could not be accessed.");
        }
        Validate.notNull(wikipediaDump, "wikipediaDump must not be null");
        Validate.notNull(outputPath, "outputPath must not be null");
        Validate.isTrue(limit > 0, "limit must be greater zero");
//        corpus.clear();
        bigramCorpus.clear();
        try {
            final int[] counter = new int[] {0};
            WikipediaUtil.parseDump(wikipediaDump, new Consumer<WikipediaPage>() {
                @Override
                public void process(WikipediaPage page) {
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
                    String pageText = page.getCleanText();
                    pageText = StringHelper.normalizeQuotes(pageText);
                    pageText = WikipediaUtil.extractSentences(pageText);
                    addCounts(pageText);
                }
            });
        } catch (StopException e) {
            // finished.
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        try {
//            corpus.save(new File(outputPath, "unigrams.gz"));
            bigramCorpus.save(new File(outputPath, "bigrams.gz"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void addCounts(String pageText) {
//        Set<String> tokenSet = CollectionHelper.newHashSet();
//
//        for (String token : Tokenizer.tokenize(pageText)) {
//            String stemmed = stemmer.stem(token.toLowerCase());
//            tokenSet.add(new String(stemmed));
//        }
//        corpus.addTermsFromDocument(tokenSet);

        Set<String> bigramSet = makeBigrams(pageText);
        bigramCorpus.addTermsFromDocument(bigramSet);
    }

    private static Set<String> makeBigrams(String text) {
        Set<String> bigramSet = CollectionHelper.newHashSet();
        List<String> tokens = Tokenizer.tokenize(text);
        outer: for (int i = 0; i <= tokens.size() - 2; i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = i; j < i + 2; j++) {
                String value = tokens.get(j).toLowerCase();
                if (value.length() == 1 && StringHelper.isPunctuation(value.charAt(0))) {
                    continue outer;
                }
                String stemmed = normalize(value);
                builder.append(stemmed).append(' ');
            }
            bigramSet.add(builder.toString().trim());
        }
        return bigramSet;
    }

    /**
     * <p>
     * Stem values, replace digits by 0s. (e.g. 60ies becomes 00ies becomes 00i, 345,678 becomes 000,000).
     * </p>
     * 
     * @param value
     * @return
     */
    private static String normalize(String value) {
        String stem = stemmer.stem(value);
//        stem = stem.replaceAll("\\d", "0");
        return stem;
    }

    public static void main(String[] args) throws IOException {
        // Set<String> biGrams = makeBigrams(FileHelper.readFileToString("src/test/resources/NewsSampleText.txt"));
        // CollectionHelper.print(biGrams);
        // System.exit(0);
        File wikipediaDump = new File("/Volumes/iMac HD/temp/enwiki-20130503-pages-articles.xml.bz2");
        File outputPath = new File("/Users/pk/Desktop");
        createCorpus(wikipediaDump, outputPath, Integer.MAX_VALUE);
    }

}
