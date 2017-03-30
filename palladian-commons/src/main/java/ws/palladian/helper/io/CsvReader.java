package ws.palladian.helper.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import ws.palladian.helper.collection.AbstractIterator2;

public class CsvReader extends AbstractIterator2<List<String>> implements Closeable {

	private final BufferedReader reader;
	private final char splitCharacter;
	private final char quoteCharacter;

	private StringBuilder buffer;
	private int lineNumber;
	private boolean closed;

	public CsvReader(InputStream stream, char splitCharacter, char quoteCharacter) {
		this(new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream))), splitCharacter, quoteCharacter);
	}

	public CsvReader(BufferedReader reader, char splitCharacter, char quoteCharacter) {
		this.reader = Objects.requireNonNull(reader);
		this.splitCharacter = splitCharacter;
		this.quoteCharacter = quoteCharacter;
		this.buffer = new StringBuilder();
		this.lineNumber = 0;
	}

	@Override
	protected List<String> getNext() {

		if (closed) {
			throw new IllegalStateException("Already closed.");
		}

		for (;;) {

			String line;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			if (line == null) {
				return finished();
			}

			lineNumber++;
			buffer.append(line);

			List<String> splitLine = DelimitedStringHelper.splitLine(buffer.toString(), splitCharacter, quoteCharacter);
			if (splitLine != null) {
				buffer = new StringBuilder();
				return splitLine;
			} else {
				buffer.append('\n');
			}
		}

	}

	// XXX empty lines should be skipped, right?

	public int getLineNumber() {
		return lineNumber;
	}

	@Override
	public void close() throws IOException {
		closed = true;
		reader.close();
	}

}
