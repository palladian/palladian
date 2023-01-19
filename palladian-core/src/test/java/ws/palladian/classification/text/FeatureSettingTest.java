package ws.palladian.classification.text;

import org.junit.Test;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

public class FeatureSettingTest {

    @Test
    public void testBackwardsCompatibility() throws FileNotFoundException, IOException {
        // FeatureSetting featureSetting = FeatureSettingBuilder.chars(5, 7).caseSensitive().characterPadding().create();
        FeatureSetting deserializedFeatureSetting = FileHelper.deserialize(ResourceHelper.getResourcePath("/model/testFeatureSetting_v1.ser"));
        // assertEquals(featureSetting, deserializedFeatureSetting);
        assertEquals(deserializedFeatureSetting.getMinNGramLength(), 5);
        assertEquals(deserializedFeatureSetting.getMaxNGramLength(), 7);
        assertTrue(deserializedFeatureSetting.isCaseSensitive());
        assertTrue(deserializedFeatureSetting.isCharacterPadding());
        // properties added later, should be null / false
        assertNull(deserializedFeatureSetting.getLanguage());
        assertFalse(deserializedFeatureSetting.isStem());
        assertFalse(deserializedFeatureSetting.isRemoveStopwords());
    }

}
