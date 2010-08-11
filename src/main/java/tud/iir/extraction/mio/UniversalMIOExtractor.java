/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tud.iir.knowledge.Entity;

/**
 * The Class UniversalMIOExtractor is a context-based MIO-Extractor.
 * 
 */
public class UniversalMIOExtractor extends GeneralAnalyzer {

    /**
     * Extract all MIOs.
     * 
     * @param mioPage the mioPage
     * @param entity the entity
     * @return the list
     */
    public List<MIO> extractAllMIOs(final MIOPage mioPage, final Entity entity) {

        final List<String> relevantMIOTypes = InCoFiConfiguration.getInstance().getMIOTypes();
        List<MIO> mioList = new ArrayList<MIO>();

        if (relevantMIOTypes.contains("flash")) {
            final FlashExtractor flashExtractor = new FlashExtractor();
            mioList.addAll(flashExtractor.extractMIOsByType(mioPage, entity));
        }

        if (relevantMIOTypes.contains("silverlight")) {
            final SilverlightExtractor slExtractor = new SilverlightExtractor();
            mioList.addAll(slExtractor.extractMIOsByType(mioPage, entity));
        }

        if (relevantMIOTypes.contains("applet")) {
            final AppletExtractor appletExtractor = new AppletExtractor();
            mioList.addAll(appletExtractor.extractMIOsByType(mioPage, entity));
        }

        if (relevantMIOTypes.contains("quicktime")) {
            final QuicktimeExtractor qtExtractor = new QuicktimeExtractor();
            mioList.addAll(qtExtractor.extractMIOsByType(mioPage, entity));

        }
        if (relevantMIOTypes.contains("html5canvas")) {
            final HTML5CanvasExtractor canvasExtractor = new HTML5CanvasExtractor();
            mioList.addAll(canvasExtractor.extractMIOsByType(mioPage, entity));
        }

        mioList = removeMIODuplicates(mioList);

        final List<String> tempMioList = new ArrayList<String>();

        for (MIO mio : mioList) {

            if (!tempMioList.contains(mio.getDirectURL())) {
                tempMioList.add(mio.getDirectURL());
            }
        }

        // Calculate Features and Interactivity
        mioList = calcFeaturesAndInteractivity(mioList, mioPage, entity);

        return mioList;
    }

    /**
     * Removes the MIO-Duplicates.
     * 
     * @param mioList the list of MIOs
     * @return the list without duplicates
     */
    private List<MIO> removeMIODuplicates(final List<MIO> mioList) {

        final List<MIO> resultList = new ArrayList<MIO>();
        final Map<String, MIO> mioMap = new HashMap<String, MIO>();

        MIO targetMIO;
        MIO slaveMIO;

        for (MIO mio : mioList) {
            if (mioMap.containsKey(mio.getDirectURL())) {

                // merge
                final MIO existingMIO = mioMap.get(mio.getDirectURL());
                if (existingMIO.isDedicatedPage()) {
                    targetMIO = existingMIO;
                    slaveMIO = mio;
                } else {
                    targetMIO = mio;
                    slaveMIO = existingMIO;
                }

                final Map<String, List<String>> targetInfoMap = targetMIO.getInfos();
                final Map<String, List<String>> infoMap = slaveMIO.getInfos();
                for (String info : infoMap.keySet()) {
                    if (!targetInfoMap.containsKey(info)) {
                        targetInfoMap.put(info, infoMap.get(info));
                    }

                }
                targetMIO.setInfos(targetInfoMap);

            } else {
                mioMap.put(mio.getDirectURL(), mio);
            }

        }

        for (Entry<String, MIO> mio : mioMap.entrySet()) {
            resultList.add(mio.getValue());
        }

        return resultList;
    }

    /**
     * Calculate trust.
     * 
     * @param retrievedMIOs the retrieved MIOs
     * @param mioPage the mio page
     * @param entity the entity
     * @return the list
     */
    private List<MIO> calcFeaturesAndInteractivity(final List<MIO> retrievedMIOs, final MIOPage mioPage,
            final Entity entity) {

        final boolean analyzeSWFContent = InCoFiConfiguration.getInstance().analyzeSWFContent;
        final MIOContextAnalyzer contextAnalyzer = new MIOContextAnalyzer(entity, mioPage);
        SWFContentAnalyzer swfContentAnalyzer = null;
        if (analyzeSWFContent) {
            swfContentAnalyzer = new SWFContentAnalyzer();
        }

        final MIOInteractivityAnalyzer interactivityAnalyzer = new MIOInteractivityAnalyzer();

        for (MIO mio : retrievedMIOs) {

            // first initialize all features
            mio.initializeFeatures();

            contextAnalyzer.setFeatures(mio);

            // analyze content of SWF-Files only
            if (analyzeSWFContent && mio.getMIOType().equalsIgnoreCase("flash")) {

                swfContentAnalyzer.analyzeContentAndSetFeatures(mio, entity);
            }
            // calculate Interactivity
            interactivityAnalyzer.setInteractivityGrade(mio, mioPage);
            // reset MIO-Infos for saving memory
            mio.resetMIOInfos();
        }

        return retrievedMIOs;
    }

}
