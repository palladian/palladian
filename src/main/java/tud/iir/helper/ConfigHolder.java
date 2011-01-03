package tud.iir.helper;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class ConfigHolder {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ConfigHolder.class);

    /** The path under which the Palladian configuration file must lie. */
    private static final String CONFIG_PATH = "config/palladian.properties";

    /** Configs for Palladian are held in the config field. */
    private PropertiesConfiguration config = null;

    static class SingletonHolder {
        static ConfigHolder instance = new ConfigHolder();
    }

    public static ConfigHolder getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * This class is a singleton and therefore the constructor is private.
     */
    private ConfigHolder() {
        try {
            setConfig(new PropertiesConfiguration(CONFIG_PATH));
        } catch (ConfigurationException e) {
            LOGGER.warn("Palladian configuration under " + CONFIG_PATH + " could not be loaded completely: "
                    + e.getMessage());
        }
    }

    /**
     * Return the value of the field with the specifield field name.
     * 
     * @param fieldName The name of the field for which a value should be retrieved.
     * @return The value of the field as Object since we don't know the type.
     */
    public Object getField(String fieldName) {
        return config.getProperty(fieldName);
    }

    public void setConfig(PropertiesConfiguration config) {
        this.config = config;
    }

    public PropertiesConfiguration getConfig() {
        return config;
    }

}
