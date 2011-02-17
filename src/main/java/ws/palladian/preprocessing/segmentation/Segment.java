package ws.palladian.preprocessing.segmentation;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * The class Segment contains all important values of a single segment of a document.
 * 
 * @author Silvio Rabe
 * 
 */
public class Segment {

    /** The logger of the class segment */
    private static final Logger LOGGER = Logger.getLogger(PageSegmenter.class);

    /** Reads the segmenter.config */
    private PropertiesConfiguration config = null;

    // ///////////////////// Attributes of a segment ///////////////////////

    /** The document to which the segment belongs */
    private Document document = null;

    /** The xPath to the segment */
    private String xPath = "";

    /** The startnode of the segment */
    private Node node = null;

    /** The depth of this node in the DOM tree of the document */
    private Integer depth = 0;

    /** The value of variability of the segment */
    private Double variability = 0.0;

    /** The color of this segment */
    public enum Color {
        GREEN, LIGHTGREEN, GREENYELLOW, YELLOW, REDYELLOW, LIGHTRED, RED
    };

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

    public Document getDocument() {
        return this.document;
    }

    public String getXPath() {
        return this.xPath;
    }

    public Node getNode() {
        return this.node;
    }

    public Double getVariability() {
        return this.variability;
    }

    public Integer getDepth() {
        return this.depth;
    }

    public Color getColor() {

        String configPath = "config/segmenter.conf";
        try {
            config = new PropertiesConfiguration(configPath);

            if (variability >= config.getDouble("step1"))
                color = Color.GREEN;
            if (variability > config.getDouble("step2"))
                color = Color.LIGHTGREEN;
            if (variability > config.getDouble("step3"))
                color = Color.GREENYELLOW;
            if (variability > config.getDouble("step4"))
                color = Color.YELLOW;
            if (variability > config.getDouble("step5"))
                color = Color.REDYELLOW;
            if (variability > config.getDouble("step6"))
                color = Color.LIGHTRED;
            if (variability > config.getDouble("step7"))
                color = Color.RED;

        } catch (ConfigurationException e) {
            LOGGER.warn("PageSegmenter configuration under " + configPath + " could not be loaded completely: "
                    + e.getMessage());
            if (variability >= 0)
                color = Color.GREEN;
            if (variability > 0.14)
                color = Color.LIGHTGREEN;
            if (variability > 0.28)
                color = Color.GREENYELLOW;
            if (variability > 0.42)
                color = Color.YELLOW;
            if (variability > 0.58)
                color = Color.REDYELLOW;
            if (variability > 0.72)
                color = Color.LIGHTRED;
            if (variability > 0.86)
                color = Color.RED;
        }
        return this.color;
    }

}
