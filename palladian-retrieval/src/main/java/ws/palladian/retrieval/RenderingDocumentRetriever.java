package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.DriverManagerType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.search.DocumentRetrievalTrial;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static io.github.bonigarcia.wdm.DriverManagerType.CHROME;

/**
 * A selenium-based retriever for web documents that should be rendered (execute JS and CSS).
 *
 * Note: This is NOT thread safe, use a RenderingDocumentRetrieverPool for parallel applications.
 *
 * @author David Urbansky, Jaroslav Vankat
 */
public class RenderingDocumentRetriever extends WebDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderingDocumentRetriever.class);

    protected RemoteWebDriver driver;
    private int timeoutSeconds = 10;

    /** We can configure the retriever to wait for certain elements on certain URLs that match the given pattern. */
    private Map<Pattern, String> waitForElementMap = new HashMap<>();

    /**
     * Default constructor, doesn't force reloading pages when <code>goTo</code> with the current url is called.
     */
    public RenderingDocumentRetriever() {
        this(CHROME, null, HttpRetriever.USER_AGENT, null);
    }

    public RenderingDocumentRetriever(DriverManagerType browser) {
        this(browser, null, HttpRetriever.USER_AGENT, null);
    }

    public RenderingDocumentRetriever(DriverManagerType browser, org.openqa.selenium.Proxy proxy, String userAgent, String driverVersionCode) {
        if (browser == DriverManagerType.FIREFOX) {
            if (driverVersionCode != null) {
                WebDriverManager.firefoxdriver().version(driverVersionCode).setup();
            } else {
                WebDriverManager.firefoxdriver().setup();
            }
            FirefoxOptions firefoxOptions = new FirefoxOptions();
            firefoxOptions.setHeadless(true);
            firefoxOptions.setAcceptInsecureCerts(true);
            firefoxOptions.addPreference("general.useragent.override", userAgent);

            if (proxy != null) {
                firefoxOptions.setCapability(CapabilityType.PROXY, proxy);
            }

            driver = new FirefoxDriver(firefoxOptions);
        } else if (browser == DriverManagerType.CHROME) {
            if (driverVersionCode != null) {
                WebDriverManager.chromedriver().version(driverVersionCode).setup();
            } else {
                WebDriverManager.chromedriver().setup();
            }
            ChromeOptions options = new ChromeOptions();
            options.setHeadless(true);
            options.setAcceptInsecureCerts(true);
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-extensions");
            options.addArguments("--start-maximized");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=" + userAgent);

            if (proxy != null) {
                options.setCapability(CapabilityType.PROXY, proxy);
            } else {
                options.addArguments("--proxy-server='direct://'");
                options.addArguments("--proxy-bypass-list=*");
            }

            driver = new ChromeDriver(options);
        }
    }

    public RenderingDocumentRetriever(DriverManagerType browser, MutableCapabilities options) {
        if (browser == DriverManagerType.FIREFOX) {
            WebDriverManager.firefoxdriver().setup();
            driver = new FirefoxDriver(options);
        } else if (browser == DriverManagerType.CHROME) {
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
    }

    /**
     * Take a screenshot and save it to the specified path.
     *
     * @param targetPath The path where the screenshot should be saved to.
     * @return The screenshot file.
     */
    public File takeScreenshot(String targetPath) {
        File scrFile = driver.getScreenshotAs(OutputType.FILE);

        File screenshotFile = null;
        try {
            BufferedImage img = ImageIO.read(scrFile);
            String imageName = String.valueOf(System.currentTimeMillis());
            String imagePath = "screenshot-" + imageName + ".png";
            if (!targetPath.isEmpty()) {
                imagePath = targetPath + File.separator + imagePath;
            }
            screenshotFile = new File(imagePath);
            ImageIO.write(img, "png", screenshotFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return screenshotFile;
    }

    /**
     * Go to a certain page and wait for the document to be loaded.
     *
     * @param url The url of the document
     */
    public void goTo(String url) {
        goTo(url, false);
    }

    public void goTo(String url, boolean forceReload) {
        if (forceReload || !driver.getCurrentUrl().equals(url)) {
            driver.get(url);
        }

        // check whether a pattern matches and we have elements to wait for
        String selector = null;
        for (Map.Entry<Pattern, String> patternStringEntry : waitForElementMap.entrySet()) {
            if (patternStringEntry.getKey().matcher(url).find()) {
                selector = patternStringEntry.getValue();
                break;
            }
        }

        if (selector != null) {
            final String cssSelector = selector;
            new WebDriverWait(driver, getTimeoutSeconds()).until(webDriver -> webDriver.findElement(By.cssSelector(cssSelector)));
        } else {
            new WebDriverWait(driver, getTimeoutSeconds()).until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        }
    }

    /**
     * Go to a certain page and wait until a condition is fulfilled (up to x seconds).
     *
     * @param url       The url of the document
     * @param condition The condition to check
     */
    public void goTo(String url, ExpectedCondition<Boolean> condition) {
        goTo(url, condition, getTimeoutSeconds());
    }

    /**
     * Go to a certain page and wait until a condition is fulfilled.
     *
     * @param url              The url of the document
     * @param condition        The condition to check
     * @param timeoutInSeconds The maximum time to wait in seconds
     */
    public void goTo(String url, ExpectedCondition<Boolean> condition, Integer timeoutInSeconds) {
        goTo(url, condition, timeoutInSeconds, false);
    }

    public void goTo(String url, ExpectedCondition<Boolean> condition, Integer timeoutInSeconds, boolean forceReload) {
        if (forceReload || !driver.getCurrentUrl().equals(url)) {
            if (!forceReload) {
                LOGGER.info(driver.getCurrentUrl() + " x " + url);
            }
            driver.get(url);
        }
        new WebDriverWait(driver, timeoutInSeconds).until(condition);
    }

    /**
     * Execute a JavaScript command
     *
     * @param cmd The statement(s) to execute
     * @return The result of the script execution (<code>WebElement</code>, <code>Number</code>,...) if the statement returns something, <code>null</code> otherwise
     */
    public Object executeScript(String cmd) {
        try {
            return driver.executeScript(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Go to certain page and retrieve the document
     *
     * @param url The url of the document
     * @return The document
     */
    public Document get(String url) {
        return get(url, null);
    }

    /**
     * Go to a certain page, wait until a condition is fulfilled and retrieve the document
     *
     * @param url       The url of the document
     * @param condition The condition to check
     * @return The document
     */
    public Document get(String url, ExpectedCondition<Boolean> condition) {
        if (condition != null) {
            goTo(url, condition);
        } else {
            goTo(url);
        }
        return getCurrentWebDocument();
    }

    public Document getCurrentWebDocument() {
        Document document = null;

        try {
            InputStream stream = new ByteArrayInputStream(driver.getPageSource().getBytes(StandardCharsets.UTF_8.name()));
            document = ParserFactory.createHtmlParser().parse(stream);
            document.setDocumentURI(driver.getCurrentUrl());
        } catch (ParserException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return document;
    }

    public Document getWebDocument(String url) {
        Document document = null;

        if (driver != null) {
            // XXX
            // document.setDocumentURI(cleanUrl);
            // document.setUserData(HTTP_RESULT_KEY, httpResult, null);
            this.goTo(url);
//            driver.get(url);
            document = getCurrentWebDocument();
            if (document == null && getErrorCallback() != null) {
                getErrorCallback().accept(new DocumentRetrievalTrial(url, null));
            }
            callRetrieverCallback(document);
        }

        return document;
    }

    @Override
    public void getWebDocuments(Collection<String> urls, Consumer<Document> callback, ProgressMonitor progressMonitor) {
        for (String url : urls) {
            // react file fileTypeConsumer?
            boolean consumerFound = reactToFileTypeConsumer(url, getFileTypeConsumers());

            if (!consumerFound) {
                Document document = getWebDocument(url);
                if (document != null) {
                    callback.accept(document);
                }
            }

            if (progressMonitor != null) {
                progressMonitor.incrementAndPrintProgress();
            }
        }
    }

    @Override
    public Set<Document> getWebDocuments(Collection<String> urls) {
        Set<Document> documents = new HashSet<>();

        for (String url : urls) {
            Document document = getWebDocument(url);
            documents.add(document);
        }

        return documents;
    }

    /**
     * Find a DOM node.
     *
     * @param selector The CSS selector
     * @return The queried node
     */
    public WebElement find(String selector) {
        return find(null, selector);
    }

    /**
     * Find a DOM node.
     *
     * @param preselector The CSS selector of the context to search
     * @param selector    The CSS selector
     * @return The queried node
     */
    public WebElement find(String preselector, String selector) {
        WebElement parent = driver.findElement(By.cssSelector(preselector != null ? preselector : "html"));
        return parent.findElement(By.cssSelector(selector));
    }

    /**
     * Find all DOM nodes matching the given selector.
     *
     * @param selector The CSS selector
     * @return List of queried nodes
     */
    public List<WebElement> findAll(String selector) {
        return findAll(null, selector);
    }

    /**
     * Find all DOM nodes matching the given selector in a specific context.
     *
     * @param preSelector The CSS selector of the context to search.
     * @param selector    The CSS selector.
     * @return List of queried nodes.
     */
    public List<WebElement> findAll(String preSelector, String selector) {
        List<WebElement> pres = driver.findElements(By.cssSelector(preSelector != null ? preSelector : "html"));
        List<WebElement> result = new ArrayList<>();
        for (WebElement el : pres) {
            result.addAll(el.findElements(By.cssSelector(selector)));
        }
        return result;
    }

    /**
     * Close the webdriver.
     */
    @Override
    public void close() {
        driver.close();
    }

    public RemoteWebDriver getDriver() {
        return driver;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<Pattern, String> getWaitForElementMap() {
        return waitForElementMap;
    }

    public void setWaitForElementMap(Map<Pattern, String> waitForElementMap) {
        this.waitForElementMap = waitForElementMap;
    }

    public static void main(String... args) throws HttpException {
        StopWatch stopWatch = new StopWatch();
        RenderingDocumentRetriever r = new RenderingDocumentRetriever(DriverManagerType.CHROME);
        //DocumentRetriever r = new DocumentRetriever();
        //HttpResult httpResult = HttpRetrieverFactory.getHttpRetriever().httpGet("https://www.patagonia.com/");
        //System.out.println(httpResult);
//        r.goTo("https://www.patagonia.com/", true);
//        Document webDocument = r.getCurrentWebDocument();
//        Document webDocument = r.getWebDocument("https://www.patagonia.com/");
        Document webDocument = r.getWebDocument("https://www.kraftrecipes.com/");
//        Document webDocument = r.getWebDocument("https://www.whatismyip.com/");
        System.out.println(r.driver.executeScript("return navigator.userAgent"));
//        Document webDocument = r.getWebDocument("https://genius.com/");
        r.takeScreenshot("");
        System.out.println(HtmlHelper.getInnerXml(webDocument));
        System.out.println(webDocument.getDocumentURI());
        System.out.println(r.driver.getTitle());
//        r.close();
        System.out.println(stopWatch.getElapsedTimeStringAndIncrement());
        r.close();
//        Document webDocument = r.getWebDocument("https://www.sitesearch360.com");
//        ((RenderingDocumentRetriever) r).takeScreenshot("");
//        System.out.println(HtmlHelper.getInnerXml(webDocument));
//        r.goTo("http://www.wikiwand.com/en/Fashion");
////        r.goTo("https://www.sitesearch360.com");
//        WebElement contentBlock = r.find("#fullContent");
//        System.out.println(contentBlock.getText());
    }
}
