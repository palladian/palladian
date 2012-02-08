/**
 * Created on: 27.01.2012 19:43:56
 */
package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.model.LabeledSequentialPattern;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.nlp.sentencedetection.AbstractSentenceDetector;

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
 * @since 1.0
 */
public final class LSPAnnotator implements PipelineProcessor {
    /**
     * 
     */
    private static final long serialVersionUID = -1433065329363584974L;

    private Set<String> keywords;

    public static final String LABEL_FEATURE_IDENTIFIER = "ws.palladian.lsplabel";

    public static final String PROVIDED_FEATURE = "ws.palladian.lsp";

    public LSPAnnotator(String[] keywords) {
        super();
        this.keywords = new HashSet<String>();
        Collections.addAll(this.keywords, keywords);
    }

    @Override
    public void process(PipelineDocument document) {
        AnnotationFeature posFeature = (AnnotationFeature)document.getFeatureVector().get(Tokenizer.PROVIDED_FEATURE);
        AnnotationFeature sentencesFeature = (AnnotationFeature)document.getFeatureVector().get(
                AbstractSentenceDetector.PROVIDED_FEATURE);
        List<Annotation> posTags = posFeature.getValue();
        List<Annotation> sentences = sentencesFeature.getValue();
        List<Annotation> markedKeywords = markKeywords(document);

        NominalFeature label = (NominalFeature)document.getFeatureVector().get(LABEL_FEATURE_IDENTIFIER);

        Collections.sort(posTags);
        Collections.sort(sentences);
        Collections.sort(markedKeywords);

        Iterator<Annotation> posTagsIterator = posTags.iterator();
        Iterator<Annotation> markedKeywordsIterator = markedKeywords.iterator();

        Annotation currentMarkedKeyword = markedKeywordsIterator.hasNext() ? markedKeywordsIterator.next() : null;
        Annotation currentPosTag = posTagsIterator.hasNext() ? posTagsIterator.next() : null;

        // create one LSP per sentence in the document.
        for (Annotation sentence : sentences) {
            Integer sentenceStartPosition = sentence.getStartPosition();
            Integer sentenceEndPosition = sentence.getEndPosition();
            List<String> sequentialPattern = new ArrayList<String>();

            Integer i = sentenceStartPosition;
            while (i < sentenceEndPosition) {
                // check for next keyword or part of speech tag.
                if (currentMarkedKeyword != null && currentMarkedKeyword.getStartPosition().equals(i)) {
                    sequentialPattern.add(currentMarkedKeyword.getValue());
                    i = currentMarkedKeyword.getEndPosition();
                } else if (currentPosTag != null && currentPosTag.getStartPosition().equals(i)) {
                    sequentialPattern.add((String)currentPosTag.getFeatureVector()
                            .get(OpenNlpPosTagger.PROVIDED_FEATURE).getValue());
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
            Feature<LabeledSequentialPattern> lspFeature = new Feature<LabeledSequentialPattern>(PROVIDED_FEATURE,
                    new LabeledSequentialPattern(sequentialPattern, label.getValue()));
            sentence.getFeatureVector().add(lspFeature);

        }
    }

    /**
     * <p>
     * Creates a list of annotations of the keywords provided to this {@code PipelineProcessor}.
     * </p>
     * 
     * @param document The {@link PipelineDocument} to process.
     * @return A {@code List} of {@code Annotation}s
     */
    private List<Annotation> markKeywords(PipelineDocument document) {
        List<Annotation> markedKeywords = new LinkedList<Annotation>();
        String originalContent = document.getOriginalContent();
        String originalContentLowerCased = originalContent.toLowerCase();
        for (String keyword : keywords) {
            Pattern keywordPattern = Pattern.compile(keyword.toLowerCase());
            Matcher keywordMatcher = keywordPattern.matcher(originalContentLowerCased);
            if (keywordMatcher.find()) {
                markedKeywords.add(new PositionAnnotation(document, keywordMatcher.start(), keywordMatcher.end()));
            }
        }
        return markedKeywords;
    }
}
