/**
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
     * Calculate dedicated page trust.
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

        // System.out.println("extract bodyContent of: " + mioPage.getUrl());
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

    // method only for testing
    // /**
    // * Calculate bodyContent.
    // *
    // * @param url the URL
    // */
    // private void calculateBodyContent(final String url) {
    // final MIOPage mioPage = new MIOPage(url);
    // final Crawler crawler = new Crawler();
    // final String content = crawler.downloadNotBlacklisted(url);
    // if (!("").equals(content)) {
    // // mioPage.setContent(crawler.downloadNotBlacklisted(url));
    // calculateDedicatedPageTrust(mioPage);
    // } else {
    // LOGGER.error("Getting bodyContent from: " + url + " failed!");
    //
    // }
    //
    // }

    /**
     * The main method.
     * 
     * @param abc the arguments
     */
    // public static void main(String[] abc) {
    // DedicatedPageDetector dpDet = new DedicatedPageDetector();
    // dpDet.calculateBodyContent("http://www.canon.de/For_Home/Product_Finder/Multifunctionals/Inkjet/PIXMA_MP990/");
    // dpDet.calculateBodyContent("http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE");
    // dpDet.calculateBodyContent("http://content.bmwusa.com/microsite/x52007/indexFlash.html");
    // dpDet.calculateBodyContent("http://www.sennheiser.com/3d-view/hd_800/index.html");
    // dpDet.calculateBodyContent("http://www.sennheiser.com/sennheiser/home_de.nsf/root/private_headphones_audiophile-headphones_500319");
    // dpDet.calculateBodyContent("http://pandorama.avatarmovie.com/");
    // dpDet.calculateBodyContent("http://www.avatarmovie.com/");
    // dpDet.calculateBodyContent("https://www.newsvine.com/_nv/accounts/login?popoff&redirect=http%3A%2F%2Fwww.newsvine.com%2F_wine%2Fsave%3Fu%3Dhttp%3A%2F%2Fwww.techradar.com%2Freviews%2Fphones%2Fmobile-phones%2Fhtc-desire-679515%2Freview");
    // dpDet.calculateBodyContent("http://www.orangeportal.sk/dnes/news/opinion.dwp?article=1909316&filter=all&level1=&level2=&node=1909316");

    // dpDet.calculateBodyContent("http://www.amazon.com/Sennheiser-HD800-Premier-Headphone/dp/B001OTZ8DA");
    // dpDet.calculateBodyContent("http://www.asiapac.com.au/Links/Ocean.htm");
    // dpDet.calculateBodyContent("http://www.cnet.com.au/sennheiser-hd-800-339294779.htm");
    // dpDet.calculateBodyContent("http://www.sennheiser.com/flash/HD_800_2/DE/base.html");

    // }

}
