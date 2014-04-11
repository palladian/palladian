/**
 * Created on: 27.01.2012 19:43:56
 */
package ws.palladian.extraction.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.pos.AbstractPosTagger;
import ws.palladian.extraction.pos.OpenNlpPosTagger;
import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;
import ws.palladian.processing.features.SequentialPattern;

/**
 * <p>
 * Takes a text that is already part of speech tagged and has marked sentences. From this text it extracts Labeled
 * Sequential Patterns [1]. The {@code PipelineProcessor} also requires a list of keywords it should mark in the
 * patterns. If a word is a keyword its value is used in the sequential pattern instead of the part of speech tag. The
 * labels are taken from the feature identified by {@link #LABEL_FEATURE_IDENTIFIER}. To summarize, the following
 * prerequisits are necessary to use this {@code PipelineProcessor}:
 * </p>
 * <ol>
 * <li>Complete text is part of speech tagged. For example by using {@link OpenNlpPosTagger}.
 * <li>Complete text is split into sentences. This may be achieved using an implementation of
 * {@link AbstractSentenceDetector}.
 * <li>The provided {@link PipelineDocument} contains a {@link NominalFeature} denoting the label to use for the
 * sequential patterns. This label is identified using {@code #LABEL_FEATURE_IDENTIFIER}.
 * <li>A list of keywords as parameter for the constructor of this class.
 * </ol>
 * <p>
 * [1] ﻿Cong, G., Wang, L., Lin, C. Y., Song, Y. I., & Sun, Y. (2008). Finding question-answer pairs from online forums.
 * Proceedings of the 31st annual international ACM SIGIR conference on Research and development in information
 * retrieval (pp. 467–474). New York, NY, USA: ACM. doi:10.1145/1390334.1390415
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class SequentialPatternAnnotator extends TextDocumentPipelineProcessor {

    // private static final Logger LOGGER = LoggerFactory.getLogger(SequentialPatternAnnotator.class);

    private Set<String> keywords;

    public static final String PROVIDED_FEATURE = "lsp";

    private Integer maxSequentialPatternSize = 0;

    private Integer minSequentialPatternSize;

    private SpanExtractionStrategy extractionStrategy;

    /**
     * <p>
     * Creates a new LSP annotator with a list of keywords. This annotator is ready to use.
     * </p>
     * 
     * @param keywords
     *            The keywords used by this annotator.
     * @param minSequentialPatternSize
     * @param maxSequentialPatternSize
     */
    public SequentialPatternAnnotator(final String[] keywords, final Integer minSequentialPatternSize,
                                      final Integer maxSequentialPatternSize,
                                      final SpanExtractionStrategy extractionStrategy) {
        super();

        Validate.notNull(keywords, "keywords must not be null");
        Validate.notNull(minSequentialPatternSize, "minSequentialPatternSize must not be null");
        Validate.notNull(maxSequentialPatternSize, "maxSequentialPatternSize must not be null");
        Validate.notNull(extractionStrategy, "extractionStrategy must not be null");
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, minSequentialPatternSize);
        Validate.inclusiveBetween(minSequentialPatternSize, Integer.MAX_VALUE, maxSequentialPatternSize);

        this.keywords = new HashSet<String>();
        Collections.addAll(this.keywords, keywords);
        this.minSequentialPatternSize = minSequentialPatternSize;
        this.maxSequentialPatternSize = maxSequentialPatternSize;
        this.extractionStrategy = extractionStrategy;
    }

    @Override
    public void processDocument(TextDocument document) {
        // LOGGER.debug(document.toString());

        List<PositionAnnotation> posTags = new ArrayList<PositionAnnotation>(document.get(
                ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE));
        List<PositionAnnotation> sentences = new ArrayList<PositionAnnotation>(document.get(
                ListFeature.class, AbstractSentenceDetector.PROVIDED_FEATURE));
        List<PositionAnnotation> markedKeywords = new ArrayList<PositionAnnotation>(markKeywords(document));

        Collections.sort(posTags);
        Collections.sort(sentences);
        Collections.sort(markedKeywords);

        Iterator<PositionAnnotation> posTagsIterator = posTags.iterator();
        Iterator<PositionAnnotation> markedKeywordsIterator = markedKeywords.iterator();

        PositionAnnotation currentMarkedKeyword = markedKeywordsIterator.hasNext() ? markedKeywordsIterator.next()
                : null;
        PositionAnnotation currentPosTag = posTagsIterator.hasNext() ? posTagsIterator.next() : null;
        ListFeature<SequentialPattern> feature = new ListFeature<SequentialPattern>(PROVIDED_FEATURE);

        // create one LSP per sentence in the document.
        for (PositionAnnotation sentence : sentences) {
            Integer sentenceStartPosition = sentence.getStartPosition();
            Integer sentenceEndPosition = sentence.getEndPosition();
            List<String> sequentialPattern = new ArrayList<String>();

            Integer i = sentenceStartPosition;
            while (i < sentenceEndPosition) {
                // check for next keyword or part of speech tag.
                if (currentMarkedKeyword != null && Integer.valueOf(currentMarkedKeyword.getStartPosition()).equals(i)) {
                    sequentialPattern.add(currentMarkedKeyword.getValue());
                    i = currentMarkedKeyword.getEndPosition();
                } else if (currentPosTag != null && Integer.valueOf(currentPosTag.getStartPosition()).equals(i)) {
                    NominalFeature posTagFeature = currentPosTag.getFeatureVector().get(
                            NominalFeature.class, AbstractPosTagger.PROVIDED_FEATURE);
                    // LOGGER.trace("currentPosTag: "+ currentPosTag.toString());
                    // LOGGER.trace("posTagFeature: "+posTagFeature);
                    sequentialPattern.add(posTagFeature.getValue());
                    i = currentPosTag.getEndPosition();
                } else {
                    i++;
                }

                // iterate keywords and part of speech tags.
                while (currentMarkedKeyword != null && currentMarkedKeyword.getStartPosition() < i
                        && markedKeywordsIterator.hasNext()) {
                    currentMarkedKeyword = markedKeywordsIterator.next();
                }

                while (currentPosTag != null && currentPosTag.getStartPosition() < i && posTagsIterator.hasNext()) {
                    currentPosTag = posTagsIterator.next();
                }
            }

            String[] arrayOfWholeSentencePattern = sequentialPattern.toArray(new String[sequentialPattern.size()]);
            List<SequentialPattern> extractedPatterns = extractionStrategy.extract(
                    arrayOfWholeSentencePattern, minSequentialPatternSize, maxSequentialPatternSize);
            feature.addAll(extractedPatterns);

        }
        document.add(feature);
    }

    /**
     * <p>
     * Creates a list of annotations of the keywords provided to this {@code PipelineProcessor}.
     * </p>
     * 
     * @param document
     *            The {@link PipelineDocument} to process.
     * @return A {@code List} of {@code Annotation}s
     */
    private List<PositionAnnotation> markKeywords(TextDocument document) {
        List<PositionAnnotation> markedKeywords = new LinkedList<PositionAnnotation>();
        String originalContent = document.getContent();
        String originalContentLowerCased = originalContent.toLowerCase();
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(document);
        for (String keyword : keywords) {
            Pattern keywordPattern = Pattern.compile(keyword.toLowerCase());
            Matcher keywordMatcher = keywordPattern.matcher(originalContentLowerCased);
            if (keywordMatcher.find()) {
                markedKeywords.add(annotationFactory.create(keywordMatcher.start(), keywordMatcher.end()));
            }
        }
        return markedKeywords;
    }
}
