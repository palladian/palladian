package tud.iir.extraction.entity.ner.dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
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

import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.extraction.content.PageSentenceExtractor;
import tud.iir.helper.CountMap;
import tud.iir.helper.DatasetCreatorInterface;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.helper.WordTransformer;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.URLDownloader;

/**
 * The DatasetCreator crawls web pages and marks the given seed entities.
 * The marked up pages are saved in:
 * <ol>
 * <li>separate (x)html files</li>
 * <li>separate text files (cleansed html)</li>
 * <li>one long text file, all text files from 2 concatenated</li>
 * </ol>
 * 
 * @author David Urbansky
 * 
 */
public class DatasetCreator implements DatasetCreatorInterface {

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
    private int sourceAPI = SourceRetrieverManager.GOOGLE;

    /** Save a map with concept name and the seeds searched for every concept. */
    private Map<String, List<String>> conceptSeeds;

    public DatasetCreator(String datasetName) {
        this.datasetName = datasetName;
    }

    /**
     * Create a dataset by searching for the seed mentions and storing the complete web pages.
     * 
     * @param seedFolderPath The path to the folder with the seed entities. Each file must be named with the concept
     *            name (_partX is ignored for markup) and there must be one seed entity per line.
     */
    @Override
    public final void createDataset(String seedFolderPath) {
        StopWatch stopWatch = new StopWatch();

        conceptSeeds = new HashMap<String, List<String>>();

        File[] seedFiles = FileHelper.getFiles(seedFolderPath);

        Set<String> conceptsSearched = new HashSet<String>();

        // iterate over all concepts (seed files)
        for (File file : seedFiles) {
            String seedFileName = FileHelper.getFileName(file.getName());
            createDatasetForConcept(seedFileName, file);
            conceptsSearched.add(getConceptNameFromFileName(seedFileName));
        }

        writeMetaInformationFile(stopWatch, conceptsSearched);

        // postProcessDataset(seedFolderPath, getDataSetLocation() + getDatasetName() + "/");

        LOGGER.info("created " + seedFiles.length + " datasets in " + stopWatch.getElapsedTimeString()
                + ", total traffic: " + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES) + "MB");

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
        .append(DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", stopWatch.getStartTime()))
        .append("\n");
        meta.append("Dataset created in: ").append(stopWatch.getElapsedTimeString()).append("\n");
        meta.append("Total Generated Traffic: ").append(Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES))
        .append("MB\n");
        meta.append("Search Engine used: ").append(SourceRetrieverManager.getName(getSourceAPI())).append("\n");
        meta.append("Minimum Mentions per Entity Targeted: ").append(getMentionsPerEntity()).append("\n");

