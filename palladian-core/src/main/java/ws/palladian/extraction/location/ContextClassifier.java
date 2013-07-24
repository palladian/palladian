package ws.palladian.extraction.location;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.Annotation;

public class ContextClassifier {

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

    public static class ClassifiedAnnotation extends Annotation {

        private final CategoryEntries categoryEntries;

        public ClassifiedAnnotation(Annotated annotation, CategoryEntries categoryEntries) {
            super(annotation.getStartPosition(), annotation.getValue(), annotation.getTag());
            this.categoryEntries = categoryEntries;
        }

        public CategoryEntries getCategoryEntries() {
            return categoryEntries;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ClassifiedAnnotation [classification=");
            builder.append(categoryEntries);
            builder.append(", getStartPosition()=");
            builder.append(getStartPosition());
            builder.append(", getTag()=");
            builder.append(getTag());
            builder.append(", getValue()=");
            builder.append(getValue());
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
                rules.put(split[0], split[1]);
            }
        });
        return rules;
    }

    public CategoryEntries classify(String text, Annotated annotation) {
        CategoryEntriesMap result = new CategoryEntriesMap();
        for (String rule : rules.keySet()) {
            if (rule.startsWith("*")) {
                // suffix rules
                String token = rule.substring(2);
                String context = getRightContext(annotation, text, 1);
                if (token.equalsIgnoreCase(context)) {
                    result.add(rules.get(rule), 1);
                }
            } else if (rule.endsWith("*")) {
                // prefix rules
                String token = rule.substring(0, rule.length() - 2);
                String context = getLeftContext(annotation, text, 1);
                if (token.equalsIgnoreCase(context)) {
                    result.add(rules.get(rule), 1);
                }
            } else {
                System.out.println("[warn] rule " + rule + " cannot be interpreted.");
                // unknown rule
            }
        }
        result.computeProbabilities();
        result.sort();
        return result;
    }
    
//    public List<Annotated> filter(List<? extends Annotated> annotations, String text) {
//        Set<String> toRemove = CollectionHelper.newHashSet();
//        for (Annotated annotation : annotations) {
//            CategoryEntries classification = classify(text, annotation);
//            if (classification.getProbability("PER") == 1) {
//                toRemove.add(annotation.getValue());
//            }
//            if (classification.getProbability("LOC") == 1) {
//                toRemove.remove(annotation.getValue());
//            }
//        }
//        List<Annotated> result = CollectionHelper.newArrayList();
//        for (Annotated annotation : annotations) {
//            if (toRemove.contains(annotation.getValue())) {
//                System.out.println("Removing " + annotation);
//                continue;
//            }
//            result.add(annotation);
//        }
//        return result;
//    }

    public List<ClassifiedAnnotation> classify(List<? extends Annotated> annotations, String text) {
        List<ClassifiedAnnotation> result = CollectionHelper.newArrayList();
        if (mode == ClassificationMode.ISOLATED) {
            for (Annotated annotation : annotations) {
                CategoryEntries classification = classify(text, annotation);
                result.add(new ClassifiedAnnotation(annotation, classification));
            }
        } else if (mode == ClassificationMode.PROPAGATION) {
            Map<String, CategoryEntriesMap> collectedProbabilities = LazyMap.create(new Factory<CategoryEntriesMap>() {
                @Override
                public CategoryEntriesMap create() {
                    return new CategoryEntriesMap();
                }
            });
            for (Annotated annotation : annotations) {
                CategoryEntries classification = classify(text, annotation);
                CategoryEntries existingClassification = collectedProbabilities.get(annotation.getValue());
                CategoryEntriesMap newClassification = CategoryEntriesMap.merge(existingClassification, classification);
                collectedProbabilities.put(annotation.getValue(), newClassification);
            }
            for (CategoryEntriesMap categoryEntry : collectedProbabilities.values()) {
                categoryEntry.computeProbabilities();
                categoryEntry.sort();
            }
            for (Annotated annotation : annotations) {
                CategoryEntriesMap classification = collectedProbabilities.get(annotation.getValue());
                result.add(new ClassifiedAnnotation(annotation, classification));
            }
        }
        return result;
    }

    public static String getLeftContext(Annotated annotation, String text, int numWords) {
        try {
            StringBuilder builder = new StringBuilder();
            int wordCounter = 0;
            int start = annotation.getStartPosition() - 1;
            for (int i = start; i >= 0; i--) {
                char current = text.charAt(i);
                if (current == ' ' && i < start) {
                    wordCounter++;
                }
                if (wordCounter >= numWords || current == '\n' || current == '.') {
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

    public static String getRightContext(Annotated annotation, String text, int numWords) {
        try {
            StringBuilder builder = new StringBuilder();
            int wordCounter = 0;
            int start = annotation.getEndPosition();
            for (int i = start; i < text.length(); i++) {
                char current = text.charAt(i);
                if (current == ' ' && i > start) {
                    wordCounter++;
                }
                if (wordCounter >= numWords || current == '\n' || current == '.') {
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

    public static List<String> getLeftContexts(Annotated annotated, String text, int numWords) {
        return Arrays.asList(getLeftContext(annotated, text, numWords).split("\\s"));
    }

    public static List<String> getRightContexts(Annotated annotated, String text, int numWords) {
        return Arrays.asList(getRightContext(annotated, text, numWords).split("\\s"));
    }

    public static void main(String[] args) {
        String text = FileHelper
                .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/2-validation/text_44148889.txt");
        // String text = FileHelper.readFileToString("src/test/resources/Dresden.wikipedia");
        // text = WikipediaUtil.stripMediaWikiMarkup(text);
        // String text = "ruler of Saxony Frederick Augustus I became King";
        text = HtmlHelper.stripHtmlTags(text);

        // Tagger tagger = new StringTagger();
        Tagger tagger = new EntityPreprocessingTagger();
        List<? extends Annotated> annotations = tagger.getAnnotations(text);
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
