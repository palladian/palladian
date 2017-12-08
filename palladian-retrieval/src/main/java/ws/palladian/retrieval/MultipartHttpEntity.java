package ws.palladian.retrieval;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.helper.functional.Factory;

/**
 * Implements a
 * <a href="http://www.w3.org/Protocols/rfc1341/7_2_Multipart.html">Multipart
 * </a> encoded HTTP entity. These are usually used when handling forms with
 * file submission.
 * 
 * Use the {@link MultipartHttpEntity.Builder} to create instances.
 * 
 * @author pk
 *
 */
public final class MultipartHttpEntity implements HttpEntity {

	/** CRLF line feed. */
	private static final String LINE_FEED = "\r\n";

	/** Double hyphen used to signal start and end of boundary parts. */
	private static final String DOUBLE_DASH = "--";

	/* package */ static final class HttpEntityPart {

		private final HttpEntity entity;
		private final String name;
		private final String fileName;

		/* package */ HttpEntityPart(HttpEntity entity, String name, String fileName) {
			this.entity = entity;
			this.name = name;
			this.fileName = fileName;
		}

		private byte[] buildSectionHeader(String boundary) {
			StringBuilder headerBuilder = new StringBuilder();
			headerBuilder.append(LINE_FEED).append(DOUBLE_DASH).append(boundary).append(LINE_FEED);
			headerBuilder.append("Content-Disposition: form-data");
			if (name != null) {
				headerBuilder.append("; name=\"").append(name).append("\"");
			}
			if (fileName != null) {
				headerBuilder.append("; filename=\"").append(fileName).append("\"");
			}
			if (entity.getContentType() != null) {
				headerBuilder.append(LINE_FEED);
				headerBuilder.append("Content-Type: ").append(entity.getContentType());
			}
			headerBuilder.append(LINE_FEED).append(LINE_FEED);
			return headerBuilder.toString().getBytes(StandardCharsets.UTF_8);
		}

		/* package */ InputStream getInputStream(String boundary) {
			return new SequenceInputStream(new ByteArrayInputStream(buildSectionHeader(boundary)),
					entity.getInputStream());
		}

		/* package */ long length(String boundary) {
			return buildSectionHeader(boundary).length + entity.length();
		}

	}

	public static final class Builder implements Factory<MultipartHttpEntity> {

		private static final String DEFAULT_BOUNDARY = "xoxoxoxoxoxoxoxoxoxo_" + System.currentTimeMillis();

		private String boundary = DEFAULT_BOUNDARY;

		private final List<HttpEntityPart> parts = new ArrayList<>();

		/**
		 * Adds an {@link HttpEntity} as a part.
		 * 
		 * @param entity
		 *            The entity, not <code>null</code>.
		 * @param name
		 *            Name of the entity, not <code>null</code> or empty.
		 * @param fileName
		 *            (optional) name of the file.
		 * @return The builder instance.
		 */
		public Builder addPart(HttpEntity entity, String name, String fileName) {
			Validate.notNull(entity, "entity must not be null");
			Validate.notEmpty(name, "name must not be empty");
			parts.add(new HttpEntityPart(entity, name, fileName));
			return this;
		}

		/**
		 * Set the boundary string which serves as separator between parts. In
		 * case this is not set explicitly, a default value is used, which
		 * should be fine usually.
		 * 
		 * @param boundary
		 *            The boundary string, not <code>null</code> or empty.
		 * @return The builder instance.
		 */
		public Builder setBoundary(String boundary) {
			Validate.notEmpty(boundary, "boundary must not be empty");
			this.boundary = boundary;
			return this;
		}

		@Override
		public MultipartHttpEntity create() {
			return new MultipartHttpEntity(this);
		}

	}

	private final String boundary;
	private final List<HttpEntityPart> parts;

	private MultipartHttpEntity(Builder builder) {
		this.boundary = builder.boundary;
		this.parts = new ArrayList<>(builder.parts);
	}

	@Override
	public long length() {
		long length = 0;
		for (HttpEntityPart part : parts) {
			length += part.length(boundary);
		}
		length += getEpilogue().length;
		return length;
	}

	@Override
	public InputStream getInputStream() {
		InputStream result = new ByteArrayInputStream(new byte[0]);
		for (HttpEntityPart part : parts) {
			result = new SequenceInputStream(result, part.getInputStream(boundary));
		}
		return new SequenceInputStream(result, new ByteArrayInputStream(getEpilogue()));
	}

	private byte[] getEpilogue() {
		return (LINE_FEED + DOUBLE_DASH + boundary + DOUBLE_DASH + LINE_FEED).getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public String getContentType() {
		return "multipart/form-data; boundary=" + boundary;
	}

}
