package tud.iir.extraction.entity.ner.dataset;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.extraction.entity.ner.FileFormatParser;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
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

        LOGGER.info("created " + seedFiles.length + " datasets in " + stopWatch.getElapsedTimeString()
                + ", total traffic: " + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES) + "MB");

    }

    /**
     * Create the dataset for one certain concept.
     * 
     * @param conceptName The name of the concept (equals the name of the seed file).
     * @param seedFile The file with entity seeds.
     */
    public void createDatasetForConcept(String conceptName, File seedFile) {

        StopWatch stopWatch = new StopWatch();

        URLDownloader urlDownloader = new URLDownloader();
        List<String> seedEntities = FileHelper.readFileToArray(seedFile);

        int ec = 0;
        for (String seedEntity : seedEntities) {

            List<String> urls = getWebPages(seedEntity);
            urlDownloader.add(urls);

            Set<Document> documents = urlDownloader.start();

            ec++;
            int uc = 0;

            for (Document document : documents) {

                if (document == null) {
                    continue;
                }
                markupWebPage(document, conceptName, seedEntities);
                uc++;

                LOGGER.info("marked up page " + document.getDocumentURI() + " " + ec + "/" + seedEntities.size() + ", "
                        + uc + "/" + urls.size());

            }

        }

        // remove duplicate lines from combined file
        FileHelper.removeDuplicateLines(getDataSetLocation() + conceptName + "/text/all.xml", getDataSetLocation()
                + conceptName + "/text/all.xml");

        LOGGER.info("created dataset with " + seedEntities.size() + " seeds in " + stopWatch.getElapsedTimeString());
    }

    private List<String> getWebPages(String seedEntity) {

        SourceRetriever sourceRetriever = new SourceRetriever();
        sourceRetriever.setResultCount(getResultsPerEntity());
        sourceRetriever.setSource(SourceRetrieverManager.GOOGLE);

        return sourceRetriever.getURLs(seedEntity, true);

    }

    private void markupWebPage(Document webPage, String conceptName, List<String> seedEntities) {

        System.out.println(webPage.getDocumentURI());

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
            webPageContent = webPageContent.replaceAll(seedEntity, "<" + conceptName.toUpperCase()
                    + " style=\"background-color:red; color:white;\">" + seedEntity + "</" + conceptName.toUpperCase()
                    + ">");
            webPageText = webPageText.replaceAll(seedEntity, "<" + conceptName.toUpperCase() + ">" + seedEntity + "</"
                    + conceptName.toUpperCase() + ">");

            LOGGER.debug("marked up page " + webPage.getDocumentURI() + " with entity " + seedEntity);
        }

        // save web page
        if (webPageContent.length() > 10) {
            FileHelper.writeToFile(
                    getDataSetLocation() + conceptName + "/html/" + StringHelper.makeSafeName(webPage.getDocumentURI())
                            + ".html", webPageContent);

            LOGGER.debug("saved html file");
        }

        // save text
        if (webPageText.length() > 0) {
            try {

                webPageText = cleanText(webPageText, conceptName);

                if (webPageText.length() > 10) {

                    FileHelper.writeToFile(
                            getDataSetLocation() + conceptName + "/text/"
                                    + StringHelper.makeSafeName(webPage.getDocumentURI()) + ".xml", webPageText);

                    FileHelper.appendFile(getDataSetLocation() + conceptName + "/text/all.xml", webPageText);

                    LOGGER.debug("saved text file");

                }

            } catch (IOException e) {
                LOGGER.fatal("could not append to all.xml");
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
            // remove set of lines that are too short
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
     * Split the all.xml file for each concept in training and testing files.
     * Also create a column representation for each file "all.tsv".
     */
    public void splitAndTransformDatasets() {

        File folder = new File(dataSetLocation);
        if (folder.exists() && folder.isDirectory()) {

            File[] files = folder.listFiles();

            for (File file : files) {
                if (file.isDirectory() && new File(dataSetLocation + file.getName() + "/text/all.xml").exists()) {
                    splitDataset(dataSetLocation + file.getName() + "/text/all.xml");
                }
            }

        }
    }

    private void splitDataset(String filePath) {

        // split dataset in training and testing
        StringBuilder training = new StringBuilder();
        StringBuilder testing = new StringBuilder();

        final Object[] obj = new StringBuilder[2];
        obj[0] = training;
        obj[1] = testing;

        LineAction la = new LineAction(obj) {

            @Override
            public void performAction(String line, int lineNumber) {
                if (line.length() == 0) {
                    return;
                }
                if (lineNumber % 2 == 0) {
                    ((StringBuilder) obj[0]).append(line).append("\n");
                } else {
                    ((StringBuilder) obj[1]).append(line).append("\n");
                }
            }
        };

        FileHelper.performActionOnEveryLine(filePath, la);

        String folderPath = FileHelper.getFilePath(filePath);
        FileHelper.writeToFile(folderPath + "training.xml", training);
        FileHelper.writeToFile(folderPath + "testing.xml", testing);

        // create column representations
        FileFormatParser.xmlToColumn(folderPath + "training.xml", folderPath + "training.tsv", "\t");
        FileFormatParser.xmlToColumn(folderPath + "testing.xml", folderPath + "testing.tsv", "\t");
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

        DatasetCreator datasetCreator = new DatasetCreator();

        datasetCreator.splitAndTransformDatasets();
        System.exit(0);

        
        datasetCreator.setResultsPerEntity(5);
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
