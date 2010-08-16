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

import tud.iir.knowledge.Entity;

/**
 * The MIOPageAnalyzer analyzes MIOPages for relevant MIOs and try to extract them.
 * 
 */
public class MIOPageAnalyzer{

    // private double trustLimit = 2;

    /**
     * Extract MIOs.
     * 
     * @param mioPages the mioPages
     * @param entity the entity
     * @return the map
     */
    public Map<String, MIO> extractMIOs(final List<MIOPage> mioPages, final Entity entity) {

        // List<MIO> extractedMIOs = new ArrayList<MIO>();
        final Map<String, MIO> cleanedMIOs = new HashMap<String, MIO>();
        final List<MIO> mios = new ArrayList<MIO>();
        // int threadCount=0;
        // System.out.println("Anzahl mioPages: " + mioPages.size());
        final UniversalMIOExtractor mioEx = new UniversalMIOExtractor();
        
        for (MIOPage mioPage : mioPages) {

            // final ThreadGroup mioThreadGroup = new ThreadGroup("mioThreadGroup");
            // final Thread mioThread = new MIOExtractorAndFeaturesCalcThread(mioThreadGroup, mioPage.getTitle() +
            // "MIOExtractionThread",mios, mioPage, entity);
            //
            // mioThread.start();
            // // threadCount++;
            // //
            // try {
            // mioThread.join();
            // threadCount++;
            //
            // } catch (InterruptedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            // while (threadCount >= 3) {
            // ThreadHelper.sleep(10000);
            // }

            // extract MIOs and calculate features         
            mios.addAll(mioEx.extractAllMIOs(mioPage, entity));

        }

        // System.out.println("miosSize: " + mios.size());
        // System.exit(1);

        // System.out.println("Anzahl MIOs vor Dublettenerkennung: " + mios.size());

        for (MIO mio : mios) {

            // first merge youtube-doubles
            // boolean isYouTube = false;
            // if (mio.getFindPageURL().contains("youtube")) {
            // if (!youtubeMIOs.contains(mio.getFindPageURL())) {
            // youtubeMIOs.add(mio.getFindPageURL());
            //
            // } else {
            // isYouTube = true;
            // }
            // }

            final MIO existingMIO = cleanedMIOs.get(mio.getDirectURL());

//            if (true/* !isYouTube */) {
                if (existingMIO == (null)) {
                    cleanedMIOs.put(mio.getDirectURL(), mio);
                } else {
                    // the case that a MIO with this directURL was already found
                    // final Map<String, List<String>> tempInfos = existingMIO.getInfos();
                   final Map<String, Double> mergedFeatures = mergeMIOFeatures(existingMIO, mio);

                    if (mio.getTrust() > existingMIO.getTrust()) {

                        mio.setFeatures(mergedFeatures);
                        cleanedMIOs.put(mio.getDirectURL(), mio);
                    } else {
                        existingMIO.setFeatures(mergedFeatures);
                        cleanedMIOs.put(existingMIO.getDirectURL(), existingMIO);
                    }
                }
//            }

        }
        // System.out.println("Anzahl MIOs nach Dublettenerkennung: " + cleanedMIOs.size());

        // MIOComparator mioComp = new MIOComparator();
        // Set<MIO> mioSet = new HashSet<MIO>();
        // for (Entry<String, MIO> mio : cleanedMIOs.entrySet()) {
        // mioSet.add(mio.getValue());
        // }

        // detectRolePages(sortedMIOs, entity);

        return cleanedMIOs;
    }

    // private void detectRolePages(Set<MIO> sortedMIOs, Entity entity) {
    // RolePageDetector rpDetector = new RolePageDetector(entity);
    // rpDetector.detectRolePages(sortedMIOs);
    // }

    /**
     * Merge MIO-Features.
     * 
     * @param mio1 the mio1
     * @param mio2 the mio2
     * @return the map
     */
    private Map<String, Double> mergeMIOFeatures(final MIO mio1, final MIO mio2) {

        final Map<String, Double> mergedFeaturesMap = new HashMap<String, Double>();

        final Map<String, Double> featuresMap1 = mio1.getFeatures();
        final Map<String, Double> featuresMap2 = mio2.getFeatures();

        // System.out.println(featuresMap1.toString());
        // System.out.println(featuresMap2.toString());

        for (String featureName : featuresMap1.keySet()) {
            final double feature1 = featuresMap1.get(featureName);
            final double feature2 = featuresMap2.get(featureName);
            if (feature1 >= feature2) {
                mergedFeaturesMap.put(featureName, feature1);
            } else {
                mergedFeaturesMap.put(featureName, feature2);
            }
        }

        return mergedFeaturesMap;
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(final String[] args) {
        // final List<MIOPage> mioPages = new ArrayList<MIOPage>();
        // final Crawler crawler = new Crawler();
        // // String
        // // content=crawler.download("http://www2.razerzone.com/Megalodon/");
        // // MIOPage page = new MIOPage("http://www2.razerzone.com/Megalodon/",
        // // content);
        //
        // // String content = crawler
        // // .download("http://www.sennheiser.com/flash/HD_800_2/DE/base.html");
        // // MIOPage page = new MIOPage(
        // // "http://www.sennheiser.com/flash/HD_800_2/DE/base.html",
        // // content);
        //
        // final String content = crawler.download("http://www.sennheiser.com/3d-view/hd_800/index.html");
        // final MIOPage page = new MIOPage("http://www.sennheiser.com/3d-view/hd_800/index.html", content);
        //
        // // String content = crawler
        // // .download("http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE");
        // // MIOPage page = new MIOPage(
        // // "http://www.canon-europe.com/z/pixma_tour/de/mp990/swf/main.html?WT.ac=CCI_PixmaTour_MP990_DE",
        // // content);
        //
        // mioPages.add(page);
        // final MIOPageAnalyzer analyzer = new MIOPageAnalyzer();
        // final Concept headphoneConcept = new Concept("headphone");
        // final Entity headphone1 = new Entity("Razer Megalodon", headphoneConcept);
        // final Entity headphone2 = new Entity("Sennheiser HD800", headphoneConcept);
        // headphoneConcept.addEntity(headphone1);
        // headphoneConcept.addEntity(headphone2);
        // analyzer.extractMIOs(mioPages, headphone2);

    }

}
