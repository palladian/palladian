/**
 * 
 */
package ws.palladian.classification.numeric;

import java.util.List;

import ws.palladian.classification.Model;
import ws.palladian.classification.NominalInstance;

/**
 * <p>
 * The model used by KNN classification algorithms. Like the {@link KnnClassifier}. 
 * </p>
 * 
 * @author Klemens Muthmann
 */
public final class KnnModel implements Model {

    /** Non-transient training instances. We need to save them as the instance based classifier depends on them. */
    private final List<NominalInstance> trainingInstances;

	public List<NominalInstance> getTrainingInstances() {
		return trainingInstances;
	}

	public KnnModel(List<NominalInstance> trainingInstances) {
    	super();
    	
    	this.trainingInstances = trainingInstances;
    }
}
