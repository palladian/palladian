package tud.iir.tagging;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * The DatasetCreator crawls web pages and marks the given seed entities.
 * The marked up pages are saved in:
 * 1. separate (x)html files
 * 2. separate text files (cleansed html)
 * 3. one long text file, all text files from 2 concatenated
 * 
 * @author David Urbansky
 * 
 */
public class DatasetCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class);

    private String datasetName = "default";

    /** A set of seed entities that are used for mark up. These should be unambiguous. */
    private Set<Entity> seedEntities;

    /** Number of web pages that should be retrieved from the search engine per entity. */
    private int resultsPerEntity = 10;

    /** The location where the dataset is stored. */
    private String dataSetLocation = "data/datasets/ner/";

    public DatasetCreator(Set<Entity> seedEntities) {
        this.seedEntities = seedEntities;
    }

    public void createDataset() {

        StopWatch stopWatch = new StopWatch();

        int ec = 0;
        for (Entity entity : seedEntities) {

            List<String> urls = getWebPages(entity);

            ec++;
            int uc = 0;

            for (String url : urls) {

                markupWebPage(url);
                uc++;

                LOGGER.info("marked up page " + url + " " + ec + "/" + seedEntities.size() + ", " + uc + "/"
                        + urls.size());

            }

        }

        LOGGER.info("created dataset with " + seedEntities.size() + " seeds in " + stopWatch.getElapsedTimeString());
    }

    private List<String> getWebPages(Entity entity) {

        SourceRetriever sourceRetriever = new SourceRetriever();
        sourceRetriever.setResultCount(getResultsPerEntity());
        sourceRetriever.setSource(SourceRetrieverManager.GOOGLE);

        return sourceRetriever.getURLs(entity.getName(), true);

    }

    private void markupWebPage(String url) {

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
        for (Entity entity : seedEntities) {
            webPageContent = webPageContent.replaceAll(entity.getName(), "<"
                    + entity.getConcept().getName().toUpperCase()
                    + " style=\"background-color:red; color:white;\">"
                    + entity.getName() + "</" + entity.getConcept().getName().toUpperCase() + ">");
            webPageText = webPageText.replaceAll(entity.getName(),
 "<" + entity.getConcept().getName().toUpperCase()
                    + ">" + entity.getName() + "</" + entity.getConcept().getName().toUpperCase()
                            + ">");

            LOGGER.debug("marked up page " + url + " with entity " + entity.getName());
        }

        // save web page
        if (webPageContent.length() > 0) {
            FileHelper.writeToFile(getDataSetLocation() + "html/" + StringHelper.makeSafeName(url) + ".html",
                    webPageContent);

            LOGGER.debug("saved html file");
        }

        // save text
        if (webPageText.length() > 0) {
            try {
            FileHelper.writeToFile(getDataSetLocation() + "text/" + StringHelper.makeSafeName(url) + ".xml",
                    webPageText);

                FileHelper.appendFile(getDataSetLocation() + "text/all.xml", webPageText);
            } catch (IOException e) {
                LOGGER.fatal("could not append to all.xml");
            }

            LOGGER.debug("saved text file");
        }

    }

    public void setResultsPerEntity(int resultsPerEntity) {
        this.resultsPerEntity = resultsPerEntity;
    }

    public int getResultsPerEntity() {
        return resultsPerEntity;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public Set<Entity> getSeedEntities() {
        return seedEntities;
    }

    public void setSeedEntities(Set<Entity> seedEntities) {
        this.seedEntities = seedEntities;
    }

    public String getDataSetLocation() {
        return dataSetLocation + getDatasetName() + "/";
    }

    public void setDataSetLocation(String dataSetLocation) {
        this.dataSetLocation = dataSetLocation;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        Concept mobilePhone = new Concept("mobilePhone");
        Set<Entity> seedEntities = new HashSet<Entity>();
        seedEntities.add(new Entity("Nexus One", mobilePhone));
        seedEntities.add(new Entity("Samsung i7110", mobilePhone));
        seedEntities.add(new Entity("HTC Hero", mobilePhone));
        seedEntities.add(new Entity("Blackberry Storm", mobilePhone));
        seedEntities.add(new Entity("iPhone 4", mobilePhone));

        DatasetCreator datasetCreator = new DatasetCreator(seedEntities);

        datasetCreator.setDatasetName("mobilephone");
        datasetCreator.setResultsPerEntity(5);
        datasetCreator.createDataset();

    }

}
