/**
 * The FlashExtractor extracts Flash-MIOs (SWF).
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

public class FlashExtractor extends AbstractMIOTypeExtractor {

    /** The mioType. */
    private static String mioType = "flash";

    /** The mioPage. */
    private transient MIOPage mioPage = null;

    /** The entity. */
    private transient Entity entity = null;

    /** The modified mioPageContent (without relevant tags). */
    private transient String modMioPageContent;

    /** The regular expression for url extraction. */
    private static String regExp = "(\".[^\",]*\\.swf\")|(\".[^\",]*\\.swf\\?.[^\"]*\")";

    /** The flashVars string. */
    private static String fVString = "flashvars";

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#extractMIOsByType(tud.iir.extraction.mio.MIOPage,
     * tud.iir.knowledge.Entity)
     */
    @Override
    List<MIO> extractMIOsByType(final MIOPage mioPage, final Entity entity) {

        this.mioPage = mioPage;
        this.entity = entity;

        final List<MIO> mioList = new ArrayList<MIO>();
        final List<String> relevantTags = extractRelevantTags(mioPage.getContentAsString());
        // check if there are swf-Files out of tags left, e.g. in comments
        mioList.addAll(findOutOfTagMIOs(modMioPageContent));

        mioList.addAll(analyzeRelevantTags(relevantTags));
        return mioList;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#extractRelevantTags(java.lang.String)
     */
    @Override
    final List<String> extractRelevantTags(final String mioPageContent) {

        modMioPageContent = "";

        final List<String> relevantTags = new ArrayList<String>();

        // extract all <object>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(mioPageContent, "object"));

        // remove the object-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(mioPageContent, "object");

        // extract all remaining <embed>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(modMioPageContent, "embed"));

        // remove all <embed>-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(modMioPageContent, "embed");

        // extract all <script>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(modMioPageContent, "script"));

        // remove all <script>-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(modMioPageContent, "script");

        return relevantTags;
    }

    /**
     * Find MIOs that are not inside a relevant tag (e.g. in comments).
     * 
     * @param mioPageContent the MIOPageContent
     * @return the list
     */
    private List<MIO> findOutOfTagMIOs(final String mioPageContent) {
        final List<MIO> flashMIOs = new ArrayList<MIO>();

        if (mioPageContent.toLowerCase(Locale.ENGLISH).contains(".swf")) {
            final List<MIO> furtherSWFs = extractMioURL(mioPageContent, mioPage,
                    "(\".[^\",;]*\\.swf\")|(\".[^\",;]*\\.swf\\?.[^\"]*\")", entity, mioType);
            flashMIOs.addAll(furtherSWFs);
        }
        return flashMIOs;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#analyzeRelevantTags(java.util.List)
     */
    @Override
    List<MIO> analyzeRelevantTags(final List<String> relevantTags) {
        final List<MIO> retrievedMIOs = new ArrayList<MIO>();
        final List<MIO> tempMIOs = new ArrayList<MIO>();

        // final List<String> altText = new ArrayList<String>();
        // StringBuffer altTextBuffer;
        // try to extract swf-file-URLs
        for (String relevantTag : relevantTags) {

            tempMIOs.clear();
            if (relevantTag.toLowerCase(Locale.ENGLISH).contains("swfobject")) {
                tempMIOs.addAll(checkSWFObject(relevantTag, mioPage));

            } else {
                // extract all swf-files from a relevant-tag
                tempMIOs.addAll(extractMioURL(relevantTag, mioPage, regExp, entity, mioType));

                // check for flashvars
                if (relevantTag.toLowerCase(Locale.ENGLISH).contains(fVString)) {

                    final List<String> flashVars = extractFlashVars(relevantTag);
                    if (!flashVars.isEmpty()) {
                        for (MIO mio : tempMIOs) {
                            adaptFlashVarsToURL(mio, flashVars);
                        }
                    }
                }
            }

            // extract ALT-Text from object and embed-tags and add to MIO-Infos
            if (!relevantTag.toLowerCase(Locale.ENGLISH).startsWith("<script")) {

                for (MIO mio : tempMIOs) {
                    final String tempAltText = extractALTTextFromTag(relevantTag);
                    if (tempAltText.length() > 2) {
                        mio.setAltText(tempAltText);
                    }
                }
            }
            // extract surrounding Information(Headlines, TextContent) and add to MIO-infos
            for (MIO mio : tempMIOs) {
                extractSurroundingInfo(relevantTag, mioPage, mio);
            }
            retrievedMIOs.addAll(tempMIOs);
        }
        return retrievedMIOs;
    }

    /**
     * SpecialCheck for swf-object-embeddings.
     * 
     * @param relevantTag the relevant tag
     * @param mioPage the mioPage
     * @return the list of MIOs
     */
    private List<MIO> checkSWFObject(final String relevantTag, final MIOPage mioPage) {

        List<MIO> tempList = new ArrayList<MIO>();

        if (relevantTag.toLowerCase(Locale.ENGLISH).contains("swfobject.embedswf")
                || relevantTag.toLowerCase(Locale.ENGLISH).contains("new swfobject(")) {

            tempList = extractMioURL(relevantTag, mioPage, regExp, entity, mioType);

            // check for flashvars
            if (relevantTag.toLowerCase(Locale.ENGLISH).contains(fVString)) {
                final List<String> flashVars = extractFlashVars(relevantTag);

                if (!flashVars.isEmpty()) {
                    for (MIO mio : tempList) {
                        adaptFlashVarsToURL(mio, flashVars);
                    }
                }
            }
        }
        return tempList;
    }

    /**
     * Extract flashVars.
     * 
     * @param tagContent the tag content
     * @return the list of flashVars
     */
    private List<String> extractFlashVars(final String tagContent) {
        // System.out.println(tagContent);
        final List<String> flashVars = new ArrayList<String>();

        // extract forms like: var flashVars = {}
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

        // extract forms like: <param name="FlashVars" value="myURL=http://weblogs.adobe.com/">
        String regExp2 = "param name=\"flashvars\"[^>]*value=\".[^\"]+\"";
        Pattern pat2 = Pattern.compile(regExp2, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pat2.matcher(tagContent);
        while (matcher2.find()) {
            // result has the form: <param name="FlashVars" value="myURL=http://weblogs.adobe.com/">
            String result = matcher2.group();
            String pattern = "";
            if (result.contains(";")) {
                // if result has the comment or CDATA-form: flashObj = flashObj + '&lt;param name="flashVars"
                // value="xmlUrl=/us/consumer/detail/galleryXML.do?model_cd=CLP-770ND/XAA&amp;amp;disMod=L&amp;"
                pattern = "value=\".[^\"]*";
            } else {
                pattern = "value=\"[^>]*\"";
            }
            result = HTMLHelper.extractTagElement(pattern, result, "value=");
            if (result.length() > 0) {
                result = result.replaceAll(";", "");
                flashVars.add(result);
            }
        }

        // extract forms like: FlashVars="myURL=http://weblogs.adobe.com/"
        String regExp3 = "flashvars=\"[^\"]*\"";
        Pattern pat3 = Pattern.compile(regExp3, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pat3.matcher(tagContent);
        while (matcher3.find()) {
            String result = matcher3.group(0);
            String pattern = "flashvars=\"[^\"]*\"";
            result = HTMLHelper.extractTagElement(pattern, result, "flashvars=");
            if (result.length() > 0) {
                flashVars.add(result);
            }
        }

        // extract forms like: flashvars.country = "us";
        String regExp4 = "flashvars\\..[^;]*;";
        final Pattern pat4 = Pattern.compile(regExp4, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher matcher4 = pat4.matcher(tagContent);
        while (matcher4.find()) {
            String result = matcher4.group(0);
            result = result.replaceFirst("flashvars.", "");
            result = result.replaceAll("\"", "");
            result = result.replaceAll(";", "");
            result = result.replaceAll("\\s", "");
            if (result.length() > 0) {
                flashVars.add(result);
            }
        }
        return flashVars;
    }

    /**
     * Adapt flashVars to the URL.
     * 
     * @param mio the MIO
     * @param flashVars the List of flashVars
     *
     */
    private void adaptFlashVarsToURL(final MIO mio, final List<String> flashVars) {

        final String url = mio.getDirectURL();

        final StringBuffer modURL = new StringBuffer();
        // Prepare directURL for adding flashVars
        modURL.append(url + "?");
        // add each flashVar
        for (String flashVar : flashVars) {
             // only concatenate not existing flashVars
            if (!modURL.toString().contains(flashVar)) {
                if (!flashVar.contains("\"")) {

                    final String tempURL = flashVar + "&";
                    modURL.append(tempURL);

                } else {
                    // adapt vars of style:
                    // videoRef:"/uk/assets/cms/6d8a4d0c-9659-4fa0-a62c-e884980879ee/Video.flv?p=081007_09:24"
                    String tempURL = flashVar.replaceAll("\"", "");
                    tempURL = tempURL.replaceFirst(":", "=");
                    modURL.append(tempURL + "&");
                }
            }
        }
        if (Crawler.isValidURL(modURL.toString(), false)) {
            mio.setDirectURL(modURL.toString());
         }
    }
}
