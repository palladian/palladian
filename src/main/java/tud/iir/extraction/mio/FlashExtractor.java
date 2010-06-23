/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

public class FlashExtractor extends GeneralAnalyzer {

    private StringHelper stringHelper;
    private Entity entity;
    private String mioPageContent;

    /**
     * Instantiates a new flash extractor.
     */
    public FlashExtractor() {
        stringHelper = new StringHelper();
    }

    /**
     * Extract flash objects.
     *
     * @param mioPage the mio page
     * @param entity the entity
     * @return the list
     */
    public List<MIO> extractFlashObjects(MIOPage mioPage, Entity entity) {

        this.entity = entity;
        this.mioPageContent = mioPage.getContent();

        List<MIO> flashMIOs = new ArrayList<MIO>();

        // extract all <object>
        List<String> relevantTags = stringHelper.getConcreteTags(mioPageContent, "object");

        // remove the object-tags
        mioPageContent = stringHelper.removeConcreteHTMLTag(mioPageContent, "object");

        // extract all remaining <embed>-tags
        relevantTags.addAll(stringHelper.getConcreteTags(mioPageContent, "embed"));

        // remove all <embed>-tags
        mioPageContent = stringHelper.removeConcreteHTMLTag(mioPageContent, "embed");

        // extract all <script>-tags
        relevantTags.addAll(stringHelper.getConcreteTags(mioPageContent, "script"));

        // remove all <script>-tags
        mioPageContent = stringHelper.removeConcreteHTMLTag(mioPageContent, "script");

        if (mioPageContent.contains(".swf") || mioPageContent.contains(".SWF")) {

            List<MIO> furtherSWFs = extractSWFURL(mioPageContent, mioPage);
            System.out.println("NOCH SWF ENTHALTEN! - " + mioPage.getUrl());
            for (MIO mio : furtherSWFs) {
                System.out.println(mio.getDirectURL());
            }
            flashMIOs.addAll(furtherSWFs);

        }

        // try to extract swf-file-URLs
        for (String relevantTag : relevantTags) {

            List<MIO> tempMIOs = new ArrayList<MIO>();

            if (relevantTag.contains("swfobject") || relevantTag.contains("swfObject")
                    || relevantTag.contains("SWFObject")) {
                // System.out.println("CheckSWFObject");
                MIO tempMio = checkSWFObject(relevantTag, mioPage);
                if (tempMio != null) {

                    // System.out.println("SWFObject enthalten! " + mioPage.getUrl());

                    // flashMIOs.add(checkSWFObject(relevantTag, mioPage));
                    tempMIOs.add(tempMio);
                }

            } else {
                // extract all swf-files from a relevant-tag
                // flashMIOs.addAll(extractSWFURL(relevantTag, mioPage));
                tempMIOs.addAll(extractSWFURL(relevantTag, mioPage));

                // check for flashvars
                if (relevantTag.contains("flashvars")) {
                    List<String> flashVars = extractFlashVars(relevantTag);
                    if (!flashVars.isEmpty()) {
                        for (MIO mio : tempMIOs) {
                            mio = adaptFlashVarsToURL(mio, flashVars);
                            mio.addInfos("flashvars", flashVars);
                        }
                    }
                }
            }

            // extract ALT-Text from object and embed-tags and add to MIO-infos
            if (!relevantTag.toLowerCase().startsWith("<script")) {
                List<String> altText = new ArrayList<String>();
                for (MIO mio : tempMIOs) {
                    altText.add(extractALTTextFromTag(relevantTag));
                    mio.addInfos("altText", altText);
                }
            }

            flashMIOs.addAll(tempMIOs);
        }

        // Calculate Trust
        MIOContextAnalyzer mioContextAnalyzer = new MIOContextAnalyzer(entity, mioPage);
        // TODO: calculate a trust for the flashMIOs of that mioPage
        System.out.println("MIO-Trust-Calculation!");

        for (MIO mio : flashMIOs) {
            // calculateTrust(mio);
            mioContextAnalyzer.calculateTrust(mio);
        }
        return flashMIOs;
    }

    /**
     * Adapt flash vars to url.
     *
     * @param mio the mio
     * @param flashVars the flash vars
     * @return the mIO
     */
    private MIO adaptFlashVarsToURL(MIO mio, List<String> flashVars) {

        String url = mio.getDirectURL();
        StringBuffer modURL = new StringBuffer();
        for (String flashVar : flashVars) {
            if (!flashVar.contains("\"") && !modURL.toString().contains(flashVar)) {
                if (modURL.equals("")) {
                    modURL.append(url + "?" + flashVar);
                } else {
                    modURL.append(modURL + "&" + flashVar);
                }
            }
            mio.setDirectURL(modURL.toString());
        }

        return mio;
    }

