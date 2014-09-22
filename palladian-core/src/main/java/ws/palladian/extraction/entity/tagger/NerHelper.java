package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

public final class NerHelper {

    private NerHelper() {
        // no instances.
    }

    public static List<String> createSentenceChunks(String text, int maxChunkLength) {
        // we need to build chunks of texts because we can not send very long texts at once to open calais
        if (text.length() < maxChunkLength) {
            return Collections.singletonList(text);
        }

        List<String> chunks = new ArrayList<String>();
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

    /**
     * 
     * @param inputContent
     * @param correctContent
     * @return
     */
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
                    throw new IllegalStateException(
                            "Length error when aligning; aligned content is shorter than expected.");
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
            if (alignedCharacter == '<'
                    && (!Character.isWhitespace(correctCharacter) || nextAlignedCharacter == 47 || jumpOne)) {
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
                alignedContent = alignedContent.substring(0, alignIndex) + "\n"
                        + alignedContent.substring(alignIndex, alignedContent.length());
                // alignIndex--;
            } else if (Character.isWhitespace(alignedCharacter)) {

                alignedContent = alignedContent.substring(0, alignIndex)
                        + alignedContent.substring(alignIndex + 1, alignedContent.length());
                if (nextAlignedCharacter == '<') {
                    alignIndex--;
                    jumpOne = true;
                } else {
                    jumpOne = false;
                }

            } else {
                alignedContent = alignedContent.substring(0, alignIndex) + " "
                        + alignedContent.substring(alignIndex, alignedContent.length());
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
        Pattern pattern = Pattern.compile("(?<=\\s)" + escapedEntity + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])"
                + escapedEntity + "(?=\\s)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        List<Integer> offsets = CollectionHelper.newArrayList();
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
        List<String> contexts = CollectionHelper.newArrayList();
        if (text.length() < annotation.getStartPosition()) {
            return contexts;
        }
        StringBuilder builder = new StringBuilder();
        for (int idx = annotation.getStartPosition() - 1; idx >= 0; idx--) {
            char ch = text.charAt(idx);
            builder.append(ch);
            if (ch == ' ' || idx == 0) {
                String value = builder.toString().trim().replaceAll("\\d", "§");
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
        List<String> contexts = CollectionHelper.newArrayList();
        StringBuilder builder = new StringBuilder();
        for (int idx = annotation.getEndPosition(); idx < text.length(); idx++) {
            char ch = text.charAt(idx);
            builder.append(ch);
            if (ch == ' ' || idx == 0) {
                String value = builder.toString().trim().replaceAll("\\d", "§");
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

}
