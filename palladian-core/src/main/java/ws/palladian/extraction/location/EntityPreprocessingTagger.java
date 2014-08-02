package ws.palladian.extraction.location;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Tagger;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.ContextTagger;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.entity.WindowSizeContextTagger;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * This {@link Tagger} executes several filtering/preprocessing steps for removing undesired entity candidates.
 * </p>
 * 
 * @author Philipp Katz
 */
public class EntityPreprocessingTagger implements Tagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityPreprocessingTagger.class);

    /** The threshold total:uppercase, above which tokens are considered being lowercase. */
    private static final double LOWERCASE_THRESHOLD = 1.75;

    /** Length of the context. */
    private static final int CONTEXT_LENGTH = 5;

    public static final String SPLIT_ANNOTATION_TAG = "PARTIAL_CANDIDATE";

    /** The base tagger, which delivers the annotations. */
    private final ContextTagger tagger;

    /** The case dictionary which contains the lowercase ratio for tokens. */
    private final Map<String, Double> caseDictionary;

    private final int longAnnotationSplit;

    public EntityPreprocessingTagger() {
        this(0);
    }

    /**
     * <p>
     * Create a new {@link EntityPreprocessingTagger}.
     * </p>
     * 
     * @param longAnnotationSplit Annotations exceeding this amount of tokens, are <i>additionally</i> split up. This
     *            means, for long annotations, additional sub-annotations are created using the case dictionary. Set to
     *            zero to disable spitting.
     */
    public EntityPreprocessingTagger(int longAnnotationSplit) {
        tagger = new WindowSizeContextTagger(StringTagger.PATTERN, StringTagger.CANDIDATE_TAG, CONTEXT_LENGTH);
        InputStream inputStream = null;
        try {
            inputStream = EntityPreprocessingTagger.class.getResourceAsStream("/caseDictionary.csv");
            caseDictionary = loadCaseDictionary(inputStream);
        } finally {
            FileHelper.close(inputStream);
        }
        this.longAnnotationSplit = longAnnotationSplit;
    }

    /**
     * Parse the case dictionary from a CSV file.
     * 
     * @param inputStream
     * @return
     */
    private static final Map<String, Double> loadCaseDictionary(InputStream inputStream) {
        final Map<String, Double> result = CollectionHelper.newHashMap();
        FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split("\t");
                Double ratio = Double.parseDouble(parts[1]) / Double.parseDouble(parts[2]);
                result.put(parts[0], ratio);
            }
        });
        return result;
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        List<ContextAnnotation> annotations = tagger.getAnnotations(text);
        List<Annotation> fixedAnnotations = CollectionHelper.newArrayList();

        Set<String> inSentence = getInSentenceCandidates(annotations);

        // XXX consider also removing within-sentence annotations by case dictionary?

        for (ContextAnnotation annotation : annotations) {
            String value = annotation.getValue();
            // only annotations at sentence start are processed, but if the annotation also occurs within a sentence, no
            // processing is required
            if (isWithinSentence(annotation)) {
                fixedAnnotations.add(annotation);
                continue;
            }
            if (inSentence.contains(value)) {
                LOGGER.trace("Skip '{}', because it appears within a sentence", value);
                fixedAnnotations.add(annotation);
                continue;
            }
            String[] parts = value.split("\\s");
            if (parts.length == 1) { // filtering of single token annotations
                double lcRatio = getLowercaseRatio(value);
                if (lcRatio > LOWERCASE_THRESHOLD) {
                    LOGGER.debug("Drop '{}' because of lc/uc ratio of {}", value, lcRatio);
                    continue;
                }
            } else { // filtering/offset correction of multi-token annotations
                // sliding cut, FIXME double spaces?
                LOGGER.trace("Start correcting '{}'", value);
                int offsetCut = 0;
                String newValue = value;
                for (String token : parts) {
                    double lcRatio = getLowercaseRatio(token);
                    if (lcRatio <= LOWERCASE_THRESHOLD) {
                        LOGGER.trace("Stop correcting '{}' at '{}' because of lc/uc ratio of {}", new Object[] {value,
                                newValue, lcRatio});
                        break;
                    }
                    offsetCut += token.length() + 1;
                    if (offsetCut >= value.length()) {
                        break;
                    }
                    newValue = value.substring(offsetCut);
                    if (inSentence.contains(newValue)) {
                        LOGGER.trace("Stop correcting '{}' as '{}' is contained within sentence", value, newValue);
                        break;
                    }
                }
                if (offsetCut >= value.length()) {
                    LOGGER.debug("Drop '{}' completely because of lc/uc ratio", value);
                    continue;
                } else if (offsetCut > 0) { // annotation start was corrected
                    LOGGER.debug("Correct '{}' to '{}' because of lc/uc ratios", value, newValue);
                    int newStart = annotation.getStartPosition() + offsetCut;
                    fixedAnnotations.add(new ImmutableAnnotation(newStart, newValue, annotation.getTag()));
                    continue;
                }
            }
            fixedAnnotations.add(annotation);
        }
        LOGGER.debug("Reduced from {} to {} with with case dictionary", annotations.size(), fixedAnnotations.size());

        if (longAnnotationSplit > 0) {
            List<Annotation> additionalAnnotations = getLongAnnotationSplit(fixedAnnotations, longAnnotationSplit);
            LOGGER.debug("Extracted additional {} annotations by splitting", additionalAnnotations.size());
            fixedAnnotations.addAll(additionalAnnotations);
        }

        return fixedAnnotations;
    }

    /**
     * Split-up long annotations, with exceed a specified length of tokens. Therefore, also the case dictionary is
     * employed; we split on lowercased words from the case dictionary.
     * 
     * @param annotations
     * @param length
     * @return List with all additionally created annotations.
     */
    List<Annotation> getLongAnnotationSplit(List<Annotation> annotations, int length) {
        List<Annotation> splitAnnotations = CollectionHelper.newArrayList();
        for (Annotation annotation : annotations) {
            String[] parts = annotation.getValue().split("\\s");
            if (parts.length >= length) {
                List<String> cumulatedTokens = CollectionHelper.newArrayList();
                for (String token : parts) {
                    double lcRatio = getLowercaseRatio(token);
                    if (lcRatio < LOWERCASE_THRESHOLD) {
                        cumulatedTokens.add(token);
                    } else if (cumulatedTokens.size() > 0) {
                        String value = StringUtils.join(cumulatedTokens, " ");
                        if (value.length() > 1) {
                            int startPosition = annotation.getStartPosition() + annotation.getValue().indexOf(value);
                            splitAnnotations.add(new ImmutableAnnotation(startPosition, value, SPLIT_ANNOTATION_TAG));
                        }
                        cumulatedTokens.clear();
                    }
                }
                if (cumulatedTokens.size() > 0) {
                    String value = StringUtils.join(cumulatedTokens, " ");
                    if (value.length() > 1) {
                        int startPosition = annotation.getStartPosition() + annotation.getValue().indexOf(value);
                        splitAnnotations.add(new ImmutableAnnotation(startPosition, value, SPLIT_ANNOTATION_TAG));
                    }
                }
            }
            // add additional splits for annotations with hyphens
            String temp = StringHelper.normalizeQuotes(annotation.getValue());
            if (temp.contains("-")) {
                String[] hyphenParts = temp.split("-");
                for (String part : hyphenParts) {
                    if (StringHelper.startsUppercase(part)) {
                        int startPosition = annotation.getStartPosition() + annotation.getValue().indexOf(part);
                        splitAnnotations.add(new ImmutableAnnotation(startPosition, part, SPLIT_ANNOTATION_TAG));
                    }
                }
            }
        }
        return splitAnnotations;
    }

    /**
     * Get the values of those annotations which occur within a sentence (i.e. all annotations which are not the first
     * word at the beginning of a sentence).
     * 
     * @param annotations
     * @return
     */
    private static Set<String> getInSentenceCandidates(List<ContextAnnotation> annotations) {
        Set<String> inSentence = CollectionHelper.newHashSet();
        for (ContextAnnotation annotation : annotations) {
            if (isWithinSentence(annotation)) {
                String value = annotation.getValue();
                LOGGER.trace("Add '{}' to in-sentence candidates ({})", value, annotation.getLeftContext());
                inSentence.add(value);
            }
        }
        return inSentence;
    }

    /**
     * Determine via the left context, if the annotation is within an sentence/paragraph (i.e. not first word).
     * 
     * @param annotation
     * @return
     */
    private static boolean isWithinSentence(ContextAnnotation annotation) {
        return annotation.getLeftContext().matches(".*[A-Za-z0-9,]+\\s");
    }

    /**
     * Get the lowercase ratio from the case dictionary.
     * 
     * @param value
     * @return
     */
    private double getLowercaseRatio(String value) {
        Double ratio = caseDictionary.get(value.toLowerCase());
        return ratio == null ? 0 : ratio;
    }

    // XXX experimental
    public String correctCapitalization(String sentence) {
        String[] split = sentence.split("\\s");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            if (i > 0) {
                result.append(" ");
            }
            String temp = part;
            // last part of sentence
            if (i == split.length - 1 && part.endsWith(".")) {
                temp = part.substring(0, part.length() - 1);
            }
            if (i > 0 && getLowercaseRatio(temp) > LOWERCASE_THRESHOLD) {
                part = part.toLowerCase();
            }
            result.append(part);
        }
        return result.toString();
    }

    public static void main(String[] args) {
        EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();
        List<Annotation> annotations = tagger.getAnnotations(HtmlHelper.stripHtmlTags(FileHelper
                .tryReadFileToString("/Users/pk/Desktop/LocationLab/TUD-Loc-2013_V1/text27.txt")));
        CollectionHelper.print(annotations);
    }

}
