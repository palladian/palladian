package ws.palladian.core.dataset.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Defines a compression format which can be used as input for the CSV reader.
 * 
 * @author pk
 * @see Compressions
 */
public interface Compression {

	/**
	 * Get an input stream for the specified file.
	 * 
	 * @param file
	 *            The file.
	 * @return The input stream with the data.
	 * @throws IOException
	 *             In case anything with I/O goes berserk.
	 */
	InputStream getInputStream(File file) throws IOException;

	/**
	 * Get an output stream to write the specified file.
	 * 
	 * @param file
	 *            The file.
	 * @return The output stream for writing to the file.
	 * @throws IOException
	 *             In case of any I/O havoc.
	 */
	OutputStream getOutputStream(File file) throws IOException;

	/**
	 * Determine by file extension, whether the given file is supported by this
	 * compression strategy.
	 * 
	 * @param file
	 *            The file.
	 * @return <code>true</code> in case the implementation can handle the file.
	 */
	boolean fileExtensionSupported(File file);

}
