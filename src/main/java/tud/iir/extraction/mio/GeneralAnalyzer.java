/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.helper.StringHelper;
import tud.iir.web.Crawler;

/**
 * The GeneralAnalyzer provides often used analyzing-methods.
 * 
 * @author Martin Werner
 */
public class GeneralAnalyzer {

    // TODO Think over to change the methods to final for override-protection or
    // to protected for only package usage

    /**
     * Get WebPage as String (without comments and JS/CSS possible).
     * 
     * @param URLString the uRL string
     * @param removeJS the remove js
     * @return the page
     */
    public String getPage(String URLString, boolean removeJS) {
        String page = "";
        Crawler craw = new Crawler(5000, 5000, 10000);
        if (craw.isValidURL(URLString, false)) {
            if (removeJS) {
                page = craw.download(URLString, false, true, true, false);
            } else {
                // page = craw.download(URLString, false, false, false, false);
                page = craw.downloadNotBlacklisted(URLString);
            }
        }
        return page;
    }

    /**
     * Gets the page.
     * 
     * @param URLString the uRL string
     * @return the page
     */
    public String getPage(String URLString) {
        return getPage(URLString, false);
    }

    /**
     * Extract values e.g for: src=, href= or title=
     * 
     * @param pattern the pattern
     * @param content the content
     * @param removeTerm the remove term
     * @return the string
     */
    public String extractElement(String pattern, String content, String removeTerm) {
        String element = "";
        Pattern p = Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(content);
        while (m.find()) {
            String result = m.group(0);
            if (!removeTerm.equals("")) {
                // remove the remove term
                result = result.replaceFirst(removeTerm, "");
                result = result.replaceFirst(removeTerm.toUpperCase(), "");
                result = result.replaceFirst(removeTerm.toLowerCase(), "");
            }
            // remove the quotation-marks
            result = result.replaceAll("\"", "");
            result = result.replaceAll("'", "");

            element = result;
        }
        return element;
    }

    /**
     * Check URL for validness and eventually modify e.g. relative path
     * 
     * @param urlCandidate the url candidate
     * @param pageURL the page url
     * @return the string
     */
    public String verifyURL(String urlCandidate, String pageURL) {

        String returnValue = "";
        // Crawler crawler = new Crawler(5000, 5000, 10000);

        String modUrlCandidate = urlCandidate.trim();
        if (modUrlCandidate.startsWith("http://")) {
            if (Crawler.isValidURL(modUrlCandidate, false)) {
                return modUrlCandidate;
            }
        } else {

            if (modUrlCandidate.length() > 2) {
                String modifiedURL = Crawler.makeFullURL(pageURL, modUrlCandidate);
                if (Crawler.isValidURL(modifiedURL, false)) {
                    return modifiedURL;
                }
            }

        }
        return returnValue;
    }

    /**
     * Extract alt text from tag.
     * 
     * @param relevantTag the relevant tag
     * @return the string
     */
    public String extractALTTextFromTag(String relevantTag) {
        String altText = "";
        int beginIndex = relevantTag.indexOf(">") + 1;
        int endIndex = relevantTag.lastIndexOf("<");
        if (beginIndex < endIndex) {
            String modRelevantTag = relevantTag.substring(beginIndex, endIndex);
            // StringHelper stringHelper = new StringHelper();
            altText = StringHelper.removeHTMLTags(modRelevantTag, true, true, false, true);
        }

        return altText;
    }

}
