package ws.palladian.extraction.token;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import org.apache.commons.io.IOUtils;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.feature.Annotation;
import ws.palladian.extraction.feature.AnnotationFeature;
import ws.palladian.extraction.feature.PositionAnnotation;
import ws.palladian.model.features.FeatureVector;

/**
 * <p>
 * A {@link TokenizerInterface} implemenation based on <a
 * href="http://opennlp.apache.org/">Apache OpenNLP</a>. OpenNLP provides
 * several different tokenizers, ranging from simple, rule-based ones to
 * learnable tokenizers relying on a trained model. For more information, see
 * the documentation <a href=
 * "http://opennlp.apache.org/documentation/1.5.2-incubating/manual/opennlp.html#tools.tokenizer"
 * >section on tokenization</a> in the OpenNLP Developer Documentation.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class OpenNlpTokenizer extends AbstractPipelineProcessor<String>
		implements TokenizerInterface {

	private static final long serialVersionUID = 1L;

	/** The OpenNLP Tokenizer to use. */
	private final opennlp.tools.tokenize.Tokenizer tokenizer;

	/**
	 * <p>
	 * Create a new {@link OpenNlpTokenizer} using a {@link SimpleTokenizer},
	 * which tokenizes based on same character classes.
	 * </p>
	 */
	public OpenNlpTokenizer() {
		this(SimpleTokenizer.INSTANCE);
	}

	/**
	 * <p>
	 * Create a new {@link OpenNlpTokenizer} using an arbitrary implementation
	 * of {@link opennlp.tools.tokenize.Tokenizer}.
	 * </p>
	 * 
	 * @param tokenizer
	 */
	public OpenNlpTokenizer(opennlp.tools.tokenize.Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	/**
	 * <p>
	 * Create a new {@link OpenNlpTokenizer} based on a learned model. Such
	 * learned models are available for example on the <a
	 * href="http://opennlp.sourceforge.net/models-1.5/">OpenNLP Tools
	 * Models</a> web page.
	 * </p>
	 * 
	 * @param modelFile
	 *            Path to the model file, must not be <code>null</code>.
	 */
	public OpenNlpTokenizer(File modelFile) {
		if (modelFile == null) {
			throw new IllegalArgumentException(
					"The model file must not be null.");
		}
		InputStream modelIn = null;
		TokenizerModel model = null;
		try {
			modelIn = new FileInputStream(modelFile);
			model = new TokenizerModel(modelIn);
		} catch (IOException e) {
			throw new IllegalStateException(
					"Error initializing OpenNLP Tokenizer from \""
							+ modelFile.getAbsolutePath() + "\": "
							+ e.getMessage());
		} finally {
			IOUtils.closeQuietly(modelIn);
		}
		this.tokenizer = new TokenizerME(model);
	}

	@Override
	protected void processDocument(PipelineDocument<String> document) {
		String content = document.getOriginalContent();
		AnnotationFeature annotationFeature = new AnnotationFeature(
				TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
		Span[] spans = tokenizer.tokenizePos(content);
		for (Span span : spans) {
			Annotation annotation = new PositionAnnotation(document,
					span.getStart(), span.getEnd());
			annotationFeature.add(annotation);
		}
		FeatureVector featureVector = document.getFeatureVector();
		featureVector.add(annotationFeature);
	}

}
