package tud.iir.knowledge;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import tud.iir.extraction.snippet.SnippetBuilder;
import tud.iir.web.AggregatedResult;

import com.aliasi.chunk.Chunk;

/**
 * The knowledge unit snippet contains the snippet text, a reference to the entity it belongs to, a reference to the aggregated result it was extracted from and
 * a feature vector containing features about the snippet which might be used for regression learning.
 * 
 * @author Christopher Friedrich
 */
public class Snippet extends Extractable {

    private static final long serialVersionUID = 4331103475156237956L;

    private Map<String, Double> features;
    private Entity entity;
    private AggregatedResult webresult;
    private String text;

    public Snippet(Entity entity, AggregatedResult webresult, String text) {
        this.features = new HashMap<String, Double>();
        this.entity = entity;
        this.webresult = webresult;
        this.text = text;
        setExtractedAt(new Date(System.currentTimeMillis()));
    }

    public double getFeature(String name) {
        return features.get(name);
    }

    public void setFeature(String name, double value) {
        features.put(name, value);
    }

    public Map<String, Double> getFeatures() {
        return features;
    }

    public Entity getEntity() {
        return entity;
    }

    public AggregatedResult getAggregatedResult() {
        return webresult;
    }

    public String getText() {
        return text;
    }

    /**
     * Whether the snippet starts with a mentioning of the related entity.
     * 
     * @return True, if it starts with an entity and False otherwise.
     */
    public boolean startsWithEntity() {
        SnippetBuilder sb = new SnippetBuilder();
        Set<Chunk> chunks = sb.getEntityChunks(entity, text, true);

        for (Chunk chunk : chunks) {
            if (chunk.start() == 0) {
                return true;
            }
        }

        // if (getText().startsWith(getEntity().getName())) {
        // return true;
        // } else if (getText().startsWith("The " + getEntity().getName())) {
        // return true;
        // } else if (getText().startsWith("A " + getEntity().getName())) {
        // return true;
        // } else if (getText().startsWith("An " + getEntity().getName())) {
        // return true;
        // }

        return false;
    }

    /**
     * Calculate the regression value using the SnippetClassifier on a trained model. XXX what's going on here?
     * 
     * @return Regression value.
     */
    public double classify() {
        // SnippetClassifier sc = new SnippetClassifier();
        // return sc.classify(this);
        return 0.0;
    }

    /**
     * @deprecated Alias for classify().
     */
    @Deprecated
    public double getRegressionRank() {
        return classify();
    }

    @Override
    public final String toString() {
        return "Snippet [entity=" + entity + ", text=" + text + "]";
    }
}