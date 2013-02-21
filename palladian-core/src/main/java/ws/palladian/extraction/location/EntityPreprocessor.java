package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

// FIXME this step must be done after person name detection!
class EntityPreprocessor {

    public static final Function<Annotation, String> ANNOTATION_TO_STRING = new Function<Annotation, String>() {
        @Override
        public String compute(Annotation input) {
            return input != null ? input.getEntity() : null;
        }
    };

    public static void main(String[] args) {
        String rawText = FileHelper
                .readFileToString("/Users/pk/Desktop/LocationLab/LocationExtractionDataset/text34.txt");
        String cleanText = HtmlHelper.stripHtmlTags(rawText);

        correctAnnotations(cleanText);
    }

    public static Map<String, String> correctAnnotations(String text) {
        Annotations annotations = StringTagger.getTaggedEntities(text);

        CollectionHelper.print(annotations);
        System.out.println("------");

        List<String> paragraphs = tokenizeParagraphs(text);
        // CollectionHelper.print(paragraphs);
        List<String> sentences = CollectionHelper.newArrayList();
        for (String paragraph : paragraphs) {
            List<String> currentSentences = Tokenizer.getSentences(paragraph);
            sentences.addAll(currentSentences);
        }

        List<String> tokens = Tokenizer.tokenize(text);
        // List<String> sentences = Tokenizer.getSentences(cleanText);

        List<Annotation> sentenceBeginAnnotations = CollectionHelper.newArrayList();
        List<Annotation> inSentenceAnnotations = CollectionHelper.newArrayList();

        for (String sentence : sentences) {

            // System.out.println(sentence);
            // System.out.println("/////");

            Annotations sentenceAnnotations = StringTagger.getTaggedEntities(sentence);
            for (Annotation annotation : sentenceAnnotations) {
                if (annotation.getOffset() == 0) {
                    sentenceBeginAnnotations.add(annotation);
                } else {
                    inSentenceAnnotations.add(annotation);
                }
            }
        }

        System.out.println("Sentence begin:");
        CollectionHelper.print(sentenceBeginAnnotations);

        System.out.println("In sentence:");
        CollectionHelper.print(inSentenceAnnotations);
        
        // List<String> sentenceBeginStrings = CollectionHelper.convert(sentenceBeginAnnotations, ANNOTATION_TO_STRING, new ArrayList<String>());
        List<String> inSentenceStrings = CollectionHelper.convert(inSentenceAnnotations, ANNOTATION_TO_STRING, new ArrayList<String>());

        Set<String> lowercaseTokens = CollectionHelper.filter(tokens, new Filter<String>() {
            @Override
            public boolean accept(String item) {
                return !StringHelper.startsUppercase(item);
            }
        }, new HashSet<String>());

        // now go through all sentence begin annotations
        Set<String> toRemove = CollectionHelper.newHashSet();
        Map<String, String> toModify = CollectionHelper.newHashMap();
        for (Annotation annotation : sentenceBeginAnnotations) {
            if (inSentenceStrings.contains(annotation.getEntity())) {
                System.out.println("Everything fine with " + annotation.getEntity());
                continue;
            }
            String value = annotation.getEntity();
            String[] tokenValues = value.split("\\s");
            if (lowercaseTokens.contains(tokenValues[0].toLowerCase())) {
                if (tokenValues.length == 1) {
                    // System.out.println("**** remove " + annotation);
                    toRemove.add(annotation.getEntity());
                } else {
                    // System.out.println("**** modify " + annotation);
                    String newValue = value.substring(tokenValues[0].length() + 1);
                    for (int i = 1; i < tokenValues.length; i++) {
                        String temp = tokenValues[i];
                        // System.out.println("> " + temp);
                        if (lowercaseTokens.contains(temp.toLowerCase())) {
                            newValue = newValue.substring(Math.min(newValue.length(), temp.length() + 1));
                        } else {
                            break;
                        }
                    }
                    toModify.put(annotation.getEntity(), newValue);
                }
            }
        }

        // System.out.println("To remove:");
        // CollectionHelper.print(toRemove);

        // System.out.println("To modify:");
        // CollectionHelper.print(toModify);

        Map<String, String> ret = CollectionHelper.newHashMap();
        ret.putAll(toModify);
        for (String toRemoveEntity : toRemove) {
            ret.put(toRemoveEntity, "");
        }

        CollectionHelper.print(ret);

        return ret;
    }

    /**
     * <p>
     * Tokenize into paragraphs. Paragraphs are assumed to be separated by at least two newline characters.
     * </p>
     * 
     * @param text The text to tokenize.
     * @return A {@link List} with paragraphs.
     */
    private static List<String> tokenizeParagraphs(String text) {
        Validate.notNull(text, "text must not be null");
        return Arrays.asList(text.split("\n{2,}"));
    }

}
