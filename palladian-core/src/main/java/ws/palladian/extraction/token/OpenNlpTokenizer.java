package ws.palladian.extraction.token;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * A {@link BaseTokenizer} implemenation based on <a href="http://opennlp.apache.org/">Apache OpenNLP</a>. OpenNLP
 * provides several different tokenizers, ranging from simple, rule-based ones to learnable tokenizers relying on a
 * trained model. For more information, see the documentation <a
 * href="http://opennlp.apache.org/documentation/1.5.2-incubating/manual/opennlp.html#tools.tokenizer">section on
 * tokenization</a> in the OpenNLP Developer Documentation.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class OpenNlpTokenizer extends BaseTokenizer {

    /** The OpenNLP Tokenizer to use. */
    private final opennlp.tools.tokenize.Tokenizer tokenizer;

    /**
     * <p>
     * Create a new {@link OpenNlpTokenizer} using a {@link SimpleTokenizer}, which tokenizes based on same character
     * classes.
     * </p>
     */
    public OpenNlpTokenizer() {
        this(SimpleTokenizer.INSTANCE);
    }

    /**
     * <p>
     * Create a new {@link OpenNlpTokenizer} using an arbitrary implementation of
     * {@link opennlp.tools.tokenize.Tokenizer}.
     * </p>
     * 
     * @param tokenizer
     */
    public OpenNlpTokenizer(opennlp.tools.tokenize.Tokenizer tokenizer) {
        Validate.notNull(tokenizer, "tokenizer must not be null");
        this.tokenizer = tokenizer;
    }

    /**
     * <p>
     * Create a new {@link OpenNlpTokenizer} based on a learned model. Such learned models are available for example on
     * the <a href="http://opennlp.sourceforge.net/models-1.5/">OpenNLP Tools Models</a> web page.
     * </p>
     * 
     * @param modelFile Path to the model file, must not be <code>null</code>.
     */
    public OpenNlpTokenizer(File modelFile) {
        Validate.notNull(modelFile, "modelFile must not be null");
        InputStream modelIn = null;
        TokenizerModel model = null;
        try {
            modelIn = new FileInputStream(modelFile);
            model = new TokenizerModel(modelIn);
        } catch (IOException e) {
            throw new IllegalStateException("Error initializing OpenNLP Tokenizer from \""
                    + modelFile.getAbsolutePath() + "\": " + e.getMessage());
        } finally {
            FileHelper.close(modelIn);
        }
        this.tokenizer = new TokenizerME(model);
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        String content = document.getContent();
        Span[] spans = tokenizer.tokenizePos(content);
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(document);
        ListFeature<PositionAnnotation> feature = new ListFeature<PositionAnnotation>(PROVIDED_FEATURE);
        for (Span span : spans) {
            feature.add(annotationFactory.create(span.getStart(), span.getEnd()));
        }
        document.add(feature);
    }

}
