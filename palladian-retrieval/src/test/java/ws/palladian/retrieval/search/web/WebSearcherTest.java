package ws.palladian.retrieval.search.web;

import java.io.FileNotFoundException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.io.ResourceHelper;

public abstract class WebSearcherTest {

    private PropertiesConfiguration config;

    protected PropertiesConfiguration getConfig() {
        if (config == null) {
            try {
                config = new PropertiesConfiguration(ResourceHelper.getResourceFile("palladian-test.properties"));
            } catch (ConfigurationException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return config;
    }

}
