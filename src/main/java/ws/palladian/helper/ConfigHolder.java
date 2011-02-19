package ws.palladian.helper;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

/**
 * <p>
 * Holds the configuration of the framework. This configuration is obtained from an external file called
 * <tt>palladian.properties</tt>. The configuration is searched at the following places, in the specified order:</p>
 * 
 * <ol>
 * <li>Path specified by the environment variable <tt>PALLADIAN_HOME</tt>,</li>
 * <li>root of the classpath,</li>
 * <li>path <tt>config/palladian.properties</tt>.</li>
 * </ol>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 * 
 */
public final class ConfigHolder {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ConfigHolder.class);

    /**
     * <p>
     * The name of the palladian configuration properties file.
     * </p>
     */
    private static final String CONFIG_NAME = "palladian.properties";

    /** The path under which the Palladian configuration file must lie. */
    private static final String CONFIG_PATH = "config/" + CONFIG_NAME;

    /** Configs for Palladian are held in the config field. */
    private PropertiesConfiguration config = null;

    /**
     * <p>
     * Wrapper class for thread safe singleton handling. See Effective Java, item 48.
     * </p>
     * 
     * @author David Urbansky
     * 
     */
    static class SingletonHolder {
        /**
         * <p>
         * The single instance of the Palladian configuration.
         * </p>
         */
        private static ConfigHolder instance = new ConfigHolder();
    }

    /**
     * <p>
     * This method realizes the singleton design pattern by providing the one single instance of the
     * {@code ConfigHolder} class.
     * </p>
     * 
     * @return The one instance of {@code ConfigHolder}.
     */
    public static ConfigHolder getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * This class is a singleton and therefore the constructor is private.
     */
    private ConfigHolder() {
        try {
            File configFile = new File(CONFIG_PATH);
            File environmentConfig = new File(System.getenv("PALLADIAN_HOME") + "/" + CONFIG_PATH);
            URL resource = ConfigHolder.class.getResource("/" + CONFIG_NAME);

            PropertiesConfiguration propertiesConfiguration = null;
            if (environmentConfig.exists()) {
                LOGGER.debug("Try to load palladian.properties from Environment: "
                        + environmentConfig.getAbsolutePath());
                propertiesConfiguration = new PropertiesConfiguration();
            } else if (resource != null) {
                File classpathConfig = new File(resource.toURI());
                LOGGER.debug("Try to load palladian.properties from Classpath: "
                        + classpathConfig.getAbsolutePath());
                propertiesConfiguration = new PropertiesConfiguration(classpathConfig);
            } else if (configFile.exists()) {
                LOGGER.debug("Try to load palladian.properties from config folder: " + configFile.getAbsolutePath());
                propertiesConfiguration = new PropertiesConfiguration(configFile);
            }
            if (propertiesConfiguration != null) {
                setConfig(propertiesConfiguration);
            } else {
                LOGGER.error("Palladian configuration file loading error.");
                // we should throw an exception here
            }
        } catch (ConfigurationException e) {
            LOGGER.error("Palladian configuration under " + CONFIG_PATH + " could not be loaded completely: "
                    + e.getMessage());
        } catch (URISyntaxException e) {
            LOGGER.error("Palladian configuration file loading error.");
        }
    }

    /**
     * Return the value of the field with the specified field name.
     * 
     * @param fieldName
     *            The name of the field for which a value should be retrieved.
     * @return The value of the field as Object since we don't know the type.
     */
    public Object getField(String fieldName) {
        return config.getProperty(fieldName);
    }

    /**
     * <p>
     * Sets the configuration of this instance of the framework to a new value. Old configuration properties are
     * overwritten.
     * </p>
     * 
     * @param config The new configuration this config holder shall hold.
     */
    private void setConfig(PropertiesConfiguration config) {
        this.config = config;
    }

    /**
     * <p>
     * Provides the current configuration held by this object.
     * </p>
     * 
     * @return The current palladian configuration.
     */
    public PropertiesConfiguration getConfig() {
        return config;
    }
}
