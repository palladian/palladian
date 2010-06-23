/**
 * The MIOPageAnalyzer analyzes MIOPages for relevant MIOs and try to extract them.
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

public class MIOPageAnalyzer extends GeneralAnalyzer {

//    private Entity entity;

    final String[] nameBlackList = { "footer", "banner", "ticker", "ads", "youtube" };

    /**
     * Extract mios.
     *
     * @param mioPages the mio pages
     * @param entity the entity
     * @return the map
     */
    public Map<String, MIO> extractMIOs(List<MIOPage> mioPages, Entity entity) {
//        this.entity = entity;
        // List<MIO> extractedMIOs = new ArrayList<MIO>();
        Map<String, MIO> cleanedMIOs = new HashMap<String, MIO>();

        // FileHelper filehelper = new FileHelper();

        for (MIOPage mioPage : mioPages) {

            // find swfs by object tag
            FlashExtractor flashEx = new FlashExtractor();
            List<MIO> mios = flashEx.extractFlashObjects(mioPage, entity);

            // find swfs from swfObject-embedding
            // mios.addAll(checkSWFObject(mioPage));

            // find swfs from free JS/Comments
            // mios.addAll(extractSWFFromComments(mioPage));

            // TODO: deduplicate and trust-calculating
            for (MIO mio : mios) {
                // String entityName = entity.getName();
                // String[] nameArray = entityName.split(" ");
                // for (String name : nameArray) {
                if (isNotBlacklisted(mio.getDirectURL()) && isNotBlacklisted(mio.getFindPageURL())) {
                    // if (isEntityRelevant(mio.getDirectURL())) {
                    // mio.setTrust(1);
                    // }
                    MIO tempMIO = cleanedMIOs.get(mio.getDirectURL());
                    if (tempMIO == (null)) {
                        cleanedMIOs.put(mio.getDirectURL(), mio);
                    } else {
                        Map<String, List> tempInfos = tempMIO.getInfos();
                        if (mio.getInfos().size() > tempInfos.size()) {
                            // TODO: more detailed comparison - trust,
                            // dptrust, which infos are different and so on
                            cleanedMIOs.put(mio.getDirectURL(), mio);
                        }
                    }

                    // }
                }
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
    private boolean isNotBlacklisted(String url) {
        for (String blackWord : nameBlackList) {
            if (url.contains(blackWord)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        List<MIOPage> mioPages = new ArrayList<MIOPage>();
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

        String content = crawler.download("http://www.sennheiser.com/3d-view/hd_800/index.html");
        MIOPage page = new MIOPage("http://www.sennheiser.com/3d-view/hd_800/index.html", content);

        // String content = crawler
        // .download("http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE");
        // MIOPage page = new MIOPage(
        // "http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE",
        // content);

        mioPages.add(page);
        MIOPageAnalyzer analyzer = new MIOPageAnalyzer();
        Concept headphoneConcept = new Concept("headphone");
        Entity headphone1 = new Entity("Razer Megalodon", headphoneConcept);
        Entity headphone2 = new Entity("Sennheiser HD800", headphoneConcept);
        headphoneConcept.addEntity(headphone1);
        headphoneConcept.addEntity(headphone2);
        analyzer.extractMIOs(mioPages, headphone2);

    }

}
