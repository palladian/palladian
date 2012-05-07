package ws.palladian.extraction.pos;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.TagAnnotation;
import ws.palladian.extraction.TagAnnotations;
import ws.palladian.extraction.feature.Annotation;
import ws.palladian.extraction.feature.AnnotationFeature;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;

/**
 * <p>
 * This is the abstract base class for all Part of Speech taggers offered by
 * Palladian. It implements two interfaces:
 * 
 * <ol>
 * <li>{@link PosTagger}, which is the "traditional" API used in Palladian. It
 * allows POS tagging for text supplied as String. In this case, the text is
 * tokenized using a default {@link TokenizerInterface} implementation specific
 * for the respective POS tagger. Subclasses may override
 * {@link #getTokenizer()} if they require a specific tokenizer.</li>
 * 
 * <li>{@link PipelineProcessor}, which works based on token annotations
 * provided by an {@link AnnotationFeature}. This means, that the input document
 * must be tokenized in advance, using one of the available
 * {@link TokenizerInterface} implementations. In this mode, the POS tags are
 * appended to the token's {@link FeatureVector}s and can be retrieved later
 * using the {@link #PROVIDED_FEATURE_DESCRIPTOR}.</li>
 * </ol>
 * </p>
 * 
 * @author Martin Wunderwald
 * @author David Urbansky
 * @author Philipp Katz
 */
public abstract class BasePosTagger extends AbstractPipelineProcessor<String>
		implements PosTagger {

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * The identifier of the feature provided by this {@link PipelineProcessor}.
	 * </p>
	 */
	public static final String PROVIDED_FEATURE = "ws.palladian.features.pos";

	/**
	 * <p>
	 * The descriptor of the feature provided by this {@link PipelineProcessor}.
	 * </p>
	 */
	public static final FeatureDescriptor<NominalFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder
			.build(PROVIDED_FEATURE, NominalFeature.class);

	/**
	 * <p>
	 * The default {@link TokenizerInterface} used if not overridden.
	 * </p>
	 */
	private static final TokenizerInterface DEFAULT_TOKENIZER = new RegExTokenizer();

	// ////////////////////////////////////////////
	// PosTagger API
	// ////////////////////////////////////////////

	@Override
	public TagAnnotations tag(String text) {
		PipelineDocument document = new PipelineDocument(text);
		try {
			getTokenizer().process(document);
			process(document);
		} catch (DocumentUnprocessableException e) {
			throw new IllegalArgumentException(e);
		}
		AnnotationFeature annotationFeature = document.getFeatureVector().get(
				TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
		TagAnnotations ret = new TagAnnotations();
		int offset = 0;
		for (Annotation annotation : annotationFeature.getValue()) {
			String tag = annotation.getFeatureVector()
					.get(PROVIDED_FEATURE_DESCRIPTOR).getValue();
			TagAnnotation tagAnnotation = new TagAnnotation(offset++, tag,
					annotation.getValue());
			ret.add(tagAnnotation);
		}
		return ret;
	}

	/**
	 * <p>
	 * Return the {@link TokenizerInterface} which this {@link PosTagger} uses
	 * when tagging String using {@link #tag(String)}. Per default, a
	 * {@link RegExTokenizer} is returned, subclasses may override this method,
	 * if a specific {@link TokenizerInterface} is required.
	 * </p>
	 * 
	 * @return The {@link TokenizerInterface} to use.
	 */
	protected TokenizerInterface getTokenizer() {
		return DEFAULT_TOKENIZER;
	}

	// ////////////////////////////////////////////
	// PipelineProcessor API
	// ////////////////////////////////////////////

	@Override
	protected void processDocument(PipelineDocument document) {
		FeatureVector featureVector = document.getFeatureVector();
		AnnotationFeature annotationFeature = featureVector
				.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
		if (annotationFeature == null) {
			throw new IllegalStateException(
					"Document content is not tokenized. Please use a tokenizer before using a POS tagger.");
		}
		tag(annotationFeature.getValue());
	}

	// ////////////////////////////////////////////
	// internal/subclass methods
	// ////////////////////////////////////////////

	/**
	 * <p>
	 * Subclasses implement this method to perform the POS tagging. The POS tags
	 * can be assigned to each annotation using the provided convenience method
	 * {@link #assignTag(Annotation, String)}.
	 * </p>
	 * 
	 * @param annotations
	 *            The list of annotations to process, this is the tokenized
	 *            text.
	 */
	protected abstract void tag(List<Annotation> annotations);

	/**
	 * <p>
	 * Helper method to convert a {@link List} of {@link Annotation}s to a
	 * {@link List} with their String values.
	 * </p>
	 * 
	 * @param annotations
	 * @return
	 */
	protected static List<String> getTokenList(List<Annotation> annotations) {
		List<String> tokenList = new ArrayList<String>(annotations.size());
		for (Annotation annotation : annotations) {
			tokenList.add(annotation.getValue());
		}
		return tokenList;
	}

	protected static String normalizeTag(String tag) {
		// return tag.replaceAll("(-|\\+).*", "");
		return tag.replaceAll("-.*", "");
	}

	/**
	 * <p>
	 * Helper method to assign a POS tag to an {@link Annotation}.
	 * </p>
	 * 
	 * @param annotation
	 * @param tag
	 */
	protected static void assignTag(Annotation annotation, String tag) {
		annotation.getFeatureVector().add(
				new NominalFeature(PROVIDED_FEATURE_DESCRIPTOR, tag
						.toUpperCase()));
	}

}
