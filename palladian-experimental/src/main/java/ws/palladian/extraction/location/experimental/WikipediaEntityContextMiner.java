package ws.palladian.extraction.location.experimental;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.core.Annotation;
import ws.palladian.extraction.DictionaryTagger;
import ws.palladian.extraction.location.ContextClassifier;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.functional.Consumer;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wiki.MediaWikiUtil;
import ws.palladian.retrieval.wiki.WikiPage;

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
public class WikipediaEntityContextMiner {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaEntityContextMiner.class);

    private static final Map<String, String> TYPE_MAP = createTypeMap();

    private static DictionaryTrieModel.Builder leftBuilder = new DictionaryTrieModel.Builder();

    private static DictionaryTrieModel.Builder rightBuilder = new DictionaryTrieModel.Builder();

    /**
     * @param wikipediaDump Path to the Wikipedia dump file (in .bz2 format), not <code>null</code>.
     * @param outputPath The path to the directory where the dictionaries will be saved, not <code>null</code>.
     * @param contextSize Size of the context in words.
     * @param limit Number of pages to read.
     * @throws IOException In case of any I/O related error.
     */
    public static void mineContexts(File wikipediaDump, File outputPath, final int contextSize, final int limit)
            throws IOException {
        Validate.notNull(wikipediaDump, "wikipediaDump must not be null");
        Validate.notNull(outputPath, "outputPath must not be null");
        Validate.isTrue(wikipediaDump.isFile(), wikipediaDump + " is not a file or could not be accessed.");
        Validate.isTrue(contextSize > 0, "contextSize must be greater zero");
        Validate.isTrue(limit > 0, "limit must be greater zero");
        if (!outputPath.isDirectory()) {
            if (!outputPath.mkdirs()) {
                throw new IllegalArgumentException(outputPath + " does not exist/could not be created.");
            }
        }
        try {
            final int[] counter = new int[] {0};
            MediaWikiUtil.parseDump(wikipediaDump, new Consumer<WikiPage>() {
                @Override
                public void process(WikiPage page) {
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
                        extractContexts(page, mappedType, contextSize);
                    }
                }
            });
        } catch (StopException e) {
            // finished.
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        }

        DictionaryModel leftModel = leftBuilder.create();
        DictionaryModel rightModel = rightBuilder.create();
        File leftFile = new File(outputPath, "leftContexts_" + wikipediaDump.getName() + "_" + contextSize + ".ser");
        File rightFile = new File(outputPath, "rightContexts_" + wikipediaDump.getName() + "_" + contextSize + ".ser");
        LOGGER.info("Left model ({}): {}", leftFile, leftModel);
        LOGGER.info("Right model ({}): {}", rightFile, rightModel);
        FileHelper.serialize(leftModel, leftFile.getPath());
        FileHelper.serialize(rightModel, rightFile.getPath());
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

    private static void extractContexts(WikiPage page, String type, int contextSize) {
        String pageText = page.getCleanText();
        pageText = StringHelper.normalizeQuotes(pageText);
        pageText = MediaWikiUtil.extractSentences(pageText);

        String title = page.getCleanTitle();
        Set<String> entityNames = CollectionHelper.newHashSet(title);
        if ("PER".equals(type)) { // last name for type "PER"
            entityNames.add(title.substring(title.lastIndexOf(" ") + 1));
        }
        if ("ORG".equals(type)) { // for "ORG", remove Inc. suffix
            entityNames.add(title.replaceAll(",? Inc.", ""));
        }
        DictionaryTagger tagger = new DictionaryTagger(entityNames);
        Set<String> leftContexts = CollectionHelper.newHashSet();
        Set<String> rightContexts = CollectionHelper.newHashSet();
        for (Annotation annotation : tagger.getAnnotations(pageText)) {
            // XXX should we replace numbers by placeholder? e.g. 1982 -> §§§§ ?
            String left = ContextClassifier.getLeftContext(annotation, pageText, contextSize).toLowerCase().trim();
            String right = ContextClassifier.getRightContext(annotation, pageText, contextSize).toLowerCase().trim();
            leftContexts.add(left);
            rightContexts.add(right);
        }
        leftBuilder.addDocument(leftContexts, type);
        rightBuilder.addDocument(rightContexts, type);
    }

    /** Used to break the callback. */
    private static final class StopException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static void main(String[] args) throws IOException {
        //String title = "Apple, Inc.";
        //System.out.println("'"+title.replaceAll(",? Inc.", "")+"'");
        //System.exit(0);
        File wikipediaDump = new File("/Volumes/LaCie500/LocationLab/enwiki-20140707-pages-articles.xml.bz2");
        File ouputPath = new File("/Users/pk/temp");
        mineContexts(wikipediaDump, ouputPath, 1, 10000);
        // mineContexts(wikipediaDump, 1, Integer.MAX_VALUE);
        // mineContexts(wikipediaDump, 2, Integer.MAX_VALUE);
        // mineContexts(wikipediaDump, 3, Integer.MAX_VALUE);
        // mineContexts(wikipediaDump, 4, Integer.MAX_VALUE);
    }

}
