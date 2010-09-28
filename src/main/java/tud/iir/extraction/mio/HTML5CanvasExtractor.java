/**
 * The HTML5CanvasExtractor extracts information about a HTML5Canvas-Object inside a page.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

import tud.iir.helper.HTMLHelper;
import tud.iir.knowledge.Entity;

public class HTML5CanvasExtractor extends AbstractMIOTypeExtractor {

    /** The mioType. */
    private static String mioType = "html5canvas";

    /** The mioPage. */
    private transient MIOPage mioPage;

    /** The entity. */
    private transient Entity entity;

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

        if (!relevantTags.isEmpty()) {
            mioList.addAll(analyzeRelevantTags(relevantTags));
        }
        return mioList;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#extractRelevantTags(java.lang.String)
     */
    @Override
    final List<String> extractRelevantTags(final String mioPageContent) {

        final List<String> relevantTags = new ArrayList<String>();

        // extract all <canvas>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(mioPageContent, "canvas"));

        return relevantTags;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#analyzeRelevantTags(java.util.List)
     */
    @Override
    final List<MIO> analyzeRelevantTags(final List<String> relevantTags) {

        final List<MIO> retrievedMIOs = new ArrayList<MIO>();
        final MIO mio = new MIO(mioType, mioPage.getUrl(), mioPage.getUrl(), entity);

        for (String relevantTag : relevantTags) {

            // extract ALT-Text from object and embed-tags and add to MIO-Infos
            final String tempAltText = extractALTTextFromTag(relevantTag);
            if (tempAltText.length() > 2) {
                mio.setAltText(tempAltText);
            }
            // extract surrounding Information(Headlines, TextContent) and add to MIO-infos
            extractSurroundingInfo(relevantTag, mioPage, mio);
        }
        retrievedMIOs.add(mio);
        return retrievedMIOs;
    }
}