    // extract an swf-URL out of concrete tag
    /**
     * Extract swfurl.
     *
     * @param concreteTag the concrete tag
     * @param mioPage the mio page
     * @return the list
     */
    private List<MIO> extractSWFURL(String concreteTag, MIOPage mioPage) {
        List<MIO> resultList = new ArrayList<MIO>();
        // String regExp = "\".[^\"]*\\.swf\"";
        String regExp = "(\".[^\"]*\\.swf\")|(\".[^\"]*\\.swf\\?.[^\"]*\")";
        Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // remove attributs like name="140.swf"
        concreteTag = concreteTag.replaceAll("name=\".[^\"]*\"", "");

        Matcher matcher = pattern.matcher(concreteTag);
        while (matcher.find()) {
            String mioAdr = matcher.group(0).replaceAll("\"", "");
            // System.out.println("URL: "+ mioAdr);
            if (!mioAdr.contains("expressinstall")) {
                MIO mio = new MIO("swf", verifyURL(mioAdr, mioPage.getUrl()), mioPage.getUrl(), entity);
                // System.out.println(verifyURL(mioAdr, mioPage.getUrl()));
                // mio.setDirectURL(verifyURL(mioAdr, mioPage.getUrl()));
                resultList.add(mio);
            }

        }

        return resultList;
    }

