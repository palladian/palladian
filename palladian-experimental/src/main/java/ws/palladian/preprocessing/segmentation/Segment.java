package ws.palladian.preprocessing.segmentation;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.ConfigHolder;

/**
 * The class Segment contains all important values of a single segment of a document.
 * 
 * @author Silvio Rabe
 * 
 */
public class Segment {

    /** The color of this segment */
    public enum Color {
        GREEN, LIGHTGREEN, GREENYELLOW, YELLOW, REDYELLOW, LIGHTRED, RED
    }

    // ///////////////////// Attributes of a segment ///////////////////////

    /** Reads the segmenter.config */
    private final PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

    /** The document to which the segment belongs */
    private Document document = null;

    /** The xPath to the segment */
    private String xPath = "";

    /** The startnode of the segment */
    private Node node = null;

    /** The depth of this node in the DOM tree of the document */
    private Integer depth = 0;

    /** The value of variability of the segment */
    private Double variability = 0.0;;

    private Color color = null;

    /**
     * The constructor for a segment.
     * 
     * @param document
     * @param xPath
     * @param node
     * @param depth
     * @param significance
     */
    public Segment(Document document, String xPath, Node node, Integer depth, Double significance) {

        // node & depth could also be calculated here

        this.document = document;
        this.xPath = xPath;
        this.node = node;
        this.depth = depth;
        this.variability = significance;

    }

    public Color getColor() {

        if (variability >= config.getDouble("pageSegmentation.step1", 0)) {
            color = Color.GREEN;
        }
        if (variability > config.getDouble("pageSegmentation.step2", 0.14)) {
            color = Color.LIGHTGREEN;
        }
        if (variability > config.getDouble("pageSegmentation.step3", 0.28)) {
            color = Color.GREENYELLOW;
        }
        if (variability > config.getDouble("pageSegmentation.step4", 0.42)) {
            color = Color.YELLOW;
        }
        if (variability > config.getDouble("pageSegmentation.step5", 0.58)) {
            color = Color.REDYELLOW;
        }
        if (variability > config.getDouble("pageSegmentation.step6", 0.72)) {
            color = Color.LIGHTRED;
        }
        if (variability > config.getDouble("pageSegmentation.step7", 0.86)) {
            color = Color.RED;
        }

        return this.color;
    }

    public Integer getDepth() {
        return this.depth;
    }

    public Document getDocument() {
        return this.document;
    }

    public Node getNode() {
        return this.node;
    }

    public Double getVariability() {
        return this.variability;
    }

    public String getXPath() {
        return this.xPath;
    }

}
