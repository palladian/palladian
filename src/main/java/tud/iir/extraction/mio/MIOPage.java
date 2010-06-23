/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import tud.iir.web.Crawler;

/**
 * An webpage which contains mio(s).
 * 
 * @author Martin Werner
 */
public class MIOPage {

    private String url;
    private String hostname;
    private String linkmatch = "";
    private String content = "";
    private String title = "";

    private boolean isLinkedPage = false;
    private String linkName = "";
    private String linkTitle = "";
    private String linkParentPage = "";

    private boolean isIFrameSource = false;
    private String iframeParentPage = "";
    private String iframeParentPageTitle = "";

    private double dedicatedPageTrust = 0;

    /**
     * Instantiates a new mIO page.
     * 
     * @param url the url
     * @param content the content
     */
    public MIOPage(String url, String content) {
        this.url = url;
        this.content = content;
        Crawler crawler = new Crawler(5000, 5000, 10000);
        crawler.setDocument(url);
        this.hostname = Crawler.getDomain(url, false);
        this.title = Crawler.extractTitle(crawler.getDocument());

    }

    /**
     * Instantiates a new mIO page.
     * 
     * @param url the url
     * @param linkmatch the linkmatch
     * @param content the content
     */
    public MIOPage(String url, String linkmatch, String content) {
        this.url = url;
        this.linkmatch = linkmatch;
        this.content = content;
        Crawler crawler = new Crawler(5000, 5000, 10000);
        crawler.setDocument(url);
        this.hostname = Crawler.getDomain(url, false);
        this.title = Crawler.extractTitle(crawler.getDocument());

    }

    /**
     * Gets the url.
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     * 
     * @param url the new url
     */
    public void setUrl(String url) {
        this.url = url;
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
     * @param pageURL the new hostname
     */
    // private void setHostname(String pageURL) {
    // Crawler crawler = new Crawler(5000, 5000, 10000);
    // this.hostname = Crawler.getDomain(pageURL, false);
    // }

    /**
     * Gets the linkmatch.
     * 
     * @return the linkmatch
     */
    public String getLinkmatch() {
        return linkmatch;
    }

    /**
     * Sets the linkmatch.
     * 
     * @param linkmatch the new linkmatch
     */
    public void setLinkmatch(String linkmatch) {
        this.linkmatch = linkmatch;
    }

    /**
     * Checks if is i frame source.
     * 
     * @return true, if is i frame source
     */
    public boolean isIFrameSource() {
        return isIFrameSource;
    }

    /**
     * Sets the i frame source.
     * 
     * @param isIFrameSource the new i frame source
     */
    public void setIFrameSource(boolean isIFrameSource) {
        this.isIFrameSource = isIFrameSource;
    }

    /**
     * Gets the content.
     * 
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content.
     * 
     * @param content the new content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the link name.
     * 
     * @return the link name
     */
    public String getLinkName() {
        return linkName;
    }

    /**
     * Sets the link name.
     * 
     * @param linkName the new link name
     */
    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    /**
     * Gets the link parent page.
     * 
     * @return the link parent page
     */
    public String getLinkParentPage() {
        return linkParentPage;
    }

    /**
     * Sets the link parent page.
     * 
     * @param linkParentPage the new link parent page
     */
    public void setLinkParentPage(String linkParentPage) {
        this.linkParentPage = linkParentPage;
    }

    /**
     * Checks if is linked page.
     * 
     * @return true, if is linked page
     */
    public boolean isLinkedPage() {
        return isLinkedPage;
    }

    /**
     * Sets the linked page.
     * 
     * @param isLinkedPage the new linked page
     */
    public void setLinkedPage(boolean isLinkedPage) {
        this.isLinkedPage = isLinkedPage;
    }

    /**
     * Gets the link title.
     * 
     * @return the link title
     */
    public String getLinkTitle() {
        return linkTitle;
    }

    /**
     * Sets the link title.
     * 
     * @param linkTitle the new link title
     */
    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    // private void getHostname(String url) {
    // Crawler crawler = new Crawler(5000, 5000, 10000);
    //
    // setHostname(crawler.getDomain(url, false));
    // }

    /**
     * Gets the dedicated page trust.
     * 
     * @return the dedicated page trust
     */
    public double getDedicatedPageTrust() {
        return dedicatedPageTrust;
    }

    /**
     * Sets the dedicated page trust.
     * 
     * @param dedicatedPageTrust the new dedicated page trust
     */
    public void setDedicatedPageTrust(double dedicatedPageTrust) {
        this.dedicatedPageTrust = dedicatedPageTrust;
    }

    /**
     * Gets the iframe parent page.
     * 
     * @return the iframe parent page
     */
    public String getIframeParentPage() {
        return iframeParentPage;
    }

    /**
     * Sets the iframe parent page.
     * 
     * @param iframeParentPage the new iframe parent page
     */
    public void setIframeParentPage(String iframeParentPage) {
        this.iframeParentPage = iframeParentPage;
    }

    /**
     * Sets the iframe parent page title.
     * 
     * @param iframeParentPageTitle the new iframe parent page title
     */
    public void setIframeParentPageTitle(String iframeParentPageTitle) {
        this.iframeParentPageTitle = iframeParentPageTitle;
    }

    /**
     * Gets the iframe parent page title.
     * 
     * @return the iframe parent page title
     */
    public String getIframeParentPageTitle() {
        return iframeParentPageTitle;
    }

    /**
     * Gets the title.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     * 
     * @param title the new title
     */
    public void setTitle(String title) {
        this.title = title;
    }

}
