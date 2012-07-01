package com.newsseecr.xperimental.wikipedia;

import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiInitializationException;

/**
 * <p>
 * This class annotates tokens in a text with data extracted from a Wikipedia dump. These annotations include: name of
 * the associated wikipedia page, name of redirect, number of outgoing and incoming links to the page.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WikipediaAnnotator extends StringDocumentPipelineProcessor {

    private static final Logger LOGGER = Logger.getLogger(WikipediaAnnotator.class);

    private static final long serialVersionUID = 1L;

    public static final String PROVIDED_FEATURE_WIKIPAGE = "ws.palladian.features.wikipedia";
    public static final String PROVIDED_FEATURE_REDIRECT = "ws.palladian.features.wikipedia.redirect";
    public static final String PROVIDED_FEATURE_INLIKS = "ws.palladian.features.wikipedia.inlinks";
    public static final String PROVIDED_FEATURE_OUTLINKS = "ws.palladian.features.wikipedia.outlinks";

    private Wikipedia wikipedia;

    public WikipediaAnnotator(DatabaseConfiguration databaseConfiguration) {
        try {
            this.wikipedia = new Wikipedia(databaseConfiguration);
        } catch (WikiInitializationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public WikipediaAnnotator() {
        try {
            DatabaseConfiguration databaseconfiguration = new DatabaseConfiguration();
            databaseconfiguration.setHost("localhost");
            databaseconfiguration.setDatabase("JWPL");
            databaseconfiguration.setUser("root");
            databaseconfiguration.setPassword("root");
            databaseconfiguration.setLanguage(Language.german);
            this.wikipedia = new Wikipedia(databaseconfiguration);
        } catch (WikiInitializationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature)featureVector.get(BaseTokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> annotations = annotationFeature.getValue();
        int count = 0;
        for (Annotation annotation : annotations) {

            count++;
            LOGGER.info(annotation.getValue() + " : " + count + "/" + annotations.size() + " = " + (float)count
                    / annotations.size());

            FeatureVector annotationFeatureVector = annotation.getFeatureVector();
            String value = annotation.getValue();
            boolean inWikipedia = false;
            int inlinks = 0;
            int outlinks = 0;

            try {
                Page page = wikipedia.getPage(value);
                inlinks = page.getNumberOfInlinks();
                outlinks = page.getNumberOfOutlinks();
                inWikipedia = true;
            } catch (WikiApiException e) {
            }

            NominalFeature wikiFeature = new NominalFeature(PROVIDED_FEATURE_WIKIPAGE, inWikipedia + "");
            annotationFeatureVector.add(wikiFeature);
            NumericFeature inlinksFeature = new NumericFeature(PROVIDED_FEATURE_INLIKS, (double)inlinks);
            NumericFeature outlinksFeature = new NumericFeature(PROVIDED_FEATURE_OUTLINKS, (double)outlinks);
            annotationFeatureVector.add(inlinksFeature);
            annotationFeatureVector.add(outlinksFeature);
        }
    }

    private boolean isInWikipedia(String value) {
        boolean ret = false;
        try {
            Page page = wikipedia.getPage(value);
            System.out.println("inlinks: " + page.getNumberOfInlinks());
            System.out.println("outlinks: " + page.getNumberOfOutlinks());
            System.out.println(page.getRedirects());
            ret = true;
        } catch (WikiApiException e) {
        }
        return ret;
    }

    public static void main(String[] args) {
        WikipediaAnnotator wikipediaAnnotator = new WikipediaAnnotator();
        System.out.println(wikipediaAnnotator.isInWikipedia("Apple"));
        System.out.println(wikipediaAnnotator.isInWikipedia("Pear"));
        System.out.println(wikipediaAnnotator.isInWikipedia("Orange"));
        System.out.println(wikipediaAnnotator.isInWikipedia("U.S."));
        System.out.println(wikipediaAnnotator.isInWikipedia("United states of America"));
        // System.out.println(wikipediaAnnotator.isInWikipedia("aöksdj"));
    }

}
