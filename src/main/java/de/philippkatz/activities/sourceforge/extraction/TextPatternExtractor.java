package de.philippkatz.activities.sourceforge.extraction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.featureextraction.Annotation;
import ws.palladian.preprocessing.featureextraction.AnnotationFeature;
import ws.palladian.preprocessing.featureextraction.PositionAnnotation;

public class TextPatternExtractor implements PipelineProcessor {
    
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
    public void process(PipelineDocument document) {
        String content = document.getOriginalContent();
        Matcher matcher = pattern.getPattern().matcher(content);
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(PROVIDED_FEATURE);
        
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
