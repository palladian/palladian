package ws.palladian.extraction.text.vector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public class MapWordVectorDictionary implements WordVectorDictionary {

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MapWordVectorDictionary.class);

	public static MapWordVectorDictionary readFromVecFile(File vecFile) {
		return readFromVecFile(vecFile, Integer.MAX_VALUE);
	}

	public static MapWordVectorDictionary readFromVecFile(File vecFile, final int lineLimit) {
		final Map<String, float[]> entries = new HashMap<>();
		final int[] vectorSize = { -1 };
		final boolean[] caseSensitive = { false };
		FileHelper.performActionOnEveryLine(vecFile, new LineAction() {
			@Override
			public void performAction(String line, int lineNumber) {
//				if (lineNumber == 0) {
//					return;
//				}
				if (lineNumber % 10000 == 0 && lineNumber > 0) {
					System.out.print('.');
					LOGGER.debug("Read {} lines", lineNumber);
				}
				int firstSpace = line.indexOf(' ');
				String token = line.substring(0, firstSpace);
				
				// automatically detect if the dictionary is case-sensitive
				if (!token.equals(token.toLowerCase())) {
					caseSensitive[0] = true;
				}
				
				String vectorEntries = line.substring(firstSpace + 1);
				String[] vectorSplit = vectorEntries.split(" ");
				if (vectorSize[0] == -1) {
					vectorSize[0] = vectorSplit.length;
				}
				float[] vector = new float[vectorSplit.length];
				for (int i = 0; i < vectorSplit.length; i++) {
					vector[i] = Float.parseFloat(vectorSplit[i]);
				}
				entries.put(token, vector);
				if (lineNumber >= lineLimit) {
					breakLineLoop();
					return;
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

	private MapWordVectorDictionary(Map<String, float[]> entries, int vectorSize, boolean caseSensitive, File vecFile) {
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
