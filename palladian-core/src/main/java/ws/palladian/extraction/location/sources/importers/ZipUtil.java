package ws.palladian.extraction.location.sources.importers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Utility for performing actions with contents of a ZIP file.
 * </p>
 * 
 * @author Philipp Katz
 */
final class ZipUtil {

    /**
     * <p>
     * Function to count and return the number of lines in file, etc. given as {@link InputStream}.
     * </p>
     */
    public static final Function<InputStream, Integer> LINE_COUNTER = new Function<InputStream, Integer>() {
        @Override
        public Integer compute(InputStream inputStream) {
            return FileHelper.getNumberOfLines(inputStream);
        }
    };

    /**
     * <p>
     * Perform a specific functionality with an entry in a ZIP file.
     * </p>
     * 
     * @param file The ZIP file, not <code>null</code>.
     * @param nameFilter The filter for checking the file names in the ZIP file, not <code>null</code>.
     * @param function The functionality to perform, implemented as {@link Function}, not <code>null</code>. If the
     *            function needs no return type, use e.g. {@link Void} as output parameter.
     * @return The result of the {@link Function}, or <code>null</code>.
     * @throws IOException In case of any error.
     */
    public static <O> O doWithZipEntry(File file, Filter<String> nameFilter, Function<InputStream, O> function)
            throws IOException {
        ZipFile zipFile = null;
        InputStream inputStream = null;
        try {
            zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            ZipEntry zipEntry = null;
            while (zipEntries.hasMoreElements()) {
                zipEntry = zipEntries.nextElement();
                String zipEntryName = zipEntry.getName();
                if (nameFilter.accept(zipEntryName)) {
                    break;
                }
            }
            if (zipEntry == null) {
                throw new IOException("No suitable ZIP entry found; make sure the correct file was supplied.");
            }
            inputStream = zipFile.getInputStream(zipEntry);
            return function.compute(inputStream);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                }
            }
            FileHelper.close(inputStream);
        }
    }

    private ZipUtil() {
        // utility class
    }

    public static void main(String[] args) throws ZipException, IOException {
        File file = new File("/Users/pk/Desktop/LocationLab/geonames.org/allCountries.zip");
        Integer result = doWithZipEntry(file, Filters.equal("allCountries.txt"), LINE_COUNTER);
        System.out.println("File in ZIP file has " + result + " lines.");
    }

}
