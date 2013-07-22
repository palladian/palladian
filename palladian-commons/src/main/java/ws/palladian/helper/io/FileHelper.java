package ws.palladian.helper.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.MathHelper;

// TODO Remove all functionalities that are provided by Apache commons.
/**
 * <p>
 * The FileHelper helps with file concerning tasks. If you add methods to this class, make sure under all circumstances
 * that all streams are closed correctly. Every time you cause a memory leak, god kills a kitten! You can use the
 * provided convenience method {@link #close(Closeable...)} which closes all objects implementing the {@link Closeable}
 * interface.
 * </p>
 * 
 * <p>
 * <b>Note on encoding:</b> All methods assume UTF-8 as encoding when reading or writing text files; the System's
 * default encoding is ignored.
 * </p>
 * 
 * @see <a href="http://www.javapractices.com/topic/TopicAction.do?Id=42">Reading and writing text files</a>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 * @author Sandro Reichert
 */
public final class FileHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

    /** The encoding used by this class, instead of relying on the System's default encoding. */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /** Constant for new line character. */
    private static final String NEWLINE_CHARACTER = "\n";

    /** Constant for image file extensions. */
    public static final List<String> IMAGE_FILE_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "gif");

    /** Constant for video file extensions. */
    public static final List<String> VIDEO_FILE_EXTENSIONS = Arrays.asList("mp4", "flv", "avi", "mpeg2", "divx", "mov",
            "xvid", "wmv");

    /** Constant for audio file extensions. */
    public static final List<String> AUDIO_FILE_EXTENSIONS = Arrays.asList("mp3", "ogg", "aac", "wav", "flac");

    /** Constant for general binary file extensions, including. */
    public static final List<String> BINARY_FILE_EXTENSIONS;

    static {
        List<String> binaryFileExtensions = new ArrayList<String>();
        binaryFileExtensions.add("pdf");
        binaryFileExtensions.add("doc");
        binaryFileExtensions.add("ppt");
        binaryFileExtensions.add("xls");
        binaryFileExtensions.add("zip");
        binaryFileExtensions.add("exe");
        binaryFileExtensions.add("msi");
        binaryFileExtensions.add("swf");
        binaryFileExtensions.addAll(VIDEO_FILE_EXTENSIONS);
        binaryFileExtensions.addAll(AUDIO_FILE_EXTENSIONS);
        binaryFileExtensions.addAll(IMAGE_FILE_EXTENSIONS);
        BINARY_FILE_EXTENSIONS = Collections.unmodifiableList(binaryFileExtensions);
    }

    /** A no-operation {@link LineAction}. Used for getting the number of lines. */
    private static final LineAction NOP_LINE_ACTION = new LineAction() {
        @Override
        public void performAction(String line, int lineNumber) {
        }
    };

    private FileHelper() {
        // prevent instantiation.
    }

    /**
     * Checks if is file name.
     * 
     * @param name the name
     * @return true, if is file name
     */
    public static boolean isFileName(String name) {
        name = name.trim();

        Pattern pattern = Pattern.compile("\\.[A-Za-z0-9]{2,5}$", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(name);

        return m.find();
    }

    /**
     * Gets the file path.<br>
     * data/models/model1.ser => data/models/<br>
     * data/models/ => data/models/<br>
     * 
     * @param path The full path.
     * @return The folder part of the path without the filename.
     */
    public static String getFilePath(String path) {
        String filePath = path;
        int lastDot = path.lastIndexOf(".");
        int lastSeparator = path.lastIndexOf("/") + 1;
        if (lastSeparator == 0) {
            lastSeparator = path.lastIndexOf("\\") + 1;
        }
        if (lastDot > -1) {
            filePath = path.substring(0, lastSeparator);
        }
        return filePath;
    }

    /**
     * Gets the file name.
     * 
     * @param path The path to the file.
     * @return The file name part of the path.
     */
    public static String getFileName(String path) {
        String fileName = path;
        int lastDot = path.lastIndexOf(".");
        int lastSeparator = path.lastIndexOf("/") + 1;
        if (lastSeparator == 0) {
            lastSeparator = path.lastIndexOf("\\") + 1;
        }
        if (lastDot > -1) {
            fileName = path.substring(lastSeparator, lastDot);
        }
        return fileName;
    }

    /**
     * Gets the folder name.
     * 
     * @param path The path to the file or folder.
     * @return The folder name of the path.
     */
    public static String getFolderName(String path) {
        String fileName = path;
        int lastSeparator = path.lastIndexOf("/") + 1;
        if (lastSeparator == 0) {
            lastSeparator = path.lastIndexOf("\\") + 1;
        }
        if (lastSeparator > -1) {
            fileName = path.substring(lastSeparator);
        }
        if (lastSeparator == 0) {
            fileName = "";
        }

        return fileName;
    }

    /**
     * Append a string to a file.
     * 
     * @param filePath The file to which the string should be appended to.
     * @param appendix The string to append.
     * @return
     */
    public static String appendToFileName(String filePath, String appendix) {
        return getFilePath(filePath) + getFileName(filePath) + appendix + "." + getFileType(filePath);
    }

    /**
     * <p>
     * Gets the file type of a URI.
     * </p>
     * 
     * @param path The path of the file
     * @return The file type without the period. E.g. abc.jpg => "jpg".
     */
    public static String getFileType(String path) {
        String fileType = "";

        if (path == null || path.isEmpty()) {
            return fileType;
        }

        int lastQM = path.indexOf("?");

        // find last dot before "?"
        int lastDot = path.lastIndexOf(".");

        if (lastQM > -1) {
            lastDot = path.substring(0, lastQM).lastIndexOf(".");
        }

        if (lastDot > -1) {
            fileType = path.substring(lastDot + 1, path.length());
        }

        // throw away everything after "?"
        lastQM = fileType.indexOf("?");
        if (lastQM > -1) {
            fileType = fileType.substring(0, lastQM);
        }

        return fileType;
    }

    /**
     * Read file to string.
     * 
     * @param path The path to the file that should be read.
     * @return The string content of the file.
     */
    public static String readFileToString(String path) {
        return readFileToString(new File(path));
    }

    public static String readFileToString(String path, String encoding) {
        return readFileToString(new File(path), encoding);
    }

    public static String readFileToString(InputStream is) {
        return StringUtils.join(readFileToArray(is), "\n");
    }
    /**
     * Read file to string.
     * 
     * @param file The file that should be read.
     * @return The string content of the file.
     */
    // TODO throw exception if file cannot be accessed.
    public static String readFileToString(File file) {
        return readFileToString(file, DEFAULT_ENCODING);
    }

    public static String readFileToString(File file, String encoding) {

        StringBuilder contents = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));

            String line = null;
            while ((line = reader.readLine()) != null) {
                contents.append(line).append(NEWLINE_CHARACTER);
            }

        } catch (FileNotFoundException e) {
            LOGGER.error(file + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(file + ", " + e.getMessage());
        } finally {
            close(reader);
        }

        return contents.toString();
    }

    /**
     * Mimic the UNIX "tail" command.
     * 
     * @param path The path of the file.
     * @param numberOfLines The number of lines from the end of the file that should be returned.
     * @return A string with text lines from the specified file.
     */
    public static List<String> tail(String path, final int numberOfLines) {

        final List<String> list = new ArrayList<String>();
        final int totalNumberOfLines = getNumberOfLines(path);

        performActionOnEveryLine(path, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (totalNumberOfLines - numberOfLines < lineNumber) {
                    list.add(line);
                }
            }
        });

        return list;
    }

    /**
     * <p>
     * Create a list with each line of the given file as an element.
     * </p>
     * 
     * @param path The path of the file.
     * @return A list with the lines as elements.
     */
    public static List<String> readFileToArray(String path) {
        return readFileToArray(new File(path));
    }

    /**
     * <p>
     * Create a list with each line of the given file as an element.
     * </p>
     * 
     * @param path The path of the file.
     * @param numberOfLines The number of lines to read.
     * @return A list with the lines as elements.
     */
    public static List<String> readFileToArray(String path, int numberOfLines) {
        return readFileToArray(new File(path), 0L, numberOfLines);
    }

    /**
     * <p>
     * Create a list with each line of the given file as an element. Skip all lines to start line.
     * </p>
     * 
     * @param path The path of the file.
     * @param startLine The first line to read.
     * @param numberOfLines The number of lines to read.
     * @return A list with the lines as elements.
     */
    public static List<String> readFileToArray(String path, long startLine, int numberOfLines) {
        return readFileToArray(new File(path), startLine, numberOfLines);
    }

    /**
     * Create a list with each line of the given file as an element.
     * 
     * @param contentFile the content file
     * @return A list with the lines as elements.
     */
    public static List<String> readFileToArray(File contentFile) {
        return readFileToArray(contentFile, 0L, -1);
    }

    /**
     * Create a list with each line of the given file as an element.
     * 
     * @param contentFile the content file
     * @param numberOfLines The number of lines to read. Use -1 to read whole file.
     * @return A list with the lines as elements.
     */
    public static List<String> readFileToArray(File contentFile, long startLine, int numberOfLines) {
        List<String> list = new ArrayList<String>();
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(contentFile);

            list = readFileToArray(inputStream, startLine, numberOfLines);

        } catch (FileNotFoundException e) {
            LOGGER.error(contentFile.getPath() + ", " + e.getMessage());
        } finally {
            close(inputStream);
        }

        return list;
    }

    public static List<String> readFileToArray(InputStream inputStream) {
        return readFileToArray(inputStream, 0L, -1);
    }

    public static List<String> readFileToArray(InputStream inputStream, long startLine, int numberOfLines) {
        List<String> list = new ArrayList<String>();

        BufferedReader reader = null;

        try {

            reader = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_ENCODING));

            long lineNumber = 1;
            String line = null;
            while ((line = reader.readLine()) != null && (numberOfLines == -1 || list.size() < numberOfLines)) {
                if (lineNumber >= startLine) {
                    list.add(line);
                }
                lineNumber++;
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(reader);
        }

        return list;
    }

    /**
     * <p>
     * Remove identical lines for the given input file and save it to the output file.
     * </p>
     * 
     * @param inputFilePath The input file.
     * @param outputFilePath Where the transformed file should be saved.
     */
    public static void removeDuplicateLines(String inputFilePath, String outputFilePath) {

        if (inputFilePath.equalsIgnoreCase(outputFilePath)) {
            removeDuplicateLines(inputFilePath);
            return;
        }

        // remember all seen hashes
        final Set<Integer> seenHashes = new HashSet<Integer>();

        final Writer writer;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath), DEFAULT_ENCODING));

            LineAction la = new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    try {
                        if (seenHashes.add(line.hashCode())) {
                            writer.write(line);
                            writer.write(NEWLINE_CHARACTER);
                        }
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            };

            FileHelper.performActionOnEveryLine(inputFilePath, la);

            close(writer);

        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage());
        }

    }

    /**
     * <p>
     * Remove identical lines for the given input file and save it to the same file.
     * </p>
     * 
     * @param inputFilePath The input file which is overwritten.
     */
    public static void removeDuplicateLines(String inputFilePath) {
        List<String> lines = readFileToArray(inputFilePath, -1);

        Set<String> lineSet = new HashSet<String>();

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (lineSet.add(line) || line.length() == 0) {
                sb.append(line).append(NEWLINE_CHARACTER);
            }
        }

        writeToFile(inputFilePath, sb);
    }

    /**
     * <p>
     * Perform an action on every line of the provided input file.
     * </p>
     * 
     * @param file The File which should be processed line by line, not <code>null</code>.
     * @param lineAction The line action that should be triggered on each line, not <code>null</code>.
     * @return The number of lines processed, <code>-1</code> in case of errors.
     */
    public static int performActionOnEveryLine(File file, LineAction lineAction) {
        Validate.notNull(file, "file must not be null");
        return performActionOnEveryLine(file.getPath(), lineAction);
    }

    /**
     * <p>
     * Perform an action on every line of the provided input file.
     * </p>
     * 
     * @param filePath The path to the file which should be processed line by line, not <code>null</code>.
     * @param lineAction The line action that should be triggered on each line, not <code>null</code>.
     * @return The number of lines processed, <code>-1</code> in case of errors.
     */
    public static int performActionOnEveryLine(String filePath, LineAction lineAction) {
        Validate.notNull(filePath, "filePath must not be null");
        Validate.notNull(lineAction, "lineAction must not be null");

        int lineNumber = -1;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            lineNumber = performActionOnEveryLine(inputStream, lineAction);
        } catch (FileNotFoundException e) {
            LOGGER.error("Encountered FileNotFoundException for \"" + filePath + "\": " + e.getMessage(), e);
        } finally {
            close(inputStream);
        }
        return lineNumber;
    }

    /**
     * <p>
     * Perform an action on every line of the provided {@link InputStream}. The input stream is <b>not</b> closed after
     * it has been read; it is your responsibility to take care of that!
     * </p>
     * 
     * @param inputStream The input stream which should be processed line by line, not <code>null</code>.
     * @param lineAction The {@link LineAction} that should be triggered on each line, not <code>null</code>.
     * @return The number of lines processed, <code>-1</code> in case of errors.
     */
    public static int performActionOnEveryLine(InputStream inputStream, LineAction lineAction) {
        Validate.notNull(inputStream, "inputStream must not be null");
        Validate.notNull(lineAction, "lineAction must not be null");

        int lineNumber = 0;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line = null;
            while ((line = bufferedReader.readLine()) != null && lineAction.looping) {
                lineAction.performAction(line, lineNumber++);
            }
        } catch (IOException e) {
            LOGGER.error("Encountered IOException: " + e.getMessage(), e);
            lineNumber = -1;
        }
        return lineNumber;
    }

    /**
     * <p>
     * Writes a Collection of Objects to a file. Each Object's {{@link #toString()} invocation represents a line.
     * </p>
     * <p>
     * If the file does not exist, it is created. If the path to the file does not exist, it is created as well.
     * </p>
     * 
     * @param filePath The file path.
     * @param lines the lines
     * @return <code>false</code> if any error occurred. It is likely that line has not been written, or only parts have
     *         been written. See error log for details (Exceptions).
     */
    public static boolean writeToFile(String filePath, Iterable<?> lines) {

        boolean success = false;

        File file = new File(filePath);
        if (!file.exists() && file.getParent() != null) {
            new File(file.getParent()).mkdirs();
        }

        Writer writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), DEFAULT_ENCODING));
            for (Object line : lines) {
                writer.write(line.toString());
                writer.write(NEWLINE_CHARACTER);
            }
            success = true;

        } catch (IOException e) {
            LOGGER.error(e.getMessage() + " : " + filePath, e);
        } finally {
            close(writer);
        }

        return success;
    }

    /**
     * <p>
     * Write text to a file. If the file path ends with "gz" or "gzip" the contents will be gzipped automatically.
     * </p>
     * <p>
     * If the file does not exist, it is created. If the path to the file does not exist, it is created as well.
     * </p>
     * 
     * @param filePath The file path where the contents should be saved to.
     * @param string The string to save.
     * @param encoding The encoding in which the file should be written.
     * @return <tt>False</tt> if any IOException occurred. It is likely that {@link string} has not been written to
     *         {@link filePath}. See error log for details (Exceptions).
     */
    public static boolean writeToFile(String filePath, CharSequence string, String encoding) {

        String fileType = getFileType(filePath);
        if (fileType.equalsIgnoreCase("gz") || fileType.equalsIgnoreCase("gzip")) {
            return gzip(string, filePath);
        }

        boolean success = false;
        File file = new File(filePath);
        if (!file.exists() && file.getParent() != null) {
            new File(file.getParent()).mkdirs();
        }

        Writer writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
            writer.write(string.toString());
            success = true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage() + " : " + filePath, e);
        } finally {
            close(writer);
        }

        return success;
    }

    public static boolean writeToFile(String filePath, CharSequence string) {
        return writeToFile(filePath, string, DEFAULT_ENCODING);
    }

    public static void writeToFile(InputStream inputStream, String fileTargetLocation) {

        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(fileTargetLocation));
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(new File(fileTargetLocation));
            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage() + " : " + fileTargetLocation, e);
        } finally {
            close(out, inputStream);
        }

    }

    /**
     * <p>
     * Appends (i. e. inserts a the end) a string to the specified file. Attention: A new line is <b>not</b> added
     * automatically!
     * </p>
     * 
     * @param filePath the file path
     * @param stringToAppend the string to append
     * @return <code>true</code>, if there were no errors, <code>false</code> otherwise.
     */
    public static boolean appendFile(String filePath, CharSequence stringToAppend) {
        boolean success = false;
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, true), DEFAULT_ENCODING));
            writer.append(stringToAppend);
            success = true;
        } catch (IOException e) {
            LOGGER.error("IOException while appending to {}", filePath, e);
        } finally {
            close(writer);
        }
        return success;
    }

    /**
     * <p>
     * Appends a line to the specified text file if it does not already exist within the file.
     * </p>
     * 
     * @param filePath the file path; file will be created if it does not exist
     * @param stringToAppend the string to append
     * @return <code>true</code>, if there were no errors, <code>false</code> otherwise.
     */
    public static boolean appendLineIfNotPresent(String filePath, final CharSequence stringToAppend) {
        boolean added = false;
        final boolean[] add = new boolean[] {true};

        // if file exists already, check if it contains specified line
        if (fileExists(filePath)) {
            performActionOnEveryLine(filePath, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    if (line.equals(stringToAppend)) {
                        add[0] = false;
                        breakLineLoop();
                    }
                }
            });
        }

        if (add[0]) {
            added = appendFile(filePath, stringToAppend + NEWLINE_CHARACTER);
        }

        return added;
    }

    /**
     * <p>
     * Prepends (i. e. inserts a the beginning) a String to the specified File.
     * </p>
     * 
     * <p>
     * Inspired by <a href="http://stackoverflow.com/questions/2537944/prepend-lines-to-file-in-java">Stack Overflow
     * Thread</a>
     * </p>
     * 
     * <p>
     * <b>Note: Prepending is much slower than appending, so do not use this if you need high performance!</b>
     * </p>
     * 
     * @param filePath the file path
     * @param stringToPrepend the string to prepend
     * @throws IOException Signals that an I/O exception has occurred.
     * @author Philipp Katz
     * @return <tt>True</tt>, if there were no errors, <tt>false</tt> otherwise.
     */
    public static boolean prependFile(String filePath, String stringToPrepend) {

        boolean success = false;
        RandomAccessFile randomAccessFile = null;

        try {

            randomAccessFile = new RandomAccessFile(filePath, "rw");

            byte[] writeBuffer = stringToPrepend.getBytes(DEFAULT_ENCODING);

            // buffer size must be at least the size of String which we prepend
            int bufferSize = Math.max(4096, writeBuffer.length);

            // positions for read/write within the file at each iteration
            long readPosition = 0;
            long writePosition = 0;

            // # of bytes to write during next iteration, or -1, if done
            int writeBytes = writeBuffer.length;

            do {

                byte[] readBuffer = new byte[bufferSize];

                // read chunk, starting at current position to the readBuffer
                randomAccessFile.seek(readPosition);
                int readBytes = randomAccessFile.read(readBuffer);

                // write chunk from the writeBuffer, starting at current position
                randomAccessFile.seek(writePosition);
                randomAccessFile.write(writeBuffer, 0, writeBytes);

                // set read data to the writeBuffer for next iteration
                writeBuffer = readBuffer;

                readPosition += bufferSize;
                writePosition += writeBytes;
                writeBytes = readBytes;

            } while (writeBytes != -1);

            success = true;

        } catch (IOException e) {
            LOGGER.error("IOException while prepending to {}", filePath, e);
        } finally {
            close(randomAccessFile);
        }

        return success;
    }

    /**
     * Deserialize a serialized object. If the filepath ends with "gz" it is automatically decompressed. This generic
     * method does the cast for you, just deserialize to the appropriate type, like
     * <tt>Foo foo = FileHelper.deserialize("foo.ser");</tt>.
     * 
     * @param <T> Type of the objects.
     * @param filePath The file path of the serialized object.
     * @return The deserialized object.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(String filePath) {

        if (getFileType(filePath).equalsIgnoreCase("gz")) {
            return (T)deserializeCompressed(filePath);
        }

        ObjectInputStream in = null;
        T obj = null;

        try {
            in = new ObjectInputStream(new FileInputStream(filePath));
            obj = (T)in.readObject();
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(in);
        }

        return obj;
    }

    /**
     * Deserialize a serialized object and compressed object.
     * 
     * @param <T> type of the objects.
     * @param filePath The file path of the serialized object.
     * @return The deserialized object.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T deserializeCompressed(String filePath) {

        ObjectInputStream ois = null;
        T obj = null;

        try {
            ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filePath)));
            obj = (T)ois.readObject();
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage() + ", file path:" + filePath);
        } catch (IOException e) {
            LOGGER.error(e.getMessage() + ", file path:" + filePath);
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage() + ", file path:" + filePath);
        } finally {
            close(ois);
        }

        return obj;
    }

    /**
     * Serialize a serializable object. If the path ends with ".gz" it is automatically saved using gzip compression.
     * 
     * @param obj The obj to serialize.
     * @param filePath The file path where the object should be serialized to.
     */
    public static boolean serialize(Serializable obj, String filePath) {

        boolean success = true;

        if (getFileType(filePath).equalsIgnoreCase("gz")) {
            return serializeCompress(obj, filePath);
        }

        ObjectOutputStream out = null;
        try {

            File outputFile = new File(FileHelper.getFilePath(filePath));
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }

            out = new ObjectOutputStream(new FileOutputStream(filePath));
            out.writeObject(obj);
        } catch (IOException e) {
            LOGGER.error("could not serialize object, " + e.getMessage() + ", " + e.getCause());
            success = false;
        } catch (OutOfMemoryError e) {
            LOGGER.error("could not serialize object, " + e.getMessage() + ", exiting now!");
            success = false;
        } catch (Exception e) {
            LOGGER.error("could not serialize object, " + e.getMessage());
            success = false;
        } finally {
            close(out);
        }

        return success;
    }

    /**
     * Serialize a serializable object and use compression.
     * 
     * @param obj The obj to serialize and compress.
     * @param filePath The file path where the object should be serialized to.
     */
    private static boolean serializeCompress(Serializable obj, String filePath) {
        boolean success = true;

        ObjectOutputStream out = null;
        try {

            File outputFile = new File(FileHelper.getFilePath(filePath));
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }

            out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filePath)));
            out.writeObject(obj);
        } catch (IOException e) {
            LOGGER.error("could not serialize object to " + filePath + ", " + e.getMessage(), e);
            success = false;
        } catch (OutOfMemoryError e) {
            LOGGER.error("could not serialize object to " + filePath + ", " + e.getMessage() + ", exiting now!");
            success = false;
        } catch (Exception e) {
            LOGGER.error("could not serialize object to " + filePath + ", " + e.getMessage());
            success = false;
        } finally {
            close(out);
        }

        return success;
    }

    /**
     * Rename a file name and return it. If you really want to rename the file, use FileHelper#renameFile(File, String).
     * 
     * @param inputFile The path to an input file which should be renamed.
     * @param newName The new name.
     * @return The name of the new path.
     * @see FileHelper#renameFile(File, String)
     */
    public static String getRenamedFilename(File inputFile, String newName) {
        String fullPath = inputFile.getAbsolutePath();

        String oldName = inputFile.getName().replaceAll("\\..*", "");
        String newPath = fullPath.replaceAll(Pattern.quote(oldName) + "\\.", newName + ".");

        return newPath;
    }

    /**
     * Rename a file in the file system.
     * 
     * @param source The original file.
     * @param destination The new filename.
     * @return <code>true</code> if and only if the file has been renamed.
     */
    public static boolean renameFile(File source, String destination) {
        try {
            File destFile = new File(destination);
            return source.renameTo(destFile);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Copy a file.
     * 
     * @param sourceFile The file to copy.
     * @param destinationFile The destination of the file.
     */
    public static void copyFile(String sourceFile, String destinationFile) {
        InputStream in = null;
        OutputStream out = null;
        try {

            File outputFile = new File(FileHelper.getFilePath(destinationFile));
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }

            in = new FileInputStream(new File(sourceFile));
            out = new FileOutputStream(new File(destinationFile));

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (FileNotFoundException ex) {
            LOGGER.error(ex.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(in, out);
        }
    }

    /**
     * Copy directory.
     * 
     * @param srcPath the src path
     * @param dstPath the dst path
     */
    public static void copyDirectory(String srcPath, String dstPath) {
        copyDirectory(new File(srcPath), new File(dstPath));
    }

    /**
     * Copy directory.
     * 
     * @param srcPath the src path
     * @param dstPath the dst path
     */
    public static void copyDirectory(File srcPath, File dstPath) {

        if (srcPath.isDirectory()) {

            if (!dstPath.exists()) {
                dstPath.mkdir();
            }

            String files[] = srcPath.list();

            for (String file : files) {
                copyDirectory(new File(srcPath, file), new File(dstPath, file));
            }
        } else {

            if (!srcPath.exists()) {
                LOGGER.warn("File or directory does not exist.");
                return;
            } else {

                InputStream in = null;
                OutputStream out = null;
                try {
                    in = new FileInputStream(srcPath);
                    out = new FileOutputStream(dstPath);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];

                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    in.close();
                    out.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                } finally {
                    close(in, out);
                }
            }
        }
    }

    /**
     * Delete a file or a directory.
     * 
     * @param filename The name of the file or directory.
     * @param deleteNonEmptyDirectory If true, and filename is a directory, it will be deleted with all its contents.
     * @return <tt>True</tt> if the deletion was successful, <tt>false</tt> otherwise.
     */
    public static boolean delete(String filename, boolean deleteNonEmptyDirectory) {
        File f = new File(filename);

        if (!f.exists()) {
            LOGGER.warn("file can not be deleted because it does not exist");
            return false;
        }

        if (!f.canWrite()) {
            LOGGER.error("file can not be deleted because of lack of permission");
            return false;
        }

        // if it is a directory, make sure it is empty
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0 && !deleteNonEmptyDirectory) {
                LOGGER.error("file can not be deleted because it is a non-empty directory");
                return false;
            } else {
                for (File directoryFile : f.listFiles()) {
                    // directoryFile.delete();
                    // changed this to work recursively -- Philipp, 2010-12-22.
                    delete(directoryFile.getPath(), true);
                }
            }
        }

        // attempt to delete it
        return f.delete();
    }

    /**
     * <p>
     * Delete a file.
     * </p>
     * 
     * @param filename The filename.
     * @return <tt>True</tt> if the deletion was successful, <tt>false</tt> otherwise.
     */
    public static boolean delete(String filename) {
        return delete(filename, true);
    }

    /**
     * Delete all files inside a directory.
     * 
     * @param dirPath the directoryPath
     * @return <tt>True</tt> if there were no errors, <tt>false</tt> otherwise.
     */
    public static boolean cleanDirectory(String dirPath) {
        File file = new File(dirPath);
        boolean returnValue = false;

        if (file.isDirectory()) {
            String[] files = file.list();
            if (files.length > 0) {
                for (File directoryFile : file.listFiles()) {
                    directoryFile.delete();
                }
                returnValue = true;
            }
        }

        return returnValue;
    }

    /**
     * Move a file to a new path, preserving the filename.
     * 
     * @param file The file to move.
     * @param newPath The new path.
     * @return <tt>True</tt> if there were no errors, <tt>false</tt> otherwise.
     */
    public static boolean move(File file, String newPath) {
        File newFile = new File(newPath);
        return file.renameTo(new File(newFile, file.getName()));
    }

    /**
     * Get all files from a certain folder.
     * 
     * @param folderPath The path to the folder.
     * @return An array of files that are in that folder.
     */
    public static File[] getFiles(String folderPath) {
        return getFiles(folderPath, "");
    }

    /**
     * Get all files from a certain folder and all files in all subfolders.
     * 
     * @param folderPath The path to the folder.
     * @return An array of files that are in that folder or in any subfolder.
     */
    public static File[] getFilesRecursive(String folderPath) {
        return getFiles(folderPath, "", true);
    }

    /**
     * Gets the files.
     * 
     * @param folderPath The folder path.
     * @param substring The substring which should appear in the filename.
     * @return The files which contain the substring.
     */
    public static File[] getFiles(String folderPath, String substring) {
        return getFiles(folderPath, substring, false);
    }

    public static File[] getFiles(String folderPath, String substring, boolean recursive) {
        return getFiles(folderPath, substring, recursive, false);
    }

    public static File[] getFilesAndDirectories(String folderPath) {
        return getFilesAndDirectories(folderPath, "", false);
    }

    public static File[] getFilesAndDirectories(String folderPath, String substring, boolean recursive) {
        return getFiles(folderPath, substring, recursive, true);
    }

    public static File[] getFiles(String folderPath, String substring, boolean recursive, boolean includeDirectories) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            List<File> matchingFiles = new ArrayList<File>();

            for (File file : files) {
                if (file.isDirectory()) {
                    if (recursive) {
                        matchingFiles.addAll(Arrays.asList(getFilesRecursive(file.getPath())));
                    } else {
                        if (!includeDirectories) {
                            continue;
                        }
                    }
                }
                if (file.getName().indexOf(substring) > -1) {
                    matchingFiles.add(file);
                }
            }

            return matchingFiles.toArray(new File[0]);
        }

        return new File[0];
    }

    public static int getNumberOfLines(InputStream inputStream) {
        return performActionOnEveryLine(inputStream, NOP_LINE_ACTION);
    }

    /**
     * <p>
     * Get the number of lines in an ASCII document.
     * </p>
     * 
     * @param fileName The name of the file.
     * @return The number of lines.
     */
    public static int getNumberOfLines(String fileName) {
        return performActionOnEveryLine(fileName, NOP_LINE_ACTION);
    }

    /**
     * <p>
     * Get the number of lines in an ASCII document.
     * </p>
     * 
     * @param file The file.
     * @return The number of lines.
     */
    public static int getNumberOfLines(File file) {
        return getNumberOfLines(file.getPath());
    }

    /**
     * Zip a number of file to one file.
     * 
     * @param files The files to zip.
     * @param targetFilename The name of the target zip file.
     */
    public static void zipFiles(File[] files, String targetFilename) {

        FileOutputStream fout = null;
        ZipOutputStream zout = null;

        try {
            byte[] buffer = new byte[1024];

            fout = new FileOutputStream(targetFilename);
            zout = new ZipOutputStream(fout);

            for (File sourceFile : files) {

                LOGGER.debug("adding " + sourceFile + " to zip");

                FileInputStream fin = new FileInputStream(sourceFile);

                // add the zip entry
                zout.putNextEntry(new ZipEntry(sourceFile.getPath()));

                // now we write the file
                int length;
                while ((length = fin.read(buffer)) > 0) {
                    zout.write(buffer, 0, length);
                }

                zout.closeEntry();
                fin.close();
            }

        } catch (IOException ioe) {
            LOGGER.error("error creating the zip, " + ioe);
        } finally {
            close(fout, zout);
        }
    }

    /**
     * Zip some text and save to a file. http://www.java2s.com/Tutorial/Java/0180__File/ZipafilewithGZIPOutputStream.htm
     * 
     * @param text The text to be zipped.
     * @param filenameOutput The name of the zipped file.
     * @return <tt>True</tt> if zipping and saving was successfully, <tt>false</tt> otherwise.
     */
    public static boolean gzip(CharSequence text, String filenameOutput) {

        GZIPOutputStream zipout = null;
        try {
            zipout = new GZIPOutputStream(new FileOutputStream(filenameOutput));

            StringReader in = new StringReader(text.toString());
            int c = 0;
            while ((c = in.read()) != -1) {
                zipout.write((byte)c);
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        } finally {
            close(zipout);
        }

        return true;
    }

    /**
     * Zip a string.
     * 
     * @param text The text to zip.
     * @return The zipped string.
     */
    public static String gzipString(String text) {
        StringOutputStream out = null;
        StringInputStream in = null;
        GZIPOutputStream zipout = null;

        try {
            in = new StringInputStream(text);
            out = new StringOutputStream();
            zipout = new GZIPOutputStream(out);

            int c = 0;
            while ((c = in.read()) != -1) {
                zipout.write((byte)c);
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return out.toString();
        } finally {
            close(out, in, zipout);
        }

        return out.toString();
    }

    /**
     * Unzip file using the command line cmd.
     * 
     * @param filenameInput the filename input
     * @param consoleCommand the console command
     */
    public static void unzipFileCmd(String filenameInput, String consoleCommand) {
        Process p = null;
        InputStream in = null;
        try {
            p = Runtime.getRuntime().exec(consoleCommand + " " + filenameInput);
            in = p.getInputStream();
            while (in.read() != -1) {
                // keep waiting until all output is rendered
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(in);
            p.destroy();
        }
    }

    /**
     * Unzip a file and return the unzipped string.
     * 
     * @param filename The name of the zipped file.
     * @return The unzipped content of the file.
     */
    public static String ungzipFileToString(String filename) {
        String result = "";
        InputStream in = null;
        try {
            in = new FileInputStream(filename);
            result = ungzipInputStreamToString(in);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(in);
        }
        return result;
    }

    /**
     * Unzip a input stream to string.
     * 
     * @param in The input stream with the zipped content.
     * @return The unzipped string.
     */
    public static String ungzipInputStreamToString(InputStream in) {
        StringBuilder content = new StringBuilder();
        GZIPInputStream zipIn = null;

        try {
            zipIn = new GZIPInputStream(in);

            Reader reader = new InputStreamReader(zipIn, "UTF-8");
            BufferedReader fin = new BufferedReader(reader);

            String s;
            while ((s = fin.readLine()) != null) {
                content.append(s);
                content.append("\n");
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(zipIn);
        }
        return content.toString();
    }

    public static boolean ungzipFile(String filename) {
        return ungzipFile(filename, "");
    }

    public static boolean ungzipFile(String filename, String targetFilename) {

        boolean success = false;

        GZIPInputStream zipIn = null;
        BufferedOutputStream dest = null;
        FileOutputStream fos = null;

        try {
            zipIn = new GZIPInputStream(new FileInputStream(filename));
            int chunkSize = 8192;
            byte[] buffer = new byte[chunkSize];
            int length;

            // write the files to the disk
            if (targetFilename.isEmpty()) {
                targetFilename = appendToFileName(filename, "_unpacked");
            }
            fos = new FileOutputStream(targetFilename);
            dest = new BufferedOutputStream(fos, chunkSize);
            while ((length = zipIn.read(buffer, 0, chunkSize)) != -1) {
                dest.write(buffer, 0, length);
            }
            dest.close();

            success = true;
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(zipIn, fos, dest);
        }

        return success;
    }

    /**
     * Unzip a file.
     * 
     * @param filename the file to unzip.
     * @return <tt>True</tt> if zipping and saving was successfully, <tt>false</tt> otherwise.
     */
    public static boolean unzipFile(String filename) {

        int bufferSize = 1024;

        ZipInputStream zis = null;
        FileInputStream fis = null;
        BufferedOutputStream dest = null;
        FileOutputStream fos = null;

        try {

            fis = new FileInputStream(filename);
            zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                LOGGER.debug("extracting: " + entry);
                int count;
                byte data[] = new byte[bufferSize];

                // write the files to the disk
                fos = new FileOutputStream(getFilePath(filename) + entry.getName());
                dest = new BufferedOutputStream(fos, bufferSize);
                while ((count = zis.read(data, 0, bufferSize)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.close();
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return false;
        } finally {
            close(dest, fis, zis, fos);
        }

        return true;
    }

    /**
     * Check whether a file exists.
     * 
     * @param filePath The path to the file to check.
     * @return <tt>True</tt> if no errors occurred, <tt>false</tt> otherwise.
     */
    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            return true;
        }
        return false;
    }

    /**
     * Check if specified directory exists.
     * 
     * @param directoryPath The path to the directory.
     * @return <tt>True</tt> if no errors occurred, <tt>false</tt> otherwise.
     */
    public static boolean directoryExists(String directoryPath) {
        File file = new File(directoryPath);
        return file.exists() && file.isDirectory();
    }

    /**
     * Create a dicrectory.
     * 
     * @param directoryPath The path to the directory.
     * @return <tt>True</tt> if no errors occurred, <tt>false</tt> otherwise.
     */
    public static boolean createDirectory(String directoryPath) {
        return new File(directoryPath).mkdir();
    }

    /**
     * <p>
     * Creates the file and its directories if do not exist yet.
     * </p>
     * 
     * @param filePath The file to create.
     * @return <code>true</code> if file and directories have been created, <code>false</code> otherwise or on every
     *         error.
     */
    public static boolean createDirectoriesAndFile(String filePath) {
        boolean success = false;

        if (filePath.endsWith("/")) {
            filePath += "del.del";
        }

        File newFile = new File(filePath);
        if (!newFile.exists()) {

            // FIXME fails with NPE if no parent directory is given (filePath = just the file name).
            File directories = new File(newFile.getParent());
            boolean directoriesExists = false;

            try {
                if (directories.exists()) {
                    directoriesExists = true;
                } else {
                    directoriesExists = directories.mkdirs();
                }

                if (directoriesExists) {
                    if (!filePath.endsWith("del.del")) {
                        success = newFile.createNewFile();
                    }
                } else {
                    LOGGER.error("could not create the directories " + filePath);
                    success = false;
                }
            } catch (IOException e) {
                LOGGER.error("could not create the file " + filePath + " : " + e.getLocalizedMessage());
                success = false;
            } catch (SecurityException e) {
                LOGGER.error("could not create the file " + filePath + " : " + e.getLocalizedMessage());
                success = false;
            }
        }
        return success;
    }

    /**
     * Concatenates file2 to the end of file1.
     * 
     * @param file1 The first file.
     * @param file2 The file that is appended to the end of file1.
     * @return <tt>True</tt>, if concatenation worked, <tt>false</tt> otherwise.
     */
    public static boolean concatenateFiles(File file1, File file2) {
        boolean concatenated = false;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        int bufferSize = 4096;

        try {

            fis = new FileInputStream(file2);
            fos = new FileOutputStream(file1, true);

            int count;
            byte data[] = new byte[bufferSize];
            while ((count = fis.read(data, 0, bufferSize)) != -1) {
                fos.write(data, 0, count);
            }

            concatenated = true;

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(fis, fos);
        }

        return concatenated;
    }

    /**
     * <p>
     * Close all given closeables, check for <code>null</code>, catch potential {@link IOException}s.
     * </p>
     * 
     * @param closeables All objects which are closeable.
     */
    public static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing {}: {}", closeable, e);
                }
            }
        }
    }

    /**
     * Add a trailing slash if it does not exist.
     * 
     * @param path The path to the directory.
     * @return The path including the trailing slash.
     */
    public static String addTrailingSlash(String path) {

        if (!path.endsWith("/") && !path.isEmpty()) {
            return path + "/";
        }

        return path;
    }

    public static String createRandomExcerpt(String filePath, int lines) throws IOException {

        String indexFilename = FileHelper.appendToFileName(filePath, "_random" + lines);
        final FileWriter indexFile = new FileWriter(indexFilename);

        int numberOfLines = FileHelper.getNumberOfLines(filePath);

        final Set<Integer> randomNumbers = MathHelper.createRandomNumbers(lines, 0, numberOfLines);

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                if (randomNumbers.size() > 0 && !randomNumbers.contains(lineNumber)) {
                    return;
                }

                try {
                    indexFile.write(line + "\n");
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }

            }

        };

        FileHelper.performActionOnEveryLine(filePath, la);

        indexFile.close();

        return indexFilename;
    }

    /**
     * The main method.
     * 
     * @param a The arguments.
     */
    public static void main(String[] a) {

        String filePath = "/Users/pk/Uni/feeddataset/gathering_TUDCS5/finalQueries.txt";
        List<String> tail = tail(filePath, 50000);
        CollectionHelper.print(tail);

        // List<String> list = readFileToArray(filePath, -1);
        // CollectionHelper.print(list);
        System.exit(0);

        // FileHelper.concatenateFiles(new File("/home/pk/Desktop/FeedDiscovery/foundFeedsMerged.txt"), new
        // File("/home/pk/Desktop/FeedDiscovery/2011-04-03_foundFeeds_philipp_PRISMA.txt"));
        // FileHelper.concatenateFiles(new File("/home/pk/Desktop/FeedDiscovery/foundFeedsMerged.txt"), new
        // File("/home/pk/Desktop/FeedDiscovery/2011-04-04_foundFeeds_philipp_newsseecr.txt"));
        // FileHelper.concatenateFiles(new File("/home/pk/Desktop/FeedDiscovery/foundFeedsMerged.txt"), new
        // File("/home/pk/Desktop/FeedDiscovery/2011-04-05_foundFeeds_philipp_newsseecr.txt"));
        // FileHelper.concatenateFiles(new File("/home/pk/Desktop/FeedDiscovery/foundFeedsMerged.txt"), new
        // File("/home/pk/Desktop/FeedDiscovery/2011-04-05_foundFeeds_philipp_PRISMA.txt"));
        // FileHelper.concatenateFiles(new File("/home/pk/Desktop/FeedDiscovery/foundFeedsMerged.txt"), new
        // File("/home/pk/Desktop/FeedDiscovery/201104051050foundFeeds_Klemens.txt"));
        // FileHelper.concatenateFiles(new File("/home/pk/Desktop/FeedDiscovery/foundFeedsMerged.txt"), new
        // File("/home/pk/Desktop/FeedDiscovery/foundFeeds_Sandro.txt"));

        // System.out.println(FileHelper.getNumberOfLines("/home/pk/Desktop/FeedDiscovery/foundFeedsMerged.txt"));

        // FileHelper.removeDuplicateLines("/home/pk/Desktop/FeedDiscovery/foundFeedsMerged.txt",
        // "/home/pk/Desktop/FeedDiscovery/foundFeedsDeduplicated.txt");

        // System.out.println(FileHelper.getNumberOfLines("/home/pk/Desktop/FeedDiscovery/foundFeedsDeduplicated.txt"));

        // FileHelper.fileContentToLines("data/a.TXT", "data/a.TXT", ",");
        // FileHelper.removeDuplicateLines("data/temp/feeds.txt", "data/temp/feeds_d.txt");

        // //////////////////////// add license to every file //////////////////////////
        // FileHelper.copyDirectory("src/tud", "data/temp/src/tud");
        // StringBuilder sb = new StringBuilder();
        // sb.append("/*\n");
        // sb.append(" * Copyright 2010 TU Dresden\n");
        // sb.append(" * Licensed under the Apache License, Version 2.0 (the \"License\"); you may not\n");
        // sb.append(" * use this file except in compliance with the License. You may obtain a copy of\n");
        // sb.append(" * the License at\n");
        // sb.append(" *\n");
        // sb.append(" *     http://www.apache.org/licenses/LICENSE-2.0\n");
        // sb.append(" *\n");
        // sb.append(" * Unless required by applicable law or agreed to in writing, software\n");
        // sb.append(" * distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT\n");
        // sb.append(" * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
        // sb.append(" * See the License for the specific language governing permissions and\n");
        // sb.append(" * limitations under the License.\n");
        // sb.append(" */\n\n");
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/entity", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/page", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/page/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/qa", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/query", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/snippet", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/control", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/entity", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/fact", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/object", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/qa", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/qa/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/snippet", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/gui", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/helper", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/helper/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/knowledge", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/multimedia", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/multimedia/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/news", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/normalization", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/normalization/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/persistence", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/persistence/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/reporting", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/tagging", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web/datasetcrawler", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web/test", sb);
        // System.exit(0);
        // ////////////////////////add license to every file //////////////////////////
        writeToFile("temp/test.txt", Arrays.asList(new String[] {"one", "two", "three", "four"}));
        System.exit(0);

        FileHelper.move(new File("abc.txt"), "data");
        System.exit(0);

        FileHelper.gzip("abc -1 sdf sdjfosd fs- 12\\n-1\\abc", "test.txt.gz");

        String unzippedText = FileHelper.ungzipFileToString("test.txt.gz");
        System.out.println(unzippedText);

        String zippedString = FileHelper.gzipString("abc -1 def");
        System.out.println(zippedString);

        FileHelper.ungzipFile("test.txt.gz", "unzipped.txt");

        // System.out.println(FileHelper.unzipString(zippedString));

        System.exit(0);

        isFileName("asdsf sd fs. afjh jerk.");
        isFileName("abc.com");
        isFileName("all.html");
        isFileName("ab.ai");
        isFileName("  abasdf.mpeg2 ");

        System.out.println(getRenamedFilename(new File("data/test/sampleTextForTagging.txt"),
                "sampleTextForTagging_tagged"));

    }

    /**
     * <p>
     * Splits a given text file into evenly sized (if possible) files each named with the original name + "_splitX".
     * </p>
     * 
     * @param filePath The file to be split.
     * @param numParts The number of evenly sized parts the file should be split into.
     */
    public static void splitAsciiFile(String filePath, int numParts) {

        int totalLines = FileHelper.getNumberOfLines(filePath);

        int linesPerSplit = (int)Math.ceil((totalLines / (double)numParts));

        BufferedReader reader = null;
        BufferedWriter writer = null;
        OutputStream out = null;

        try {

            out = new FileOutputStream(appendToFileName(filePath, "_split1"));
            writer = new BufferedWriter(new OutputStreamWriter(out, DEFAULT_ENCODING));

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), DEFAULT_ENCODING));

            String line = null;
            int lineNumber = 1;
            int i = 2;
            while ((line = reader.readLine()) != null) {

                if (lineNumber % linesPerSplit == 0) {
                    if (i == numParts + 1) {
                        break;
                    }

                    out = new FileOutputStream(appendToFileName(filePath, "_split" + i));
                    writer = new BufferedWriter(new OutputStreamWriter(out, DEFAULT_ENCODING));
                    i++;
                }

                writer.write(line + "\n");

                lineNumber++;
            }

        } catch (FileNotFoundException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        } finally {
            close(reader, writer, out);
        }

    }

    /**
     * <p>
     * Shuffles the order of lines in a given file.
     * </p>
     * 
     * @param filePath The path of the file which lines should be shuffled.
     */
    public static void shuffleLines(String filePath) {
        List<String> lines = FileHelper.readFileToArray(filePath);
        Collections.shuffle(lines);
        FileHelper.writeToFile(filePath, lines);
    }

    /** The Palladian-specific temp. directory. */
    private static volatile File tempDirectory = null;

    /**
     * <p>
     * Get the Palladian-specific temporary directory. The temp directory is created in the VM's temp directory as
     * specified in <code>java.io.tmpdir</code> as subdirectory with the name <code>palladian-[timestamp]</code>. This
     * directory and all its contents are deleted upon VM termination. The temp directory should be used for storing all
     * intermediate data.
     * </p>
     * 
     * @return The {@link File} representing the temp directory.
     */
    public static File getTempDir() {
        // Thread-safe
        if (tempDirectory == null) {
            synchronized (FileHelper.class) {
                if (tempDirectory == null) {
                    File baseDirectory = new File(System.getProperty("java.io.tmpdir"));
                    String directoryName = "palladian-" + System.currentTimeMillis();
                    File newTempDirectory = new File(baseDirectory, directoryName);
                    if (!newTempDirectory.mkdir()) {
                        throw new IllegalStateException("Could not create the temporary directory " + directoryName
                                + " in " + baseDirectory.getPath());
                    }
                    tempDirectory = newTempDirectory;

                    // clean up, when VM shuts down
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            boolean success = delete(tempDirectory.getPath(), true);
                            if (!success) {
                                LOGGER.error("Error while deleting temporary directory {}", tempDirectory);
                            }
                        }
                    });

                    // LOGGER.debug("Temp directory is {}", tempDirectory);
                }
            }
        }
        return tempDirectory;
    }

}
