package tud.iir.extraction.mio;

/**
 * A RolePage is a Page which has a central role within a concept, e.g. www.gsmarena.com for concept mobilePhone
 * 
 * @author Martin Werner
 */
public class RolePage {

    private String hostname;
    private int count;

    public RolePage(String hostname, int count) {
        this.hostname = hostname;
        this.count = count;
    }

    public void calcCount() {
        count++;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

}