        // check which concepts have entities with less than the minimum mentions
        for (Object[] object : getConceptsWithFewMentions()) {
            String conceptName = (String) object[0];
            String entitiesWithFewMentions = (String) object[1];
            Double averageMentionsPerEntity = (Double) object[2];
            meta.append("  Too Few Mentions: ").append(conceptName).append("\n  Entities with few mentions: ")
            .append(entitiesWithFewMentions).append("\n  Average Mentions per Entity: ")
            .append(averageMentionsPerEntity);
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
     * @return A set with information about 0: the concept name, 1: the list of entities with few mentions, 2: the
     *         average mentions per entity.
     */
    private Set<Object[]> getConceptsWithFewMentions() {

        Set<Object[]> objectSet = new HashSet<Object[]>();

        // File[] seedFiles = FileHelper.getFiles(seedFolderPath);

        // iterate over all concepts (seed files)
        for (Entry<String, List<String>> conceptSeedEntry : conceptSeeds.entrySet()) {
            // String seedFileName = FileHelper.getFileName(file.getName());

            String seedFileName = conceptSeedEntry.getKey();

            Object[] o = new Object[3];
            o[0] = seedFileName;

            File[] markedUpFiles = FileHelper.getFiles(getDataSetLocation() + seedFileName);
            CountMap countMap = new CountMap();
            for (File markedUpFile : markedUpFiles) {
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
    private String getConceptNameFromFileName(String fileName) {
        return WordTransformer.wordToSingular(fileName.replaceAll("_part(\\d)", ""));
    }

    /**
     * Create the dataset for one certain concept.
     * 
     * @param seedFileName The name of the concept (equals the name of the seed file).
     * @param seedFile The file with entity seeds.
     */
    private void createDatasetForConcept(String seedFileName, File seedFile) {

        StopWatch stopWatch = new StopWatch();

        URLDownloader urlDownloader = new URLDownloader();
        List<String> seedEntities = FileHelper.readFileToArray(seedFile);

        // get a random sample of seeds from the list
        Collection<String> randomSet = MathHelper.randomSample(seedEntities, getSeedsPerConcept());

        for (String randomSeed : randomSet) {
            seedEntities.add(randomSeed);
        }

        // mix the entities
        // Set<String> mixedSeedEntities = new HashSet<String>();
        // mixedSeedEntities.addAll(seedEntities);

        // write a seed file in classification format with all the seeds used for the current concept
        StringBuilder seedFileCopy = new StringBuilder();

        int ec = 0;
        for (String seedEntity : seedEntities) {

            seedFileCopy.append(seedEntity).append("###")
            .append(getConceptNameFromFileName(seedFileName).toUpperCase()).append("\n");

            List<String> urls = getWebPages(seedEntity);
            urlDownloader.add(urls);

            Set<Document> documents = urlDownloader.start();

            ec++;
            int uc = 0;

            for (Document document : documents) {

                if (document == null) {
                    continue;
                }
                markupWebPage(document, seedFileName, seedEntities);
                uc++;

                LOGGER.debug("marked up page " + document.getDocumentURI() + " " + ec + "/" + seedEntities.size()
                        + ", "
                        + uc + "/" + urls.size());
            }

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
    private List<String> getWebPages(String seedEntity) {
        LOGGER.info("get web pages for seed: " + seedEntity);

        SourceRetriever sourceRetriever = new SourceRetriever();
        sourceRetriever.setResultCount(10 * getMentionsPerEntity());
        sourceRetriever.setSource(getSourceAPI());

        return sourceRetriever.getURLs(seedEntity, true);
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
            webPageContent = Crawler.documentToString(webPage);
            // webPageText = new PageContentExtractor().setDocument(webPage).getResultText();
            webPageText = new PageSentenceExtractor().setDocument(webPage).getMainContentText();
        } catch (Exception e) {
            LOGGER.error("could not extract clean content from " + webPage.getDocumentURI() + ", " + e.getMessage());
            return;
        }

        // mark up all seed entities
        for (String seedEntity : seedEntities) {

            String escapedSeed = StringHelper.escapeForRegularExpression(seedEntity);
            String searchRegexp = "(?<=\\s)" + escapedSeed + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])" + escapedSeed
            + "(?=\\s)";

            // mark up html
            webPageContent = webPageContent.replaceAll(searchRegexp, "<" + conceptName.toUpperCase()
                    + " style=\"background-color:red; color:white;\">" + seedEntity + "</" + conceptName.toUpperCase()
                    + ">");

            // mark up text
            webPageText = webPageText.replaceAll(searchRegexp, "<" + conceptName.toUpperCase() + ">" + seedEntity
                    + "</" + conceptName.toUpperCase() + ">");

            LOGGER.debug("marked up page " + webPage.getDocumentURI() + " with entity " + seedEntity);
        }

        // save web page
        if (webPageContent.length() > 10) {
            FileHelper.writeToFile(
                    getDataSetLocation() + seedFileName + "/html/"
                    + StringHelper.makeSafeName(webPage.getDocumentURI(), 30)
                    + ".html", webPageContent);

            LOGGER.debug("saved html file");
        }

        // save text
        if (webPageText.length() > 0) {

            webPageText = cleanText(webPageText, conceptName);

            if (webPageText.length() > 10) {

                String filePath = getDataSetLocation() + seedFileName + "/"
                        + StringHelper.makeSafeName(webPage.getDocumentURI(), 30) + ".xml";
                FileHelper.writeToFile(filePath, webPageText);

                FileHelper.removeDuplicateLines(filePath, filePath);

                LOGGER.debug("saved text file");
            }

        }
    }

    /**
     * Remove sets of short lines which are usually tables or other irrelevant content that was incorrectly added as
     * page content.
     * 
     * @param inputText The text that should be cleansed.
     * @param tagName The name of the tag.
     * @return The cleansed text.
     */
    private String cleanText(String inputText, String tagName) {

        String text = inputText;

        try {
            // remove sets of lines that are too short
            text = text.replaceAll("(\n)+(.{0,80}(\n)){4,}", "\n");

            // remove lines without mentions
            Pattern pattern = Pattern.compile("^((?!" + tagName.toUpperCase() + ").)*$", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            text = matcher.replaceAll("");

            // remove lines without context around the entity
            pattern = Pattern.compile("^<" + tagName.toUpperCase() + ">.*?</" + tagName.toUpperCase() + ">$",
                    Pattern.MULTILINE);
            matcher = pattern.matcher(text);
            text = matcher.replaceAll("");

            // remove gaps
            text = text.replaceAll("(\n){3,}", "\n");

            // remove empty line in the beginning
            text = text.replaceAll("^\n", "");

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

        File[] seedFiles = FileHelper.getFiles(seedFolderPath);

        // iterate over all concepts (seed files)
        for (File file : seedFiles) {
            String seedFileName = FileHelper.getFileName(file.getName());

            if (seedFileName.length() == 0) {
                continue;
            }

            FileWriter combinedFile = null;
            try {
                combinedFile = new FileWriter(dataSetLocation + seedFileName + "/all.xml");
            } catch (IOException e) {
                LOGGER.error("could not create file, " + e.getMessage());
            }

            File[] taggedFiles = FileHelper.getFiles(dataSetLocation + seedFileName + "/");
            for (File taggedFile : taggedFiles) {

                String content = FileHelper.readFileToString(taggedFile);

                if (content.length() < 5) {
                    continue;
                }

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
            }
        }

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

    public void setSourceAPI(int sourceAPI) {
        this.sourceAPI = sourceAPI;
    }

    public int getSourceAPI() {
        return sourceAPI;
    }


    /**
     * @param args
     * @throws PageContentExtractorException
     */
    public static void main(String[] args) throws PageContentExtractorException {

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
        // DatasetCreator.postProcessDataset("data/knowledgeBase/seedEntities/", "data/datasets/ner/www_test2/");
        // System.exit(0);
        DatasetCreator datasetCreator = new DatasetCreator("www");
        datasetCreator.setDataSetLocation("data/datasets/ner/");

        // datasetCreator.splitAndTransformDatasets();
        // System.exit(0);

        datasetCreator.setSourceAPI(SourceRetrieverManager.BING);
        datasetCreator.setMentionsPerEntity(5);
        datasetCreator.setSeedsPerConcept(5);
        datasetCreator.createDataset("data/knowledgeBase/seedEntities/");
        System.exit(1);

        String text = FileHelper.readFileToString("data/temp/all.xml");
        // remove set of lines that are too short
        text = text.replaceAll("(\n)+(.{0,80}(\n)){4,}", "\n");

        // remove lines without mentions
        Pattern pattern = Pattern.compile("^((?!POLITICIAN).)*$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        text = matcher.replaceAll("");
        text = text.replaceAll("(\n){3,}", "\n");

        FileHelper.writeToFile("data/temp/allCleansed.xml", text);

    }

}