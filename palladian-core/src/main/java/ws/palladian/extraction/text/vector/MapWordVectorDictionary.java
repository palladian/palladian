package ws.palladian.extraction.text.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/***
 * Keep the word vectors in a map.
 * @author Philipp Katz, David Urbansky
 */
public class MapWordVectorDictionary implements WordVectorDictionary {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MapWordVectorDictionary.class);

    public static MapWordVectorDictionary readFromVecFile(File vecFile) {
        return readFromVecFile(vecFile, Integer.MAX_VALUE);
    }

    public static MapWordVectorDictionary readFromVecFile(File vecFile, final int lineLimit) {
        ProgressMonitor progressMonitor = new ProgressMonitor(FileHelper.getNumberOfLines(vecFile), 0.1, "Reading vectors from " + vecFile);
        final Map<String, float[]> entries = new HashMap<>();
        final int[] vectorSize = {-1};
        final boolean[] caseSensitive = {false};
        FileHelper.performActionOnEveryLine(vecFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                progressMonitor.incrementAndPrintProgress();
                String[] values = line.split(" ");
                String token = values[0];

                // automatically detect if the dictionary is case-sensitive
                if (!token.equals(token.toLowerCase())) {
                    caseSensitive[0] = true;
                }

                if (vectorSize[0] == -1) {
                    vectorSize[0] = values.length - 1;
                }
                float[] vector = new float[values.length - 1];
                for (int i = 1; i < values.length; i++) {
                    vector[i - 1] = Float.parseFloat(values[i]);
                }
                entries.put(token, vector);
                if (lineNumber >= lineLimit) {
                    breakLineLoop();
                }
            }
        });
        LOGGER.debug("Dictionary is case sensitive? {}", caseSensitive[0]);
        return new MapWordVectorDictionary(entries, vectorSize[0], caseSensitive[0], vecFile);
    }

    private final Map<String, float[]> entries;
    private final int vectorSize;
    private final boolean caseSensitive;
    private final File vecFile;

    MapWordVectorDictionary(Map<String, float[]> entries, int vectorSize, boolean caseSensitive, File vecFile) {
        this.entries = entries;
        this.vectorSize = vectorSize;
        this.caseSensitive = caseSensitive;
        this.vecFile = vecFile;
    }

    @Override
    public float[] getVector(String word) {
        return entries.get(word);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public int vectorSize() {
        return vectorSize;
    }

    @Override
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public String toString() {
        return "MapWordVectorDictionary [" + vecFile.getName() + "]";
    }
}
