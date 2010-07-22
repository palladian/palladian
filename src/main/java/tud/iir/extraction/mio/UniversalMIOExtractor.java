/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.helper.HTMLHelper;
import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

/**
 * The Class UniversalMIOExtractor is a context-based MIO-Extractor.
 * 
 */
public class UniversalMIOExtractor extends GeneralAnalyzer {

    /** The entity. */
    private Entity entity;

    /** The mioPageCcontent. */
    private String mioPageContent;

    /** The modified mioPageContent (without relevant tags). */
    private String modMioPageContent;

    /** The mioPage. */
    private MIOPage mioPage;

    /** The Constant FLASH. */
    private static final String FLASH = "flash";

    /** The Constant APPLET. */
    private static final String APPLET = "applet";

    /**
     * Extract all MIOs.
     * 
     * @param mioPage the mioPage
     * @param entity the entity
     * @return the list
     */
    public List<MIO> extractAllMIOs(final MIOPage mioPage, final Entity entity) {
        final List<MIO> mioList = new ArrayList<MIO>();
        mioList.addAll(extractMIOsByType(mioPage, entity, FLASH));
        mioList.addAll(extractMIOsByType(mioPage, entity, APPLET));
        mioList.addAll(extractMIOsByType(mioPage, entity, "silverlight"));
        mioList.addAll(extractMIOsByType(mioPage, entity, "quicktime"));

        return mioList;
    }

    /**
     * Extract MIOs by type.
     * 
     * @param mioPage the mioPage
     * @param entity the entity
     * @param mioType the mio-type
     * @return the list
     */
    private List<MIO> extractMIOsByType(final MIOPage mioPage, final Entity entity, final String mioType) {
        this.entity = entity;
        this.mioPageContent = mioPage.getContent();
        this.mioPage = mioPage;

        List<MIO> mioList = new ArrayList<MIO>();
        final List<String> relevantTags = extractRelevantTags(mioType);
        mioList.addAll(findOutOfTagMIOs(modMioPageContent, mioType));
        mioList = analyzeRelevantTags(relevantTags, mioType);
        return mioList;
    }

    /**
     * Extract relevant tags, that could contain MIOs depending from mio-type.
     * <object>,<embed>,<script>,<applet>-tags
     * 
     * @param mioType the mio-type
     * @return the list of relevant tags
     */
    private List<String> extractRelevantTags(final String mioType) {

        modMioPageContent = mioPageContent;
        final List<String> relevantTags = new ArrayList<String>();

        // extract all <object>
        relevantTags.addAll(HTMLHelper.getConcreteTags(mioPageContent, "object"));

        // remove the object-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(mioPageContent, "object");

        // extract all remaining <embed>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(mioPageContent, "embed"));

        // remove all <embed>-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(mioPageContent, "embed");

        // extract all <script>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(mioPageContent, "script"));

        // remove all <script>-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(mioPageContent, "script");

        if (mioType.equals(APPLET)) {
            // extract all <object>
            relevantTags.addAll(HTMLHelper.getConcreteTags(mioPageContent, APPLET));

            // remove the object-tags
            modMioPageContent = HTMLHelper.removeConcreteHTMLTag(mioPageContent, APPLET);
        }

        return relevantTags;

    }

