/**
 * Created on: 17.04.2012 23:52:40
 */
package ws.palladian.extraction.feature;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.PipelineDocument;

/**
 * <p>
 * Lowercases the content of the document.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class LowerCaser extends AbstractPipelineProcessor<String> {

	/**
	 * <p>
	 * Used to serialize objects of this class. Should only change of the set of
	 * attributes of this class changes.
	 * </p>
	 */
	private static final long serialVersionUID = -5655408816402154527L;

	/**
	 * {@see AbstractPipelineProcessor#AbstractPipelineProcessor()}
	 */
	public LowerCaser() {
		super();
	}

	/**
	 * {@see AbstractPipelineProcessor#AbstractPipelineProcessor(Collection)}
	 * 
	 * @param documentToInputMapping
	 *            {@see 
	 *            AbstractPipelineProcessor#AbstractPipelineProcessor(Collection
	 *            )}
	 */
	public LowerCaser(Collection<Pair<String, String>> documentToInputMapping) {
		super(documentToInputMapping);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void processDocument(PipelineDocument<String> document) {
		String text = document.getOriginalContent();
		String modifiedText = text.toLowerCase();
		document.setModifiedContent(modifiedText);
	}

}
