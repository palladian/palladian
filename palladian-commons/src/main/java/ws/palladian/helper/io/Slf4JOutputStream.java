package ws.palladian.helper.io;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

/**
 * <p>
 * OutputStream adapter for SLF4J's {@link Logger}. This class is useful, in case an external library is used which does
 * logging via <code>System.out</code> or <code>System.err</code>. System.out can be redirected to SLF4J like so:
 * <code>System.setOut(new PrintStream(new Slf4JOutputStream(logger, Level.INFO), true));</code>.
 * </p>
 * 
 * @author pk
 */
public class Slf4JOutputStream extends OutputStream {

    /** Possible log-levels provided by SLF4. */
    public enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    /** Initial buffer size. */
    private static final int BUFFER_LENGTH = 1024;

    /** The system-specific line separator. */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /** Buffer. */
    private byte[] buffer;

    /** Length of the content in the buffer. */
    private int length;

    /** The logger to which to redirect. */
    private final Logger logger;

    /** The log level. */
    private final Level level;

    /**
     * <p>
     * Create a new {@link Slf4JOutputStream}.
     * </p>
     * 
     * @param logger The {@link Logger} where the output is redirected to, not <code>null</code>.
     * @param level The SLF4J level (trace, debug, info, warn, error), not <code>null</code>.
     */
    public Slf4JOutputStream(Logger logger, Level level) {
        Validate.notNull(logger, "logger must not be null");
        Validate.notNull(level, "level must not be null");
        this.level = level;
        this.logger = logger;
        reset();
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public void write(int b) throws IOException {
        if (length == buffer.length) {
            byte[] newBuffer = new byte[buffer.length + BUFFER_LENGTH];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }
        buffer[length++] = (byte)b;
    }

    @Override
    public void flush() {
        if (length == 0) {
            return;
        }
        if (length == LINE_SEPARATOR.length() && LINE_SEPARATOR.charAt(0) == buffer[0] && // no line separators
                (length == 1 || // mac, unix
                length == 2 && LINE_SEPARATOR.charAt(1) == buffer[1])) { // windows
            reset();
            return;
        }
        String message = new String(buffer, 0, length).trim();
        log(message, logger, level);
        reset();
    }

    private final void reset() {
        buffer = new byte[BUFFER_LENGTH];
        length = 0;
    }

    private static void log(String message, Logger logger, Level level) {
        switch (level) {
            case TRACE:
                logger.trace(message);
                break;
            case DEBUG:
                logger.debug(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
            default:
                throw new IllegalStateException("Unknown log level: \"" + level + "\".");
        }
    }

}
