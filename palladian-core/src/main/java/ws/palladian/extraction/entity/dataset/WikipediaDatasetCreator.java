package ws.palladian.extraction.entity.dataset;

import static ws.palladian.core.AnnotationFilters.tag;
import static ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.LanguageMode.English;
import static ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.TrainingMode.Sparse;
import static ws.palladian.helper.collection.CollectionHelper.remove;
import static ws.palladian.helper.constants.Language.ENGLISH;
import static ws.palladian.helper.functional.Filters.fileExtension;
import static ws.palladian.helper.functional.Filters.not;
import static ws.palladian.helper.io.FileHelper.DEFAULT_ENCODING;
import static ws.palladian.retrieval.wiki.MediaWikiDescriptor.Builder.wikipedia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.PruningStrategies;
import ws.palladian.core.Annotation;
import ws.palladian.extraction.DictionaryTagger;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.extraction.entity.tagger.PalladianNer;
import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings;
import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.Builder;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LruMap;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.wiki.InfoboxTypeMapper;
import ws.palladian.retrieval.wiki.MediaWikiDescriptor;
import ws.palladian.retrieval.wiki.MediaWikiUtil;
import ws.palladian.retrieval.wiki.WikiLink;
import ws.palladian.retrieval.wiki.WikiPage;
import ws.palladian.retrieval.wiki.WikiPageReference;

/**
 * <p>
 * Mine training data for Palladian's NER in sparse training mode. (similar to the {@link DatasetCreator}, this class
 * creates annotated training files, however, while the {@link DatasetCreator} uses a web search engine, this method
 * mines the English Wikipedia and exploits infobox types to determine a page's concept type). The advantage of this
 * method is, that we do not need any manually selected seed entities, however we need a type mapping from the infobox
 * types (which is provided through Palladian's {@link InfoboxTypeMapper}).
 * 
 * @author pk
 */