    // extract Parameters of Flashvars from a concrete tag
    /**
     * Extract flash vars.
     *
     * @param tagContent the tag content
     * @return the list
     */
    private List<String> extractFlashVars(String tagContent) {
        List<String> flashVars = new ArrayList<String>();

        // extract var flashVars = {}
        String regExp = "flashvars(\\s?)=(\\s?)\\{[^\\}]*\\};";
        Pattern p = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(tagContent);
        while (m.find()) {
            // result has the form: flashvars = {cool};
            String result = m.group(0);
            result = result.replaceAll("flashvars(\\s?)=(\\s?)", "");
            result = result.trim();
            result = result.substring(0, result.length() - 1);
            result = result.replaceAll("[\\{,\\}]", "");
            result = result.trim();
            // result has the form: cool
            if (result.length() > 0) {
                flashVars.add(result);
            }
        }

        // extract <param name="FlashVars"
        // value="myURL=http://weblogs.adobe.com/">
        String regExp2 = "<param[^>]*flashvars[^>]*>";
        Pattern p2 = Pattern.compile(regExp2, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(tagContent);
        while (m2.find()) {
            // result has the form: <param name="FlashVars"
            // value="myURL=http://weblogs.adobe.com/">
            String result = m2.group(0);
            String pattern = "value=\"[^>]*\"";
            result = extractElement(pattern, result, "value=");
            if (result.length() > 0) {
                flashVars.add(result);
            }

        }

        // extract FlashVars="myURL=http://weblogs.adobe.com/"
        String regExp3 = "flashvars=\"[^\"]*\"";
        Pattern p3 = Pattern.compile(regExp3, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m3 = p3.matcher(tagContent);
        while (m3.find()) {
            String result = m3.group(0);
            String pattern = "flashvars=\"[^\"]*\"";
            result = extractElement(pattern, result, "flashvars=");
            if (result.length() > 0) {
                flashVars.add(result);
            }
        }

        if (flashVars.size() > 0) {
            System.out.println("mehrere FlashVars: ");
            for (String fVar : flashVars) {
                System.out.println(fVar);

            }
            System.out.println("____________________");

        }

        return flashVars;
    }

    /**
     * Check swf object.
     *
     * @param relevantTag the relevant tag
     * @param mioPage the mio page
     * @return the mIO
     */
    private MIO checkSWFObject(String relevantTag, MIOPage mioPage) {

        MIO mio = null;
        // String content = mioPage.getContent();
        if (relevantTag.contains("swfobject.embedSWF") || relevantTag.contains("new SWFObject(")) {
            // StringHelper stringHelper = new StringHelper();
            // get all <script>-tags
            // List<String> jsList = stringHelper.getConcreteTags(content,
            // "script");
            // analyze <script>-tags for swfobject-content
            // for (String jsTag : jsList) {
            // if (jsTag.contains("swfobject")) {
            // System.out.println(jsTag);
            List<MIO> tempList = extractSWFURL(relevantTag, mioPage);

            // only concentrate on one SWFURL of the relevantTag, because they
            // all are the same
            if (tempList.size() > 0) {
                mio = tempList.get(0);
            }

            // check for flashvars
            if (relevantTag.contains("flashvars")) {
                List<String> flashVars = extractFlashVars(relevantTag);

                if (!flashVars.isEmpty()) {
                    // for (MIO mio : tempList) {
                    mio = adaptFlashVarsToURL(mio, flashVars);
                    mio.addInfos("flashvars", flashVars);
                    // }
                }
            }

            // analyze for queryParamValues
            if (relevantTag.contains("getQueryParamValue")) {

                String regExp = "getQueryParamValue\\(.[^\\)]*\\)";
                // String queryParamValue= extractElement(regExp, relevantTag,
                // "getQueryParamValue(");
                // //remove the closing ")"
                // queryParamValue = queryParamValue.substring(0,
                // queryParamValue.length()-1);

                Pattern p = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(relevantTag);
                List<String> queryParamValues = new ArrayList<String>();
                while (m.find()) {
                    // result has the form: flashvars = {cool};
                    String result = m.group(0);
                    queryParamValues.add("result");

                }

                mio.addInfos("queryParamValues", queryParamValues);
            }

            return mio;
        }

        // if (relevantTag.contains("new SWFObject(")){
        // System.out.println("contains new SWFObject(");
        // }
        return null;
    }

    /**
     * Extract swf from comments.
     *
     * @param mioPage the mio page
     * @return the list
     */
    private List<MIO> extractSWFFromComments(MIOPage mioPage) {
        List<MIO> resultList = new ArrayList<MIO>();
        StringHelper stringHelper = new StringHelper();
        List<String> relevantTags = stringHelper.getConcreteTags(mioPage.getContent(), "<!--", "-->");
        for (String relevantTag : relevantTags) {

            List<MIO> tempList = extractSWFURL(relevantTag, mioPage);
            if (relevantTag.contains("flashvars")) {
                List flashVars = extractFlashVars(relevantTag);
                if (!flashVars.isEmpty()) {
                    for (MIO mio : tempList) {
                        mio.addInfos("flashvars", flashVars);
                    }
                }

            }
            resultList.addAll(tempList);

        }
        return resultList;
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {

        Crawler crawler = new Crawler();
        // String
        // content=crawler.download("http://www2.razerzone.com/Megalodon/");
        // MIOPage page = new MIOPage("http://www2.razerzone.com/Megalodon/",
        // content);

        // String content = crawler
        // .download("http://www.sennheiser.com/flash/HD_800_2/DE/base.html");
        // MIOPage page = new MIOPage(
        // "http://www.sennheiser.com/flash/HD_800_2/DE/base.html",
        // content);

        // String content = crawler
        // .download("http://www.sennheiser.com/3d-view/hd_800/index.html");
        // MIOPage page = new MIOPage(
        // "http://www.sennheiser.com/3d-view/hd_800/index.html", content);

        // String content = crawler
        // .download("http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE");
        // MIOPage page = new MIOPage(
        // "http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE",
        // content);

        // String content = crawler
        // .download("http://click.search123.uk.com/cgi-bin/clickthru.cgi?EI=02688&Q=samsung+s8500+wave&NGT=pDKLE8RD8FvUBNvAfEv+xjLcdmNBZmiMFNZTHXILNOcl/aVhYpvXm9M+CuGSzabnyqxXPu/WtpifeKSPNCKVq1VejEFG1T0OMv5W4FGrOOBEvGOHgftJTakep/ELYjxP8AE9TNkAXAyLK0JdshN28ZbXpzYj/lr7+TEpHkqmmx4gfsW4iyIMzk7b2h8oiTrk24ILOcsDuXKY034Jq3wOrR1YIEBShYGMbrGIpFFMm6V/uZmqBLC+xQOEAKgbY+Pq/8JPUUNRPRlwYmy7vTqGvxSaxaZeZZEEUJ4/BiXVjm6NbFXnKX+9jiLVEK6jOh4lYf3hYf8g1ORg+O7Y6yV2tFzAWuAS+y79e77MEF6e56WBxXZIpDUgjp/92evDreDADkEu4WxyywXpAtPajH8CZmf7OW+/gcXJmOHjAbPSDFUsd6XQIqFUQG/YguIlfbjyyXx5kJctY6ZEI0bszS4WUUFS9NREU689rEHTwaJ8ccgxcI56+15rU8dw2DYEZ5Y1xQfKD0Mq9N4gqXyJPNGO/YlCJQA48xdeJ3hTVIDN4p5PqjsWUfJitjlSyQYsxZ4u6HmMhiawh3AM7hh1D2c8tznkuvunUhpSpZiZUAIm8z8bf5hlElHQ7SQeZ5Fl2Bd7dyOWKnqWV6ieIccsRzpP78XfFAdM8Cp1Hms104n40FnoSI5pdO4mxWVEebIEtZC0LEe/cW+lxM8c9Bsqp1Owzw2J7rOelOUh3nGHM0lUHwu3SonMN8/913/3t7WO20lYx2+YGJI9liVTOLjryc7Bz1YeRVV0pdpGadSWGkTjNe3/JGkI9NoTA5+x6Xg9Y54qR6JOA8YZXQg=&x=1");
        // MIOPage page = new MIOPage(
        // "http://click.search123.uk.com/cgi-bin/clickthru.cgi?EI=02688&Q=samsung+s8500+wave&NGT=pDKLE8RD8FvUBNvAfEv+xjLcdmNBZmiMFNZTHXILNOcl/aVhYpvXm9M+CuGSzabnyqxXPu/WtpifeKSPNCKVq1VejEFG1T0OMv5W4FGrOOBEvGOHgftJTakep/ELYjxP8AE9TNkAXAyLK0JdshN28ZbXpzYj/lr7+TEpHkqmmx4gfsW4iyIMzk7b2h8oiTrk24ILOcsDuXKY034Jq3wOrR1YIEBShYGMbrGIpFFMm6V/uZmqBLC+xQOEAKgbY+Pq/8JPUUNRPRlwYmy7vTqGvxSaxaZeZZEEUJ4/BiXVjm6NbFXnKX+9jiLVEK6jOh4lYf3hYf8g1ORg+O7Y6yV2tFzAWuAS+y79e77MEF6e56WBxXZIpDUgjp/92evDreDADkEu4WxyywXpAtPajH8CZmf7OW+/gcXJmOHjAbPSDFUsd6XQIqFUQG/YguIlfbjyyXx5kJctY6ZEI0bszS4WUUFS9NREU689rEHTwaJ8ccgxcI56+15rU8dw2DYEZ5Y1xQfKD0Mq9N4gqXyJPNGO/YlCJQA48xdeJ3hTVIDN4p5PqjsWUfJitjlSyQYsxZ4u6HmMhiawh3AM7hh1D2c8tznkuvunUhpSpZiZUAIm8z8bf5hlElHQ7SQeZ5Fl2Bd7dyOWKnqWV6ieIccsRzpP78XfFAdM8Cp1Hms104n40FnoSI5pdO4mxWVEebIEtZC0LEe/cW+lxM8c9Bsqp1Owzw2J7rOelOUh3nGHM0lUHwu3SonMN8/913/3t7WO20lYx2+YGJI9liVTOLjryc7Bz1YeRVV0pdpGadSWGkTjNe3/JGkI9NoTA5+x6Xg9Y54qR6JOA8YZXQg=&x=1",
        // content);

        // String content = crawler.download("http://s8500.samsungmobile.de/");
        // MIOPage page = new MIOPage("http://s8500.samsungmobile.de/", content);

        String content = crawler
                .download("http://www.moviesklix.com/2010/02/samsung-wave-s8500-pircie-in-india-samsung-wave-s8500-review-specifications/");
        MIOPage page = new MIOPage(
                "http://www.moviesklix.com/2010/02/samsung-wave-s8500-pircie-in-india-samsung-wave-s8500-review-specifications/",
                content);

        Concept headphoneConcept = new Concept("mobilePhone");
        // Entity headphone1 = new Entity("Razer Megalodon", headphoneConcept);
        Entity headphone2 = new Entity("samsung s8500 wave", headphoneConcept);
        // headphoneConcept.addEntity(headphone1);
        // headphoneConcept.addEntity(headphone2);
        // analyzer.extractMIOs(mioPages, headphone2);
        FlashExtractor flashEx = new FlashExtractor();
        List<MIO> resultList = flashEx.extractFlashObjects(page, headphone2);

        for (MIO mio : resultList) {
            System.out.println(mio.getDirectURL() + "  " + mio.getFindPageURL() + " TRUST: " + mio.getTrust());
        }

    }

}
