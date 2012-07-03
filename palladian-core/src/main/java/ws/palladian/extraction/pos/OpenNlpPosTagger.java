package ws.palladian.extraction.pos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import ws.palladian.helper.Cache;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * <a href="http://opennlp.apache.org/">Apache OpenNLP</a> based POS tagger.
 * </p>
 * 
 * @see <a href="http://opennlp.sourceforge.net/models-1.5/">Download</a> page for models.
 * @author Martin Wunderwald
 * @author Philipp Katz
 */
public final class OpenNlpPosTagger extends BasePosTagger {

    private static final long serialVersionUID = 1L;

    /** The name of this POS tagger. */
    private static final String TAGGER_NAME = "OpenNLP POS-Tagger";

    /** The actual OpenNLP POS tagger. */
    private final POSTagger tagger;

    public OpenNlpPosTagger(File modelFile) {
        super();
        Validate.notNull(modelFile, "The model file must not be null.");
        this.tagger = loadModel(modelFile);
    }

    private final POSTagger loadModel(File modelFile) {
        String modelPath = modelFile.getAbsolutePath();
        POSTagger model = (POSTagger)Cache.getInstance().getDataObject(modelPath);
        if (model == null) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(modelFile);
                model = new POSTaggerME(new POSModel(inputStream));
                Cache.getInstance().putDataObject(modelPath, model);
            } catch (IOException e) {
                throw new IllegalStateException("Error initializing OpenNLP POS Tagger from \"" + modelPath + "\": "
                        + e.getMessage());
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return model;
    }

    @Override
    public void tag(List<Annotation> annotations) {
        List<String> tokenList = getTokenList(annotations);
        String[] tags = tagger.tag(tokenList.toArray(new String[annotations.size()]));
        for (int i = 0; i < tags.length; i++) {
            assignTag(annotations.get(i), tags[i]);
        }
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

}
