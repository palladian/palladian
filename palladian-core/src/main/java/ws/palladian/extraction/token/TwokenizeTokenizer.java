package ws.palladian.extraction.token;

import java.util.List;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.feature.AnnotationFeature;
import ws.palladian.extraction.feature.PositionAnnotation;
import edu.cmu.cs.lti.ark.tweetnlp.Twokenize;

/**
 * <p>
 * Tokenizer based on the <i>Twokenize</i> algorithm available from <a
 * href="https://github.com/brendano/tweetmotif">here</a>. This class uses the
 * ported Scala version delivered with <a
 * href="http://code.google.com/p/ark-tweet-nlp/">ark-tweet-nlp</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TwokenizeTokenizer extends AbstractPipelineProcessor<String>
		implements TokenizerInterface {

	private static final long serialVersionUID = 1L;

	@Override
	protected void processDocument(PipelineDocument<String> document) {
		String text = document.getOriginalContent();
		List<String> tokens = Twokenize.tokenizeForTagger_J(text);
		AnnotationFeature annotationFeature = new AnnotationFeature(
				PROVIDED_FEATURE_DESCRIPTOR);
		int endPosition = 0;
		for (String token : tokens) {
			int startPosition = text.indexOf(token, endPosition);
			endPosition = startPosition + token.length();
			annotationFeature.add(new PositionAnnotation(document,
					startPosition, endPosition));
		}
		document.getFeatureVector().add(annotationFeature);
	}

}
