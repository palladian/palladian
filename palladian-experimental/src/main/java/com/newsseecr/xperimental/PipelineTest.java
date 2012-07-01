package com.newsseecr.xperimental;

import java.util.Iterator;
import java.util.List;

import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.content.PalladianContentExtractor;
import ws.palladian.extraction.content.WebPageContentExtractor;
import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.extraction.feature.DuplicateTokenRemover;
import ws.palladian.extraction.feature.NGramCreator;
import ws.palladian.extraction.feature.RegExTokenRemover;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.feature.TokenOverlapRemover;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PerformanceCheckProcessingPipeline;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;

import com.newsseecr.xperimental.wikipedia.WikipediaAnnotator;

public class PipelineTest {

    public static void main(String[] args) throws PageContentExtractorException, DocumentUnprocessableException {

        WebPageContentExtractor extractor = new PalladianContentExtractor();
        // extractor.setDocument("http://edition.cnn.com/2011/OPINION/08/25/iftikhar.arab.spring/index.html?hpt=hp_c1");
        // extractor.setDocument("http://arstechnica.com/business/news/2011/08/the-ipad-is-a-personal-computer-true-or-false.ars");
        extractor.setDocument("http://www.engadget.com/2011/08/25/tim-cook-who-is-apples-new-ceo/");
        String text = extractor.getResultText();

        // ProcessingPipeline pipeline = new ProcessingPipeline();
        ProcessingPipeline pipeline = new PerformanceCheckProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new NGramCreator(2, 4));
        // pipeline.add(new TokenSpreadCalculator());
        // pipeline.add(new FrequencyCalculator());
        pipeline.add(new RegExTokenRemover("\\p{Punct}"));
        pipeline.add(new RegExTokenRemover(".{1,2}"));
        pipeline.add(new DuplicateTokenRemover());
        pipeline.add(new WikipediaAnnotator());
        pipeline.add(new StopTokenRemover(Language.ENGLISH));
        pipeline.add(new StringDocumentPipelineProcessor() {
            @Override
            public void processDocument(PipelineDocument<String> document) {
                FeatureVector featureVector = document.getFeatureVector();
                AnnotationFeature annotationFeature = (AnnotationFeature)featureVector
                        .get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
                List<Annotation> annotations = annotationFeature.getValue();
                Iterator<Annotation> iterator = annotations.iterator();
                while (iterator.hasNext()) {
                    Annotation annotation = iterator.next();
                    NominalFeature wikiFeature = (NominalFeature)annotation.getFeatureVector().get(
                            WikipediaAnnotator.PROVIDED_FEATURE_WIKIPAGE);
                    if (wikiFeature.getValue().equals("false")) {
                        // System.out.println("removed " + annotation);
                        iterator.remove();
                    }
                }
            }
        });
        pipeline.add(new TokenOverlapRemover());

        // String text = FileHelper.readFileToString("/Users/pk/Desktop/aptgetupdate.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/tagesschau2.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/pg76.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/pg2229.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/SPIEGEL.txt");
        // String text = "Philipp Katz, Naußlitzer Str. 20, 01187 Dresden, philipp@philippkatz.de";
        // String text = "Irren ist menschlich.";
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/tagesschau.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/faz.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/ntv.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/taz.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/spiegel.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/vbush.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/openReport.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/text.txt");
        // String text = FileHelper.readFileToString("/Users/pk/Desktop/bbc.txt");
        // String text =
        // "Pakistan threatens to cut off access to a facility used by NATO forces, signaling a growing rift that began when U.S. commandos killed Osama bin Laden in Pakistan.";
        PipelineDocument document = pipeline.process(new PipelineDocument(text));
        System.out.println(pipeline);

        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature tokenList = (AnnotationFeature)featureVector.get(BaseTokenizer.PROVIDED_FEATURE);
        System.out.println("# tokens " + tokenList.getValue().size());
        System.out.println(tokenList.toStringList());

        System.out.println(createCSVHeader(tokenList.getValue().get(0)));

        for (Annotation annotation : tokenList.getValue()) {
            /*
             * NominalFeature redirectFeature =
             * (NominalFeature)annotation.getFeatureVector().get(WikipediaAnnotator.PROVIDED_FEATURE_REDIRECT);
             * if (redirectFeature == null) {
             * System.out.println(annotation.getValue());
             * } else {
             * System.out.println(annotation.getValue() + " -> " + redirectFeature.getValue());
             * }
             */
            System.out.println(toCSV(annotation));
        }

        // Feature<List<String>> keywords = (Feature<List<String>>)
        // featureVector.get(KeywordAnnotator.PROVIDED_FEATURE);
        // CollectionHelper.print(keywords.getValue());

    }

    public static String createCSVHeader(Annotation annotation) {
        StringBuilder builder = new StringBuilder();
        builder.append("value").append(";");
        // builder.append("startPosition").append(";");
        // builder.append("endPosition").append(";");

        Feature<?>[] features = annotation.getFeatureVector().toArray();
        for (Feature<?> feature : features) {
            builder.append(feature.getName()).append(";");
        }

        return builder.toString();
    }

    public static String toCSV(Annotation annotation) {
        StringBuilder builder = new StringBuilder();

        builder.append("\"").append(annotation.getValue()).append("\"").append(";");
        // builder.append(annotation.getStartPosition()).append(";");
        // builder.append(annotation.getEndPosition()).append(";");

        Feature<?>[] features = annotation.getFeatureVector().toArray();
        for (Feature<?> feature : features) {
            builder.append(feature.getValue()).append(";");
        }

        return builder.toString();
    }

}
