package ws.palladian.preprocessing.featureextraction;

import java.util.Set;

public interface TermCorpus {

    void addTermsFromDocument(Set<String> terms);

    void serialize(String filePath);

    double getDf(String term);

    int getNumDocs();

}