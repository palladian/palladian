package ws.palladian.core.dataset.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Different compression formats, which can be used directly for CSV data import
 * and which will be uncompressed on the fly.
 * 
 * @author pk
 */
public enum Compressions implements Compression {

	NONE {
		@Override
		public InputStream getInputStream(File file) throws IOException {
			return new FileInputStream(file);
		}

		@Override
		public boolean fileExtensionSupported(File file) {
			return true;
		}
	},

	/**
	 * ZIP compression. This method assumes, that the ZIP file contains exactly
	 * one entry which is the CSV data.
	 */
	ZIP {
		@Override
		public InputStream getInputStream(File file) throws IOException {
			InputStream inputStream = new FileInputStream(file);
			ZipInputStream zipInputStream = new ZipInputStream(inputStream);
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			if (zipEntry == null) {
				throw new IOException("ZIP file contains no file");
			}
			return zipInputStream;
		}

		@Override
		public boolean fileExtensionSupported(File file) {
			return file.getName().toLowerCase().endsWith(".zip");
		}
	},

	GZIP {
		@Override
		public InputStream getInputStream(File file) throws IOException {
			InputStream inputStream = new FileInputStream(file);
			return new GZIPInputStream(inputStream);
		}

		@Override
		public boolean fileExtensionSupported(File file) {
			return file.getName().toLowerCase().endsWith(".gz");
		}
	};

	/**
	 * Get a matching {@link Compression} based on the provided file extension.
	 * 
	 * @param filePath
	 *            The input file.
	 * @return The compression.
	 */
	public static Compression get(File filePath) {
		for (Compression compression : values()) {
			if (compression != NONE && compression.fileExtensionSupported(filePath)) {
				return compression;
			}
		}
		return NONE;
	}

}