@SuppressWarnings({"deprecation", "unused"})
class WikipediaDatasetCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaDatasetCreator.class);

    /** Temporary tag which is assigned to unknown types (filtered out later). */
    private static final String IGNORE_TAG = "*IGNORE*";

    /** Name of the directory where to put annotated pages which have no mapped type. */
    private static final String NO_MAPPED_TYPE = "NONE";

    private static final MediaWikiDescriptor descriptor = wikipedia().language(ENGLISH).create();

    /** Number of downloaded articles to cache in memory. */
    private static final int ARTICLE_CACHE_SIZE = 10000;

    private static final LruMap<String, WikiPage> ARTICLE_CACHE = LruMap.accessOrder(ARTICLE_CACHE_SIZE);

    private static String process(WikiPage article) {
        List<WikiLink> links = article.getLinks();

        Map<String, String> resolvedLinkEntities = resolveLinkedEntities(descriptor, links);

        String infoboxType = article.getInfoboxType();
        if (infoboxType != null && !infoboxType.isEmpty()) {
            String articleType = InfoboxTypeMapper.getConLLType(infoboxType);
            if (articleType != null && !articleType.isEmpty()) {
                // add the page type itself
                List<String> titles = article.getAlternativeTitles();
                for (String title : titles) {
                    title = title.replaceAll(",$", ""); // strip trailing punctuation
                    resolvedLinkEntities.put(title, articleType);
                }
                resolvedLinkEntities.put(article.getTitle(), articleType);
                resolvedLinkEntities.put(article.getCleanTitle(), articleType);
            }
        }

        Map<String, String> additionalItems = new HashMap<>();
        Set<String> toRemoveItems = new HashSet<>();
        for (Entry<String, String> entry : resolvedLinkEntities.entrySet()) {
            String title = entry.getKey();
            String type = entry.getValue();
            if ("PER".equals(type)) {
                // strip parts in parenthesis, and Jr./Sr. suffix e.g.:
                // Jasper Johns, Jr. -> Jasper Johns
                // William King (artist) -> William King
                title = cleanPersonName(title);
                additionalItems.put(title.substring(title.lastIndexOf(" ") + 1), type);
                String[] split = title.split("\\s");
                if (split.length == 3) { // variant without middle name
                    additionalItems.put(split[0] + " " + split[2], type);
                }
            }
            if ("ORG".equals(type)) { // for "ORG", remove Inc. suffix
                additionalItems.put(title.replaceAll(",? Inc.", ""), type);
            }
            if ("LOC".equals(type)) { // split "LOC" disambiguation pattern, e.g. "Paris, Texas"
                if (title.contains(", ")) {
                    LOGGER.debug("Splitting LOC '{}'", title);
                    String[] split = title.split(", ");
                    additionalItems.put(split[0], type);
                    additionalItems.put(split[1], type);
                    toRemoveItems.add(title);
                }
            }
        }
        LOGGER.debug("Generated {} additional entries", additionalItems.size());
        LOGGER.debug("Removing {} entries", toRemoveItems.size());
        resolvedLinkEntities.putAll(additionalItems);
        resolvedLinkEntities.keySet().removeAll(toRemoveItems);
        resolvedLinkEntities.remove(StringUtils.EMPTY); // remove empty key

        // CollectionHelper.print(resolvedLinkEntities);

        DictionaryTagger tagger = new DictionaryTagger(resolvedLinkEntities, true);
        List<Annotation> annotations = tagger.getAnnotations(article.getCleanText());

        int numRemoved = remove(annotations, not(tag(IGNORE_TAG)));
        LOGGER.debug("Removed {} ignored/unknown types", numRemoved);

        return NerHelper.tag(article.getCleanText(), annotations, TaggingFormat.XML);
    }

    static String cleanPersonName(String title) {
        title = title.replaceAll("\\s\\([^)]*\\)", "");
        title = title.replaceAll(",?\\s(Jr|Sr)\\.", "");
        return title.trim();
    }

    /**
     * @param string The string.
     * @return The percentage of tokens starting upper case.
     */
    static double getUcTokenPercentage(String string) {
        int upper = 0;
        int lower = 0;
        String[] split = string.split("\\s");
        for (String part : split) {
            if (part.length() > 0) {
                char firstChar = part.charAt(0);
                if (Character.isUpperCase(firstChar)) {
                    upper++;
                } else if (Character.isLowerCase(firstChar)) {
                    lower++;
                }
            }
        }
        return (double)upper / (upper + lower);
    }

    private static Map<String, String> resolveLinkedEntities(MediaWikiDescriptor descriptor, List<WikiLink> links) {
        Map<String, String> typeMapping = CollectionHelper.newLinkedHashMap();
        for (WikiLink link : links) {
            String destination = link.getDestination();
            if (typeMapping.containsKey(destination) || destination.isEmpty() || destination.startsWith("file")) {
                continue;
            }
            String title = link.getTitle();
            if (title == null || title.isEmpty()) {
                title = destination;
            }
            if (getUcTokenPercentage(title) < 0.5) {
                LOGGER.debug("Skip '{}' because of UC token percentage", title);
                continue;
            }
            WikiPage linkedArticle;
            try {
                linkedArticle = retrieveArticle(destination);
            } catch (Exception e) {
                LOGGER.debug("Error when accessing '{}'", destination);
                typeMapping.put(destination, IGNORE_TAG);
                continue;
            }
            if (linkedArticle == null) {
                LOGGER.debug("No article with name '{}'", destination);
                typeMapping.put(destination, IGNORE_TAG);
                continue;
            }
            String infoboxType = linkedArticle.getInfoboxType();
            if (infoboxType != null) {
                String mappedType = CollectionHelper.coalesce(InfoboxTypeMapper.getConLLType(infoboxType), IGNORE_TAG);
                typeMapping.put(destination, mappedType);
                String linkTitle = link.getTitle();
                if (linkTitle != null && linkTitle.length() > 0) {
                    typeMapping.put(linkTitle, mappedType);
                }
                List<String> alternativeTitles = linkedArticle.getAlternativeTitles();
                for (String altTitle : alternativeTitles) {
                    if (altTitle.length() > 1) {
                        typeMapping.put(altTitle, mappedType);
                    }
                }
            }
        }
        return typeMapping;
    }

    /**
     * Retrieve an article with caching.
     * 
     * @param destination The name destination of the article.
     * @return The retrieved article (either from cache, or downloaded).
     */
    private static WikiPage retrieveArticle(String destination) {
        WikiPage linkedArticle;
        synchronized (ARTICLE_CACHE) {
            linkedArticle = ARTICLE_CACHE.get(destination);
        }
        if (linkedArticle != null) {
            LOGGER.debug("Cache hit for {}, cache size {}", destination, ARTICLE_CACHE.size());
            return linkedArticle;
        }
        linkedArticle = retrieveArticleFollowRedirects(destination);
        if (linkedArticle != null) {
            synchronized (ARTICLE_CACHE) {
                LOGGER.trace("Cache fail for {}", destination);
                ARTICLE_CACHE.put(destination, linkedArticle);
            }
        }
        return linkedArticle;
    }

    private static WikiPage retrieveArticleFollowRedirects(String destination) {
        String currentDestination = destination;
        for (int i = 0; i < 10; i++) {
            WikiPage linkedArticle = MediaWikiUtil.retrieveArticle(descriptor, currentDestination);
            if (linkedArticle != null && linkedArticle.isRedirect()) {
                LOGGER.debug("Redirect {} -> {}", currentDestination, linkedArticle.getRedirectTitle());
                currentDestination = linkedArticle.getRedirectTitle();
            } else {
                return linkedArticle;
            }
        }
        LOGGER.warn("Too many redirects for {}, giving up", destination);
        return null;
    }

    public static void createCombinedAnnotationFiles(File datasetDirectory, File destinationPath) {
        Validate.notNull(datasetDirectory, "datasetDirectory must not be null");
        Validate.isTrue(datasetDirectory.isDirectory(), "datasetDirectory is not a directory");
        List<File> files = FileHelper.getFiles(datasetDirectory, fileExtension(".txt"));
        createCombinedAnnotationFiles(files, destinationPath);
    }

    public static void createCombinedAnnotationFiles(Collection<File> datasetFiles, File destinationPath) {
        Validate.notNull(datasetFiles, "datasetFiles must not be null");
        Validate.notNull(destinationPath, "destinationPath must not be null");
        Validate.isTrue(destinationPath.isDirectory(), "destinationPath is not a directory");
        ProgressMonitor monitor = new ProgressMonitor();
        monitor.startTask(null, datasetFiles.size());
        File combinedXmlFile = new File(destinationPath, "annotations-combined.xml");
        combinedXmlFile.delete();

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(combinedXmlFile), DEFAULT_ENCODING));
            for (File file : datasetFiles) {
                monitor.increment();
                List<String> lines = FileHelper.readFileToArray(file);
                boolean firstLine = true;
                for (String line : lines) {
                    // skip first line (i.e. paragraph on the article); I observed that the first paragraph in the
                    // Wikipedia often follows a similar template; this way, we might fit too strongly on the Wikipedia.
                    // By skipping the first paragraph, I assume that we have more realistic training examples.
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    line = line.trim();
                    boolean skip = false;
                    skip |= line.isEmpty();
                    skip |= line.startsWith("*");
                    skip |= !line.endsWith(".");
                    skip |= !line.contains("<");
                    skip |= line.length() > 0 && !Character.isUpperCase(line.charAt(0)) && line.charAt(0) != '<';
                    if (skip) {
                        continue;
                    }
                    writer.write(line);
                    writer.write('\n');
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("IOException", e);
        } finally {
            FileHelper.close(writer);
        }
        File combinedColumnFile = new File(destinationPath, "annotations-combined.txt");
        combinedColumnFile.delete();
        FileFormatParser.xmlToColumn(combinedXmlFile.getPath(), combinedColumnFile.getPath(), "\t");
    }

    public static void createCombinedAnnotationFilesEvaluation(File datasetDirectory, File destinationPath) {
        Validate.notNull(datasetDirectory, "datasetDirectory must not be null");
        Validate.notNull(destinationPath, "destinationPath must not be null");
        List<File> files = FileHelper.getFiles(datasetDirectory, fileExtension(".txt"));
        int numFiles = files.size();
        for (int sampleSize = 100; sampleSize <= numFiles; sampleSize *= 2) {
            Collection<File> sampledFiles = MathHelper.sample(files, sampleSize);
            File sampleDestinationPath = new File(destinationPath, "sample-" + sampleSize);
            sampleDestinationPath.mkdirs();
            LOGGER.info("Creating sampling for size {}", sampledFiles.size());
            createCombinedAnnotationFiles(sampledFiles, sampleDestinationPath);
        }
    }

    /**
     * Start the mining process.
     * 
     * @param numThreads The number of threads.
     * @param destinationPath The path to the directory where to store the results.
     */
    public static void mineWikipedia(int numThreads, final File destinationPath) {
        Validate.isTrue(numThreads > 0, "numThreads must be greater zero");
        Validate.notNull(destinationPath, "destinationPath must not be null");
        if (!destinationPath.isDirectory()) {
            Validate.isTrue(destinationPath.mkdirs(), "destinationPath did not exist and could not be created");
        }
        Validate.isTrue(ProcessHelper.getFreeMemory() > SizeUnit.MEGABYTES.toBytes(750),
                "assign at least 1 GB heap memory (necessary for caching");
        final AtomicInteger counter = new AtomicInteger();
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < numThreads; i++) {
            new Thread() {
                @Override
                public void run() {
                    for (;;) {
                        try {
                            WikiPage article;
                            try {
                                WikiPageReference articleReference = MediaWikiUtil.retrieveRandomArticle(descriptor);
                                article = retrieveArticle(articleReference.getTitle());
                            } catch (Exception e) {
                                LOGGER.debug("Error when trying to get article");
                                continue;
                            }
                            String taggedArticle = process(article);
                            if (taggedArticle != null) {
                                String articleType = CollectionHelper.coalesce(
                                        InfoboxTypeMapper.getConLLType(article.getInfoboxType()), NO_MAPPED_TYPE);
                                File destinationDirectory = new File(destinationPath, articleType);
                                String fileNameTitle = article.getTitle().replaceAll("\\s", "_").replace(';', '_')
                                        .replace('/', '_').replaceAll("_+", "_");
                                File file = new File(destinationDirectory, fileNameTitle + ".txt");
                                FileHelper.writeToFile(file.getPath(), taggedArticle);
                                int count = counter.incrementAndGet();
                                if (count % 10 == 0) {
                                    double throughputHour = (double)TimeUnit.HOURS.toMillis(1) * count
                                            / (System.currentTimeMillis() - startTime);
                                    NumberFormat format = new DecimalFormat("##");
                                    LOGGER.info("Processed {} articles, throughput: ~ {} articles/hour", counter,
                                            format.format(throughputHour));
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.debug("Exception in {}", getName(), e);
                        }
                    }
                }
            }.start();
        }
    }

    public static void main(String[] args) {
        // createCombinedAnnotationFilesEvaluation(new File("/Users/pk/Desktop/Wikipedia-EN-entity-dataset-1411490099116"), new File("/Users/pk/Desktop/samples"));
        createCombinedAnnotationFiles(new File("/Users/pk/temp/Wikipedia-EN-entity-dataset-1412853485035"), new File("/Users/pk/temp"));
        // System.exit(0);

        // mineWikipedia(10, new File("/Users/pk/temp/Wikipedia-EN-entity-dataset-" + System.currentTimeMillis()));
        // WikiPage article;
        // article = MediaWikiUtil.retrieveArticle(descriptor, "Stuttgart");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "Dresden");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "Flein");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "Venice");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "Jim Carrey");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "BMW");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "Barack Obama");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "International_Academy");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "Northgate_High_School_(Newnan)");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "Brenau_University");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "USA_Boxing");
        // article = MediaWikiUtil.retrieveArticle(descriptor, "103rd Street (IND Eighth Avenue Line)");
        // System.out.println(process(article));
    }

}
