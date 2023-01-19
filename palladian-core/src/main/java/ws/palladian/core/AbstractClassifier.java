package ws.palladian.core;

public abstract class AbstractClassifier<M extends Model> implements Classifier<M> {

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
