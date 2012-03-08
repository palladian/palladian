package ws.palladian.helper.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * <p>
 * Helper class to handle resources.
 * </p>
 * 
 * @author Philipp Katz
 */
public class ResourceHelper {

    private ResourceHelper() {

    }

    /**
     * <p>
     * Get a full path from a path relative to the class path. Inspired from Spring's ResourceUtils. Useful for locating
     * resource files in JUnit test. This method is operating system independent and correctly treats spaces in file
     * names.
     * </p>
     * 
     * @param resourceLocation
     *            Relative path to the desired resource in the class path.
     * @return Absolute, operating system specific path.
     * @throws FileNotFoundException
     *             If the file cannot be found at the specified location.
     */
    public static String getResourcePath(String resourceLocation) throws FileNotFoundException {

        resourceLocation = stripPath(resourceLocation);

        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceLocation);

        if (url == null) {
            throw new FileNotFoundException();
        }

        String resourcePath;
        try {
            resourcePath = URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        return resourcePath;

    }

    /**
     * <p>
     * Strip the leading slash, if present. We do not want it, as we load the resource using the classloader.
     * </p>
     * 
     * @param resourceLocation
     * @return
     */
    private static String stripPath(String resourceLocation) {
        if (resourceLocation.startsWith("/")) {
            resourceLocation = resourceLocation.substring(1);
        }
        return resourceLocation;
    }

    /**
     * <p>
     * Get a File with full path relative to the class path.
     * </p>
     * 
     * @param resourceLocation Relative path to the desired resource in the class path.
     * @return File with absolute, operating system specific path.
     * @throws FileNotFoundException If the file cannot be found at the specified location.
     */
    public static File getResourceFile(String resourceLocation) throws FileNotFoundException {
        String resourcePath = getResourcePath(resourceLocation);
        File file = new File(resourcePath);
        return file;
    }

    /**
     * <p>
     * Get an InputStream from a file with a path relative to the class path.
     * </p>
     * 
     * @param resourceLocation Relative path to the desired resource in the class path.
     * @return The InputStream for the specified file path.
     * @throws FileNotFoundException If the file cannot be found at the specified location.
     */
    public static InputStream getResourceStream(String resourceLocation) throws FileNotFoundException {

        resourceLocation = stripPath(resourceLocation);
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceLocation);

        if (inputStream == null) {
            throw new FileNotFoundException();
        }

        return inputStream;
    }

}
