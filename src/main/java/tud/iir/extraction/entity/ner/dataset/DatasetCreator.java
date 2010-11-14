package tud.iir.extraction.entity.ner.dataset;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.DatasetCreatorInterface;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
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

    /** Number of web pages that should be retrieved from the search engine per entity. */
    private int resultsPerEntity = 10;

    /** The location where the dataset is stored. */
    private String dataSetLocation = "data/datasets/ner/";

    /** The search API to use. */
    private int sourceAPI = SourceRetrieverManager.GOOGLE;

    public DatasetCreator(String datasetName) {
        this.datasetName = datasetName;
    }

    /**
     * Create a dataset by searching for the seed mentions and storing the complete web pages.
     * 
     * @param seedFolderPath The path to the folder with the seed entities. Each file must be named with the concept
     *            name (_partX is ignored for markup) and there must be one seed entity per line.
     */
    public void createDataset(String seedFolderPath) {
        StopWatch stopWatch = new StopWatch();

        File[] seedFiles = FileHelper.getFiles(seedFolderPath);

        Set<String> conceptsSearched = new HashSet<String>();
        for (File file : seedFiles) {
            String seedFileName = FileHelper.getFileName(file.getName());
            createDatasetForConcept(seedFileName, file);
            conceptsSearched.add(getConceptNameFromFileName(seedFileName));
        }

        writeMetaInformationFile(stopWatch, conceptsSearched);

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
        meta.append("Results per Entity: ").append(getResultsPerEntity()).append("\n");
        meta.append("Concepts Searched (").append(conceptsSearched.size()).append("):\n");
        for (String conceptName : conceptsSearched) {
            meta.append("    ").append(conceptName).append("\n");
        }

        FileHelper.writeToFile(getDataSetLocation() + "metaInformation.txt", meta);
    }

    /**
     * File names can contain "_partX" which should be ignored for concept tagging.<br>
     * politician_part1 => politician
     * 
     * @param fileName The name of the seed file.
     * @return The name of the concept.
     */
    private String getConceptNameFromFileName(String fileName) {
        return fileName.replaceAll("_part(\\d)", "");
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

                LOGGER.info("marked up page " + document.getDocumentURI() + " " + ec + "/" + seedEntities.size() + ", "
                        + uc + "/" + urls.size());
            }

        }

        // write the seed file into a special folder
        FileHelper.writeToFile(getDataSetLocation() + seedFileName + "/seeds/seeds.txt", seedFileCopy);

        // remove duplicate lines from combined file
        // FileHelper.removeDuplicateLines(getDataSetLocation() + conceptName + "/text/all.xml", getDataSetLocation()+
        // conceptName + "/text/all.xml");

        LOGGER.info("created dataset for concept " + seedFileName + " with " + seedEntities.size() + " seeds in "
                + stopWatch.getElapsedTimeString());
    }

    /**
     * Get a list URLs to web pages that contain the seed entity.
     * 
     * @param seedEntity The name of the seed entity.
     * @return A list of URLs containing the seed entity.
     */
    private List<String> getWebPages(String seedEntity) {
        LOGGER.info("get web pages for seed: " + seedEntity);

        SourceRetriever sourceRetriever = new SourceRetriever();
        sourceRetriever.setResultCount(getResultsPerEntity());
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

        LOGGER.info("mark up web page: " + webPage.getDocumentURI() + " (" + seedFileName + ")");

        String conceptName = getConceptNameFromFileName(seedFileName);

        // Crawler c = new Crawler();
        // webPage = c.getWebDocument("http://www.letourdefrance.btinternet.co.uk/vin01.html");

        String webPageContent = Crawler.documentToString(webPage);

        String webPageText = "";
        try {
            webPageText = new PageContentExtractor().setDocument(webPage).getResultText();
        } catch (PageContentExtractorException e) {
            LOGGER.error("could not extract clean content from " + webPage.getDocumentURI() + ", " + e.getMessage());
            return;
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

                FileHelper.writeToFile(
                        getDataSetLocation() + seedFileName + "/"
                                + StringHelper.makeSafeName(webPage.getDocumentURI(), 30) + ".xml", webPageText);

                LOGGER.debug("saved text file");
            }

        }
    }

    /**
     * Remove sets of short lines which are usually tables or other irrelevant content that was incorrectly added as
     * page content.
     * 
     * @param text The text that should be cleansed.
     * @return The cleansed text.
     */
    private String cleanText(String text, String tagName) {
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

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setResultsPerEntity(int resultsPerEntity) {
        this.resultsPerEntity = resultsPerEntity;
    }

    public int getResultsPerEntity() {
        return resultsPerEntity;
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
     * Split the all.xml file for each concept in training and testing files.
     * Also create a column representation for each file "all.tsv".
     */
    /*
     * public void splitAndTransformDatasets() {
     * File folder = new File(dataSetLocation);
     * if (folder.exists() && folder.isDirectory()) {
     * File[] files = folder.listFiles();
     * for (File file : files) {
     * if (file.isDirectory() && new File(dataSetLocation + file.getName() + "/text/all.xml").exists()) {
     * splitDataset(dataSetLocation + file.getName() + "/text/all.xml");
     * }
     * }
     * }
     * }
     */

    /*
     * private void splitDataset(String filePath) {
     * // split dataset in training and testing
     * StringBuilder training = new StringBuilder();
     * StringBuilder testing = new StringBuilder();
     * final Object[] obj = new StringBuilder[2];
     * obj[0] = training;
     * obj[1] = testing;
     * LineAction la = new LineAction(obj) {
     * @Override
     * public void performAction(String line, int lineNumber) {
     * if (line.length() == 0) {
     * return;
     * }
     * if (lineNumber % 2 == 0) {
     * ((StringBuilder) obj[0]).append(line).append("\n");
     * } else {
     * ((StringBuilder) obj[1]).append(line).append("\n");
     * }
     * }
     * };
     * FileHelper.performActionOnEveryLine(filePath, la);
     * String folderPath = FileHelper.getFilePath(filePath);
     * FileHelper.writeToFile(folderPath + "training.xml", training);
     * FileHelper.writeToFile(folderPath + "testing.xml", testing);
     * // create column representations
     * FileFormatParser.xmlToColumn(folderPath + "training.xml", folderPath + "training.tsv", "\t");
     * FileFormatParser.xmlToColumn(folderPath + "testing.xml", folderPath + "testing.tsv", "\t");
     * }
     */

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

        DatasetCreator datasetCreator = new DatasetCreator("www_test");
        datasetCreator.setDataSetLocation("data/datasets/ner/");

        // datasetCreator.splitAndTransformDatasets();
        // System.exit(0);

        
        datasetCreator.setResultsPerEntity(5);
        datasetCreator.createDataset("data/knowledgeBase/seedEntities2/");
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
