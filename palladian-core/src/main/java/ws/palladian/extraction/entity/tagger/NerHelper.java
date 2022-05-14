package ws.palladian.extraction.entity.tagger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ws.palladian.core.Token.VALUE_CONVERTER;

public final class NerHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(NerHelper.class);

    private static final Pattern NUMBER_PATTERN = PatternHelper.compileOrGet("\\d");

    private NerHelper() {
        // no instances.
    }

    public static List<String> createSentenceChunks(String text, int maxChunkLength) {
        // we need to build chunks of texts because we can not send very long texts at once to open calais
        if (text.length() < maxChunkLength) {
            return Collections.singletonList(text);
        }

        List<String> chunks = new ArrayList<>();
        List<String> sentences = Tokenizer.getSentences(text);
        StringBuilder currentChunk = new StringBuilder(maxChunkLength);
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > maxChunkLength && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }
        chunks.add(currentChunk.toString());
        return chunks;
    }

    /**
     * The output of the named entity recognition is not well formatted and we need to align it with the input data.
     *
     * @param file The file where the prediction output is written in BIO format. This file will be overwritten.
     */
    public static void alignContent(File alignFile, String correctContent) {
        // transform to XML
        String alignFilePath = alignFile.getPath();
        FileFormatParser.columnToXml(alignFilePath, alignFilePath, "\t");
        String alignedContent = FileHelper.tryReadFileToString(alignFilePath);

        alignContentText(alignedContent, correctContent);
        FileHelper.writeToFile(alignFilePath, alignedContent);
    }

    public static String alignContentText(String inputContent, String correctContent) {
        String alignedContent = inputContent;

        // compare contents, ignore tags and align content with inputText (correctContent)
        // the index for the aligned context is different because of the tags
        int alignIndex = 0;
        boolean jumpOne = false;
        for (int i = 0; i < correctContent.length(); i++, alignIndex++) {
            char correctCharacter = correctContent.charAt(i);
            char alignedCharacter = alignedContent.charAt(alignIndex);
            char nextAlignedCharacter = 0;
            if (i < correctContent.length() - 1) {
                if (alignIndex + 1 >= alignedContent.length()) {
                    throw new IllegalStateException("Length error when aligning; aligned content is shorter than expected.");
                }
                nextAlignedCharacter = alignedContent.charAt(alignIndex + 1);
            }

            // if same, continue
            if (correctCharacter == alignedCharacter) {
                continue;
            }

            // don't distinguish between " and ' and `
            List<Character> quoteCharacters = Arrays.asList('"', '\'', '`');
            if (quoteCharacters.contains(correctCharacter) && quoteCharacters.contains(alignedCharacter)) {
                continue;
            }

            // characters are different

            // if tag "<" skip it
            if (alignedCharacter == '<' && (!Character.isWhitespace(correctCharacter) || nextAlignedCharacter == 47 || jumpOne)) {
                do {
                    alignIndex++;
                    alignedCharacter = alignedContent.charAt(alignIndex);
                } while (alignedCharacter != '>');

                if (jumpOne) {
                    alignIndex++;
                    jumpOne = false;
                }
                alignedCharacter = alignedContent.charAt(++alignIndex);

                if (alignedCharacter == '<') {
                    do {
                        alignIndex++;
                        alignedCharacter = alignedContent.charAt(alignIndex);
                    } while (alignedCharacter != '>');
                    alignedCharacter = alignedContent.charAt(++alignIndex);
                }

                nextAlignedCharacter = alignedContent.charAt(alignIndex + 1);

                // check again if the characters are the same
                if (correctCharacter == alignedCharacter) {
                    continue;
                }
            }

            if (correctCharacter == '\n') {
                alignedContent = alignedContent.substring(0, alignIndex) + "\n" + alignedContent.substring(alignIndex);
                // alignIndex--;
            } else if (Character.isWhitespace(alignedCharacter)) {

                alignedContent = alignedContent.substring(0, alignIndex) + alignedContent.substring(alignIndex + 1);
                if (nextAlignedCharacter == '<') {
                    alignIndex--;
                    jumpOne = true;
                } else {
                    jumpOne = false;
                }

            } else {
                alignedContent = alignedContent.substring(0, alignIndex) + " " + alignedContent.substring(alignIndex);
            }
        }

        return alignedContent;
    }

    public static String tag(String inputText, List<? extends Annotation> annotations, TaggingFormat taggingFormat) {
        StringBuilder taggedText = new StringBuilder();

        int lastEndIndex = 0;

        // we need to sort in ascending order first
        Collections.sort(annotations);

        Annotation lastAnnotation = null;
        for (Annotation annotation : annotations) {

            // ignore nested annotations
            if (annotation.getStartPosition() < lastEndIndex) {
                continue;
            }

            String tagName = annotation.getTag();

            String previousAppend = inputText.substring(lastEndIndex, annotation.getStartPosition());
            taggedText.append(previousAppend);

            String correctText = inputText.substring(annotation.getStartPosition(), annotation.getEndPosition());

            if (!correctText.equalsIgnoreCase(annotation.getValue()) && correctText.indexOf("\n") == -1) {
                StringBuilder errorString = new StringBuilder();
                errorString.append("alignment error, the annotation candidates don't match the text:\n");
                errorString.append("found: " + correctText + "\n");
                errorString.append("instead of: " + annotation.getValue() + "(" + annotation + ")\n");
                errorString.append("last annotation: " + lastAnnotation);
                throw new IllegalStateException(errorString.toString());
            }

            if (taggingFormat == TaggingFormat.XML) {

                taggedText.append("<").append(tagName).append(">");
                taggedText.append(annotation.getValue());
                taggedText.append("</").append(tagName).append(">");

            } else if (taggingFormat == TaggingFormat.BRACKETS) {

                taggedText.append("[").append(tagName).append(" ");
                taggedText.append(annotation.getValue());
                taggedText.append(" ]");

            } else if (taggingFormat == TaggingFormat.SLASHES) {

                List<String> tokens = Tokenizer.tokenize(annotation.getValue());
                int i = 1;
                if (!previousAppend.equals(" ") && lastAnnotation != null) {
                    taggedText.append(" ");
                }
                for (String token : tokens) {
                    taggedText.append(token).append("/").append(tagName);
                    if (i < tokens.size()) {
                        taggedText.append(" ");
                    }
                    i++;
                }

            }

            lastEndIndex = annotation.getEndPosition();
            lastAnnotation = annotation;
        }

        taggedText.append(inputText.substring(lastEndIndex));

        return taggedText.toString();
    }

    public static List<Integer> getEntityOffsets(String text, String entityName) {
        String escapedEntity = Pattern.quote(entityName);
        Pattern pattern = Pattern.compile("(?<=\\s)" + escapedEntity + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])" + escapedEntity + "(?=\\s)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        List<Integer> offsets = new ArrayList<>();
        while (matcher.find()) {
            int offset = matcher.start();
            offsets.add(offset);
        }
        return offsets;
    }

    /**
     * Get left context tokens for the given annotation. For example, for an annotation "New York", the text
     * "going to New York", and a specified length of 2, the following contexts would be extracted: "to" and "going to".
     *
     * @param annotation The annotation, not <code>null</code>.
     * @param text The text, which is referred to by the annotation, not <code>null</code>.
     * @param size The size in tokens.
     * @return A list with cumulated left context tokens from length 1 ... n.
     */
    public static List<String> getLeftContexts(Annotation annotation, String text, int size) {
        List<String> contexts = new ArrayList<>();
        if (text.length() < annotation.getStartPosition()) {
            return contexts;
        }
        StringBuilder builder = new StringBuilder();
        for (int idx = annotation.getStartPosition() - 1; idx >= 0; idx--) {
            char ch = text.charAt(idx);
            builder.append(ch);
            if (ch == ' ' || idx == 0) {
                String value = NUMBER_PATTERN.matcher(builder.toString().trim()).replaceAll("§");
                if (value.length() > 0) {
                    contexts.add(StringHelper.reverseString(value));
                }
            }
            if (contexts.size() == size) {
                break;
            }
        }
        return contexts;
    }

    /**
     * Get right context tokens for the given annotation. For example, for an annotation "New York", the text
     * "New York is a city", and a specified length of 3, the following contexts would be extracted: "is", "is a", and
     * "is a city".
     *
     * @param annotation The annotation, not <code>null</code>.
     * @param text The text, which is referred to by the annotation, not <code>null</code>.
     * @param size The size in tokens.
     * @return A list with cumulated right context tokens from length 1 ... n.
     */
    public static List<String> getRightContexts(Annotation annotation, String text, int size) {
        List<String> contexts = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (int idx = annotation.getEndPosition(); idx < text.length(); idx++) {
            char ch = text.charAt(idx);
            builder.append(ch);
            if (ch == ' ' || idx == 0) {
                String value = NUMBER_PATTERN.matcher(builder.toString().trim()).replaceAll("§");
                if (value.length() > 0) {
                    if (StringHelper.isPunctuation(value.charAt(value.length() - 1))) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (value.length() > 0) {
                        contexts.add(value);
                    }
                }
            }
            if (contexts.size() == size) {
                break;
            }
        }
        return contexts;
    }

    public static String getCharacterContext(Annotation annotation, String text, int size) {
        int offset = annotation.getStartPosition();
        String entityName = annotation.getValue();
        int length = entityName.length();
        String leftContext = text.substring(Math.max(0, offset - size), offset).trim();
        String rightContext = text.substring(offset + length, Math.min(text.length(), offset + length + size)).trim();
        return leftContext + "__" + rightContext;
    }

    /**
     * Combine annotations that are right next to each other having the same tag.
     *
     * @param annotations The annotations to combine.
     * @return The combined annotations.
     */
    public static Annotations<ClassifiedAnnotation> combineAnnotations(Annotations<ClassifiedAnnotation> annotations) {
        Annotations<ClassifiedAnnotation> combinedAnnotations = new Annotations<>();
        annotations.sort();
        ClassifiedAnnotation previous = null;
        ClassifiedAnnotation previousCombined = null;
        for (ClassifiedAnnotation current : annotations) {
            if (current.getTag().equalsIgnoreCase("o")) {
                continue;
            }
            if (previous != null && current.sameTag(previous) && current.getStartPosition() == previous.getEndPosition() + 1) {
                if (previousCombined == null) {
                    previousCombined = previous;
                }
                int startPosition = previousCombined.getStartPosition();
                String value = previousCombined.getValue() + " " + current.getValue();
                ClassifiedAnnotation combined = new ClassifiedAnnotation(startPosition, value, previous.getCategoryEntries());
                combinedAnnotations.add(combined);
                previousCombined = combined;
                combinedAnnotations.remove(previousCombined);
            } else {
                combinedAnnotations.add(current);
                previousCombined = null;
            }
            previous = current;
        }
        return combinedAnnotations;
    }

    public static void removeDateFragments(Set<Annotation> annotations) {
        Set<Annotation> toAdd = new HashSet<>();
        Set<Annotation> toRemove = new HashSet<>();
        for (Annotation annotation : annotations) {
            Annotation result = removeDateFragment(annotation);
            if (result != null) {
                toRemove.add(annotation);
                toAdd.add(result);
            }
        }
        LOGGER.debug("Removed {} partial date annotations", toRemove.size());
        annotations.addAll(toAdd);
        annotations.removeAll(toRemove);
    }


    public static void removeDates(Set<Annotation> annotations) {
        int numRemoved = CollectionHelper.remove(annotations, annotation -> !isDateFragment(annotation.getValue()));
        LOGGER.debug("Removed {} purely date annotations", numRemoved);
    }

    public static void unwrapEntities(Set<Annotation> annotations) {
        unwrapEntities(annotations, null);
    }
    public static void unwrapEntities(Set<Annotation> annotations, PalladianNerModel model) {
        Set<Annotation> toAdd = new HashSet<>();
        Set<Annotation> toRemove = new HashSet<>();
        for (Annotation annotation : annotations) {
            boolean isAllUppercase = StringHelper.isCompletelyUppercase(annotation.getValue());
            if (isAllUppercase) {
                Set<Annotation> unwrapped = unwrapAnnotations(annotation, annotations, model);
                if (unwrapped.size() > 0) {
                    toAdd.addAll(unwrapped);
                    toRemove.add(annotation);
                }
            }
        }
        annotations.removeAll(toRemove);
        annotations.addAll(toAdd);
        LOGGER.debug("Unwrapping removed {}, added {} entities", toRemove.size(), toAdd.size());
    }

    /**
     * <p>
     * If the annotation is completely upper case, like "NEW YORK CITY AND DRESDEN", try to find which of the given
     * annotation are part of this entity. The given example contains two entities that might be in the given annotation
     * set. If so, we return the found annotations.
     *
     * @param annotation The annotation to check.
     * @param annotations The annotations we are searching for in this entity.
     * @return A set of annotations found in this annotation.
     */
    private static Set<Annotation> unwrapAnnotations(Annotation annotation, Set<Annotation> annotations, PalladianNerModel model) {
        Set<String> otherValues = new HashSet<>();
        for (Annotation currentAnnotation : annotations) {
            if (!currentAnnotation.equals(annotation)) {
                otherValues.add(currentAnnotation.getValue().toLowerCase());
            }
        }
        Set<Annotation> unwrappedAnnotations = new HashSet<>();
        String annotationValue = annotation.getValue();
        List<String> parts = StringHelper.getSubPhrases(annotationValue);
        for (String part : parts) {
            String partValue = part.toLowerCase();
            if (otherValues.contains(partValue) || (model != null && model.entityDictionaryContains(partValue))) {
                int startPosition = annotation.getStartPosition() + annotationValue.toLowerCase().indexOf(partValue);
                unwrappedAnnotations.add(new ImmutableAnnotation(startPosition, part));
            }
        }
        if (LOGGER.isDebugEnabled() && unwrappedAnnotations.size() > 0) {
            List<String> unwrappedParts = CollectionHelper.convertList(unwrappedAnnotations, VALUE_CONVERTER);
            LOGGER.debug("Unwrapped {} in {} parts: {}", annotationValue, unwrappedAnnotations.size(), unwrappedParts);
        }
        return unwrappedAnnotations;
    }


    /**
     * Check whether the given text is a date fragment, e.g. "June".
     *
     * @param value The value to check.
     * @return <code>true</code> in case the text is a date fragment.
     */
    public static boolean isDateFragment(String value) {
        for (String dateFragment : RegExp.DATE_FRAGMENTS) {
            if (StringUtils.isBlank(value.replaceAll(dateFragment, " "))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Try to remove date fragments from the given annotation, e.g. "June John Hiatt" becomes "John Hiatt".
     *
     * @param annotation The annotation to process.
     * @return A new annotation with removed date fragments and fixed offsets, or <code>null</code> in case the given
     *         annotation did not contain a date fragment.
     */
    public static Annotation removeDateFragment(Annotation annotation) {
        String newValue = annotation.getValue();
        int newOffset = annotation.getStartPosition();
        for (String dateFragment : RegExp.DATE_FRAGMENTS) {
            String regExp = "(?:" + dateFragment + ")\\.?";
            String beginRegExp = "^" + regExp + " ";
            String endRegExp = " " + regExp + "$";
            Pattern beginPattern = PatternHelper.compileOrGet(beginRegExp);
            Pattern endPattern = PatternHelper.compileOrGet(endRegExp);
            int textLength = newValue.length();
            if (StringHelper.countRegexMatches(newValue, beginPattern) > 0) {
                newValue = beginPattern.matcher(newValue).replaceAll(" ").trim();
                newOffset += textLength - newValue.length();
            }
            if (StringHelper.countRegexMatches(newValue, endPattern) > 0) {
                newValue = endPattern.matcher(newValue).replaceAll(" ").trim();
            }
        }
        if (annotation.getValue().equals(newValue)) {
            return null;
        }
        LOGGER.debug("Removed date fragment from '{}' gives '{}'", annotation.getValue(), newValue);
        return new ImmutableAnnotation(newOffset, newValue, annotation.getTag());
    }
}
