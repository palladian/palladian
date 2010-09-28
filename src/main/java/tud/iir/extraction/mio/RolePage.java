/**
 * A RolePage is a Page which has a central role within a concept, e.g. www.gsmarena.com for concept mobilePhone
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

public class RolePage {

    /** The hostname of the page. */
    private String hostname;

    /** The count. */
    private int count = 1;

    /** The id. */
    private transient int id = 0;

    /** The concept id. */
    private int conceptID;

    /**
     * Instantiates a new rolePage.
     * 
     * @param hostname the hostname
     * @param conceptID the concept id
     */
    public RolePage(final String hostname, final int conceptID) {
        this.hostname = hostname;
        this.conceptID = conceptID;
    }

    /**
     * Instantiates a new rolePage (especially for loading from database).
     * 
     * @param hostname the hostname
     * @param count the count
     * @param conceptID the concept id
     */
    public RolePage(final String hostname, final int count, final int conceptID) {
        this.hostname = hostname;
        this.count = count;
        this.conceptID = conceptID;
    }

    /**
     * Calc count.
     */
    public void incrementCount() {
        count++;
    }

    /**
     * Gets the hostname.
     * 
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the hostname.
     * 
     * @param hostname the new hostname
     */
    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    /**
     * Gets the count.
     * 
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count.
     * 
     * @param count the new count
     */
    public void setCount(final int count) {
        this.count = count;
    }

    /**
     * Gets the id.
     * 
     * @return the id
     */
    public int getID() {
        return id;
    }

    /**
     * Sets the id.
     * 
     * @param id the new id
     */
    public void setID(final int id) {
        this.id = id;
    }

    /**
     * Gets the concept id.
     * 
     * @return the concept id
     */
    public int getConceptID() {
        return conceptID;
    }

    /**
     * Sets the concept id.
     * 
     * @param conceptID the new concept id
     */
    public void setConceptID(final int conceptID) {
        this.conceptID = conceptID;
    }

}
