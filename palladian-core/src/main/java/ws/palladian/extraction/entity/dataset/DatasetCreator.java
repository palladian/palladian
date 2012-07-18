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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.content.ReadabilityContentExtractor;
import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.helper.ConfigHolder;
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
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.DownloadFilter;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.SearcherFactory;
import ws.palladian.retrieval.search.web.BingSearcher;
import ws.palladian.retrieval.search.web.GoogleSearcher;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;
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
 * 
 */
public class DatasetCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class);

    /** The name of the dataset. */
    private String datasetName = "untitled";

    /** Number of mentions each seed entity should have at least. */
    private int mentionsPerEntity = 10;

    /** Number of seeds per concept. */
    private int seedsPerConcept = 30;

    /** The location where the dataset is stored. */
    private String dataSetLocation = "data/datasets/ner/";

    /** The search API to use. */
    private WebSearcher<WebResult> searcher = new GoogleSearcher();

    /** Save a map with concept name and the seeds searched for every concept. */
    private Map<String, List<String>> conceptSeeds;

    /** The filter for the crawler. We are not interested in binary files. */
    private final DownloadFilter downloadFilter;

    public DatasetCreator(String datasetName) {
        this.datasetName = datasetName;
        downloadFilter = new DownloadFilter();
        downloadFilter.setExcludeFileTypes(FileHelper.BINARY_FILE_EXTENSIONS);
    }

    /**
     * <p>
     * Create a dataset by searching for the seed mentions and storing the complete web pages.
     * </p>
     * 
     * @param seedFolderPath The path to the folder with the seed entities. Each file must be named with the concept
     *            name (_partX is ignored for markup) and there must be one seed entity per line.
     */
    public final void createDataset(String seedFolderPath) {
        StopWatch stopWatch = new StopWatch();

        conceptSeeds = new HashMap<String, List<String>>();

        File[] seedFiles = FileHelper.getFiles(seedFolderPath);

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

        // postProcessDataset(seedFolderPath, getDataSetLocation() + getDatasetName() + "/");

        LOGGER.info("created " + seedFiles.length + " datasets in " + stopWatch.getElapsedTimeString()
                + ", total traffic: " + HttpRetriever.getSessionDownloadSize(SizeUnit.MEGABYTES) + "MB");
    }

    /**
     * Write meta information about the created dataset.
     * 
     * @param stopWatch The stop watch.
     * @param conceptsSearched The concepts that were searched.
     */
    protected void writeMetaInformationFile(StopWatch stopWatch, Set<String> conceptsSearched) {
        writeMetaInformationFile(stopWatch, conceptsSearched, "");
    }

    protected void writeMetaInformationFile(StopWatch stopWatch, Set<String> conceptsSearched, String seedFolderPath) {

        if (conceptsSearched == null) {

            conceptsSearched = new HashSet<String>();

            File[] seedFiles = FileHelper.getFiles(seedFolderPath);

            // iterate over all concepts (seed files)
            for (File file : seedFiles) {

                String seedFileName = FileHelper.getFileName(file.getName());
                if (seedFileName.length() > 1) {
                    conceptsSearched.add(getConceptNameFromFileName(seedFileName));
                }
            }

        }

        StringBuilder meta = new StringBuilder();

        meta.append("Start Date of Creation: ")
        .append(DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", stopWatch.getStartTime())).append("\n");
        meta.append("Dataset created in: ").append(stopWatch.getElapsedTimeString()).append("\n");
        meta.append("Total Generated Traffic: ").append(HttpRetriever.getSessionDownloadSize(SizeUnit.MEGABYTES))
        .append("MB\n");
        meta.append("Search Engine used: ").append(searcher.getName()).append("\n");
        meta.append("Minimum Mentions per Entity Targeted: ").append(getMentionsPerEntity()).append("\n");

        // check which concepts have entities with their number of mentions
        for (Object[] object : getConceptsMentions()) {
            String conceptName = (String) object[0];
            String entitiesWithFewMentions = (String) object[1];
            if (entitiesWithFewMentions.length() == 0) {
                entitiesWithFewMentions = "-";
            }
            Double averageMentionsPerEntity = (Double) object[2];
            meta.append("  Concept: ").append(conceptName).append("\n  Entities with few mentions: ")
            .append(entitiesWithFewMentions).append("\n  Average Mentions per Entity: ")
            .append(averageMentionsPerEntity).append("\n\n");
        }

        meta.append("Concepts Searched (").append(conceptsSearched.size()).append("):\n");
        for (String conceptName : conceptsSearched) {
            meta.append("    ").append(conceptName).append("\n");
        }

        FileHelper.writeToFile(getDataSetLocation() + "metaInformation.txt", meta);
    }

    /**
     * Get information about concepts and entities that have too few mentions.
     * 
     * @return A set with information about 0: the concept name, 1: the list of entities with too few mentions, 2: the
     *         average mentions per entity.
     */
    protected Set<Object[]> getConceptsMentions() {

        Set<Object[]> objectSet = new HashSet<Object[]>();

        if (conceptSeeds == null) {
            conceptSeeds = new HashMap<String, List<String>>();

            File[] seedFiles = FileHelper.getFiles(getDataSetLocation());
            for (File file : seedFiles) {
                String conceptName = FileHelper.getFileName(file.getName());
                List<String> seeds = FileHelper.readFileToArray(getDataSetLocation() + "/" + conceptName
                        + "/seeds/seeds.txt");

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
            // String seedFileName = FileHelper.getFileName(file.getName());

            String seedFileName = conceptSeedEntry.getKey();

            Object[] o = new Object[3];
            o[0] = seedFileName;

            File[] markedUpFiles = FileHelper.getFiles(getDataSetLocation() + seedFileName);
            CountMap countMap = new CountMap();
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
                        countMap.increment(seedEntity);
                    }
                }
            }

            String entitiesWithFewMentions = "";
            int totalMentions = 0;
            for (Entry<Object, Integer> entry : countMap.entrySet()) {
                if (entry.getValue() < getMentionsPerEntity()) {
                    entitiesWithFewMentions += entry.getKey() + "(" + entry.getValue() + "), ";
                }
                totalMentions += entry.getValue();
            }
            o[1] = entitiesWithFewMentions;
            o[2] = totalMentions / (double) countMap.size();

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

        StopWatch stopWatch = new StopWatch();

        DocumentRetriever urlDownloader = new DocumentRetriever();
        urlDownloader.setDownloadFilter(downloadFilter);
        List<String> seedEntities = FileHelper.readFileToArray(seedFile);

        // remove first line which is not a seed
        seedEntities.remove(0);

        // get a random sample of seeds from the list
        Collection<String> randomSet = MathHelper.randomSample(seedEntities, getSeedsPerConcept());

        seedEntities = new ArrayList<String>();
        seedEntities.addAll(randomSet);

        // mix the entities
        // Set<String> mixedSeedEntities = new HashSet<String>();
        // mixedSeedEntities.addAll(seedEntities);

        // write a seed file in classification format with all the seeds used for the current concept
        StringBuilder seedFileCopy = new StringBuilder();

        int ec = 0;
        for (String seedEntity : seedEntities) {

            StopWatch sw = new StopWatch();

            LOGGER.info("start processing seed entity " + seedEntity + " (" + seedFileName + ")");

            seedFileCopy.append(seedEntity).append("###")
            .append(getConceptNameFromFileName(seedFileName).toUpperCase()).append("\n");

            List<String> urls = getWebPages(seedEntity, seedFileName);

            Set<Document> documents = urlDownloader.getWebDocuments(urls);
            LOGGER.info("downloaded " + urls.size() + " URLs for " + seedEntity + " (" + seedFileName + ")");

            ec++;
            int uc = 0;

            for (Document document : documents) {

                if (document == null) {
                    continue;
                }
                markupWebPage(document, seedFileName, seedEntities);
                uc++;

                LOGGER.debug("marked up page " + document.getDocumentURI() + " " + ec + "/" + seedEntities.size()
                        + ", " + uc + "/" + urls.size());
            }

            LOGGER.info("processed seed entity:" + seedEntity + " (" + seedFileName + ")" + " in "
                    + sw.getElapsedTimeString());
        }

        conceptSeeds.put(seedFileName, seedEntities);

        // write the seed file into a special folder
        FileHelper.writeToFile(getDataSetLocation() + seedFileName + "/seeds/seeds.txt", seedFileCopy);

        // remove duplicate lines from combined file
        // FileHelper.removeDuplicateLines(getDataSetLocation() + conceptName + "/text/all.xml", getDataSetLocation()+
        // conceptName + "/text/all.xml");

        LOGGER.info("created dataset for concept " + seedFileName + " with " + seedEntities.size() + " seeds in "
                + stopWatch.getElapsedTimeString());
    }

    /**
     * Get a list of URLs to web pages that contain the seed entity.
     * 
     * @param seedEntity The name of the seed entity.
     * @return A list of URLs containing the seed entity.
     */
    private List<String> getWebPages(String seedEntity, String conceptName) {
        LOGGER.info("get web pages for seed: " + seedEntity);

        String query = "\"" + seedEntity + "\" " + conceptName.toLowerCase();
        List<String> result = Collections.emptyList();
        try {
            result = searcher.searchUrls(query, getMentionsPerEntity(), Language.ENGLISH);
        } catch (SearcherException e) {
            LOGGER.error(e);
        }
        return result;
    }

    /**
     * Mark up all seed entities for the concept on the web page. Save the marked up html and text.
     * 
     * @param webPage The web page to mark up.
     * @param seedFileName The name of the concept.
     * @param seedEntities A list of seed entities that should be searched after and marked up.
     */
    private void markupWebPage(Document webPage, String seedFileName, List<String> seedEntities) {

        LOGGER.debug("mark up web page: " + webPage.getDocumentURI() + " (" + seedFileName + ")");

        String conceptName = getConceptNameFromFileName(seedFileName);

        // Crawler c = new Crawler();
        // webPage = c.getWebDocument("http://www.letourdefrance.btinternet.co.uk/vin01.html");

        String webPageContent = "";

        String webPageText = "";
        try {
            webPageContent = HtmlHelper.xmlToString(webPage, false);
            // webPageText = new PageContentExtractor().setDocument(webPage).getResultText();
            webPageText = new ReadabilityContentExtractor().setDocument(webPage).getResultText();

            if (webPageText.length() < 100) {
                return;
            }
        } catch (Exception e) {
            LOGGER.error("could not extract clean content from " + webPage.getDocumentURI() + ", " + e.getMessage());
            return;
        } catch (Error e) {
            LOGGER.error("could not extract clean content from " + webPage.getDocumentURI() + ", " + e.getMessage());
            return;
        }

        // remove tags
        webPageText = HtmlHelper.stripHtmlTags(webPageText);

        // mark up all seed entities
        boolean foundMarkup = false;
        for (String seedEntity : seedEntities) {

            try {

                String escapedSeed = StringHelper.escapeForRegularExpression(seedEntity);
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

            } catch (Error e) {
                LOGGER.error("something went wrong marking up the page content with seed " + seedEntity + ", "
                        + e.getMessage());
            } catch (Exception e) {
                LOGGER.error("something went wrong marking up the page content with seed " + seedEntity + ", "
                        + e.getMessage());
            }
            LOGGER.debug("marked up page " + webPage.getDocumentURI() + " with entity " + seedEntity);
        }

        // save web page
        if (webPageContent.length() > 100 && foundMarkup) {
            FileHelper.writeToFile(
                    getDataSetLocation() + seedFileName + "/html/"
                    + StringHelper.makeSafeName(UrlHelper.getCleanUrl(webPage.getDocumentURI()), 30) + ".html",
                    webPageContent);

            LOGGER.debug("saved html file");
        }

        // save text
        if (webPageText.length() > 50 && foundMarkup) {

            String filePath = getDataSetLocation() + seedFileName + "/"
            + StringHelper.makeSafeName(webPage.getDocumentURI(), 30) + ".xml";
            FileHelper.writeToFile(filePath, webPageText);

            FileHelper.removeDuplicateLines(filePath, filePath);

            LOGGER.debug("saved text file");
        }
    }

    public static void cleanDataset(String datasetRoot, String datasetName, String seedFolderPath,
            boolean copyToNewFolder) {

        StopWatch stopWatch = new StopWatch();

        String sourceLocation = datasetRoot + datasetName + "/";
        String targetLocation = sourceLocation;

        if (copyToNewFolder) {
            targetLocation += "_cleansed/";
        }

        File[] seedFiles = FileHelper.getFiles(seedFolderPath);

        // iterate over all concepts (seed files)
        for (File file : seedFiles) {

            String seedFileName = FileHelper.getFileName(file.getName());
            String conceptName = getConceptNameFromFileName(seedFileName);

            if (seedFileName.length() > 1) {

                File[] taggedFiles = FileHelper.getFiles(datasetRoot + datasetName + "/" + seedFileName + "/");

                // iterate over all text files
                for (File taggedFile : taggedFiles) {

                    if (taggedFile.isDirectory()) {
                        continue;
                    }

                    String content = FileHelper.readFileToString(taggedFile);

                    String cleansedText = cleanText(content, conceptName);

                    if (cleansedText.length() > 10) {

                        String filePath = targetLocation + seedFileName + "/"
                        + FileHelper.getFileName(taggedFile.getPath()) + ".xml";
                        FileHelper.writeToFile(filePath, cleansedText);

                        FileHelper.removeDuplicateLines(filePath, filePath);

                        LOGGER.debug("saved cleansed text file");
                    }

                }
            }

        }

        // copy the meta information file to the new directory
        if (copyToNewFolder) {
            FileHelper.copyFile(sourceLocation + "/metaInformation.txt", targetLocation + "/metaInformation.txt");
        }

        LOGGER.info("dataset cleansed in " + stopWatch.getElapsedTimeString());
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

        } catch (Error e) {
            LOGGER.error(e.getMessage());
            text = "";
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            text = "";
        }

        return text;
    }

    /**
     * Remove possibly duplicate entries from seed files.
     * 
     * @param seedFolderPath The path to the folder that holds the seed files.
     */
    public static void deduplicateSeedLists(String seedFolderPath) {

        File[] seedFiles = FileHelper.getFiles(seedFolderPath);

        for (File file : seedFiles) {
            FileHelper.removeDuplicateLines(file.getAbsolutePath(), file.getAbsolutePath());
        }

    }

    /**
     * Perform cleanup and combining tasks. In particular, create a single text file for each concept containing all
     * file contents.
     */
    public static void postProcessDataset(String seedFolderPath, String dataSetLocation) {

        StopWatch stopWatch = new StopWatch();

        File[] seedFiles = FileHelper.getFiles(seedFolderPath);

        List<String> combinedFilePaths = new ArrayList<String>();

        // iterate over all concepts (seed files)
        for (File file : seedFiles) {
            String seedFileName = FileHelper.getFileName(file.getName());

            String conceptName = StringHelper.makeCamelCase(
                    WordTransformer.wordToSingular(seedFileName, Language.ENGLISH), true);

            if (seedFileName.length() == 0) {
                continue;
            }

            FileWriter combinedFile = null;
            try {
                String filePath = dataSetLocation + seedFileName + "/combined/all" + conceptName + ".xml";
                new File(FileHelper.getFilePath(filePath)).mkdirs();
                combinedFile = new FileWriter(filePath);
                combinedFilePaths.add(filePath);
            } catch (IOException e) {
                LOGGER.error("could not create file, " + e.getMessage());
            }

            File[] taggedFiles = FileHelper.getFiles(dataSetLocation + seedFileName + "/");
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

                try {
                    combinedFile.write(content);
                    combinedFile.write("\n");
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            }

            try {
                combinedFile.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            } finally {
                FileHelper.close(combinedFile);
            }
        }

        // combined all combined files from each concept to one super combined file
        FileWriter combinedFile = null;
        try {
            combinedFile = new FileWriter(dataSetLocation + "all.xml");
        } catch (IOException e) {
            LOGGER.error("couldn't create all.xml file, " + e.getMessage());
        }
        for (String combinedFilePath : combinedFilePaths) {
            String content = FileHelper.readFileToString(combinedFilePath);
            content = "\n\n----------------------------------------------- NEW CONCEPT -----------------------------------------------"
                + content;
            try {
                combinedFile.write(content);
                combinedFile.flush();
            } catch (IOException e) {
                LOGGER.error("could not combine files, " + e.getMessage());
            }
        }

        try {
            combinedFile.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            FileHelper.close(combinedFile);
        }

        LOGGER.info("post processed dataset in " + stopWatch.getElapsedTimeString());
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setMentionsPerEntity(int mentionsPerEntity) {
        this.mentionsPerEntity = mentionsPerEntity;
    }

    public int getMentionsPerEntity() {
        return mentionsPerEntity;
    }

    public void setSeedsPerConcept(int seedsPerConcept) {
        this.seedsPerConcept = seedsPerConcept;
    }

    public int getSeedsPerConcept() {
        return seedsPerConcept;
    }

    public String getDataSetLocation() {
        return dataSetLocation + getDatasetName() + "/";
    }

    public void setDataSetLocation(String dataSetLocation) {
        this.dataSetLocation = dataSetLocation;
    }

    public void setWebSearcher(WebSearcher<WebResult> searcher) {
        this.searcher = searcher;
    }

    public WebSearcher<WebResult> getWebSearcher() {
        return searcher;
    }

    public String generateDataset(String trainingFilePath, int numberOfSeedsPerConcept, int minMentionsPerSeed) {

        StopWatch stopWatch = new StopWatch();

        LOGGER.info("start generating dataset with " + numberOfSeedsPerConcept + " seeds per concept and at least "
                + minMentionsPerSeed + " mentions per seed");

        // get seed annotations from the training file
        Annotations annotations = FileFormatParser.getSeedAnnotations(trainingFilePath, numberOfSeedsPerConcept);

        // write the seeds to files
        Map<String, StringBuilder> fileMap = new HashMap<String, StringBuilder>();
        for (Annotation annotation : annotations) {
            StringBuilder seedFileContent = fileMap.get(annotation.getInstanceCategoryName());
            if (seedFileContent == null) {
                seedFileContent = new StringBuilder();
                // we need to write a header
                seedFileContent.append("Seeds for ").append(annotation.getInstanceCategoryName()).append("\n");
                fileMap.put(annotation.getInstanceCategoryName(), seedFileContent);
            }

            seedFileContent.append(annotation.getEntity()).append("\n");
        }

        String seedFolderPath = getDataSetLocation() + "seedEntities/";
        Set<Entry<String, StringBuilder>> entrySet = fileMap.entrySet();
        for (Entry<String, StringBuilder> entry : entrySet) {
            FileHelper.writeToFile(seedFolderPath + entry.getKey() + ".txt", entry.getValue());
        }

        setWebSearcher(SearcherFactory.createSearcher(BingSearcher.class, ConfigHolder.getInstance().getConfig()));
        setMentionsPerEntity(minMentionsPerSeed);
        setSeedsPerConcept(numberOfSeedsPerConcept);
        createDataset(seedFolderPath);

        cleanDataset(dataSetLocation, getDatasetName(), seedFolderPath, false);
        postProcessDataset(seedFolderPath, getDataSetLocation());

        // replace "new document" and "new concept" with proper string "docstart" and "" respectively
        String content = FileHelper.readFileToString(getDataSetLocation() + "all.xml");
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

        FileHelper.writeToFile(getDataSetLocation() + "allCleansed.xml", content);

        String finalColumnTaggedFilePath = getDataSetLocation() + "allColumn.txt";
        FileFormatParser.xmlToColumn(getDataSetLocation() + "allCleansed.xml", finalColumnTaggedFilePath, "\t");

        // get the broken DOCSTART lines correct
        content = FileHelper.readFileToString(finalColumnTaggedFilePath);
        content = content.replaceAll("=-\tO\nDOCSTART\tO\n-\tO", "=-DOCSTART-\tO");

        FileHelper.writeToFile(finalColumnTaggedFilePath, content);

        LOGGER.info("generated dataset in " + stopWatch.getTotalElapsedTimeString());

        return finalColumnTaggedFilePath;
    }

    public static void generateDatasets(String targetFolder, String name, String seedTrainingSetPath, int minSeeds,
            int maxSeeds, int minMentionsPerSeed) {

        for (int i = minSeeds; i <= maxSeeds; i++) {
            DatasetCreator dsc = new DatasetCreator(name + i);
            dsc.setDataSetLocation(targetFolder);
            dsc.generateDataset(seedTrainingSetPath, i, minMentionsPerSeed);

            // copy the cleansed, combined "allColumn.txt" from each subfolder in the main folder
            FileHelper.copyFile(dsc.getDataSetLocation() + "allColumn.txt", targetFolder + "seedsTest" + i + ".txt");

        }
    }

    /**
     * @param args
     * @throws PageContentExtractorException
     */
    public static void main(String[] args) throws PageContentExtractorException {

        // DatasetCreator.generateDatasets("www_eval_seeds","data/datasets/ner/conll/training.txt", 1, 50, 5);
        DatasetCreator.generateDatasets("data/temp/autoGeneratedData/", "www_eval_seeds",
                "data/datasets/ner/conll/training.txt", 1, 50, 5);
        System.exit(0);

        // String cleansedPath = "data/datasets/ner/www_eval_cleansed/";
        //
        // // replace "new document" and "new concept" with proper string "docstart" and "" respectively
        // String content = FileHelper.readFileToString(cleansedPath + "all.xml");
        // content = content.replaceAll("-+ NEW CONCEPT.*", "");
        // content = content.replaceAll("-+ NEW DOCUMENT .#.*", "=-<DOCSTART>-");
        //
        // // delete all lines containing no tagged entity
        // Pattern pattern = Pattern.compile("^((?!<[A-Z]{1,20}?>).)*$", Pattern.MULTILINE);
        // Matcher matcher = pattern.matcher(content);
        // content = matcher.replaceAll("");
        //
        // content = content.replace("=-<DOCSTART>-", "=-DOCSTART-");
        //
        // content = content.replaceAll("(\n){3,}", "\n");
        //
        // pattern = Pattern.compile("(=-DOCSTART-\n?){1,}");
        // matcher = pattern.matcher(content);
        // content = matcher.replaceAll("\n=-DOCSTART-\n\n");
        //
        // content = content.replaceAll("(\n){3,}", "\n");
        //
        // FileHelper.writeToFile(cleansedPath + "allCleansed.xml", content);
        //
        // FileFormatParser.xmlToColumn(cleansedPath + "allCleansed.xml", cleansedPath + "allColumn.txt", "\t");
        //
        // // get the broken DOCSTART lines correct
        // content = FileHelper.readFileToString(cleansedPath + "allColumn.txt");
        // content = content.replaceAll("=-\tO\nDOCSTART\tO\n-\tO", "=-DOCSTART-");
        // FileHelper.writeToFile(cleansedPath + "allColumn.txt", content);
        //
        // System.exit(0);

        // FileHelper.removeDuplicateLines("data/datasets/ner/politician/text/all.xml",
        // "data/datasets/ner/politician/text/allC.xml");
        // System.exit(0);
        // PageContentExtractor pce = new PageContentExtractor();
        // System.out.println(pce.getResultText("http://www.whitehouse.gov/about/presidents/abrahamlincoln/"));
        // Crawler c = new Crawler();
        // Document webPage = c.getWebDocument("http://www.whitehouse.gov/about/presidents/abrahamlincoln/");
        // System.out.println(webPage.getDocumentURI());
        // System.out.println(new PageContentExtractor().setDocument(webPage).getResultText());
        // System.exit(0);

        // String seed = "test";
        // System.out.println("test shold testwise or test, be replaced test...".replaceAll("(?<=\\s)" + seed
        // + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])" + seed
        // + "(?=\\s)", "ooo" + seed + "ppp"));
        // System.exit(0);

        // DatasetCreator.deduplicateSeedLists("data/knowledgeBase/seedEntities/");
        // DatasetCreator.cleanDataset("H:\\PalladianData\\Datasets\\wwwner\\ner\\", "www",
        // "data/knowledgeBase/seedEntities/");
        // DatasetCreator.postProcessDataset("data/knowledgeBase/seedEntities/",
        // "H:\\PalladianData\\Datasets\\wwwner\\ner\\www_cleansed\\");
        // System.exit(0);
        DatasetCreator datasetCreator = new DatasetCreator("www_eval");
        cleanDataset("data/datasets/ner/", "www_eval", "data/temp/seedEntities", true);
        postProcessDataset("data/temp/seedEntities/", "data/datasets/ner/www_eval_cleansed/");
        System.exit(0);
        // datasetCreator.setDataSetLocation("C:\\Safe\\");

        // datasetCreator.getConceptsMentions();
        // datasetCreator.writeMetaInformationFile(new StopWatch(), null, "data/knowledgeBase/seedEntities/");
        // DatasetCreator.postProcessDataset("data/knowledgeBase/seedEntities/", "H:\\PalladianData\\Datasets\\www\\");
        // System.exit(0);

        datasetCreator.setWebSearcher(SearcherFactory.createSearcher(BingSearcher.class, ConfigHolder.getInstance().getConfig()));
        datasetCreator.setMentionsPerEntity(2);
        datasetCreator.setSeedsPerConcept(2);
        datasetCreator.createDataset("data/knowledgeBase/seedEntities/");
        System.exit(0);

        String text = FileHelper.readFileToString("data/temp/all.xml");
        // remove set of lines that are too short
        text = text.replaceAll("(\n)+(.{0,80}(\n)){4,}", "\n");

        // remove lines without mentions
        Pattern pattern1 = Pattern.compile("^((?!POLITICIAN).)*$", Pattern.MULTILINE);
        Matcher matcher1 = pattern1.matcher(text);
        text = matcher1.replaceAll("");
        text = text.replaceAll("(\n){3,}", "\n");

        FileHelper.writeToFile("data/temp/allCleansed.xml", text);

    }

}