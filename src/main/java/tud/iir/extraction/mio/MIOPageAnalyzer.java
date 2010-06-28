/**
 * 
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

/**
 * The MIOPageAnalyzer analyzes MIOPages for relevant MIOs and try to extract them.
 * 
 */
public class MIOPageAnalyzer extends GeneralAnalyzer {

    // private Entity entity;

    // final String[] nameBlackList = { "footer", "banner", "ticker", "ads", "youtube", "expressinstall" };

    /**
     * Extract mios.
     * 
     * @param mioPages the mio pages
     * @param entity the entity
     * @return the map
     */
    public Map<String, MIO> extractMIOs(final List<MIOPage> mioPages, final Entity entity) {

        // List<MIO> extractedMIOs = new ArrayList<MIO>();
        final Map<String, MIO> cleanedMIOs = new HashMap<String, MIO>();

        for (MIOPage mioPage : mioPages) {

            // find swfs by object tag
            final UniversalMIOExtractor mioEx = new UniversalMIOExtractor();
            final List<MIO> mios = mioEx.extractAllMIOs(mioPage, entity);

            // find swfs from free JS/Comments
            // mios.addAll(extractSWFFromComments(mioPage));

            // TODO: deduplicate and trust-calculating
            for (MIO mio : mios) {
                // String entityName = entity.getName();
                // String[] nameArray = entityName.split(" ");
                // for (String name : nameArray) {
                // if (isNotBlacklisted(mio.getDirectURL()) && isNotBlacklisted(mio.getFindPageURL())) {
                // if (isEntityRelevant(mio.getDirectURL())) {
                // mio.setTrust(1);
                // }
                final MIO tempMIO = cleanedMIOs.get(mio.getDirectURL());
                if (tempMIO == (null)) {
                    cleanedMIOs.put(mio.getDirectURL(), mio);
                } else {
                    final Map<String, List> tempInfos = tempMIO.getInfos();
                    if (mio.getInfos().size() > tempInfos.size()) {
                        // TODO: more detailed comparison - trust,
                        // dptrust, which infos are different and so on
                        cleanedMIOs.put(mio.getDirectURL(), mio);
                    }
                }

                // }
                // }
            }
        }
        // System.out.println(cleanedMIOs.toString());

        return cleanedMIOs;
    }

    // private boolean isEntityRelevant(String url) {
    //
    // SearchWordMatcher swm = new SearchWordMatcher(entity.getName());
    // if (swm.containsSearchWordOrMorphs(url)) {
    // return true;
    // }
    //
    // return false;
    // }

    /**
     * Checks if is not blacklisted.
     * 
     * @param url the url
     * @return true, if is not blacklisted
     */
    // private boolean isNotBlacklisted(String url) {
    // for (String blackWord : nameBlackList) {
    // if (url.contains(blackWord)) {
    // return false;
    // }
    // }
    // return true;
    // }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(final String[] args) {
        final List<MIOPage> mioPages = new ArrayList<MIOPage>();
        final Crawler crawler = new Crawler();
        // String
        // content=crawler.download("http://www2.razerzone.com/Megalodon/");
        // MIOPage page = new MIOPage("http://www2.razerzone.com/Megalodon/",
        // content);

        // String content = crawler
        // .download("http://www.sennheiser.com/flash/HD_800_2/DE/base.html");
        // MIOPage page = new MIOPage(
        // "http://www.sennheiser.com/flash/HD_800_2/DE/base.html",
        // content);

        final String content = crawler.download("http://www.sennheiser.com/3d-view/hd_800/index.html");
        final MIOPage page = new MIOPage("http://www.sennheiser.com/3d-view/hd_800/index.html", content);

        // String content = crawler
        // .download("http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE");
        // MIOPage page = new MIOPage(
        // "http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE",
        // content);

        mioPages.add(page);
        final MIOPageAnalyzer analyzer = new MIOPageAnalyzer();
        final Concept headphoneConcept = new Concept("headphone");
        final Entity headphone1 = new Entity("Razer Megalodon", headphoneConcept);
        final Entity headphone2 = new Entity("Sennheiser HD800", headphoneConcept);
        headphoneConcept.addEntity(headphone1);
        headphoneConcept.addEntity(headphone2);
        analyzer.extractMIOs(mioPages, headphone2);

    }

}
