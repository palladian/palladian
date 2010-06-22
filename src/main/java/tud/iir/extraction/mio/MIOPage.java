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

    public MIOPage(String url, String content) {
        this.url = url;
        this.content = content;
        Crawler crawler = new Crawler(5000, 5000, 10000);
        crawler.setDocument(url);
        this.hostname = Crawler.getDomain(url, false);
        this.title = Crawler.extractTitle(crawler.getDocument());

    }

    public MIOPage(String url, String linkmatch, String content) {
        this.url = url;
        this.linkmatch = linkmatch;
        this.content = content;
        Crawler crawler = new Crawler(5000, 5000, 10000);
        crawler.setDocument(url);
        this.hostname = Crawler.getDomain(url, false);
        this.title = Crawler.extractTitle(crawler.getDocument());

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHostname() {
        return hostname;
    }

    private void setHostname(String pageURL) {
        Crawler crawler = new Crawler(5000, 5000, 10000);
        this.hostname = Crawler.getDomain(pageURL, false);
    }

    public String getLinkmatch() {
        return linkmatch;
    }

    public void setLinkmatch(String linkmatch) {
        this.linkmatch = linkmatch;
    }

    public boolean isIFrameSource() {
        return isIFrameSource;
    }

    public void setIFrameSource(boolean isIFrameSource) {
        this.isIFrameSource = isIFrameSource;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public String getLinkParentPage() {
        return linkParentPage;
    }

    public void setLinkParentPage(String linkParentPage) {
        this.linkParentPage = linkParentPage;
    }

    public boolean isLinkedPage() {
        return isLinkedPage;
    }

    public void setLinkedPage(boolean isLinkedPage) {
        this.isLinkedPage = isLinkedPage;
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    // private void getHostname(String url) {
    // Crawler crawler = new Crawler(5000, 5000, 10000);
    //
    // setHostname(crawler.getDomain(url, false));
    // }

    public double getDedicatedPageTrust() {
        return dedicatedPageTrust;
    }

    public void setDedicatedPageTrust(double dedicatedPageTrust) {
        this.dedicatedPageTrust = dedicatedPageTrust;
    }

    public String getIframeParentPage() {
        return iframeParentPage;
    }

    public void setIframeParentPage(String iframeParentPage) {
        this.iframeParentPage = iframeParentPage;
    }

    public void setIframeParentPageTitle(String iframeParentPageTitle) {
        this.iframeParentPageTitle = iframeParentPageTitle;
    }

    public String getIframeParentPageTitle() {
        return iframeParentPageTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
