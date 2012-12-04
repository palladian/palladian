package ws.palladian.extraction.pos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;

import org.apache.commons.lang.Validate;

import ws.palladian.helper.Cache;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.PositionAnnotation;

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
                FileHelper.close(inputStream);
            }
        }
        return model;
    }

    @Override
    public void tag(List<PositionAnnotation> annotations) {
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
