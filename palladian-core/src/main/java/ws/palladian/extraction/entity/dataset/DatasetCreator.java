package ws.palladian.extraction.entity.dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.extraction.content.ReadabilityContentExtractor;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.DownloadFilter;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.WebContent;
import ws.palladian.retrieval.search.web.BasicWebContent;
import ws.palladian.retrieval.search.web.BlekkoSearcher;
import ws.palladian.semantics.WordTransformer;

/**
 * <p>
 * The DatasetCreator crawls web pages and marks the given seed entities. The marked up pages are saved in:
 * </p>
 * <ol>
 * <li>separate (X)HTML files</li>
 * <li>separate text files (cleansed HTML)</li>
 * <li>one long text file, all text files from 2 concatenated</li>
 * </ol>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class DatasetCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetCreator.class);

    /** Number of mentions each seed entity should have at least. */
    private final int mentionsPerSeed;

    /** Number of seeds per concept. */
    private final int seedsPerConcept;

    /** The search API to use. */
    private final Searcher<WebContent> searcher;

    /** Save a map with concept name and the seeds searched for every concept. */
    private Map<String, List<String>> conceptSeeds;

    /** The filter for the crawler. We are not interested in binary files. */
    private final DownloadFilter downloadFilter;

    private final File datasetLocation;

    private final boolean queryWithConceptName;

    /**
     * <p>
     * Instantiate a new DatasetCreator.
     * </p>
     * 
     * @param datasetLocation The location to the directory where to store the dataset, not <code>null</code>.
     * @param searcher The {@link WebSearcher} to use for searching, not <code>null</code>.
     * @param seedsPerConcept The number of seed entities to take for each concept, greater zero.
     * @param mentionsPerSeed The minimum mentions which each seed entity should have at least, greater zero. This
     *            basically resembles the number of queries to the search engine per entity.
     * @param queryWithConceptName Specify whether to add the name of the concept to the query (e.g.
     *            <code>"Porsche 911" car</code>).
     */
    public DatasetCreator(File datasetLocation, Searcher<WebContent> searcher, int seedsPerConcept,
            int mentionsPerSeed, boolean queryWithConceptName) {
        Validate.notNull(datasetLocation, "datasetLocation must not be null");
        if (!datasetLocation.exists() && !datasetLocation.mkdirs()) {
            throw new IllegalStateException("Could not create directory " + datasetLocation);
        }
        Validate.isTrue(datasetLocation.isDirectory(), "datasetLocation must point to a directory");
        Validate.notNull(searcher, "searcher must not be null");
        Validate.isTrue(seedsPerConcept > 0, "seedsPerConcept must be greater zero");
        Validate.isTrue(mentionsPerSeed > 0, "mentionsPerSeed must be greater zero");

        this.datasetLocation = datasetLocation;
        downloadFilter = new DownloadFilter();
        downloadFilter.setExcludeFileTypes(FileHelper.BINARY_FILE_EXTENSIONS);
        this.searcher = searcher;
        this.seedsPerConcept = seedsPerConcept;
        this.mentionsPerSeed = mentionsPerSeed;
        this.queryWithConceptName = queryWithConceptName;
    }

    /**
     * <p>
     * Create a dataset by searching for the seed mentions and storing the complete web pages.
     * </p>
     * 
     * @param seedDirectory The path to the folder with the seed entities. Each file must be named with the concept
     *            name (_partX is ignored for markup) and there must be one seed entity per line.
     */
    private final void createDataset(File seedDirectory) {
        StopWatch stopWatch = new StopWatch();

        conceptSeeds = new HashMap<String, List<String>>();
        File[] seedFiles = FileHelper.getFiles(seedDirectory.getPath());
        Set<String> conceptsSearched = new HashSet<String>();

        // iterate over all concepts (seed files)
        for (File file : seedFiles) {
            String seedFileName = FileHelper.getFileName(file.getName());
            if (seedFileName.length() > 1) {
                createDatasetForConcept(seedFileName, file);
                conceptsSearched.add(getConceptNameFromFileName(seedFileName));
            }
        }

        writeMetaInformationFile(stopWatch, conceptsSearched);

        LOGGER.info("created {} datasets in {}, total traffic: {} MB", seedFiles.length, stopWatch,
                HttpRetriever.getTraffic(SizeUnit.MEGABYTES));
    }

    /**
     * Write meta information about the created dataset.
     * 
     * @param stopWatch The stop watch.
     * @param conceptsSearched The concepts that were searched.
     */
    private void writeMetaInformationFile(StopWatch stopWatch, Set<String> conceptsSearched) {

        StringBuilder meta = new StringBuilder();

        meta.append("Start Date of Creation: ")
        .append(DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", stopWatch.getStartTime())).append("\n");
        meta.append("Dataset created in: ").append(stopWatch.getElapsedTimeString()).append("\n");
        meta.append("Total Generated Traffic: ").append(HttpRetriever.getTraffic(SizeUnit.MEGABYTES)).append("MB\n");
        meta.append("Search Engine used: ").append(searcher.getName()).append("\n");
        meta.append("Minimum Mentions per Entity Targeted: ").append(mentionsPerSeed).append("\n");

        // check which concepts have entities with their number of mentions
        for (Object[] object : getConceptsMentions()) {
            String conceptName = (String)object[0];
            String entitiesWithFewMentions = (String)object[1];
            if (entitiesWithFewMentions.length() == 0) {
                entitiesWithFewMentions = "-";
            }
            Double averageMentionsPerEntity = (Double)object[2];
            meta.append("  Concept: ").append(conceptName).append("\n  Entities with few mentions: ")
            .append(entitiesWithFewMentions).append("\n  Average Mentions per Entity: ")
            .append(averageMentionsPerEntity).append("\n\n");
        }

        meta.append("Concepts Searched (").append(conceptsSearched.size()).append("):\n");
        for (String conceptName : conceptsSearched) {
            meta.append("    ").append(conceptName).append("\n");
        }

        FileHelper.writeToFile(new File(datasetLocation, "metaInformation.txt").getPath(), meta);
    }

    /**
     * Get information about concepts and entities that have too few mentions.
     * 
     * @return A set with information about 0: the concept name, 1: the list of entities with too few mentions, 2: the
     *         average mentions per entity.
     */
    private Set<Object[]> getConceptsMentions() {

        Set<Object[]> objectSet = new HashSet<Object[]>();

        if (conceptSeeds == null) {
            conceptSeeds = new HashMap<String, List<String>>();

            File[] seedFiles = FileHelper.getFiles(datasetLocation.getPath());
            for (File file : seedFiles) {
                String conceptName = FileHelper.getFileName(file.getName());
                List<String> seeds = FileHelper.readFileToArray(new File(datasetLocation, conceptName
                        + "/seeds/seeds.txt"));

                if (seeds.isEmpty()) {
                    continue;
                }

                List<String> seedNames = new ArrayList<String>();
                for (String string : seeds) {
                    String[] seedLine = string.split("###");
                    seedNames.add(seedLine[0]);
                }
                conceptSeeds.put(conceptName, seedNames);
            }
        }

        // iterate over all concepts (seed files)
        for (Entry<String, List<String>> conceptSeedEntry : conceptSeeds.entrySet()) {

            String seedFileName = conceptSeedEntry.getKey();

            Object[] o = new Object[3];
            o[0] = seedFileName;

            File[] markedUpFiles = FileHelper.getFiles(new File(datasetLocation, seedFileName).getPath());
            CountMap<String> countMap = CountMap.create();
            for (File markedUpFile : markedUpFiles) {
                if (markedUpFile.isDirectory()) {
                    continue;
                }
                for (String seedEntity : conceptSeedEntry.getValue()) {

                    String fileContent = FileHelper.readFileToString(markedUpFile);

                    // count occurrences of the seed entity
                    Pattern pattern = Pattern.compile("<.*?>\\s?" + seedEntity + "\\s?</.*?>", Pattern.MULTILINE);
                    Matcher matcher = pattern.matcher(fileContent);

                    while (matcher.find()) {
                        countMap.add(seedEntity);
                    }
                }
            }

            String entitiesWithFewMentions = "";
            int totalMentions = 0;
            for (String item : countMap) {
                int count = countMap.getCount(item);
                if (count < mentionsPerSeed) {
                    entitiesWithFewMentions += item + "(" + count + "), ";
                }
                totalMentions += count;
            }
            o[1] = entitiesWithFewMentions;
            o[2] = totalMentions / (double)countMap.size();

            objectSet.add(o);
        }

        return objectSet;
    }

    /**
     * File names can contain "_partX" which should be ignored for concept tagging.<br>
     * politician_part1 => politician
     * 
     * @param fileName The name of the seed file.
     * @return The name of the concept.
     */
    private static String getConceptNameFromFileName(String fileName) {
        return WordTransformer.wordToSingular(fileName.replaceAll("_part(\\d)", ""), Language.ENGLISH);
    }

    /**
     * Create the dataset for one certain concept.
     * 
     * @param seedFileName The name of the concept (equals the name of the seed file).
     * @param seedFile The file with entity seeds.
     */
    private void createDatasetForConcept(String seedFileName, File seedFile) {

        LOGGER.info("Creating dataset for {}", seedFileName);

        DocumentRetriever urlDownloader = new DocumentRetriever();
        urlDownloader.setDownloadFilter(downloadFilter);
        List<String> seedEntities = FileHelper.readFileToArray(seedFile);

        // remove first line which is not a seed
        seedEntities.remove(0);

        // get a random sample of seeds from the list
        Collection<String> randomSet = MathHelper.randomSample(seedEntities, seedsPerConcept);

        seedEntities = new ArrayList<String>(randomSet);

        // write a seed file in classification format with all the seeds used for the current concept
        StringBuilder seedFileCopy = new StringBuilder();

        ProgressMonitor progressMonitor = new ProgressMonitor(seedEntities.size(), 1);
        int entityCount = 0;

        for (String seedEntity : seedEntities) {

            StopWatch sw = new StopWatch();

            // ProgressHelper.printProgress(entityCount, seedEntities.size(), 1, stopWatch);
            progressMonitor.incrementAndPrintProgress();
            LOGGER.info("start processing seed entity {} ({})", seedEntity, seedFileName);

            seedFileCopy.append(seedEntity).append("###")
                    .append(getConceptNameFromFileName(seedFileName).toUpperCase()).append("\n");

            List<String> urls = getWebPages(seedEntity, seedFileName);

            Set<Document> documents = urlDownloader.getWebDocuments(urls);
            LOGGER.info("downloaded {} URLs for ({})", urls.size(), seedEntity, seedFileName);

            entityCount++;
            int urlCount = 0;

            for (Document document : documents) {

                if (document == null) {
                    continue;
                }
                markupWebPage(document, seedFileName, seedEntities);
                urlCount++;

                LOGGER.debug("marked up page {} {}/{}, {}/{}", document.getDocumentURI(), entityCount,
                        seedEntities.size(), urlCount, urls.size());
            }

            LOGGER.info("processed seed entity: {} ({}) in {}", seedEntity, seedFileName, sw);
        }

        conceptSeeds.put(seedFileName, seedEntities);

        // write the seed file into a special folder
        FileHelper.writeToFile(new File(datasetLocation, seedFileName + "/seeds/seeds.txt").getPath(), seedFileCopy);

        LOGGER.info("created dataset for concept {} with {} seeds in {}", seedFileName, seedEntities.size(),
                progressMonitor.getTotalElapsedTimeString());
    }

    /**
     * Get a list of URLs to web pages that contain the seed entity.
     * 
     * @param seedEntity The name of the seed entity.
     * @param conceptName The name of the concept.
     * @return A list of URLs containing the seed entity.
     */
    private List<String> getWebPages(String seedEntity, String conceptName) {
        LOGGER.info("get web pages for seed '{}' with {}", seedEntity, searcher);

        String query = "\"" + seedEntity + "\"";
        if (queryWithConceptName) {
            query += " " + conceptName.toLowerCase();
        }
        try {
            return searcher.searchUrls(query, mentionsPerSeed, Language.ENGLISH);
        } catch (SearcherException e) {
            LOGGER.error("Searcher exception while searching for {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * Mark up all seed entities for the concept on the web page. Save the marked up html and text.
     * 
     * @param webPage The web page to mark up.
     * @param seedFileName The name of the concept.
     * @param seedEntities A list of seed entities that should be searched after and marked up.
     */
    private void markupWebPage(Document webPage, String seedFileName, List<String> seedEntities) {

        LOGGER.debug("mark up web page: {} ({})", webPage.getDocumentURI(), seedFileName);

        String conceptName = getConceptNameFromFileName(seedFileName);
        String webPageContent;
        String webPageText;
        try {
            webPageContent = HtmlHelper.xmlToString(webPage, false);
            webPageText = new ReadabilityContentExtractor().setDocument(webPage).getResultText();

            if (webPageText.length() < 100) {
                return;
            }
        } catch (Exception e) {
            LOGGER.error("could not extract clean content from {}: {}", webPage.getDocumentURI(), e.getMessage());
            return;
        } catch (Error e) {
            LOGGER.error("could not extract clean content from {}: {}", webPage.getDocumentURI(), e.getMessage());
            return;
        }

        // remove tags
        webPageText = HtmlHelper.stripHtmlTags(webPageText);

        // mark up all seed entities
        boolean foundMarkup = false;
        for (String seedEntity : seedEntities) {

            try {

                String escapedSeed = Pattern.quote(seedEntity);

                // XXX hard-coded special treatment for type "PER"; here also the last part is marked up
                // (e.g. "Tina Turner" -> "Turner)
                if ("person".equalsIgnoreCase(conceptName)) {
                    String lastName = seedEntity.substring(seedEntity.lastIndexOf(' ') + 1, seedEntity.length());
                    escapedSeed = "(" + Pattern.quote(seedEntity) + "|" + Pattern.quote(lastName) + ")";
                }
                String searchRegexp = "(?<=\\s)" + escapedSeed + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])" + escapedSeed
                        + "(?=(\\s|[.,!?]))";

                if (webPageText.contains(seedEntity)) {
                    foundMarkup = true;

                    // mark up html
                    webPageContent = webPageContent.replaceAll(searchRegexp,
                            "<" + conceptName.toUpperCase() + " style=\"background-color:red; color:white;\">"
                                    + seedEntity + "</" + conceptName.toUpperCase() + ">");

                    // mark up text
                    webPageText = webPageText.replaceAll(searchRegexp, "<" + conceptName.toUpperCase() + ">"
                            + seedEntity + "</" + conceptName.toUpperCase() + ">");

                }
            } catch (Throwable t) {
                LOGGER.error("something went wrong marking up the page content with seed {}, {}", seedEntity,
                        t.getMessage());
            }
            LOGGER.debug("marked up page {} with entity {}", webPage.getDocumentURI(), seedEntity);
        }

        // save web page
        if (webPageContent.length() > 100 && foundMarkup) {
            FileHelper.writeToFile(
                    new File(datasetLocation, seedFileName + "/html/"
                            + StringHelper.makeSafeName(UrlHelper.getCleanUrl(webPage.getDocumentURI()), 30) + ".html")
                            .getPath(), webPageContent);
            LOGGER.debug("saved html file");
        }

        // save text
        if (webPageText.length() > 50 && foundMarkup) {

            File filePath = new File(datasetLocation, seedFileName + "/"
                    + StringHelper.makeSafeName(webPage.getDocumentURI(), 30) + ".xml");
            FileHelper.writeToFile(filePath.getPath(), webPageText);

            FileHelper.removeDuplicateLines(filePath.getPath(), filePath.getPath());

            LOGGER.debug("saved text file");
        }
    }

    private static void cleanDataset(File datasetLocation, File seedEntityDirectory, boolean copyToNewFolder) {

        StopWatch stopWatch = new StopWatch();

        File targetLocation = datasetLocation;

        if (copyToNewFolder) {
            targetLocation = new File(targetLocation.getPath() + "_cleansed");
        }

        File[] seedFiles = FileHelper.getFiles(seedEntityDirectory.getPath());

        // iterate over all concepts (seed files)
        for (File file : seedFiles) {

            String seedFileName = FileHelper.getFileName(file.getName());
            String conceptName = getConceptNameFromFileName(seedFileName);

            if (seedFileName.length() > 1) {

                File entityDirectory = new File(datasetLocation, seedFileName);
                File[] taggedFiles = FileHelper.getFiles(entityDirectory.getPath());

                // iterate over all text files
                for (File taggedFile : taggedFiles) {

                    if (taggedFile.isDirectory()) {
                        continue;
                    }

                    String content = FileHelper.readFileToString(taggedFile);

                    String cleansedText = cleanText(content, conceptName);

                    if (cleansedText.length() > 10) {

                        File filePath = new File(entityDirectory, FileHelper.getFileName(taggedFile.getPath()) + ".xml");
                        FileHelper.writeToFile(filePath.getPath(), cleansedText);

                        FileHelper.removeDuplicateLines(filePath.getPath(), filePath.getPath());

                        LOGGER.debug("saved cleansed text file");
                    }
                }
            }
        }

        // copy the meta information file to the new directory
        if (copyToNewFolder) {
            FileHelper.copyFile(new File(datasetLocation, "/metaInformation.txt").getPath(), new File(targetLocation,
                    "/metaInformation.txt").getPath());
        }

        LOGGER.info("dataset cleansed in {}", stopWatch);
    }

    /**
     * Remove sets of short lines which are usually tables or other irrelevant content that was incorrectly added as
     * page content.
     * 
     * @param inputText The text that should be cleansed.
     * @param tagName The name of the tag.
     * @return The cleansed text.
     */
    private static String cleanText(String inputText, String tagName) {

        String text = inputText;

        try {
            // remove sets of lines that are too short
            text = text.replaceAll("(\n)+(.{0,80}(\n)){4,}", "\n");

            Pattern pattern;
            Matcher matcher;

            // remove lines without mentions
            // pattern = Pattern.compile("^((?!" + tagName.toUpperCase() + ").)*$", Pattern.MULTILINE);
            // matcher = pattern.matcher(text);
            // text = matcher.replaceAll("");

            // remove lines without context around the entity
            pattern = Pattern.compile("^<" + tagName.toUpperCase() + ">.*?</" + tagName.toUpperCase() + ">$",
                    Pattern.MULTILINE);
            matcher = pattern.matcher(text);
            text = matcher.replaceAll("");

            // remove gaps
            text = text.replaceAll("(\n){3,}", "\n");

            // remove empty line in the beginning
            // text = text.replaceAll("^\n", "");

        } catch (Throwable t) {
            LOGGER.error("Encountered {}", t.toString());
            text = "";
        }

        return text;
    }

    /**
     * Perform cleanup and combining tasks. In particular, create a single text file for each concept containing all
     * file contents.
     */
    private static void postProcessDataset(File seedDirectory, File dataSetLocation) {

        StopWatch stopWatch = new StopWatch();

        File[] seedFiles = FileHelper.getFiles(seedDirectory.getPath());

        List<File> combinedFilePaths = new ArrayList<File>();

        // iterate over all concepts (seed files)
        for (File file : seedFiles) {
            String seedFileName = FileHelper.getFileName(file.getName());

            String conceptName = StringHelper.makeCamelCase(
                    WordTransformer.wordToSingular(seedFileName, Language.ENGLISH), true);

            if (seedFileName.length() == 0) {
                continue;
            }

            File combinedDirectory = new File(dataSetLocation, seedFileName + "/combined");
            combinedDirectory.mkdirs();
            File filePath = new File(combinedDirectory, "all" + conceptName + ".xml");
            FileWriter combinedFile = null;
            combinedFilePaths.add(filePath);
            try {
                combinedFile = new FileWriter(filePath);

                File[] taggedFiles = FileHelper.getFiles(new File(dataSetLocation, seedFileName).getPath());
                int counter = 0;
                for (File taggedFile : taggedFiles) {

                    if (taggedFile.isDirectory()) {
                        continue;
                    }

                    String content = FileHelper.readFileToString(taggedFile);

                    if (content.length() < 5) {
                        continue;
                    }

                    counter++;
                    content = "\n\n----------------------------------------------- NEW DOCUMENT (#" + counter + " / "
                            + conceptName + ") -----------------------------------------------\n\n" + content;

                    combinedFile.write(content);
                    combinedFile.write("\n");
                }
            } catch (IOException e) {
                LOGGER.error("Error while writing to {}: {}", filePath, e);
            } finally {
                FileHelper.close(combinedFile);
            }
        }

        // combined all combined files from each concept to one super combined file
        FileWriter combinedFile = null;
        try {
            combinedFile = new FileWriter(new File(dataSetLocation, "all.xml"));
            for (File combinedFilePath : combinedFilePaths) {
                String content = FileHelper.readFileToString(combinedFilePath);
                content = "\n\n----------------------------------------------- NEW CONCEPT -----------------------------------------------"
                        + content;
                combinedFile.write(content);
                combinedFile.flush();
            }
            combinedFile.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            FileHelper.close(combinedFile);
        }
        LOGGER.info("post processed dataset in {}", stopWatch);
    }

    /**
     * <p>
     * Generate a dataset from the given seed file.
     * </p>
     * 
     * @param seedFile The file containing the entity seeds, not <code>null</code>. File must be in tab-separated column
     *            format.
     */
    public void generateDataset(File seedFile) {
        Validate.notNull(seedFile, "seedFile must not be null");
        Validate.isTrue(seedFile.isFile(), "seedFile must point to a file");

        // get seed annotations from the training file
        generateDataset(FileFormatParser.getSeedAnnotations(seedFile.getPath(), seedsPerConcept));
    }

    /**
     * <p>
     * Generate a dataset from the given annotations.
     * </p>
     * 
     * @param annotations The annotations with value/tag type.
     */
    public void generateDataset(Collection<? extends Annotation> annotations) {
        Validate.notNull(annotations, "annotations must not be null");

        StopWatch stopWatch = new StopWatch();

        LOGGER.info("start generating dataset with {} seeds per concept and at least {} mentions per seed",
                seedsPerConcept, mentionsPerSeed);


        // write the seeds to files
        Map<String, StringBuilder> fileMap = new HashMap<String, StringBuilder>();
        for (Annotation annotation : annotations) {
            StringBuilder seedFileContent = fileMap.get(annotation.getTag());
            if (seedFileContent == null) {
                seedFileContent = new StringBuilder();
                // we need to write a header
                seedFileContent.append("Seeds for ").append(annotation.getTag()).append("\n");
                fileMap.put(annotation.getTag(), seedFileContent);
            }

            seedFileContent.append(annotation.getValue()).append("\n");
        }

        File seedEntityDirectory = new File(datasetLocation, "seedEntities");
        seedEntityDirectory.mkdir();
        Set<Entry<String, StringBuilder>> entrySet = fileMap.entrySet();
        for (Entry<String, StringBuilder> entry : entrySet) {
            File outputFile = new File(seedEntityDirectory, entry.getKey() + ".txt");
            FileHelper.writeToFile(outputFile.getPath(), entry.getValue());
        }

        createDataset(seedEntityDirectory);

        cleanDataset(datasetLocation, seedEntityDirectory, false);
        postProcessDataset(seedEntityDirectory, datasetLocation);

        // replace "new document" and "new concept" with proper string "docstart" and "" respectively
        String content = FileHelper.readFileToString(new File(datasetLocation, "all.xml"));
        content = content.replaceAll("-+ NEW CONCEPT.*", "");
        content = content.replaceAll("-+ NEW DOCUMENT .#.*", "=-<DOCSTART>-");

        // delete all lines containing no tagged entity
        Pattern pattern = Pattern.compile("^((?!<[A-Z]{1,20}?>).)*$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        content = matcher.replaceAll("");

        content = content.replace("=-<DOCSTART>-", "=-DOCSTART-");

        content = content.replaceAll("(\n){3,}", "\n");

        pattern = Pattern.compile("(=-DOCSTART-\n?){1,}");
        matcher = pattern.matcher(content);
        content = matcher.replaceAll("\n=-DOCSTART-\n\n");

        content = content.replaceAll("(\n){3,}", "\n");

        String cleansedXmlFile = new File(datasetLocation, "allCleansed.xml").getPath();
        FileHelper.writeToFile(cleansedXmlFile, content);

        String finalColumnTaggedFile = new File(datasetLocation, "allColumn.txt").getPath();
        FileFormatParser.xmlToColumn(cleansedXmlFile, finalColumnTaggedFile, "\t");

        // get the broken DOCSTART lines correct
        content = FileHelper.readFileToString(finalColumnTaggedFile);
        content = content.replaceAll("=-\tO\nDOCSTART\tO\n-\tO", "=-DOCSTART-\tO");

        FileHelper.writeToFile(finalColumnTaggedFile, content);

        LOGGER.info("generated dataset in {}", stopWatch);
    }

    /**
     * <p>
     * Generate datasets with increasing number of seeds.
     * </p>
     * 
     * @param datasetLocation The location to the directory where to store the dataset, not <code>null</code>.
     * @param searcher The {@link WebSearcher} to use for searching, not <code>null</code>.
     * @param seedFile The file containing the entity seeds, not <code>null</code>. File must be in tab-separated column
     *            format.
     * @param minSeeds Starting/minimum number number of seeds, greater zero.
     * @param maxSeeds Ending/maximum number of seeds, greater minSeeds.
     * @param mentionsPerSeed The minimum mentions which each seed entity should have at least, greater zero. This
     *            basically resembles the number of queries to the search engine per entity.
     */
    public static void generateDatasets(File datasetLocation, Searcher<WebContent> searcher, File seedFile,
            int minSeeds, int maxSeeds, int mentionsPerSeed) {
        Validate.notNull(datasetLocation, "datasetLocation must not be null");
        if (!datasetLocation.exists() && !datasetLocation.mkdirs()) {
            throw new IllegalStateException("Could not create directory " + datasetLocation);
        }
        Validate.isTrue(datasetLocation.isDirectory(), "datasetLocation must point to a directory");
        Validate.notNull(searcher, "searcher must not be null");
        Validate.isTrue(minSeeds > 0, "minSeeds must be greater zero");
        Validate.isTrue(maxSeeds > minSeeds, "maxSeeds must be greater minSeeds");
        Validate.isTrue(mentionsPerSeed > 0, "mentionsPerSeed must be greater zero");

        for (int seeds = minSeeds; seeds <= maxSeeds; seeds++) {
            File currentLocation = new File(datasetLocation, String.valueOf(seeds));
            DatasetCreator dsc = new DatasetCreator(currentLocation, searcher, seeds, mentionsPerSeed, true);
            dsc.generateDataset(seedFile);

            // copy the cleansed, combined "allColumn.txt" from each subfolder in the main folder
            FileHelper.copyFile(new File(currentLocation, "allColumn.txt").getPath(), new File(datasetLocation,
                    "seedsTest" + seeds + ".txt").getPath());
        }
    }

    public static void main(String[] args) {

        Searcher<WebContent> searcher = new BlekkoSearcher();

        File outputDirectory = new File("/Volumes/iMac HD/temp/locationNerDataset");
        DatasetCreator datasetCreator = new DatasetCreator(outputDirectory, searcher, 800, 100, false);
        datasetCreator.generateDataset(new File("/Users/pk/Dropbox/LocationLab/ALL_per+loc_shuf.txt"));

        // DatasetCreator.generateDatasets(new File("data/temp/autoGeneratedData/www_eval_seeds"), searcher, new File(
        // "data/datasets/ner/conll/allConll.txt"), 1, 50, 5);
    }

}