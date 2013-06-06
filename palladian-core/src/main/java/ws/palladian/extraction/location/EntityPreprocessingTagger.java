package ws.palladian.extraction.location;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.ContextTagger;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.entity.WindowSizeContextTagger;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotated;

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
    private static final double LOWERCASE_THRESHOLD = 1.25;

    /** Length of the context. */
    private static final int CONTEXT_LENGTH = 5;

    /** The base tagger, which delivers the annotations. */
    private final ContextTagger tagger;

    /** The case dictionary which contains the lowercase ratio for tokens. */
    private final Map<String, Double> caseDictionary;

    public EntityPreprocessingTagger() {
        tagger = new WindowSizeContextTagger(StringTagger.PATTERN, StringTagger.CANDIDATE_TAG, CONTEXT_LENGTH);
        InputStream inputStream = null;
        try {
            inputStream = EntityPreprocessingTagger.class.getResourceAsStream("/caseDictionary.csv");
            // inputStream = EntityPreprocessingTagger.class.getResourceAsStream("/wikipediaCaseDictionary.csv");
            caseDictionary = loadCaseDictionary(inputStream);
        } finally {
            FileHelper.close(inputStream);
        }
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
                Double ratio = Double.valueOf(parts[1]) / Double.valueOf(parts[2]);
                result.put(parts[0], ratio);
            }
        });
        return result;
    }

    @Override
    public List<Annotated> getAnnotations(String text) {
        List<ContextAnnotation> annotations = tagger.getAnnotations(text);
        List<Annotated> fixedAnnotations = CollectionHelper.newArrayList();

        Set<String> lowercaseTokens = getLowercaseTokens(text);
        Set<String> inSentence = getInSentenceCandidates(annotations);

        // XXX consider also removing within-sentence annotations by case dictionary?

        for (ContextAnnotation annotation : annotations) {
            String value = annotation.getValue();
            // only annotations at sentence start are processed, but if the annotation also occurs within a sentence, no
            // processing is required
            if (!isAtSentenceStart(annotation)) {
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
                } else if (lowercaseTokens.contains(value.toLowerCase())) {
                    LOGGER.debug("Drop '{}' because it occurs lowercased", value);
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
                    fixedAnnotations.add(new Annotation(newStart, newValue, annotation.getTag()));
                    continue;
                }
            }
            fixedAnnotations.add(annotation);
        }
        LOGGER.debug("Reduced from {} to {} with with case dictionary", annotations.size(), fixedAnnotations.size());
        return fixedAnnotations;
    }

    /**
     * Get all lowercase tokens from the text.
     * 
     * @param text
     * @return
     */
    private static Set<String> getLowercaseTokens(String text) {
        Set<String> lowercaseTokens = CollectionHelper.newHashSet();
        List<String> tokens = Tokenizer.tokenize(text);
        for (String token : tokens) {
            if (!StringHelper.startsUppercase(token)) {
                lowercaseTokens.add(token);
            }
        }
        return lowercaseTokens;
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
            if (!isAtSentenceStart(annotation)) {
                String value = annotation.getValue();
                LOGGER.trace("Add '{}' to in-sentence candidates ({})", value, annotation.getLeftContext());
                inSentence.add(value);
            }
        }
        return inSentence;
    }

    /**
     * Determine via the left context, if the annotation is at sentence/paragraph start.
     * 
     * @param annotation
     * @return
     */
    private static boolean isAtSentenceStart(ContextAnnotation annotation) {
        // return annotation.getLeftContext().matches(".*[.:?!]\\s+|.*\n{2,}|$^");
        return annotation.getLeftContext().matches(".*[.:?!][^A-Za-z0-9]+|.*\n{2,}|$^");
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

    public static void main(String[] args) {
        EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();
        List<Annotated> annotations = tagger.getAnnotations(HtmlHelper.stripHtmlTags(FileHelper
                .readFileToString("/Users/pk/Desktop/LocationLab/TUD-Loc-2013_V1/text27.txt")));
        CollectionHelper.print(annotations);
    }

}
