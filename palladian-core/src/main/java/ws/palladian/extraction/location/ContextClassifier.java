package ws.palladian.extraction.location;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

/**
 * <p>
 * The {@link ContextClassifier} is used for classifying {@link Annotation} items by its text context. The contexts are
 * to be defined manually as rule set and are then evaluated. The result is a {@link ClassifiedAnnotation}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class ContextClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextClassifier.class);

    // TODO create dedicated rule class with apply logic
    private static final Map<String, String> rules = readRules(ContextClassifier.class
            .getResourceAsStream("/perLocContexts.csv"));

    private final ClassificationMode mode;

    public static enum ClassificationMode {
        /** Classify each annotation separately, this usually yields to different results per annotation. */
        ISOLATED,
        /**
         * Collect classification values for identical annotations, so that every annotation is classified separately.
         * This is the assumption of a "single sense per discourse".
         */
        PROPAGATION
    }

    public static class ClassifiedAnnotation extends ImmutableAnnotation {

        private final CategoryEntries categoryEntries;

        public ClassifiedAnnotation(Annotation annotation, CategoryEntries categoryEntries) {
            super(annotation.getStartPosition(), annotation.getValue(), annotation.getTag());
            this.categoryEntries = categoryEntries;
        }

        public CategoryEntries getCategoryEntries() {
            return categoryEntries;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ClassifiedAnnotation [value=");
            builder.append(getValue());
            builder.append(", startPosition=");
            builder.append(getStartPosition());
            builder.append(", classification=");
            builder.append(categoryEntries);
            builder.append("]");
            return builder.toString();
        }

    }

    public ContextClassifier(ClassificationMode mode) {
        this.mode = mode;
    }

    private static final Map<String, String> readRules(InputStream inputStream) {
        final Map<String, String> rules = CollectionHelper.newHashMap();
        FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("\t");
                if (line.startsWith("#") || split.length != 2) {
                    return;
                }
                rules.put(split[0], split[1]);
            }
        });
        LOGGER.debug("Loaded {} context rules", rules.size());
        return rules;
    }

    public CategoryEntries classify(String text, Annotation annotation) {
        CategoryEntriesBuilder result = new CategoryEntriesBuilder();
        for (String rule : rules.keySet()) {
            if (rule.startsWith("* ")) { // note the space
                // suffix rules
                String token = rule.substring(2);
                int windowSize = token.split("\\s").length;
                String context = getRightContext(annotation, text, windowSize);
                if (token.equalsIgnoreCase(context)) {
                    result.add(rules.get(rule), 1);
                }
            } else if (rule.endsWith(" *")) {
                // prefix rules
                String token = rule.substring(0, rule.length() - 2);
                int windowSize = token.split("\\s").length;
                String context = getLeftContext(annotation, text, windowSize);
                if (token.equalsIgnoreCase(context)) {
                    result.add(rules.get(rule), 1);
                }
            } else if (rule.startsWith("*")) {
                // suffix proximity rules
                int windowSize = countAsteriscs(rule);
                String token = rule.replaceAll("\\**\\s", "");
                List<String> contextTokens = getRightContexts(annotation, text, windowSize);
                if (containsIgnoreCase(contextTokens, token)) {
                    result.add(rules.get(rule), 1);
                }
            } else if (rule.endsWith("*")) {
                // prefix proximity rules
                int windowSize = countAsteriscs(rule);
                String token = rule.replaceAll("\\s\\**", "");
                List<String> contextTokens = getLeftContexts(annotation, text, windowSize);
                if (containsIgnoreCase(contextTokens, token)) {
                    result.add(rules.get(rule), 1);
                }
            } else {
                // unknown rule
                LOGGER.warn("rule " + rule + " cannot be interpreted.");
            }
        }
        return result.create();
    }

    private boolean containsIgnoreCase(Collection<String> collection, String string) {
        for (String item : collection) {
            if (item.equalsIgnoreCase(string)) {
                return true;
            }
        }
        return false;
    }

    private int countAsteriscs(String rule) {
        return rule.length() - rule.replace("*", "").length();
    }

    public List<ClassifiedAnnotation> classify(List<? extends Annotation> annotations, String text) {
        List<ClassifiedAnnotation> result = CollectionHelper.newArrayList();
        if (mode == ClassificationMode.ISOLATED) {
            for (Annotation annotation : annotations) {
                CategoryEntries classification = classify(text, annotation);
                result.add(new ClassifiedAnnotation(annotation, classification));
            }
        } else if (mode == ClassificationMode.PROPAGATION) {
            Map<String, CategoryEntriesBuilder> collectedProbabilities = LazyMap
                    .create(new Factory<CategoryEntriesBuilder>() {
                        @Override
                        public CategoryEntriesBuilder create() {
                            return new CategoryEntriesBuilder();
                        }
                    });
            for (Annotation annotation : annotations) {
                CategoryEntries classification = classify(text, annotation);
                collectedProbabilities.get(annotation.getValue()).add(classification);
            }
            for (Annotation annotation : annotations) {
                CategoryEntries classification = collectedProbabilities.get(annotation.getValue()).create();
                result.add(new ClassifiedAnnotation(annotation, classification));
            }
        }
        return result;
    }

    public static String getLeftContext(Annotation annotation, String text, int numWords) {
        try {
            StringBuilder builder = new StringBuilder();
            int wordCounter = 0;
            int start = annotation.getStartPosition() - 1;
            for (int i = start; i >= 0; i--) {
                char current = text.charAt(i);
                if (current == ' ' && i < start) {
                    wordCounter++;
                }
                if (wordCounter >= numWords || current == '\n' || StringHelper.isPunctuation(current)) {
                    break;
                }
                builder.append(current);
            }
            return StringHelper.reverseString(builder.toString()).trim();
        } catch (Exception e) {
            // this exception is only caused by nested annotations as far as i can see
            // System.out.println("Exception for:");
            // System.out.println("Text:\n");
            // System.out.println(text);
            // System.out.println(annotation);
            // throw new IllegalStateException(e);
        }
        return null;
    }

    public static String getRightContext(Annotation annotation, String text, int numWords) {
        try {
            StringBuilder builder = new StringBuilder();
            int wordCounter = 0;
            int start = annotation.getEndPosition();
            for (int i = start; i < text.length(); i++) {
                char current = text.charAt(i);
                if (current == ' ' && i > start) {
                    wordCounter++;
                }
                if (wordCounter >= numWords || current == '\n' || StringHelper.isPunctuation(current)) {
                    break;
                }
                builder.append(current);
            }
            return builder.toString().trim();
        } catch (Exception e) {
            // see above.
        }
        return null;
    }

    public static List<String> getLeftContexts(Annotation annotation, String text, int numWords) {
        return Arrays.asList(getLeftContext(annotation, text, numWords).split("\\s"));
    }

    public static List<String> getRightContexts(Annotation annotation, String text, int numWords) {
        return Arrays.asList(getRightContext(annotation, text, numWords).split("\\s"));
    }

    public static void main(String[] args) {
        String text = FileHelper
                .tryReadFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/2-validation/text_44148889.txt");
        // String text = FileHelper.readFileToString("src/test/resources/Dresden.wikipedia");
        // text = WikipediaUtil.stripMediaWikiMarkup(text);
        // String text = "ruler of Saxony Frederick Augustus I became King";
        text = HtmlHelper.stripHtmlTags(text);

        // Tagger tagger = new StringTagger();
        Tagger tagger = new EntityPreprocessingTagger();
        List<? extends Annotation> annotations = tagger.getAnnotations(text);
        ContextClassifier classifier = new ContextClassifier(ClassificationMode.PROPAGATION);
        // classifier.filter(annotations, text);
        List<ClassifiedAnnotation> classification = classifier.classify(annotations, text);
        CollectionHelper.print(classification);
        //        for (Annotated annotation : annotations) {
        //            CategoryEntries result = classifier.classify(text, annotation);
        //            // CategoryEntries result = classifier.classify(text, annotation);
        //            if (result.getProbability("PER") == 1) {
        //                System.out.println(annotation.getValue() + " : " + result);
        //            }
        //        }
    }
}
