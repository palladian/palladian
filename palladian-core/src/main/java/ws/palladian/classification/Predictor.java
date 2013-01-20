//package ws.palladian.classification;
//
//import java.util.List;
//
//import ws.palladian.processing.features.FeatureVector;
//
///**
// * <p>
// * A predictor uses a trained model to predict a continuous value.
// * </p>
// * 
// * @author Philipp Katz
// * @author David Urbansky
// * 
// * @param <M> The type of the model that is used to predict an outcome.
// */
//public interface Predictor<M extends Model> {
//
//    M train(List<Instance> instances);
//
//    // FIXME activate method below
//    // M train(Dataset dataset);
//
//    double predict(FeatureVector vector, M model);
//
//}