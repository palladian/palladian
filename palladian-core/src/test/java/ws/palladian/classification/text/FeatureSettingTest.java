package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class FeatureSettingTest {

    @Test
    public void testBackwardsCompatibility() throws FileNotFoundException, IOException {
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(5, 7).caseSensitive().characterPadding().create();
        FeatureSetting deserializedFeatureSetting = FileHelper.deserialize(ResourceHelper
                .getResourcePath("/model/testFeatureSetting_v1.ser"));
        assertEquals(featureSetting, deserializedFeatureSetting);
    }

}
