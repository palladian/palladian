package ws.palladian.helper.io;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.collection.AbstractIterator2;

import java.io.*;

public final class LineIterator extends AbstractIterator2<String> implements CloseableIterator<String> {
    private final BufferedReader reader;
    private boolean closed;

    public LineIterator(File filePath) {
        Validate.notNull(filePath, "filePath must not be null");
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            reader = new BufferedReader(new InputStreamReader(inputStream, FileHelper.DEFAULT_ENCODING));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(filePath + " not found.");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unupported encoding: " + FileHelper.DEFAULT_ENCODING);
        }

    }

    @Override
    protected String getNext() {
        if (closed) {
            return finished();
        }
        try {
            String line = reader.readLine();
            if (line == null) {
                close();
                return finished();
            }
            return line;
        } catch (IOException e) {
            throw new IllegalStateException("I/O exception while trying to read from file", e);
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
        closed = true;
    }
}
