/**
 * Created on 08.07.2013 22:58:39
 */
package ws.palladian.classification.featureselection;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.helper.collection.InverseFilter;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * Tests whether the FeatureRanker produce the desired results.
 * </p>
 * 
 * @author Klemens Muthmann
 * 
 */
public class FeatureRankerIT extends TestCase {
    
    private static final String DATASET_SAMPLE = "/locationIT/datasetSample.csv";
    
    @Test
    public void testChiSquaredOnLocationFeatures() throws Exception {
        String csvFilePath = this.getClass().getResource(DATASET_SAMPLE).getFile();
        List<Trainable> dataset = ClassificationUtils.readCsv(csvFilePath, true);

        FeatureRanker ranker = new ChiSquaredFeatureRanker(new AverageMergingStrategy());
        FeatureRanking featureRanking = ranker.rankFeatures(dataset);
        System.out.println(featureRanking);
    }
}
