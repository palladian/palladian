package de.philippkatz.activities.sourceforge.extraction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

public class TextPatternExtractor extends StringDocumentPipelineProcessor {

    private static final long serialVersionUID = 1L;

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TextPatternExtractor.class);

    /** Name of the feature provided by this PipelineProcessor. */
    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens";

    private final NamedPattern pattern;

    public TextPatternExtractor(NamedPattern pattern) {
        this.pattern = pattern;
    }

    public TextPatternExtractor(Pattern pattern, String name) {
        this(new NamedPattern(pattern, name));
    }

    public TextPatternExtractor(String pattern, String name) {
        this(Pattern.compile(pattern), name);
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        String content = document.getContent();
        Matcher matcher = pattern.getPattern().matcher(content);
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature)featureVector.get(PROVIDED_FEATURE);

        if (annotationFeature == null) {
            annotationFeature = new AnnotationFeature(PROVIDED_FEATURE);
            featureVector.add(annotationFeature);
        }

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            Annotation annotation = new PositionAnnotation(document, start, end);
            annotation.getFeatureVector().add(new NominalFeature("patternName", pattern.getName()));
            annotationFeature.add(annotation);
            LOGGER.debug(matcher.group() + "(" + start + "," + end + ")");
        }
    }
}
