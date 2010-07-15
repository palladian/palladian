/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

/**
 * A RolePage is a Page which has a central role within a concept, e.g. www.gsmarena.com for concept mobilePhone
 * 
 * @author Martin Werner
 */
public class RolePage {

    /** The hostname of the page. */
    private String hostname;

    /** The count. */
    private int count=1;
    
    /** The id. */
    private int id=0;

 

    /**
     * Instantiates a new role page.
     * 
     * @param hostname the hostname
     * @param count the count
     */
    public RolePage(String hostname, int count) {
        this.hostname = hostname;
        this.count = count;
    }
    
    /**
     * Instantiates a new role page.
     *
     * @param hostname the hostname
     */
    public RolePage(String hostname){
        this.hostname = hostname;
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
    public void setHostname(String hostname) {
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
    public void setCount(int count) {
        this.count = count;
    }
    
    /**
     * Gets the id.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(int id) {
        this.id = id;
    }

}
