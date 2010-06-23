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

    private String hostname;
    private int count;

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
     * Calc count.
     */
    public void calcCount() {
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

}
