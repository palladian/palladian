package ws.palladian.extraction.location.experimental;

import java.io.File;
import java.io.IOException;
import java.util.List;
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
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.functional.Consumer;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wiki.InfoboxTypeMapper;
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
    
    private static DictionaryTrieModel.Builder entityBuilder = new DictionaryTrieModel.Builder();

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
                    if (page.getNamespaceId() != WikiPage.MAIN_NAMESPACE) {
                        return;
                    }
                    String pageType = page.getInfoboxType();
                    if (pageType == null) {
                        return;
                    }
                    String mappedType = InfoboxTypeMapper.getConLLType(pageType);
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

        DictionaryModel entityModel = entityBuilder.create();
        DictionaryModel leftModel = leftBuilder.create();
        DictionaryModel rightModel = rightBuilder.create();
        File entityFile = new File(outputPath, "entities_" + wikipediaDump.getName() + ".ser");
        File leftFile = new File(outputPath, "leftContexts_" + wikipediaDump.getName() + "_" + contextSize + ".ser");
        File rightFile = new File(outputPath, "rightContexts_" + wikipediaDump.getName() + "_" + contextSize + ".ser");
        LOGGER.info("Entity model ({}): {}", entityFile, entityModel);
        LOGGER.info("Left model ({}): {}", leftFile, leftModel);
        LOGGER.info("Right model ({}): {}", rightFile, rightModel);
        FileHelper.serialize(entityModel, entityFile.getPath());
        FileHelper.serialize(leftModel, leftFile.getPath());
        FileHelper.serialize(rightModel, rightFile.getPath());
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
        // alternative titles given on the page; e.g. "United States of America", "USA", "U.S.A." ...
        List<String> alternativeTitles = page.getAlternativeTitles();
        entityNames.addAll(alternativeTitles);
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
        
        { // experimental; count entity by size of article (assumption: more important articles contain more content)
            int count = page.getCleanText().split("\\s").length;
            Bag<String> countedEntities = Bag.create();
            for (String entityName : entityNames) {
                countedEntities.add(entityName.toLowerCase().trim(), count);
            }
            entityBuilder.addDocument(countedEntities, type);
        }
        // entityBuilder.addDocument(CollectionHelper.convertSet(entityNames, Functions.LOWERCASE), type);
        leftBuilder.addDocument(leftContexts, type);
        rightBuilder.addDocument(rightContexts, type);
    }

    /** Used to break the callback. */
    private static final class StopException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static void main(String[] args) throws IOException {
        File wikipediaDump = new File("/Volumes/iMac HD/temp/enwiki-20140707-pages-articles.xml.bz2");
        File ouputPath = new File("/Users/pk/temp");
        mineContexts(wikipediaDump, ouputPath, 1, Integer.MAX_VALUE);
    }

}
