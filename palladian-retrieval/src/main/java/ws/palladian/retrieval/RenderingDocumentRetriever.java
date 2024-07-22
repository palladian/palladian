package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.ThreadHelper;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.search.DocumentRetrievalTrial;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A selenium-based retriever for web documents that should be rendered (execute JS and CSS).
 * <p>
 * Note: This is NOT thread safe, use a RenderingDocumentRetrieverPool for parallel applications.
 *
 * @author David Urbansky, Jaroslav Vankat
 */
public class RenderingDocumentRetriever extends JsEnabledDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderingDocumentRetriever.class);

    protected RemoteWebDriver driver;

    protected Consumer<NoSuchSessionException> noSuchSessionExceptionCallback;

    /**
     * Default constructor, doesn't force reloading pages when <code>goTo</code> with the current url is called.
     */
    public RenderingDocumentRetriever() {
        this(DriverManagerType.CHROME, null, HttpRetriever.USER_AGENT, null);
    }

    public RenderingDocumentRetriever(DriverManagerType browser) {
        this(browser, null, HttpRetriever.USER_AGENT, null);
    }

    public RenderingDocumentRetriever(DriverManagerType browser, org.openqa.selenium.Proxy proxy, String userAgent, String driverVersionCode) {
        this(browser, proxy, userAgent, driverVersionCode, null);
    }

    public RenderingDocumentRetriever(DriverManagerType browser, org.openqa.selenium.Proxy proxy, String userAgent, String driverVersionCode, String binaryPath) {
        this(browser, proxy, userAgent, driverVersionCode, binaryPath, null);
    }

    public RenderingDocumentRetriever(DriverManagerType browser, org.openqa.selenium.Proxy proxy, String userAgent, String driverVersionCode, String binaryPath,
            Set<String> additionalOptions) {
        String downloadFilePath = "data/selenium-downloads";
        if (browser == DriverManagerType.FIREFOX) {
            if (driverVersionCode != null) {
                WebDriverManager.firefoxdriver().driverVersion(driverVersionCode).setup();
            } else {
                WebDriverManager.firefoxdriver().setup();
            }

            FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("browser.download.dir", downloadFilePath);
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.manager.showWhenStarting", false);

            FirefoxOptions options = new FirefoxOptions();
            options.setAcceptInsecureCerts(true);
            options.addPreference("general.useragent.override", userAgent);
            options.addPreference("intl.accept_languages", "en-US");
            options.addArguments("--headless");
            options.setProfile(profile);
            if (binaryPath != null) {
                options.setBinary(binaryPath);
            }

            if (proxy != null) {
                options.setCapability(CapabilityType.PROXY, proxy);
            }

            driver = new FirefoxDriver(options);
        } else if (browser == DriverManagerType.CHROME) {
            if (driverVersionCode != null) {
                //                WebDriverManager.chromedriver().browserVersion(driverVersionCode).setup();
                WebDriverManager.chromedriver().driverVersion(driverVersionCode).setup();
            } else {
                WebDriverManager.chromedriver().setup();
            }

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadFilePath);
            prefs.put("download.prompt_for_download", false);

            ChromeOptions options = new ChromeOptions();
            try {
                options.setAcceptInsecureCerts(true);
            } catch (Throwable throwable) {
                LOGGER.error("problem setting accept insecure certs", throwable);
            }

            options.addArguments("--headless");
            options.addArguments("--lang=en-US");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-extensions");
            options.addArguments("--start-maximized");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=" + userAgent);

            if (additionalOptions != null) {
                for (String additionalOption : additionalOptions) {
                    options.addArguments(additionalOption);
                }
            }

            options.setExperimentalOption("prefs", prefs);
            if (binaryPath != null) {
                options.setBinary(binaryPath);
            }

            if (proxy != null) {
                options.setCapability(CapabilityType.PROXY, proxy);
            } else {
                options.addArguments("--proxy-server='direct://'");
                options.addArguments("--proxy-bypass-list=*");
            }

            driver = new ChromeDriver(options);
        } else if (browser == DriverManagerType.CHROMIUM) {
            if (driverVersionCode != null) {
                WebDriverManager.chromiumdriver().driverVersion(driverVersionCode).setup();
            } else {
                WebDriverManager.chromiumdriver().setup();
            }

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadFilePath);
            prefs.put("download.prompt_for_download", false);

            ChromeOptions options = new ChromeOptions();
            options.setAcceptInsecureCerts(true);

            options.addArguments("--headless");
            options.addArguments("--lang=en-US");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-extensions");
            options.addArguments("--start-maximized");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=" + userAgent);

            if (additionalOptions != null) {
                for (String additionalOption : additionalOptions) {
                    options.addArguments(additionalOption);
                }
            }

            options.setExperimentalOption("prefs", prefs);
            if (binaryPath != null) {
                options.setBinary(binaryPath);
            }

            if (proxy != null) {
                options.setCapability(CapabilityType.PROXY, proxy);
            } else {
                options.addArguments("--proxy-server='direct://'");
                options.addArguments("--proxy-bypass-list=*");
            }

            driver = new ChromeDriver(options);
        }
    }

    public RenderingDocumentRetriever(RemoteWebDriver driver) {
        this.driver = driver;
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
        String currentUrl = Optional.ofNullable(driver.getCurrentUrl()).orElse("");

        if (cookies != null && !cookies.isEmpty()) {
            String domain = UrlHelper.getDomain(url, false, true);
            String currentDomain = UrlHelper.getDomain(currentUrl, false, true);
            if (!domain.equalsIgnoreCase(currentDomain)) { // first navigate to the domain so we can set cookies
                driver.get(url);
            }
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                try {
                    driver.manage().addCookie(new Cookie(entry.getKey(), entry.getValue(), domain, "/", null, false, false));
                } catch (Exception e) {
                    LOGGER.error("Could not set cookie", e);
                }
            }
        }

        if (forceReload || !currentUrl.equals(url)) {
            driver.get(url);
        }

        // check whether a pattern matches and we have elements to wait for
        Set<String> selectors = new HashSet<>();
        for (Map.Entry<Pattern, Set<String>> entry : getWaitForElementsMap().entrySet()) {
            if (entry.getKey().matcher(url).find()) {
                selectors.addAll(entry.getValue());
                break;
            }
        }

        try {
            if (!selectors.isEmpty()) {
                new WebDriverWait(driver, Duration.ofSeconds(getTimeoutSeconds())).until(webDriver -> {
                    for (String cssSelector : selectors) {
                        if (webDriver.findElement(By.cssSelector(cssSelector)) == null) {
                            return false;
                        }
                    }
                    return true;
                });
            } else {
                new WebDriverWait(driver, Duration.ofSeconds(getTimeoutSeconds())).until(
                        webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            }
        } catch (Exception e) {
            if (getWaitExceptionCallback() != null) {
                getWaitExceptionCallback().accept(new WaitException(url, e, CollectionHelper.joinReadable(selectors)));
            }
            LOGGER.error("problem with waiting", e);
        } catch (Throwable e) {
            LOGGER.error("problem with waiting", e);
            ThreadHelper.deepSleep(500);
        }
        driver.manage().deleteAllCookies();
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
        try {
            if (forceReload || !driver.getCurrentUrl().equals(url)) {
                if (!forceReload) {
                    LOGGER.info(driver.getCurrentUrl() + " x " + url);
                }
                driver.get(url);
            }
        } catch (NoSuchSessionException e) {
            LOGGER.error("problem getting session", e);
            if (getNoSuchSessionExceptionCallback() != null) {
                getNoSuchSessionExceptionCallback().accept(e);
            }
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds)).until(condition);
        } catch (Exception e) {
            LOGGER.error("problem with waiting for condition", e);
            if (getWaitExceptionCallback() != null) {
                getWaitExceptionCallback().accept(new WaitException(url, e, null));
            }
        }
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
            InputStream stream = new ByteArrayInputStream(driver.getPageSource().getBytes(StandardCharsets.UTF_8));
            InputSource inputSource = new InputSource(stream);
            inputSource.setEncoding(StandardCharsets.UTF_8.name());
            document = ParserFactory.createHtmlParser().parse(inputSource);
            document.setDocumentURI(driver.getCurrentUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }

    @Override
    public Document getWebDocument(String url) {
        Document document = null;

        if (driver != null) {
            // react file fileTypeConsumer?
            boolean consumerFound = reactToFileTypeConsumer(url, getFileTypeConsumers());

            if (!consumerFound) {
                // XXX
                // document.setDocumentURI(cleanUrl);
                // document.setUserData(HTTP_RESULT_KEY, httpResult, null);
                try {
                    this.goTo(url);
                    document = getCurrentWebDocument();
                } catch (Exception e) {
                    LOGGER.error("problem opening page", e);
                }
                if (document == null && getErrorCallback() != null) {
                    getErrorCallback().accept(new DocumentRetrievalTrial(url, null));
                }
                try {
                    callRetrieverCallback(document);
                } catch (Exception e) {
                    LOGGER.error("problem with retriever callback", e);
                }
            }
        }

        return document;
    }

    @Override
    public void getWebDocuments(Collection<String> urls, Consumer<Document> callback, ProgressMonitor progressMonitor) {
        for (String url : urls) {
            getRequestThrottle().hold();
            // react file fileTypeConsumer?
            boolean consumerFound = reactToFileTypeConsumer(url, getFileTypeConsumers());

            if (!consumerFound) {
                if (getRetrieverCallbacks().isEmpty()) {
                    addRetrieverCallback(callback);
                }
                getWebDocument(url);
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
            getRequestThrottle().hold();
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

    public boolean closeAndQuit() {
        try {
            driver.close();
            driver.quit();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public RemoteWebDriver getDriver() {
        return driver;
    }

    @Override
    public void deleteAllCookies() {
        super.deleteAllCookies();
        try {
            driver.manage().deleteAllCookies();
        } catch (NoSuchSessionException e) {
            LOGGER.error("problem getting session", e);
            if (getNoSuchSessionExceptionCallback() != null) {
                getNoSuchSessionExceptionCallback().accept(e);
            }
        }
    }

    @Override
    public int requestsLeft() {
        return Integer.MAX_VALUE;
    }

    public void setDriver(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public Consumer<NoSuchSessionException> getNoSuchSessionExceptionCallback() {
        return noSuchSessionExceptionCallback;
    }

    public void setNoSuchSessionExceptionCallback(Consumer<NoSuchSessionException> noSuchSessionExceptionCallback) {
        this.noSuchSessionExceptionCallback = noSuchSessionExceptionCallback;
    }

    public static void main(String... args) throws HttpException {
        StopWatch stopWatch = new StopWatch();
        RenderingDocumentRetriever r = new RenderingDocumentRetriever(DriverManagerType.CHROME);
        // DocumentRetriever r = new DocumentRetriever();
        // HttpResult httpResult = HttpRetrieverFactory.getHttpRetriever().httpGet("https://www.patagonia.com/");
        // System.out.println(httpResult);
        // r.goTo("https://www.patagonia.com/", true);
        // Document webDocument = r.getCurrentWebDocument();
        // Document webDocument = r.getWebDocument("https://www.patagonia.com/");
        Document webDocument = r.getWebDocument("https://www.kraftrecipes.com/");
        // Document webDocument = r.getWebDocument("https://www.whatismyip.com/");
        System.out.println(r.driver.executeScript("return navigator.userAgent"));
        // Document webDocument = r.getWebDocument("https://genius.com/");
        r.takeScreenshot("");
        System.out.println(HtmlHelper.getInnerXml(webDocument));
        System.out.println(webDocument.getDocumentURI());
        System.out.println(r.driver.getTitle());
        // r.close();
        System.out.println(stopWatch.getElapsedTimeStringAndIncrement());
        r.close();
        // Document webDocument = r.getWebDocument("https://www.sitesearch360.com");
        // ((RenderingDocumentRetriever) r).takeScreenshot("");
        // System.out.println(HtmlHelper.getInnerXml(webDocument));
        // r.goTo("http://www.wikiwand.com/en/Fashion");
        //// r.goTo("https://www.sitesearch360.com");
        // WebElement contentBlock = r.find("#fullContent");
        // System.out.println(contentBlock.getText());
    }
}
