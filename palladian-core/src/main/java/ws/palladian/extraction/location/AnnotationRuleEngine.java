package ws.palladian.extraction.location;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

public class AnnotationRuleEngine {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationRuleEngine.class);

    private static final String BOUNDARY_CHAR = "|";

    private static final String REGEX_START = "{{";
    
    private static final String REGEX_END = "}}";

    /** A rule which can be applied to entities; in case the rule matches, a {@link Action} is triggered. */
    private static abstract class Rule {
        final String outcome;
        final Action action;

        Rule(String outcome, Action action) {
            this.outcome = outcome;
            this.action = action;
        }

        abstract boolean matches(String text, Annotation annotation);

        void apply(Annotation annotation, String text, Map<Annotation, CategoryEntriesBuilder> probabilities) {
            if (matches(text, annotation)) {
                action.apply(annotation, probabilities, outcome);
            }
        }
    }

    /** An action which is taken, when a rule matches. */
    public static enum Action {
        /** Removes the given annotation. */
        REMOVE_THIS {
            @Override
            void apply(Annotation annotation, Map<Annotation, CategoryEntriesBuilder> probs, String outcome) {
                probs.remove(annotation);
            }
        },
        /** Removes the given annotation and other annotations, which contain a token from the current. */
        REMOVE_FRAGMENTS {
            @Override
            void apply(Annotation annotation, Map<Annotation, CategoryEntriesBuilder> probs, String outcome) {
                Set<String> parts = CollectionHelper.newHashSet(annotation.getValue().split("\\s"));
                Iterator<Annotation> iterator = probs.keySet().iterator();
                while (iterator.hasNext()) {
                    if (StringHelper.containsWord(parts, iterator.next().getValue())) {
                        iterator.remove();
                    }
                }
            }
        },
        /** Classifies the given annotation. */
        CLASSIFY_THIS {
            @Override
            void apply(Annotation annotation, Map<Annotation, CategoryEntriesBuilder> probs, String outcome) {
                probs.get(annotation).add(outcome, 1);
            }
        },
        /**
         * Propagate rule to annotations with exact same value; e.g. when an annotation was classified as "PER" by its
         * context, the classification is inherited to other annotations with the same value, even if they do not have
         * the particular context.
         */
        CLASSIFY_VALUE {
            @Override
            void apply(Annotation annotation, Map<Annotation, CategoryEntriesBuilder> probs, String outcome) {
                for (Entry<Annotation, CategoryEntriesBuilder> annotationEntry : probs.entrySet()) {
                    Annotation otherAnnotation = annotationEntry.getKey();
                    if (otherAnnotation.getValue().equalsIgnoreCase(annotation.getValue())) {
                        annotationEntry.getValue().add(outcome, 1);
                    }
                }
            }
        },
        /**
         * Propagate rule to annotations which share a fragment; in comparison to {@link #EXACT}, this makes to
         * propagation even more aggressive/fuzzy. Consider the example "Mr. John Smith", which was classified as "PER".
         * In this mode, also the entities "John" and "Smith" would inherit the classification "PER".
         */
        CLASSIFY_FRAGMENTS {
            @Override
            void apply(Annotation annotation, Map<Annotation, CategoryEntriesBuilder> probs, String outcome) {
                Set<String> parts = CollectionHelper.newHashSet(annotation.getValue().split("\\s"));
                for (Entry<Annotation, CategoryEntriesBuilder> annotationEntry : probs.entrySet()) {
                    Annotation otherAnnotation = annotationEntry.getKey();
                    if (StringHelper.containsWord(parts, otherAnnotation.getValue())) {
                        probs.get(otherAnnotation).add(outcome, 1);
                    }
                }
            }
        };

        abstract void apply(Annotation annotation, Map<Annotation, CategoryEntriesBuilder> probs, String outcome);
    };

    private final List<Rule> rules;

    private static List<Rule> parseRules(InputStream inputStream) {
        final List<Rule> rules = CollectionHelper.newArrayList();
        FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (line.isEmpty() || line.startsWith("#")) {
                    return;
                }
                rules.add(parseRule(line, lineNumber));
            }
        });
        LOGGER.debug("Loaded {} rules", rules.size());
        return rules;
    }

    private static Rule parseRule(String line, int lineNumber) {
        String[] split = line.split("\t");
        if (split.length != 2 && split.length != 3) {
            throw new IllegalStateException("Could not parse '" + line + "' in line " + lineNumber + ".");
        }
        String condition = split[0];
        Action action = Action.valueOf(split[1]);
        String outcome = split.length == 3 ? split[2] : null;
        if (condition.startsWith(BOUNDARY_CHAR + REGEX_START) && condition.endsWith(REGEX_END + BOUNDARY_CHAR)) {
            // regex rule for entity
            final Pattern pattern = Pattern.compile(condition.substring(3, condition.length() - 3));
            return new Rule(outcome, action) {
                @Override
                boolean matches(String text, Annotation annotation) {
                    return pattern.matcher(annotation.getValue()).matches();
                }
            };
        } else {
            String patternString = condition.replace(BOUNDARY_CHAR, "").trim().replace(".", "\\.").replace("*", ".*");
            final Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
            final int contextSize = patternString.split("\\s").length;
            if (condition.startsWith(BOUNDARY_CHAR) && condition.endsWith(BOUNDARY_CHAR)) {
                // entity rule
                return new Rule(outcome, action) {
                    @Override
                    boolean matches(String text, Annotation annotation) {
                        return pattern.matcher(annotation.getValue()).matches();
                    }
                };
            } else if (condition.startsWith(BOUNDARY_CHAR)) {
                // prefix rule
                return new Rule(outcome, action) {
                    @Override
                    boolean matches(String text, Annotation annotation) {
                        List<String> temp = NerHelper.getRightContexts(annotation, text, contextSize);
                        String rightContext = CollectionHelper.getLast(temp);
                        return rightContext != null && pattern.matcher(rightContext).matches();
                    }
                };
            } else if (condition.endsWith(BOUNDARY_CHAR)) {
                // suffix rule
                return new Rule(outcome, action) {
                    @Override
                    boolean matches(String text, Annotation annotation) {
                        List<String> temp = NerHelper.getLeftContexts(annotation, text, contextSize);
                        String leftContext = CollectionHelper.getLast(temp);
                        return leftContext != null && pattern.matcher(leftContext).matches();
                    }
                };
            }
        }
        // invalid rule
        throw new IllegalStateException("Could not parse '" + line + "' in line " + lineNumber + ".");
    }

    public AnnotationRuleEngine(InputStream inputStream) {
        Validate.notNull(inputStream, "inputStream must not be null");
        this.rules = parseRules(inputStream);
    }

    public List<ClassifiedAnnotation> apply(String text, List<? extends Annotation> annotations) {
        Map<Annotation, CategoryEntriesBuilder> probabilities = CollectionHelper.newLinkedHashMap();
        for (Annotation annotation : annotations) {
            probabilities.put(annotation, new CategoryEntriesBuilder());
        }
        for (Annotation annotation : annotations) {
            for (Rule rule : rules) {
                rule.apply(annotation, text, probabilities);
            }
        }
        List<ClassifiedAnnotation> result = CollectionHelper.newArrayList();
        for (Entry<Annotation, CategoryEntriesBuilder> resultEntry : probabilities.entrySet()) {
            result.add(new ClassifiedAnnotation(resultEntry.getKey(), resultEntry.getValue().create()));
        }
        return result;
    }

}
