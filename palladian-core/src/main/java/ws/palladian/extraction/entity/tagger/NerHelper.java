package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Annotated;

public final class NerHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NerHelper.class);

    private NerHelper() {
        // no instances.
    }

    public static boolean overlaps(Annotated a1, Annotated a2) {
        if (a1.getStartPosition() <= a2.getStartPosition() && a1.getEndPosition() >= a2.getStartPosition()
                || a1.getStartPosition() <= a2.getEndPosition() && a1.getEndPosition() >= a2.getStartPosition()) {
            return true;
        }
        return false;
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
        String alignedContent = FileHelper.readFileToString(alignFilePath);

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
            Character correctCharacter = correctContent.charAt(i);
            Character alignedCharacter = alignedContent.charAt(alignIndex);
            Character nextAlignedCharacter = 0;
            if (i < correctContent.length() - 1) {
                if (alignIndex + 1 >= alignedContent.length()) {
                    LOGGER.warn("Length error when aligning; aligned content is shorter than expected.");
                    break;
                }
                nextAlignedCharacter = alignedContent.charAt(alignIndex + 1);
            }

            // if same, continue
            if (correctCharacter.equals(alignedCharacter)) {
                continue;
            }

            // don't distinguish between " and '
            if ((correctCharacter.charValue() == 34 || correctCharacter.charValue() == 39)
                    && (alignedCharacter.charValue() == 34 || alignedCharacter.charValue() == 39)) {
                continue;
            }

            // characters are different

            // if tag "<" skip it
            if (alignedCharacter.charValue() == 60
                    && (!Character.isWhitespace(correctCharacter) || nextAlignedCharacter.charValue() == 47 || jumpOne)) {
                do {
                    alignIndex++;
                    alignedCharacter = alignedContent.charAt(alignIndex);
                } while (alignedCharacter.charValue() != 62);

                if (jumpOne) {
                    alignIndex++;
                    jumpOne = false;
                }
                alignedCharacter = alignedContent.charAt(++alignIndex);

                if (alignedCharacter.charValue() == 60) {
                    do {
                        alignIndex++;
                        alignedCharacter = alignedContent.charAt(alignIndex);
                    } while (alignedCharacter.charValue() != 62);
                    alignedCharacter = alignedContent.charAt(++alignIndex);
                }

                nextAlignedCharacter = alignedContent.charAt(alignIndex + 1);

                // check again if the characters are the same
                if (correctCharacter.equals(alignedCharacter)) {
                    continue;
                }
            }

            if (correctCharacter.charValue() == 10) {
                alignedContent = alignedContent.substring(0, alignIndex) + "\n"
                        + alignedContent.substring(alignIndex, alignedContent.length());
                // alignIndex--;
            } else if (Character.isWhitespace(alignedCharacter)) {

                alignedContent = alignedContent.substring(0, alignIndex)
                        + alignedContent.substring(alignIndex + 1, alignedContent.length());
                if (nextAlignedCharacter.charValue() == 60) {
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

    public static String tag(String inputText, List<? extends Annotated> annotations, TaggingFormat taggingFormat) {
        StringBuilder taggedText = new StringBuilder();

        int lastEndIndex = 0;

        // we need to sort in ascending order first
        Collections.sort(annotations);

        Annotated lastAnnotation = null;
        for (Annotated annotation : annotations) {

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

}
