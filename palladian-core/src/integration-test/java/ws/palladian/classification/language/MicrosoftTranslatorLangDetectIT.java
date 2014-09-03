package ws.palladian.classification.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.helper.constants.Language;
import ws.palladian.integrationtests.ITHelper;

public class MicrosoftTranslatorLangDetectIT {

    private static String testClientId;
    private static String testClientSecret;

    @BeforeClass
    public static void getAuthenticationData() {
        Configuration config = ITHelper.getTestConfig();
        testClientId = config.getString("api.azure.clientId");
        testClientSecret = config.getString("api.azure.clientSecret");
        assertTrue("palladian-test.properties must provide an Azure client ID", StringUtils.isNotBlank(testClientId));
        assertTrue("palladian-test.properties must provide an Azure client secret",
                StringUtils.isNotBlank(testClientSecret));
    }

    @Test
    public void testMicrosoftTranslator() {
        LanguageClassifier langDetect = new MicrosoftTranslatorLangDetect(testClientId, testClientSecret);
        assertEquals(Language.ENGLISH, langDetect.classify("The quick brown fox jumps over the lazy dog."));
        assertEquals(Language.GERMAN, langDetect.classify("Der schnelle braune Fuchs springt über den faulen Hund."));
        assertEquals(Language.FRENCH, langDetect.classify("Le vif renard brun saute par-dessus le chien paresseux."));
    }

}
