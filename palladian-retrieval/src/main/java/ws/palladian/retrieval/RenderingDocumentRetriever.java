package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static io.github.bonigarcia.wdm.DriverManagerType.CHROME;

/**
 * A selenium-based retriever for web documents that should be rendered (execute JS and CSS).
 *
 * @author Jaroslav Vankat, David Urbansky
 */
public class RenderingDocumentRetriever extends WebDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderingDocumentRetriever.class);

    protected RemoteWebDriver driver;
    private int timeoutSeconds = 10;

    /**
     * Default constructor, doesn't force reloading pages when <code>goTo</code> with the current url is called.
     */
    public RenderingDocumentRetriever() {
        StopWatch sw = new StopWatch();

        WebDriverManager.chromedriver().setup();
        WebDriverManager.getInstance(CHROME).setup();

        // start new headless browser instance
        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        driver = new ChromeDriver(options);

        LOGGER.info("Starting up a browser instance took " + sw.getElapsedTimeString());
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
     * Go to a certain page and (hopefully) wait for the document to be loaded.
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
        } catch (ParserException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return document;
    }

    public Document getWebDocument(String url) {
        Document document = null;

        if (driver != null) {
            driver.get(url);
            document = getCurrentWebDocument();
        }

        return document;
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

    public static void main(String... args) {
        WebDocumentRetriever r = new RenderingDocumentRetriever();
        Document webDocument = r.getWebDocument("https://genius.com/");
        ((RenderingDocumentRetriever) r).takeScreenshot("");
        System.out.println(HtmlHelper.getInnerXml(webDocument));
        ((RenderingDocumentRetriever) r).close();
//        Document webDocument = r.getWebDocument("https://www.sitesearch360.com");
//        ((RenderingDocumentRetriever) r).takeScreenshot("");
//        System.out.println(HtmlHelper.getInnerXml(webDocument));
//        r.goTo("http://www.wikiwand.com/en/Fashion");
////        r.goTo("https://www.sitesearch360.com");
//        WebElement contentBlock = r.find("#fullContent");
//        System.out.println(contentBlock.getText());
    }
}
