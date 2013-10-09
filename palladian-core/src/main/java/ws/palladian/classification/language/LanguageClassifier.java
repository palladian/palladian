package ws.palladian.classification.language;

import ws.palladian.helper.constants.Language;

public interface LanguageClassifier {

    Language classify(String text);

}
