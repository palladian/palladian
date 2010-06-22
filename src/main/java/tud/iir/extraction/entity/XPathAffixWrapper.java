package tud.iir.extraction.entity;

/**
 * The XPathAffixWrapper class.
 * 
 * @author David Urbansky
 */
public class XPathAffixWrapper extends AffixWrapper {

    private String xPath = "";

    public XPathAffixWrapper(String prefix, String suffix, String xPath) {
        super(prefix, suffix);
    }

    /**
     * @return the xPath
     */
    public String getXPath() {
        return xPath;
    }

    /**
     * @param path the xPath to set
     */
    public void setXPath(String path) {
        xPath = path;
    }
}