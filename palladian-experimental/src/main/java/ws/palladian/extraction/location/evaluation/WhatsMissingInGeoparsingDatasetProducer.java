package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.persistence.lucene.LuceneLocationSource;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;

/**
 * This code produces data in the proper format for evaluating the Palladian
 * location extractor using the Python evaluation scripts useed in “What's
 * Missing In Geoparsing?”; Milan Gritta, Mohammad Taher Pilehvar, Nut
 * Limsopatham, Nigel Collier, 2017.
 * 
 * See here for paper and code:
 * 
 * https://github.com/milangritta/WhatsMissingInGeoparsing
 * 
 * @author Philipp Katz
 */
class WhatsMissingInGeoparsingDatasetProducer {
    
    public static void main(String[] args) throws Exception {
        
        LocationSource source = LuceneLocationSource.open("/Users/pk/Desktop/Location_Lab_Revisited/Palladian_Location_Database_2022-04-19_13-45-08");
        
        PalladianLocationExtractor palladianHR = new PalladianLocationExtractor(source, new HeuristicDisambiguation());
        
        // attention: for fairness use a model which was trained only on TUD-Loc-2013
        // (i.e. which does not include LGL training data)
        QuickDtModel model = FileHelper.deserialize("/Users/pk/Desktop/Location_Lab_Revisited/disambiguation-models/locationDisambiguationModel-tudLoc2013-100trees.ser.gz");
        PalladianLocationExtractor palladianML = new PalladianLocationExtractor(source, new FeatureBasedDisambiguation(model));
        
        // ******************************** LGL dataset (589 pages) ********************************
        File lglXML = new File("/Users/pk/Desktop/Location_Lab_Revisited/WhatsMissingInGeoparsing/lgl.xml");
        
        List<String> lines1 = new ArrayList<>();
        ProgressReporter progress1 = new ProgressMonitor(589);
        LocalGlobalLexiconReader.parse(lglXML, document -> process(palladianHR, lines1, document, progress1));
        FileHelper.writeToFile("/Users/pk/Desktop/Location_Lab_Revisited/WhatsMissingInGeoparsing/data/lgl_palladian_hr.txt", lines1);

        List<String> lines2 = new ArrayList<>();
        ProgressReporter progress2 = new ProgressMonitor(589);
        LocalGlobalLexiconReader.parse(lglXML, document -> process(palladianML, lines2, document, progress2));
        FileHelper.writeToFile("/Users/pk/Desktop/Location_Lab_Revisited/WhatsMissingInGeoparsing/data/lgl_palladian_ml.txt", lines2);
        
        // ******************************** WikiToR dataset (5,000 pages) ********************************
        File wikiTorXML = new File("/Users/pk/Desktop/Location_Lab_Revisited/WhatsMissingInGeoparsing/WikToR(SciPaper).xml");
        
        List<String> lines3 = new ArrayList<>();
        ProgressReporter progress3 = new ProgressMonitor(5000);
        WikiToRDatasetReader.parse(wikiTorXML, document -> process(palladianHR, lines3, document, progress3));
        FileHelper.writeToFile("/Users/pk/Desktop/Location_Lab_Revisited/WhatsMissingInGeoparsing/data/wiki_palladian_hr.txt", lines3);
        
        List<String> lines4 = new ArrayList<>();
        ProgressReporter progress4 = new ProgressMonitor(5000);
        WikiToRDatasetReader.parse(wikiTorXML, document -> process(palladianML, lines4, document, progress4));
        FileHelper.writeToFile("/Users/pk/Desktop/Location_Lab_Revisited/WhatsMissingInGeoparsing/data/wiki_palladian_ml.txt", lines4);
    }

    private static void process(PalladianLocationExtractor extractor, List<String> lines, LocationDocument doc, ProgressReporter progress) {
        progress.increment();
        List<LocationAnnotation> annotations = extractor.getAnnotations(doc.getText());
        List<String> items = new ArrayList<>();
        for (LocationAnnotation annotation : annotations) {
            String locationName = annotation.getLocation().getPrimaryName();
            String matchedName = annotation.getValue();
            Double lat = annotation.getLocation().getCoords().map(GeoCoordinate::getLatitude).orElse(null);
            Double lng = annotation.getLocation().getCoords().map(GeoCoordinate::getLongitude).orElse(null);
            int start = annotation.getStartPosition();
            int end = annotation.getEndPosition();
            String item = locationName + ",," + matchedName + ",," + lat + ",," + lng + ",," + start + ",," + end;
            items.add(item);
        }
        lines.add(String.join("||", items));
    }
}
