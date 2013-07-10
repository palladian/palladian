package ws.palladian.classification.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.constants.Language;

public class MicrosoftTranslatorLangDetectIT {

    private static final String TEST_CLIENT_ID = "palladian";
    private static final String TEST_CLIENT_SECRET = "w9oWOWg1ZyGw4nvRKGutpx5WpxS+IAsMPudXBvBRN2o";

    @Test
    public void testMicrosoftTranslator() {
        LanguageClassifier langDetect = new MicrosoftTranslatorLangDetect(TEST_CLIENT_ID, TEST_CLIENT_SECRET);
        assertEquals(Language.ENGLISH, langDetect.classify("The quick brown fox jumps over the lazy dog."));
        assertEquals(Language.GERMAN, langDetect.classify("Der schnelle braune Fuchs springt über den faulen Hund."));
        assertEquals(Language.FRENCH, langDetect.classify("Le vif renard brun saute par-dessus le chien paresseux."));
    }

}