    /**
     * Analyze relevant tags.
     * 
     * @param relevantTags the relevant tags
     * @param mioType the mio-type
     * @return the list
     */
    private List<MIO> analyzeRelevantTags(final List<String> relevantTags, final String mioType) {

        List<MIO> retrievedMIOs = new ArrayList<MIO>();

        // try to extract swf-file-URLs
        for (String relevantTag : relevantTags) {

            final List<MIO> tempMIOs = new ArrayList<MIO>();

            if (mioType.equals(FLASH)) {

                if (relevantTag.toLowerCase(Locale.ENGLISH).contains("swfobject")) {

                    tempMIOs.addAll(checkSWFObject(relevantTag, mioPage));

                } else {
                    // extract all swf-files from a relevant-tag
                    tempMIOs.addAll(extractMioWithURL(relevantTag, mioPage, mioType));

                    // check for flashvars
                    if (relevantTag.toLowerCase(Locale.ENGLISH).contains("flashvars")) {
                        final List<String> flashVars = extractFlashVars(relevantTag);
                        if (!flashVars.isEmpty()) {
                            for (MIO mio : tempMIOs) {
                                mio = adaptFlashVarsToURL(mio, flashVars);
                                mio.addInfos("flashvars", flashVars);
                            }
                        }
                    }
                }
            } else {
                tempMIOs.addAll(extractMioWithURL(relevantTag, mioPage, mioType));
            }

            // extract ALT-Text from object and embed-tags and add to MIO-Infos
            if (!relevantTag.toLowerCase(Locale.ENGLISH).startsWith("<script")) {

                for (MIO mio : tempMIOs) {
                    final String tempAltText = extractALTTextFromTag(relevantTag);
                    final List<String> altText = new ArrayList<String>();
                    if (tempAltText.length() > 2) {
                        altText.add(tempAltText);
                        mio.addInfos("altText", altText);
                    }
                }

                // extract surrounding Information(Headlines, TextContent) and add to MIO-infos
                // final List<String> headlines = new ArrayList<String>();
                for (MIO mio : tempMIOs) {
                    mio = extractSurroundingInfos(relevantTag, mioPage, mio);
                    mio = extractXMLInfos(relevantTag, mio);
                }
            }

            retrievedMIOs.addAll(tempMIOs);
        }

        // Calculate Trust
        retrievedMIOs = calcTrustAndInteractivity(retrievedMIOs);

        return retrievedMIOs;
    }

    /**
     * Adapt flashVars to URL.
     * 
     * @param mio the mio
     * @param flashVars the flashVars
     * @return the MIO
     */
    private MIO adaptFlashVarsToURL(final MIO mio, final List<String> flashVars) {

        final String url = mio.getDirectURL();
        final StringBuffer modURL = new StringBuffer();
        for (String flashVar : flashVars) {
            if (!flashVar.contains("\"") && !modURL.toString().contains(flashVar)) {
                if (modURL.length() < 1) {
                    final String tempURL = url + "?" + flashVar;
                    modURL.append(tempURL);
                } else {
                    final String tempURL = modURL + "&" + flashVar;
                    modURL.append(tempURL);
                }
            }
            if (Crawler.isValidURL(modURL.toString(), false)) {
                mio.setDirectURL(modURL.toString());
            }

        }

        return mio;
    }

    /**
     * Extract swfURL out of concrete tag.
     * 
     * @param concreteTag the concrete tag
     * @param mioPage the mio page
     * @param mioType the mio type
     * @return the list
     */
    private List<MIO> extractMioWithURL(final String concreteTag, final MIOPage mioPage, final String mioType) {
        final List<MIO> resultList = new ArrayList<MIO>();
        // String regExp = "\".[^\"]*\\.swf\"";
        String regExp = "";
        if (mioType.equals(FLASH)) {
            regExp = "(\".[^\",]*\\.swf\")|(\".[^\",]*\\.swf\\?.[^\"]*\")";
        } else {
            if (mioType.equals(APPLET)) {
                regExp = "(\".[^\"]*\\.class\")|(\".[^\"]*\\.class\\?.[^\"]*\")";
            } else {
                if ("silverlight".equals(mioType)) {
                    regExp = "(\".[^\"]*\\.xap\")|(\".[^\"]*\\.xap\\?.[^\"]*\")";
                } else {
                    if ("quicktime".equals(mioType)) {
                        regExp = "(\".[^\"]*\\.mov\")|(\".[^\"]*\\.mov\\?.[^\"]*\")";
                    }
                }
            }
        }

        final Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // remove wrong-leading-attributes like name="140.swf"
        final String modConcreteTag = concreteTag.replaceAll("name=\".[^\"]*\"", "");

        final Matcher matcher = pattern.matcher(modConcreteTag);
        while (matcher.find()) {
            final String mioAdr = matcher.group(0).replaceAll("\"", "");
            // System.out.println("URL: "+ mioAdr);
            String mioURL = verifyURL(mioAdr, mioPage.getUrl());
            String testString = mioURL.toLowerCase(Locale.ENGLISH);
            if (testString.contains("expressinstall") || testString.contains("banner") || testString.contains("header")) {
                // dont generate new mio
            } else {
                if (mioURL.length() > 4 && mioURL.contains(".")) {
                    final MIO mio = new MIO(mioType, mioURL, mioPage.getUrl(), entity);
                    resultList.add(mio);
                }
            }

            // System.out.println(verifyURL(mioAdr, mioPage.getUrl()));
            // mio.setDirectURL(verifyURL(mioAdr, mioPage.getUrl()));

        }

        return resultList;
    }

