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

    /** The url. */
    private String url;

    /** The hostname. */
    private String hostname;

    /** The content. */
    // private String content = "";

    /** The title of a MIOPage. */
    private String title = "";

    /** To check if this MIOPage was linked. */
    private boolean isLinkedPage = false;

    /** The name of the link. */
    private String linkName = "";

    /** The title of the link. */
    private String linkTitle = "";

    /** The parent page that linked to this MIOPage. */
    private String linkParentPage = "";

    /** To check if this MIOPage is an iframe-source. */
    private boolean isIFrameSource = false;

    /** The iframe-Page that embeds this MIOPage. */
    private String iframeParentPage = "";

    /** The title of the iframe-page that embeds this MIOpage. */
    private String iframeParentPageTitle = "";

    /** The dedicated page trust. */
    private double dedicatedPageTrust = 0;

    // /** the document that is created after retrieving a web page */
    // private Document webDocument = null;

    /**
     * Instantiates a new mIO page.
     * 
     * @param url the URL
     * @param content the content
     */
    public MIOPage(final String url) {
        // System.out.println("GENERATE NEW MIOPAGE " + url);
        this.url = url;
        // this.content = content;
        // final Crawler crawler = new Crawler();
        // crawler.setDocument(url);
        this.hostname = Crawler.getDomain(url, false);
        // this.webDocument = crawler.getWebDocument(url);
        // Document doc = crawler.getWebDocument(url);
        // System.out.println("READY!");
        // this.title = Crawler.extractTitle(doc);

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getHostname() {
        return hostname;
    }

    public boolean isIFrameSource() {
        return isIFrameSource;
    }

    public void setIFrameSource(final boolean isIFrameSource) {
        this.isIFrameSource = isIFrameSource;
    }

    public String getContent() {
        Crawler crawler = new Crawler();
        String content = crawler.downloadNotBlacklisted(url);
        return content;
    }

    // public void setContent(final String content) {
    // this.content = content;
    // }

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(final String linkName) {
        this.linkName = linkName;
    }

    public String getLinkParentPage() {
        return linkParentPage;
    }

    public void setLinkParentPage(final String linkParentPage) {
        this.linkParentPage = linkParentPage;
    }

    public boolean isLinkedPage() {
        return isLinkedPage;
    }

    public void setLinkedPage(final boolean isLinkedPage) {
        this.isLinkedPage = isLinkedPage;
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public void setLinkTitle(final String linkTitle) {
        this.linkTitle = linkTitle;
    }

    public double getDedicatedPageTrust() {
        return dedicatedPageTrust;
    }

    public void setDedicatedPageTrust(final double dedicatedPageTrust) {
        this.dedicatedPageTrust = dedicatedPageTrust;
    }

    public String getIframeParentPage() {
        return iframeParentPage;
    }

    public void setIframeParentPage(final String iframeParentPage) {
        this.iframeParentPage = iframeParentPage;
    }

    public void setIframeParentPageTitle(final String iframeParentPageTitle) {
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

    // public void setDocument(final Document document) {
    // this.webDocument = document;
    // }
    //
    // public Document getDocument() {
    // return webDocument;
    // }

}
