package tud.iir.extraction.content;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Segment {
	
    private static final Logger LOGGER = Logger.getLogger(PageSegmenter.class);
    
    private PropertiesConfiguration config = null;

	
	private Document document = null;

	private String xPath = "";
	
	private Node node = null;
	
	private Integer depth = 0;
	
	private Double significance = 0.0;
	
	public enum Color {GREEN, LIGHTGREEN, GREENYELLOW, YELLOW, REDYELLOW, LIGHTRED, RED};
	
	private Color color = null; 
	
	
	
	
	public Segment(Document document, String xPath, Node node, Integer depth, Double significance) {
		
		//node & depth can also be calculated here
		
		this.document = document;
		this.xPath = xPath;
		this.node = node;
		this.depth = depth;
		this.significance = significance;
		
        //loadConfig("config/segmenter.conf");

	}
	
	public String getXPath() {
		return this.xPath;
	}
	
	public Node getNode() {
		return this.node;
	}
	
	public Double getSignificance() {
		return this.significance;
	}
	
	public Integer getDepth() {
		return this.depth;
	}
	
	public Color getColor() {
		
		String configPath = "config/segmenter.conf";
        try {
			config = new PropertiesConfiguration(configPath);	
	
	    	if (significance>=config.getDouble("step1")) color=Color.RED;
	    	if (significance>config.getDouble("step2")) color=Color.LIGHTRED;
	    	if (significance>config.getDouble("step3")) color=Color.REDYELLOW;
	    	if (significance>config.getDouble("step4")) color=Color.YELLOW;
	    	if (significance>config.getDouble("step5")) color=Color.GREENYELLOW;
	    	if (significance>config.getDouble("step6")) color=Color.LIGHTGREEN;
	    	if (significance>config.getDouble("step7")) color=Color.GREEN;
        
        } catch (ConfigurationException e) {
            LOGGER.warn("PageSegmenter configuration under " + configPath + " could not be loaded completely: "
                    + e.getMessage());
            if (significance>=0) color=Color.RED;
	    	if (significance>0.14) color=Color.LIGHTRED;
	    	if (significance>0.28) color=Color.REDYELLOW;
	    	if (significance>0.42) color=Color.YELLOW;
	    	if (significance>0.58) color=Color.GREENYELLOW;
	    	if (significance>0.72) color=Color.LIGHTGREEN;
	    	if (significance>0.86) color=Color.GREEN;
        }		
		return this.color;
	}

}
