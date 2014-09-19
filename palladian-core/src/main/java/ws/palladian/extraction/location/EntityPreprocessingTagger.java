package ws.palladian.extraction.location;

import static ws.palladian.extraction.entity.StringTagger.CANDIDATE_TAG;

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
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * This {@link Tagger} builds on Palladian's {@link StringTagger}, but executes several filtering/preprocessing steps
 * for removing undesired entity candidates. On the other hand it splits up long candidates in additional smaller units.
 * Focus of this process is on high recall of potential entities.
 * </p>
 * 
 * @author Philipp Katz
 */
public class EntityPreprocessingTagger implements Tagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityPreprocessingTagger.class);

    /** The threshold total:uppercase, above which tokens are considered being lowercase. */
    private static final double LOWERCASE_THRESHOLD = 2;

    /** The base tagger, which delivers the annotations. */
    private static final Tagger TAGGER = StringTagger.INSTANCE;

    /** The case dictionary which contains the lowercase ratio for tokens. */
    private static final Map<String, Double> CASE_DICTIONARY = loadCaseDictionaryFromResources("/.caseDictionary.csv");

    private final int longAnnotationSplit;

    public EntityPreprocessingTagger() {
        this(0);
    }

    private static Map<String, Double> loadCaseDictionaryFromResources(String string) {
        InputStream inputStream = null;
        try {
            inputStream = EntityPreprocessingTagger.class.getResourceAsStream("/caseDictionary.csv");
            return loadCaseDictionary(inputStream);
        } finally {
            FileHelper.close(inputStream);
        }
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
                if (ratio >= LOWERCASE_THRESHOLD) { // only add what we really need; save some memory
                    result.put(parts[0], ratio);
                }
            }
        });
        return result;
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        List<? extends Annotation> annotations = TAGGER.getAnnotations(text);
        List<Annotation> fixedAnnotations = CollectionHelper.newArrayList();

        Set<String> inSentence = getInSentenceCandidates(text, annotations);
        inSentence = CollectionHelper.filterSet(inSentence, new Filter<String>() {
            @Override
            public boolean accept(String item) {
                return getLowercaseRatio(item) <= LOWERCASE_THRESHOLD;
            }
        });
        if (inSentence.isEmpty()) { // do not try to fix any phrases, if we do not have any sentences at all (#294)
            fixedAnnotations.addAll(annotations);
            return fixedAnnotations;
        }

        for (Annotation annotation : annotations) {
            String value = annotation.getValue();
            // only annotations at sentence start are processed, but if the annotation also occurs within a sentence, no
            // processing is required
            if (isWithinSentence(text, annotation)) {
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
        LOGGER.debug("Reduced from {} to {} with case dictionary", annotations.size(), fixedAnnotations.size());

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
                            splitAnnotations.add(new ImmutableAnnotation(startPosition, value, CANDIDATE_TAG));
                        }
                        cumulatedTokens.clear();
                    }
                }
                if (cumulatedTokens.size() > 0) {
                    String value = StringUtils.join(cumulatedTokens, " ");
                    if (!value.equals(annotation.getValue()) && value.length() > 1) {
                        int startPosition = annotation.getStartPosition() + annotation.getValue().indexOf(value);
                        splitAnnotations.add(new ImmutableAnnotation(startPosition, value, CANDIDATE_TAG));
                    }
                }
            }
            // add additional splits for annotations with hyphens
            // add additional splits for annotations with &
            String temp = StringHelper.normalizeQuotes(annotation.getValue());
            if (temp.contains("-") || temp.contains("&")) {
                String[] hyphenParts = temp.split("[-&]");
                for (String part : hyphenParts) {
                    String trimmedPart = part.trim();
                    if (StringHelper.startsUppercase(trimmedPart)) {
                        int startPosition = annotation.getStartPosition() + annotation.getValue().indexOf(trimmedPart);
                        splitAnnotations.add(new ImmutableAnnotation(startPosition, trimmedPart, CANDIDATE_TAG));
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
    private static Set<String> getInSentenceCandidates(String text, List<? extends Annotation> annotations) {
        Set<String> inSentence = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            if (isWithinSentence(text, annotation)) {
                String value = annotation.getValue();
                LOGGER.trace("Add '{}' to in-sentence candidates", value);
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
    private static boolean isWithinSentence(String text, Annotation annotation) {
        int start = annotation.getStartPosition();
        return text.substring(Math.max(0, start - 10), start).matches(".*[A-Za-z0-9,]+\\s");
    }

    /**
     * Get the lowercase ratio from the case dictionary.
     * 
     * @param value
     * @return
     */
    private double getLowercaseRatio(String value) {
        Double ratio = CASE_DICTIONARY.get(value.toLowerCase());
        return ratio == null ? 0 : ratio;
    }

    // XXX experimental
    public String correctCapitalization(String value) {
        String[] split = value.split("\\s");
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
            if (getLowercaseRatio(temp) > LOWERCASE_THRESHOLD) {
                part = part.toLowerCase();
            }
            result.append(part);
        }
        return result.toString();
    }

}
