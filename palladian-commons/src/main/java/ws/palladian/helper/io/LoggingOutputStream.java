package ws.palladian.helper.io;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;

public class LoggingOutputStream extends OutputStream {
    private final Logger logger;

    public LoggingOutputStream(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void write(byte[] b) throws IOException {
        String string = new String(b);
        if (!string.trim().isEmpty()) {
            logger.info(string);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        String string = new String(b, off, len);
        if (!string.trim().isEmpty()) {
            logger.info(string);
        }
    }

    @Override
    public void write(int b) throws IOException {
        String string = String.valueOf((char)b);
        if (!string.trim().isEmpty()) {
            logger.info(string);
        }
    }
}