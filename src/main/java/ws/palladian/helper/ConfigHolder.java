package ws.palladian.helper;

import java.io.File;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

/**
 * <p>
 * Holds the configuration of the framework. This configuration is obtained from an external file called
 * <tt>palladian.properties</tt>. The configuration is searched at the following places, in the specified order:
 * </p>
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

    /**
     * <p>
     * Wrapper class for thread safe singleton handling. See "Effective Java", item 48.
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

    /**
     * The version of the palladian.properties.default file. This should be incremented whenever changes are made to
     * that file. The loader will then check the version number of the palladian.properties and warns if it is outdated.
     */
    private static final int VERSION = 2;

    /**
     * <p>
     * This method realizes the singleton design pattern by providing the one single instance of the {@code
     * ConfigHolder} class.
     * </p>
     * 
     * @return The one instance of {@code ConfigHolder}.
     */
    public static ConfigHolder getInstance() {
        return SingletonHolder.instance;
    }

    /** Configs for Palladian are held in the config field. */
    private PropertiesConfiguration config = null;

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
                LOGGER.info("try to load palladian.properties from environment PALLADIAN_HOME: "
                        + environmentConfig.getAbsolutePath());
                propertiesConfiguration = new PropertiesConfiguration();
                if (!propertiesConfiguration.getKeys().hasNext()) {
                    propertiesConfiguration = null;
                    LOGGER.error("failed to load palladian.properties from environment PALLADIAN_HOME: "
                            + environmentConfig.getAbsolutePath());
                }
            } else if (resource != null) {
                File classpathConfig = new File(resource.getFile());
                LOGGER.info("try to load palladian.properties from classpath: " + classpathConfig.getAbsolutePath());
                propertiesConfiguration = new PropertiesConfiguration(classpathConfig);
                if (!propertiesConfiguration.getKeys().hasNext()) {
                    propertiesConfiguration = null;
                    LOGGER.error("failed to load palladian.properties from classpath: "
                            + classpathConfig.getAbsolutePath());
                }
            } else if (configFile.exists()) {
                LOGGER.info("try to load palladian.properties from config folder: " + configFile.getAbsolutePath());
                propertiesConfiguration = new PropertiesConfiguration(configFile);
                if (!propertiesConfiguration.getKeys().hasNext()) {
                    propertiesConfiguration = null;
                    LOGGER.error("failed to load palladian.properties from config folder: "
                            + configFile.getAbsolutePath());
                }
            }
            if (propertiesConfiguration != null) {

                // check whether the version is up to date
                if (propertiesConfiguration.containsKey("config.version")) {
                    int fileVersion = propertiesConfiguration.getInt("config.version");
                    if (fileVersion != VERSION) {
                        LOGGER.warn("the palladian.properties file is outdated, it is version " + fileVersion
                                + " but the latest version is " + VERSION + ", please consider updating");
                    }
                } else {
                    LOGGER.warn("the palladian.properties file is outdated, it has no 'config.version' field, please consider updating");
                }

                setConfig(propertiesConfiguration);
            } else {
                LOGGER.error("palladian configuration file loading failed");
                // we should throw an exception here
            }
        } catch (ConfigurationException e) {
            LOGGER.error("Palladian configuration under " + CONFIG_PATH + " could not be loaded completely: "
                    + e.getMessage());
        }
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
}
