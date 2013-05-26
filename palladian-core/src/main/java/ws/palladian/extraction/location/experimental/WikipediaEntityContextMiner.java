package ws.palladian.extraction.location.experimental;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.sources.importers.MultiStreamBZip2InputStream;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wikipedia.WikipediaPage;
import ws.palladian.retrieval.wikipedia.WikipediaPageCallback;
import ws.palladian.retrieval.wikipedia.WikipediaPageContentHandler;
import ws.palladian.retrieval.wikipedia.WikipediaUtil;

/**
 * <p>
 * Extract entity contexts from Wikipedia dumps.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WikipediaEntityContextMiner {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaEntityContextMiner.class);

    private static final CountMap<String> leftContexts = CountMap.create();
    private static final CountMap<String> rightContexts = CountMap.create();

    public static void main(String[] args) throws Exception {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser parser = saxParserFactory.newSAXParser();
        File redirects = new File("/Users/pk/Downloads/enwiki-latest-pages-articles.xml.bz2");
        InputStream inputStream = new MultiStreamBZip2InputStream(new BufferedInputStream(
                new FileInputStream(redirects)));
        final Set<String> persons = new HashSet<String>(Arrays.asList("person", "officeholder", "military person",
                "scientist", "writer", "mlb player", "artist", "politician", "president", "governor", "monarch",
                "nfl player", "congressman", "f1 driver", "prime minister", "ice hockey player", "philosopher"));
        final int[] counter = new int[] {0};
        parser.parse(inputStream, new WikipediaPageContentHandler(new WikipediaPageCallback() {
            @Override
            public void callback(WikipediaPage page) {
                String type = page.getInfoboxType();
                if (type == null) {
                    return;
                }
                if (persons.contains(type)) {
                    extractContexts(page, 2);
                    LOGGER.info("Extracted from {} pages", counter[0]);
                    if (counter[0]++ == 20000) {
                        writeContexts(leftContexts, "leftContexts2.csv", 2);
                        writeContexts(rightContexts, "rightContexts2.csv", 2);
                        System.exit(0);
                    }
                }
            }
        }));
    }

    private static void writeContexts(CountMap<String> contextCounts, String fileName, int minOccurrence) {
        LOGGER.info("Writing context list to '{}'", fileName);
        LinkedHashMap<String, Integer> sortedMap = contextCounts.getSortedMapDescending();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName));
            for (String context : sortedMap.keySet()) {
                int count = contextCounts.getCount(context);
                if (count < minOccurrence) {
                    break;
                }
                writer.append(String.format("%s###%s\n", context, count));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(writer);
        }
    }

    // XXX only consider real sentences/paragraphs here (no bullet lists, no headings)
    private static void extractContexts(WikipediaPage page, int contextSize) {
        String pageText = WikipediaUtil.stripMediaWikiMarkup(page.getText());
        pageText = StringHelper.normalizeQuotes(pageText);
        String entityName = page.getTitle();
        String lastName = entityName.substring(entityName.lastIndexOf(" ") + 1);
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
        leftContexts.addAll(documentLeftContexts);
        rightContexts.addAll(documentRightContexts);
    }

}
