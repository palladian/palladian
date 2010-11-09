package tud.iir.classification.language;

import java.util.Set;

import tud.iir.classification.page.ClassifierManager;
import tud.iir.classification.page.TextClassifier;

public class PalladianLangDetect extends LanguageClassifier {

    TextClassifier palladianClassifier;

    /** We can specify which classes are possible and discard all others for the classification task. */
    Set<String> possibleClasses = null;

    public PalladianLangDetect(String modelPath) {
        palladianClassifier = ClassifierManager.load(modelPath);
    }

    public PalladianLangDetect() {
        palladianClassifier = ClassifierManager.load("data/models/palladianLanguageClassifier/LanguageClassifier.ser");
    }

    public Set<String> getPossibleClasses() {
        return possibleClasses;
    }

    public void setPossibleClasses(Set<String> possibleClasses) {
        this.possibleClasses = possibleClasses;
    }

    @Override
    public String classify(String text) {
        return palladianClassifier.classify(text, getPossibleClasses()).getAssignedCategoryEntryNames();
    }

}
