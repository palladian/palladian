package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.io.FileHelper;

final class NerHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NerHelper.class);

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

    private NerHelper() {
        // no instances.
    }

}
