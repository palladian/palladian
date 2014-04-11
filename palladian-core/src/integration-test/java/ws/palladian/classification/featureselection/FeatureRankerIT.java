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
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.InverseFilter;
import ws.palladian.helper.collection.RegexFilter;

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
    private List<Instance> dataset;
    
    @Before
    public void setUp() throws Exception {
//        String csvFilePath = this.getClass().getResource(DATASET_SAMPLE).getFile();
//        String csvFilePath = "/home/muthmann/location_disambiguation_1373234035433.csv";
        String csvFilePath = this.getClass().getResource(DATASET_SAMPLE).getFile();
        List<Instance> inputDataset = ClassificationUtils.readCsv(csvFilePath, true);
        dataset = ClassificationUtils.readCsv(csvFilePath, true);
        dataset = ClassificationUtils.filterFeatures(dataset, InverseFilter.create(new RegexFilter("marker=.*")));
        
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
