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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wikipedia.MultiStreamBZip2InputStream;
import ws.palladian.retrieval.wikipedia.WikipediaPage;
import ws.palladian.retrieval.wikipedia.WikipediaPageCallback;
import ws.palladian.retrieval.wikipedia.WikipediaPageContentHandler;
import ws.palladian.retrieval.wikipedia.WikipediaUtil;

/**
 * <p>
 * Extract entity contexts from Wikipedia dumps. Contexts are the words around a specific entity (e.g. for the entity
 * type PER, a typical right context could be "was born in"). This context miner goes through the Wikipedia dump and
 * uses info boxes to detect the type of pages. In case a type could be determined for a page (PER, ORG, LOC, MISC), the
 * entity's occurrences on the page are detected and the contexts are extracted.
 * </p>
 * 
 * @author Philipp Katz
 */
class WikipediaEntityContextMiner {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaEntityContextMiner.class);

    private static final String CSV_SEPARATOR = "###";

    private static final Map<String, String> TYPE_MAP = createTypeMap();

    private static final CountMatrix<String> leftContexts = CountMatrix.create();

    private static final CountMatrix<String> rightContexts = CountMatrix.create();

    private static final CountMap<String> typeCounts = CountMap.create();

    /**
     * @param wikipediaDump Path to the Wikipedia dump file (in .bz2 format).
     * @param contextSize Size of the context in words.
     * @param limit Number of pages to read.
     */
    public static void mineContexts(File wikipediaDump, final int contextSize, final int limit) {
        if (!wikipediaDump.isFile()) {
            throw new IllegalArgumentException(wikipediaDump + " is not a file or could not be accessed.");
        }
        Validate.isTrue(contextSize > 0, "contextSize must be greater zero");
        Validate.isTrue(limit > 0, "limit must be greater zero");
        leftContexts.clear();
        rightContexts.clear();
        typeCounts.clear();
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser parser = saxParserFactory.newSAXParser();
            InputStream inputStream = new MultiStreamBZip2InputStream(new BufferedInputStream(new FileInputStream(
                    wikipediaDump)));
            final int[] counter = new int[] {0};
            parser.parse(inputStream, new WikipediaPageContentHandler(new WikipediaPageCallback() {
                @Override
                public void callback(WikipediaPage page) {
                    if (counter[0]++ == limit) {
                        throw new StopException();
                    }
                    if (ProcessHelper.getFreeMemory() < SizeUnit.MEGABYTES.toBytes(128)) {
                        LOGGER.info("Memory nearly exhausted, stopping. Make sure to assign lots of heap memory before running!");
                        throw new StopException();
                    }
                    String pageType = page.getInfoboxType();
                    if (pageType == null) {
                        return;
                    }
                    String mappedType = TYPE_MAP.get(pageType);
                    if (mappedType != null) {
                        typeCounts.add(mappedType);
                        extractContexts(page, mappedType, contextSize);
                    }
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
        LOGGER.info("Document type statistics: {}, total documents: {}", typeCounts, typeCounts.totalSize());
        writeContexts(leftContexts, "leftContexts_" + contextSize + ".csv");
        writeContexts(rightContexts, "rightContexts_" + contextSize + ".csv");
    }

    private static Map<String, String> createTypeMap() {
        Map<String, String> result = CollectionHelper.newHashMap();
        result.put("settlement", "LOC");
        result.put("album", "MISC");
        result.put("person", "PER");
        result.put("football biography", "PER");
        result.put("film", "MISC");
        // result.put("musical artist", ""); // ambiguous, may be person, may be band
        result.put("single", "MISC");
        result.put("company", "ORG");
        result.put("french commune", "LOC");
        result.put("nrhp", "LOC"); // National Register of Historic Places
        result.put("book", "MISC");
        result.put("ship begin", "MISC");
        result.put("television", "MISC");
        result.put("officeholder", "PER");
        result.put("military person", "PER");
        result.put("school", "ORG");
        result.put("uk place", "LOC");
        result.put("mlb player", "PER");
        result.put("radio station", "MISC");
        result.put("road", "LOC");
        result.put("writer", "PER");
        result.put("university", "ORG");
        result.put("scientist", "PER");
        result.put("football club", "ORG");
        result.put("vg", "MISC"); // video game
        result.put("military unit", "MISC");
        result.put("sportsperson", "PER");
        result.put("mountain", "LOC");
        result.put("german location", "LOC");
        result.put("airport", "LOC");
        // result.put("planet", "");
        result.put("ice hockey player", "PER");
        result.put("nfl player", "PER");
        result.put("cricketer", "PER");
        result.put("military conflict", "MISC");
        result.put("station", "LOC");
        result.put("aircraft begin", "MISC");
        result.put("software", "MISC");
        result.put("lake", "LOC");
        // result.put("artist", "");
        result.put("politician", "PER");
        result.put("italian comune", "LOC");
        result.put("river", "LOC");
        result.put("australian place", "LOC");
        result.put("language", "MISC");
        // result.put("building", "");
        result.put("television episode", "MISC");
        result.put("organization", "ORG");
        // result.put("indian jurisdiction", "");
        // result.put("stadium", "");
        // result.put("royalty", "");
        result.put("gridiron football person", "PER");
        result.put("protected area", "LOC");
        result.put("football club season", "MISC");
        result.put("election", "MISC");
        result.put("college coach", "PER");
        result.put("journal", "MISC");
        return Collections.unmodifiableMap(result);
    }

    private static void writeContexts(CountMatrix<String> contextMatrix, String fileName) {
        Set<String> types = contextMatrix.getColumnKeys();
        Set<String> contexts = contextMatrix.getRowKeys();
        LOGGER.info("Writing context list to '{}', # contexts: {}", fileName, contexts.size());

        Writer writer = null;
        try {
            int maximumTypeCount = 0;
            writer = new BufferedWriter(new FileWriter(fileName));
            // write header
            StringBuilder header = new StringBuilder();
            header.append("context");
            for (String type : types) {
                header.append(CSV_SEPARATOR).append(type);
                maximumTypeCount = Math.max(maximumTypeCount, typeCounts.getCount(type));
            }
            header.append('\n');
            writer.append(header);

            // write counts
            for (String context : contexts) {
                StringBuilder line = new StringBuilder();
                line.append(context);
                int maximumCategoryCount = 0;
                for (String type : types) {
                    int count = contextMatrix.getCount(type, context);

                    // normalize the count in regards to the # of documents
                    // XXX maybe it would make more sense to normalize by text length?
                    double normalization = (double)maximumTypeCount / typeCounts.getCount(type);
                    int normalizedCount = (int)Math.round(count * normalization);

                    maximumCategoryCount = Math.max(maximumCategoryCount, normalizedCount);
                    line.append(CSV_SEPARATOR).append(normalizedCount);
                }
                line.append('\n');

                // only write, if at least one column is larger than zero
                if (maximumCategoryCount > 0) {
                    writer.append(line);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(writer);
        }
    }

    private static void extractContexts(WikipediaPage page, String type, int contextSize) {
        String pageText = WikipediaUtil.stripMediaWikiMarkup(page.getText());
        pageText = StringHelper.normalizeQuotes(pageText);
        pageText = WikipediaUtil.extractSentences(pageText);

        String entityName = page.getCleanTitle();
        String lastName = entityName.substring(entityName.lastIndexOf(" ") + 1); // only use for "PER"?
        Pattern pattern = Pattern.compile(String.format("((?:\\w+[^\\w]{1,5}){%s})(?:%s|%s)((?:[^\\w]{1,5}\\w+){%s})",
                contextSize, Pattern.quote(entityName), Pattern.quote(lastName), contextSize));
        Matcher matcher = pattern.matcher(pageText);
        Set<String> documentRightContexts = CollectionHelper.newHashSet();
        Set<String> documentLeftContexts = CollectionHelper.newHashSet();
        while (matcher.find()) {
            String leftContext = matcher.group(1).trim().toLowerCase();
            String rightContext = matcher.group(2).trim().toLowerCase();
            // skip contexts with line break
            if (!leftContext.contains("\n")) {
                documentLeftContexts.add(leftContext);
            }
            if (!rightContext.contains("\n")) {
                documentRightContexts.add(rightContext);
            }
        }
        addToMatrix(leftContexts, documentLeftContexts, type);
        addToMatrix(rightContexts, documentRightContexts, type);
    }

    private static void addToMatrix(CountMatrix<String> contexts, Set<String> documentContexts, String type) {
        for (String context : documentContexts) {
            contexts.add(type, context);
        }
    }

    /** Used to break the callback. */
    private static final class StopException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static void main(String[] args) {
        File wikipediaDump = new File("/Volumes/iMac HD/temp/enwiki-20130503-pages-articles.xml.bz2");
        mineContexts(wikipediaDump, 1, Integer.MAX_VALUE);
        mineContexts(wikipediaDump, 2, Integer.MAX_VALUE);
        mineContexts(wikipediaDump, 3, Integer.MAX_VALUE);
        mineContexts(wikipediaDump, 4, Integer.MAX_VALUE);
    }

}
