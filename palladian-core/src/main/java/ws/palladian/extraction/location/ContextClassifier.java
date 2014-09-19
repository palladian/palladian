package ws.palladian.extraction.location;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

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

    private CategoryEntries classify(String text, Annotation annotation) {
        CategoryEntriesBuilder result = new CategoryEntriesBuilder();
        List<String> rightContexts = NerHelper.getRightContexts(annotation, text, 3);
        List<String> leftContexts = NerHelper.getLeftContexts(annotation, text, 3);

        for (String rule : rules.keySet()) {
            if (rule.startsWith("* ")) { // note the space
                String token = rule.substring(2);
                for (String value : rightContexts) {
                    if (value.equalsIgnoreCase(token)) {
                        result.add(rules.get(rule), 1);
                        break;
                    }
                }
            } else if (rule.endsWith(" *")) {
                String token = rule.substring(0, rule.length() - 2);
                for (String value : leftContexts) {
                    if (value.equalsIgnoreCase(token)) {
                        result.add(rules.get(rule), 1);
                        break;
                    }
                }
            } else {
                // unknown rule
                LOGGER.warn("rule " + rule + " cannot be interpreted.");
            }
        }
        return result.create();
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

}
