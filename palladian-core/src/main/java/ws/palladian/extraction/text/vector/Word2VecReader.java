package ws.palladian.extraction.text.vector;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import java.util.function.Predicate;

public class Word2VecReader {

	private final static long ONE_GB = 1024 * 1024 * 1024;

	private static final Predicate<String> FILTER = new Predicate<String>() {
		@Override
		public boolean test(String word) {
			return !word.contains("_");
		}
	};
	// https://github.com/medallia/Word2VecJava/blob/master/src/main/java/com/medallia/word2vec/Word2VecModel.java
	// https://github.com/medallia/Word2VecJava/issues/44
	// https://github.com/jkinkead/Word2VecJava/blob/841b0cf8c063fb32ed7ee117871ccbb68075b05b/src/main/java/com/medallia/word2vec/Word2VecModel.java

	public static MapWordVectorDictionary fromBinFile(File binFile) throws IOException {

		try (FileInputStream fis = new FileInputStream(binFile)) {
			FileChannel channel = fis.getChannel();
			MappedByteBuffer buffer = channel.map(READ_ONLY, 0, Math.min(channel.size(), MAX_VALUE));
			buffer.order(ByteOrder.BIG_ENDIAN);
			int bufferCount = 1;
			// Java's NIO only allows memory-mapping up to 2GB. To work around this problem,
			// we re-map every gigabyte. To calculate offsets correctly, we have to keep
			// track how many gigabytes we've already skipped. That's what this is for.

			StringBuilder sb = new StringBuilder();
			char c = (char) buffer.get();
			while (c != '\n') {
				sb.append(c);
				c = (char) buffer.get();
			}
			String firstLine = sb.toString();
			int index = firstLine.indexOf(' ');
			if (index == 1) {
				throw new IllegalStateException(String.format("Expected a space in the first line of file '%s': '%s'",
						binFile.getAbsolutePath(), firstLine));
			}

			int vocabSize = Integer.parseInt(firstLine.substring(0, index));
			int vectorSize = Integer.parseInt(firstLine.substring(index + 1));

			Map<String, float[]> entries = new HashMap<>();
			boolean caseSensitive = false;

			for (int vectorIdx = 0; vectorIdx < vocabSize; vectorIdx++) {

				// read vocab
				sb.setLength(0);
				c = (char) buffer.get();
				while (c != ' ') {
					// ignore newlines in front of words (some binary files have newline,
					// some don't)
					if (c != '\n') {
						sb.append(c);
					}
					c = (char) buffer.get();
				}

				if (vectorIdx % 100000 == 0 && vectorIdx > 0) {
					System.out.println((int) ((float) vectorIdx / vocabSize * 100) + "%");
				}

				// read vector
				float[] floats = new float[vectorSize];
				buffer.asFloatBuffer().get(floats);
				String word = sb.toString();
				if (FILTER.test(word)) {
					entries.put(word, floats);
				}

				// automatically detect if the dictionary is case-sensitive
				if (!word.equals(word.toLowerCase())) {
					caseSensitive = true;
				}

				buffer.position(buffer.position() + 4 * vectorSize);

				// remap file
				if (buffer.position() > ONE_GB) {
					int newPosition = (int) (buffer.position() - ONE_GB);
					long size = Math.min(channel.size() - ONE_GB * bufferCount, Integer.MAX_VALUE);
					buffer = channel.map(FileChannel.MapMode.READ_ONLY, ONE_GB * bufferCount, size);
					buffer.order(ByteOrder.BIG_ENDIAN);
					buffer.position(newPosition);
					bufferCount += 1;
				}
			}
			return new MapWordVectorDictionary(entries, vectorSize, caseSensitive, binFile);
		}

	}

	public static void main(String[] args) throws IOException {
		MapWordVectorDictionary dict = fromBinFile(new File("/Users/pk/Downloads/GoogleNews-vectors-negative300.bin"));
		System.out.println(dict.size());
	}

}
