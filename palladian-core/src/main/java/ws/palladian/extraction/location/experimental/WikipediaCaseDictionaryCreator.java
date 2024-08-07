package ws.palladian.extraction.location.experimental;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wiki.MediaWikiUtil;
import ws.palladian.retrieval.wiki.WikiPage;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class WikipediaCaseDictionaryCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaCaseDictionaryCreator.class);

    private static final Bag<String> wordCounts = new Bag<>();

    private static final Bag<String> uppercaseCounts = new Bag<>();

    /**
     * @param wikipediaDump Path to the Wikipedia dump file (in .bz2 format).
     * @param outputFile    The CSV to which to write (existing files will be overwritten).
     * @param limit         Number of pages to read.
     */
    public static void mineCaseDictionary(File wikipediaDump, File outputFile, final int limit) {
        if (!wikipediaDump.isFile()) {
            throw new IllegalArgumentException(wikipediaDump + " is not a file or could not be accessed.");
        }
        Validate.isTrue(limit > 0, "limit must be greater zero");
        try {
            final int[] counter = new int[]{0};
            MediaWikiUtil.parseDump(wikipediaDump, new Consumer<WikiPage>() {
                @Override
                public void accept(WikiPage page) {
                    if (page.getNamespaceId() != WikiPage.MAIN_NAMESPACE) {
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
                    String pageText = page.getCleanText();
                    pageText = StringHelper.normalizeQuotes(pageText);
                    pageText = MediaWikiUtil.extractSentences(pageText);
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
            Set<String> words = wordCounts.uniqueItems();
            writer = new BufferedWriter(new FileWriter(outputFile));
            for (String word : words) {
                int totalCount = wordCounts.count(word);
                int uppercaseCount = uppercaseCounts.count(word);
                writer.write(String.format("%s\t%s\t%s\n", word, totalCount, uppercaseCount));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(writer);
        }
    }

    public static void clean(File caseDictionaryInput, File caseDictionaryOutput) {
        final Writer[] writer = new Writer[1];
        final int[] counter = new int[]{0};
        try {
            writer[0] = new BufferedWriter(new FileWriter(caseDictionaryOutput));
            int lines = FileHelper.performActionOnEveryLine(caseDictionaryInput.getPath(), new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    try {
                        String[] split = line.split("\t");
                        String value = split[0];
                        int count = Integer.parseInt(split[1]);
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
