package ws.palladian.retrieval.helper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class CookieAndAdCleanup {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieAndAdCleanup.class);
    private static Rules RULES;

    private static class Rules {
        Map<String, List<String>> searchPairs;
        Map<String, JsonElement> hostRules;
        Map<String, List<String>> cookies;
        Map<String, List<Map<String, Object>>> storage;
    }

    static {
        try (InputStream is = CookieAndAdCleanup.class.getResourceAsStream("/cookie_rules.json")) {
            if (is != null) {
                Gson gson = new Gson();
                RULES = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), Rules.class);
            } else {
                LOGGER.error("cookie_rules.json not found in resources");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load cookie_rules.json", e);
        }
    }

    public static void cleanup(WebDriver driver) {
        if (RULES == null)
            return;

        try {
            String currentUrl = driver.getCurrentUrl();
            String hostname = getHostname(currentUrl);

            // 1. Apply Cookies/Storage first (might prevent banner from showing)
            applyCookiesAndStorage(driver, hostname);

            // 2. Apply Host Rules
            applyHostRules(driver, hostname);

            // 3. Apply Generic Search Pairs
            applySearchPairs(driver);

            // 4. Heuristics for Adblock/Cookie banners
            applyHeuristics(driver);

        } catch (Exception e) {
            LOGGER.warn("Error during cookie cleanup for " + driver.getCurrentUrl(), e);
        }
    }

    private static String getHostname(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return "";
        }
    }

    private static void applyCookiesAndStorage(WebDriver driver, String hostname) {
        // Cookies (simple strings "name=value")
        // Note: Selenium Cookie API or JS. JS is often easier if we don't care about domain scoping logic detail in Selenium
        if (RULES.cookies != null) {
            applyDomainRules(RULES.cookies, hostname, (cookies) -> {
                for (String cookie : cookies) {
                    try {
                        ((JavascriptExecutor) driver).executeScript("document.cookie = arguments[0];", cookie);
                    } catch (Exception e) {
                    }
                }
            });
        }

        // Storage (localStorage/sessionStorage)
        if (RULES.storage != null) {
            applyDomainRules(RULES.storage, hostname, (items) -> {
                for (Map<String, Object> item : items) {
                    try {
                        String key = (String) item.get("key");
                        String value = (String) item.get("value");
                        // Some rules might imply strict session storage or logical storage, simple map here
                        // Assuming default is localStorage, but extension logic uses sessionStorage often for strict mode?
                        // common2.js uses sessionStorage.
                        ((JavascriptExecutor) driver).executeScript("sessionStorage.setItem(arguments[0], arguments[1]); localStorage.setItem(arguments[0], arguments[1]);", key,
                                value);
                    } catch (Exception e) {
                    }
                }
            });
        }
    }

    private static <T> void applyDomainRules(Map<String, T> map, String hostname, java.util.function.Consumer<T> action) {
        // Exact match
        if (map.containsKey(hostname)) {
            action.accept(map.get(hostname));
        }
        // Suffix match (basic) - common.js logic is more complex with splitting, but we can try simple walking up
        String[] parts = hostname.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            StringBuilder sb = new StringBuilder();
            for (int k = i; k < parts.length; k++) {
                if (sb.length() > 0)
                    sb.append(".");
                sb.append(parts[k]);
            }
            String sub = sb.toString();
            if (map.containsKey(sub) && !sub.equals(hostname)) {
                action.accept(map.get(sub));
            }
        }
    }

    private static void applyHostRules(WebDriver driver, String hostname) {
        if (RULES.hostRules == null)
            return;

        // Similar walking up logic for host rules
        String[] parts = hostname.split("\\.");
        for (int i = 0; i < parts.length; i++) { // start from full hostname down to root
            StringBuilder sb = new StringBuilder();
            for (int k = i; k < parts.length; k++) {
                if (sb.length() > 0)
                    sb.append(".");
                sb.append(parts[k]);
            }
            String sub = sb.toString();
            if (RULES.hostRules.containsKey(sub)) {
                JsonElement rule = RULES.hostRules.get(sub);
                executeRule(driver, rule);
                // Should we stop after first match? common5.js returns, so yes.
                break;
            }
        }
    }

    private static void executeRule(WebDriver driver, JsonElement ruleEl) {
        if (!ruleEl.isJsonObject())
            return;
        JsonObject rule = ruleEl.getAsJsonObject();
        String type = rule.get("type").getAsString();

        try {
            if ("selector".equals(type)) {
                clickSelector(driver, rule.get("value").getAsString());
            } else if ("chain".equals(type)) {
                JsonElement values = rule.get("values");
                if (values.isJsonArray()) {
                    for (JsonElement val : values.getAsJsonArray()) {
                        clickSelector(driver, val.getAsString());
                    }
                }
            } else if ("if".equals(type)) {
                String condition = rule.get("condition").getAsString();
                if (elementExists(driver, condition)) {
                    JsonElement actions = rule.get("trueAction");
                    if (actions.isJsonArray()) {
                        for (JsonElement val : actions.getAsJsonArray()) {
                            clickSelector(driver, val.getAsString());
                        }
                    }
                }
            } else if ("if_else".equals(type)) {
                String condition = rule.get("condition").getAsString();
                JsonElement actions = elementExists(driver, condition) ? rule.get("trueAction") : rule.get("falseAction");
                if (actions != null && actions.isJsonArray()) {
                    for (JsonElement val : actions.getAsJsonArray()) {
                        clickSelector(driver, val.getAsString());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore failure in rule execution
        }
    }

    private static void applySearchPairs(WebDriver driver) {
        if (RULES.searchPairs == null)
            return;

        for (Map.Entry<String, List<String>> entry : RULES.searchPairs.entrySet()) {
            String keySelector = entry.getKey();
            if (elementExists(driver, keySelector)) {
                for (String selector : entry.getValue()) {
                    clickSelector(driver, selector);
                    // remove entire keyselector element
                    try {
                        WebElement element = driver.findElement(getBy(keySelector));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].remove();", element);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private static void applyHeuristics(WebDriver driver) {
        // Remove common overlay masks
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var all = document.querySelectorAll('div, section, aside, iframe');" + "for (var i = 0; i < all.length; i++) {" + "  var el = all[i];"
                            + "  var style = window.getComputedStyle(el);" + "  var z = parseInt(style.zIndex);" + "  if (z > 500 || style.position === 'fixed') {"
                            + "    var text = el.innerText.toLowerCase();"
                            + "    if (text.length < 1500 && (text.includes('cookie') || text.includes('agree') || text.includes('consent') || text.includes('adblock') || text.includes('ad block') || text.includes('disable ad'))) {"
                            + "       if (text.includes('adblock') || text.includes('ad block')) {" + "           el.remove();" + // Aggressively remove adblock warnings
                            "       } else {" + "           // For cookies, try to find a button first" +
                            // If we click, we might trigger reload or something, safe is sometimes to remove or click 'accept'
                            // Heuristic click:
                            "           var btn = el.querySelector('button, a[class*=\"btn\"], input[type=\"button\"]');"
                            + "           if (btn && (btn.innerText.toLowerCase().includes('accept') || btn.innerText.toLowerCase().includes('agree') || btn.innerText.toLowerCase().includes('ok') || btn.innerText.toLowerCase().includes('yes'))) {"
                            + "               btn.click();" + "           } else {" + "               el.remove();" + // Just remove the banner if no obvious button
                            "           }" + "       }" + "    }" + "  }" + "}" + "document.body.style.overflow = 'auto';" // Restore scrolling
            );
        } catch (Exception e) {
            // ignore
        }
    }

    private static boolean elementExists(WebDriver driver, String selector) {
        try {
            return !driver.findElements(getBy(selector)).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static void clickSelector(WebDriver driver, String selector) {
        try {
            List<WebElement> elements = driver.findElements(getBy(selector));
            for (WebElement el : elements) {
                if (el.isDisplayed()) {
                    el.click();
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                    }
                } else {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                }
            }
        } catch (Exception e) {
        }
    }

    private static By getBy(String selector) {
        if (selector.startsWith("/") || selector.startsWith("(")) {
            return By.xpath(selector);
        }
        return By.cssSelector(selector);
    }
}
