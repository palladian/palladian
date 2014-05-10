package ws.palladian.helper.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;

/**
 * Input stream which gives feedback using a {@link ProgressMonitor}. Use this, when reading large files and you want to
 * give progress updates to the user.
 * 
 * @author pk
 * 
 */
public final class ProgressReporterInputStream extends FilterInputStream {

    private final ProgressReporter reporter;

    /**
     * Create a {@link ProgressReporterInputStream} for the given {@link InputStream}.
     * 
     * @param inputStream The input stream, not <code>null</code>.
     * @param reporter The progress reporter, not <code>null</code>.
     * @see <b>Important:</b> In case, you need to handle a file which size is bigger than {@link Integer#MAX_VALUE},
     *      use the {@link #ProgressReporterInputStream(File)} constructor!
     */
    public ProgressReporterInputStream(InputStream inputStream, ProgressReporter reporter) {
        super(inputStream);
        Validate.notNull(inputStream, "inputStream must not be null");
        Validate.notNull(reporter, "reporter must not be null");
        try {
            reporter.startTask(null, inputStream.available());
        } catch (IOException e) {
            // ignore
        }
        this.reporter = reporter;
    }

    /**
     * Create a {@link ProgressReporterInputStream} reading from the specified file.
     * 
     * @param file The file, not <code>null</code>.
     * @param reporter The progress reporter, not <code>null</code>.
     * @throws FileNotFoundException In case the file could not be found.
     * @throws IllegalArgumentException In case the given file is no file.
     */
    public ProgressReporterInputStream(File file, ProgressReporter reporter) throws FileNotFoundException {
        super(new BufferedInputStream(new FileInputStream(file)));
        Validate.notNull(file, "file must not be null");
        Validate.notNull(reporter, "reporter must not be null");
        if (!file.isFile()) {
            throw new IllegalArgumentException(file + " is no a file");
        }
        reporter.startTask(file.getName(), file.length());
        this.reporter = reporter;
    }

    @Override
    public int read() throws IOException {
        int data = super.read();
        updateProgress(1);
        return data;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int length = super.read(b);
        updateProgress(length);
        return length;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int length = super.read(b, off, len);
        updateProgress(length);
        return length;
    }

    @Override
    public long skip(long n) throws IOException {
        long length = super.skip(n);
        reporter.increment(length);
        return length;
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private void updateProgress(long length) {
        if (length > 0) {
            reporter.increment(length);
        }
    }

}
