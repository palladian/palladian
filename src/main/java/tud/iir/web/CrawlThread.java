package tud.iir.web;

/**
 * One thread for the crawler.
 * 
 * @author David Urbansky
 */
class CrawlThread extends Thread {
    Crawler crawler = null;
    String url = "";

    public CrawlThread(Crawler crawler, String url, ThreadGroup threadGroup, String threadName) {
        super(threadGroup, threadName);
        System.out.println(this);
        this.crawler = crawler;
        this.url = url;
    }

    @Override
    public void run() {
        crawler.crawl(url);
        crawler.decreaseThreadCount();
    }
}
