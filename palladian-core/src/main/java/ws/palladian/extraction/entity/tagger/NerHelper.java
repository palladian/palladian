package ws.palladian.extraction.entity.tagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ws.palladian.extraction.token.Tokenizer;

final class NerHelper {

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

    private NerHelper() {
        // no instances.
    }

}
