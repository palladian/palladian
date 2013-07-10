/**
 * Created on 08.07.2013 22:58:39
 */
package ws.palladian.classification.featureselection;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * Tests whether the FeatureRanker produce the desired results.
 * </p>
 * 
 * @author Klemens Muthmann
 * 
 */
public class FeatureRankerIT {
    
    private static final String DATASET_SAMPLE = "/locationIT/datasetSample.csv";
    /** The logger for this class. Configure it using /src/test/resources/log4j.properties*/
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureRankerIT.class);
    private List<Trainable> dataset;
    
    @Before
    public void setUp() throws Exception {
        String csvFilePath = this.getClass().getResource(DATASET_SAMPLE).getFile();
        dataset = ClassificationUtils.readCsv(csvFilePath, true);
        
//        List<Trainable> filteredDataset = CollectionHelper.newArrayList();
//        for(Trainable trainable:dataset) {
//            Instance filteredInstance = new Instance(trainable.getTargetClass());
//            for(Feature<?> feature:trainable.getFeatureVector().getAll()) {
//                if(feature.getName().equals("marker=road")) {
//                    filteredInstance.getFeatureVector().add(feature);
//                }
//            }
//            filteredDataset.add(filteredInstance);
//        }
    }
    
    @Test
    public void testChiSquaredOnLocationFeatures() throws Exception {
        FeatureRanker ranker = new ChiSquaredFeatureRanker(new AverageMergingStrategy());
        FeatureRanking featureRanking = ranker.rankFeatures(dataset);
        LOGGER.debug(featureRanking.toString());
    }
    
    @Test
    public void testInformationGainOnLocationFeatures() throws Exception {
        FeatureRanker ranker = new InformationGainFeatureRanker();
        FeatureRanking featureRanking = ranker.rankFeatures(dataset);
        LOGGER.debug(featureRanking.toString());
    }
}
