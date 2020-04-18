package ws.palladian.classifiers.cloudservices;

import java.io.File;
import java.util.List;

public interface ImageClassifier {
    List<String> classify(File image) throws Exception;

    void setMaxLabels(int maxLabels);
}
