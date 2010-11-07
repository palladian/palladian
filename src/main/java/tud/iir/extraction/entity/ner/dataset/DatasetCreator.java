package tud.iir.extraction.entity.ner.dataset;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

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
public class DatasetCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class);

    /** Number of web pages that should be retrieved from the search engine per entity. */
    private int resultsPerEntity = 10;

    /** The location where the dataset is stored. */
    private String dataSetLocation = "data/datasets/ner/";

    public DatasetCreator() {

    }

    /**
     * Create a dataset by searching for the seed mentions and storing the complete web pages.
     * 
     * @param seedFolderPath The path to the folder with the seed entities. Each file must be named with the concept
     *            name and there must be one seed entity per line.
     */
    public void createDataset(String seedFolderPath) {
        StopWatch stopWatch = new StopWatch();

        File[] seedFiles = FileHelper.getFiles(seedFolderPath);

        for (File file : seedFiles) {
            createDatasetForConcept(FileHelper.getFileName(file.getName()), file);
        }

        LOGGER.info("created " + seedFiles.length + "datasets in " + stopWatch.getElapsedTimeString());
    }

    /**
     * Create the dataset for one certain concept.
     * 
     * @param conceptName The name of the concept (equals the name of the seed file).
     * @param seedFile The file with entity seeds.
     */
    public void createDatasetForConcept(String conceptName, File seedFile) {

        StopWatch stopWatch = new StopWatch();

        List<String> seedEntities = FileHelper.readFileToArray(seedFile);

        int ec = 0;
        for (String seedEntity : seedEntities) {

            List<String> urls = getWebPages(seedEntity);

            ec++;
            int uc = 0;

            for (String url : urls) {

                markupWebPage(url, conceptName, seedEntities);
                uc++;

                LOGGER.info("marked up page " + url + " " + ec + "/" + seedEntities.size() + ", " + uc + "/"
                        + urls.size());

            }

        }

        LOGGER.info("created dataset with " + seedEntities.size() + " seeds in " + stopWatch.getElapsedTimeString());
    }

    private List<String> getWebPages(String seedEntity) {

        SourceRetriever sourceRetriever = new SourceRetriever();
        sourceRetriever.setResultCount(getResultsPerEntity());
        sourceRetriever.setSource(SourceRetrieverManager.GOOGLE);

        return sourceRetriever.getURLs(seedEntity, true);

    }

    private void markupWebPage(String url, String conceptName, List<String> seedEntities) {

        Crawler c = new Crawler();
        c.setFeedAutodiscovery(false);
        Document webPage = c.getWebDocument(url);
        String webPageContent = Crawler.documentToString(webPage);

        String webPageText = "";
        try {
            webPageText = new PageContentExtractor().setDocument(webPage).getResultText();
        } catch (PageContentExtractorException e) {
            LOGGER.error("could not extract clean content from " + url + ", " + e.getMessage());
        }

        // mark up all seed entities
        for (String seedEntity : seedEntities) {
            webPageContent = webPageContent.replaceAll(seedEntity, "<" + conceptName.toUpperCase()
                    + " style=\"background-color:red; color:white;\">" + seedEntity + "</" + conceptName.toUpperCase()
                    + ">");
            webPageText = webPageText.replaceAll(seedEntity, "<" + conceptName.toUpperCase() + ">" + seedEntity + "</"
                    + conceptName.toUpperCase() + ">");

            LOGGER.debug("marked up page " + url + " with entity " + seedEntity);
        }

        // save web page
        if (webPageContent.length() > 0) {
            FileHelper.writeToFile(getDataSetLocation() + conceptName + "/html/" + StringHelper.makeSafeName(url)
                    + ".html",
                    webPageContent);

            LOGGER.debug("saved html file");
        }

        // save text
        if (webPageText.length() > 0) {
            try {

                webPageText = cleanText(webPageText);

                FileHelper.writeToFile(getDataSetLocation() + conceptName + "/text/" + StringHelper.makeSafeName(url)
                        + ".xml",
                        webPageText);

                FileHelper.appendFile(getDataSetLocation() + conceptName + "/text/all.xml", webPageText);
            } catch (IOException e) {
                LOGGER.fatal("could not append to all.xml");
            }

            LOGGER.debug("saved text file");
        }

    }

    /**
     * Remove sets of short lines which are usually tables or other irrelevant content that was incorrectly added as
     * page content.
     * 
     * @param text The text that should be cleansed.
     * @return The cleansed text.
     */
    private String cleanText(String text) {
        // remove set of lines that are too short
        text = text.replaceAll("(\n)+(.{0,80}(\n)){4,}", "\n");

        // remove lines without mentions
        Pattern pattern = Pattern.compile("^((?!POLITICIAN).)*$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        text = matcher.replaceAll("");
        text = text.replaceAll("(\n){3,}", "\n");

        return text;
    }

    public void setResultsPerEntity(int resultsPerEntity) {
        this.resultsPerEntity = resultsPerEntity;
    }

    public int getResultsPerEntity() {
        return resultsPerEntity;
    }

    public String getDataSetLocation() {
        return dataSetLocation;
    }

    public void setDataSetLocation(String dataSetLocation) {
        this.dataSetLocation = dataSetLocation;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // DatasetCreator datasetCreator = new DatasetCreator();
        // datasetCreator.setResultsPerEntity(5);
        // datasetCreator.createDataset("data/knowledgeBase/seedEntities/");

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
