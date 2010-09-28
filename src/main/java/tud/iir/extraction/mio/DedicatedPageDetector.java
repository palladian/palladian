/**
 * This class allows to calculate a trust-Value which indicates a DedicatedPage.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.List;

import tud.iir.helper.HTMLHelper;
import tud.iir.web.Crawler;

/**
 * The DedicatedPageDetector calculate for a given MIOPage a TrustValue for being a DedicatedPage.
 * 
 * @author Martin Werner
 */
public class DedicatedPageDetector {

    /**
     * Calculate DedicatedPage-Trust.
     * 
     * @param mioPage the mioPage
     */
    public void calculateDedicatedPageTrust(final MIOPage mioPage) {

        final String pageContent = mioPage.getContentAsString();

        // check for existence of Flash-DedicatedPage-Indicators
        int flashInd = 0;
        if (pageContent.contains("index.swf") || pageContent.contains("main.swf") || pageContent.contains("start.swf")
                || pageContent.contains("base.swf")) {
            flashInd++;
        }

        final String bodyContent = Crawler.extractBodyContent(pageContent, false);
        if (!("error").equalsIgnoreCase(bodyContent)) {

            // check for existence of images in the body
            final List<String> imageTags = HTMLHelper.getConcreteTags(bodyContent, "img");
            final int numberOfImages = imageTags.size();

            // check for links in the body
            final List<String> linksList = HTMLHelper.getConcreteTags(bodyContent, "a");
            final int numberOfLinks = linksList.size();

            // get the textual content of the body only
            final String trimmedBodyContent = Crawler.extractBodyContent(pageContent, true);

            // get length of textual body content
            final int trimmedBodyContentLength = trimmedBodyContent.length();

            // calculate the DedicatedPage-Trust with the values
            final double dpTrust = calculateDPTrustFactor(trimmedBodyContentLength, numberOfLinks, numberOfImages,
                    flashInd);

            mioPage.setDedicatedPageTrust(dpTrust);
        }
    }

    /**
     * Calculate a DedicatedPageTrust with the help of the given values.
     * 
     * @param contentLength the content length
     * @param numberOfLinks the number of links
     * @param numberOfImages the number of images
     * @param flashInd the flashIndicator
     * @return the double
     */
    private double calculateDPTrustFactor(final int contentLength, final int numberOfLinks, final int numberOfImages,
            final int flashInd) {

        double returnValue = 0;
        final double linkValue = calcSingleTrust(numberOfLinks, false);
        final double imageValue = calcSingleTrust(numberOfImages, false);
        double contentValue = calcSingleTrust(contentLength, true);

        contentValue = contentValue * 2;

        // ignore blank-pages
        if (!(flashInd == 0 && numberOfLinks == 0 && numberOfImages == 0 && contentLength == 0)) {
            returnValue = (linkValue + imageValue + contentValue + flashInd) / (4 + flashInd);
        }

        if (returnValue > 1) {
            returnValue = 1;
        }

        return returnValue;
    }

    /**
     * Calculate a DedicatedPageTrust for a single value.
     * 
     * @param singleValue the single value
     * @param isContentLength the is content length
     * @return the double
     */
    private double calcSingleTrust(final int singleValue, final boolean isContentLength) {
        double singleTrust = 0;
        int divisor = 1;
        if (isContentLength) {
            divisor = 960;
        } else {
            divisor = 10;
        }
        if (singleValue == 0) {
            singleTrust = 1;
        } else {

            singleTrust = 1 - ((double) singleValue / divisor);
            if (singleTrust < 0) {
                singleTrust = 0;
            }
        }
        return singleTrust;
    }
}
