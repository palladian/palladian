/**
 * Created on: 28.10.2012 08:51:48
 */
package ws.palladian.processing;

/**
 * <p>
 * A {@link PipelineDocument} responsible for wrapping text content. This document should be used whenever some requires
 * a {@link PipelineDocument} for {@link String}s.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public class TextDocument extends PipelineDocument<String> {

    /**
     * <p>
     * Creates a new completely initialized {@link TextDocument}, wrapping the provided content.
     * </p>
     * 
     * @param content A {@link String} representing the text this {@link TextDocument} shall wrap.
     */
    public TextDocument(String content) {
        super(content);
    }

}