    /**
     * Find MIOs that are not inside a relevant tag.
     * 
     * @param mioPageContent the mioPageContent without relevant tags
     * @param mioType the mio-type
     * @return the list of mios
     */
    private List<MIO> findOutOfTagMIOs(final String mioPageContent, final String mioType) {
        final List<MIO> flashMIOs = new ArrayList<MIO>();

        if (mioType.equals(FLASH) && mioPageContent.toLowerCase(Locale.ENGLISH).contains(".swf")) {

            final List<MIO> furtherSWFs = extractMioWithURL(mioPageContent, mioPage, mioType);
            // System.out.println("NOCH SWF ENTHALTEN! - " + mioPage.getUrl());
            // for (MIO mio : furtherSWFs) {
            // System.out.println(mio.getDirectURL());
            // }
            flashMIOs.addAll(furtherSWFs);

        }
        return flashMIOs;
    }

    /**
     * Extract Parameters of flashVars.
     * 
     * @param tagContent the content of the concrete tag
     * @return the list of flashVars
     */
    private List<String> extractFlashVars(final String tagContent) {
        final List<String> flashVars = new ArrayList<String>();

        // extract var flashVars = {}
        String regExp = "flashvars(\\s?)=(\\s?)\\{[^\\}]*\\};";
        final Pattern pat1 = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher matcher1 = pat1.matcher(tagContent);
        while (matcher1.find()) {
            // result has the form: flashvars = {cool};
            String result = matcher1.group(0);
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
        final Pattern pat2 = Pattern.compile(regExp2, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher matcher2 = pat2.matcher(tagContent);
        while (matcher2.find()) {
            // result has the form: <param name="FlashVars"
            // value="myURL=http://weblogs.adobe.com/">
            String result = matcher2.group();
            String pattern = "value=\"[^>]*\"";
            // System.out.println("---------" + result);
            result = extractElement(pattern, result, "value=");
            if (result.length() > 0) {
                flashVars.add(result);
            }

        }

        // extract FlashVars="myURL=http://weblogs.adobe.com/"
        String regExp3 = "flashvars=\"[^\"]*\"";
        Pattern pat3 = Pattern.compile(regExp3, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pat3.matcher(tagContent);
        while (matcher3.find()) {
            String result = matcher3.group(0);
            String pattern = "flashvars=\"[^\"]*\"";
            result = extractElement(pattern, result, "flashvars=");
            if (result.length() > 0) {
                flashVars.add(result);
            }
        }

        // extract flashvars.country = "us";
        String regExp4 = "flashvars\\..*[^;];";
        Pattern pat4 = Pattern.compile(regExp4, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher4 = pat4.matcher(tagContent);
        while (matcher4.find()) {
            String result = matcher4.group(0);
            result = result.replaceFirst("flashvars.", "");
            result = result.replaceAll("\"", "");
            result = result.replaceAll("\\s", "");
            if (result.length() > 0) {
                flashVars.add(result);
            }
        }

        // if (flashVars.size() > 0) {
        // // System.out.println("mehrere FlashVars: ");
        // for (String fVar : flashVars) {
        // System.out.println(fVar);
        //
        // }
        // System.out.println("____________________");
        //
        // }

        return flashVars;
    }

    /**
     * Special Check for swfObject.
     * 
     * @param relevantTag the relevant tag
     * @param mioPage the mio page
     * @return the mIO
     */
    private List<MIO> checkSWFObject(final String relevantTag, final MIOPage mioPage) {

        List<MIO> tempList = new ArrayList<MIO>();

        // String content = mioPage.getContent();
        if (relevantTag.toLowerCase(Locale.ENGLISH).contains("swfobject.embedswf")
                || relevantTag.toLowerCase(Locale.ENGLISH).contains("new swfobject(")) {

            tempList = extractMioWithURL(relevantTag, mioPage, FLASH);

            // only concentrate on one SWFURL of the relevantTag, because they
            // all are the same
            // if (tempList.size() > 0) {
            // mio = tempList.get(0);
            // }

            // check for flashvars
            if (relevantTag.toLowerCase(Locale.ENGLISH).contains("flashvars")) {
                List<String> flashVars = extractFlashVars(relevantTag);

                if (!flashVars.isEmpty()) {
                    for (MIO mio : tempList) {
                        mio = adaptFlashVarsToURL(mio, flashVars);
                        mio.addInfos("flashvars", flashVars);
                    }
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

                Pattern pat = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher matcher = pat.matcher(relevantTag);
                List<String> queryParamValues = new ArrayList<String>();
                while (matcher.find()) {
                    // result has the form: flashvars = {cool};
                    String result = matcher.group(0);
                    queryParamValues.add(result);

                }
                for (MIO mio : tempList) {
                    mio.addInfos("queryParamValues", queryParamValues);
                }

            }

        }

        // if (relevantTag.contains("new SWFObject(")){
        // System.out.println("contains new SWFObject(");
        // }
        return tempList;
    }

    /**
     * Calculate trust.
     * 
     * @param retrievedMIOs the retrieved MIOs
     * @return the list
     */
    private List<MIO> calcTrustAndInteractivity(final List<MIO> retrievedMIOs) {
        final MIOContextAnalyzer contextAnalyzer = new MIOContextAnalyzer(entity, mioPage);
        MIOContentAnalyzer contentAnalyzer = new MIOContentAnalyzer();
        MIOInteractivityAnalyzer interactivityAnalyzer = new MIOInteractivityAnalyzer();

        // System.out.println("MIO-Trust-Calculation!");

        for (MIO mio : retrievedMIOs) {

            contextAnalyzer.calculateTrust(mio);
            contextAnalyzer.setFeatures(mio);

            contentAnalyzer.analyzeContent(mio, entity);
            contentAnalyzer.calculateTrust(mio);
            interactivityAnalyzer.setInteractivityGrade(mio, mioPage);
            // reset MIO-Infos for saving memory
            mio.resetMIOInfos();
        }

        return retrievedMIOs;
    }

    /**
     * Extract swf from comments.
     * 
     * @param args the arguments
     * @return the list
     */
    // private List<MIO> extractSWFFromComments(MIOPage mioPage) {
    // List<MIO> resultList = new ArrayList<MIO>();
    // // StringHelper stringHelper = new StringHelper();
    // List<String> relevantTags = StringHelper.getConcreteTags(mioPage.getContent(), "<!--", "-->");
    // for (String relevantTag : relevantTags) {
    //
    // List<MIO> tempList = extractMIOWithURL(relevantTag, mioPage, "flash");
    // if (relevantTag.contains("flashvars")) {
    // List flashVars = extractFlashVars(relevantTag);
    // if (!flashVars.isEmpty()) {
    // for (MIO mio : tempList) {
    // mio.addInfos("flashvars", flashVars);
    // }
    // }
    //
    // }
    // resultList.addAll(tempList);
    //
    // }
    // return resultList;
    // }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(String[] args) {

        // Crawler crawler = new Crawler();
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

        // String content = crawler.download("http://s8500.samsungmobile.de/");
        // MIOPage page = new MIOPage("http://s8500.samsungmobile.de/", content);

        // String content = crawler
        // .download("http://www.canon-europe.com/z/pixma_tour/de/mp990/" +
        // "swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE");
        // MIOPage page = new MIOPage(
        // "http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html" +
        // "?WT.ac=CCI_PixmaTour_MP990_DE", content);

        // Concept headphoneConcept = new Concept("printer");
        // Entity headphone1 = new Entity("Razer Megalodon", headphoneConcept);
        // Entity headphone2 = new Entity("Canon MP990", headphoneConcept);
        // headphoneConcept.addEntity(headphone1);
        // headphoneConcept.addEntity(headphone2);
        // analyzer.extractMIOs(mioPages, headphone2);
        // UniversalMIOExtractor flashEx = new UniversalMIOExtractor();
        // List<MIO> resultList = flashEx.extractAllMIOs(page, headphone2);

        // for (MIO mio : resultList) {
        // System.out.println(mio.getDirectURL() + "  " + mio.getFindPageURL() + " TRUST: " + mio.getTrust());
        // }

    }

}
