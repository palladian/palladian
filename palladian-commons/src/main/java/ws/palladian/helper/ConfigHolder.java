package ws.palladian.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

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
 * @deprecated Will be removed in future versions of Palladian.
 * 
 */
@Deprecated
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
         * The single INSTANCE of the Palladian configuration.
         * </p>
         */
        private static final ConfigHolder INSTANCE = new ConfigHolder();
    }

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ConfigHolder.class);

    /**
     * <p>
     * The name of the Palladian configuration properties file.
     * </p>
     */
    private static final String CONFIG_NAME = "palladian.properties";

    /**
     * <p>
     * The path under which the Palladian configuration file must lie.
     * </p>
     */
    private static final String CONFIG_PATH = "config/" + CONFIG_NAME;

    /**
     * <p>
     * Name of the environment variable denoting the Palladian home directory.
     * </p>
     */
    private static final String PALLADIAN_HOME_ENV = "PALLADIAN_HOME";

    /**
     * <p>
     * The version of the palladian.properties.default file. This should be incremented whenever changes are made to
     * that file. The loader will then check the version number of the palladian.properties and warns if it is outdated.
     * </p>
     */
    private static final int VERSION = 20;

    /**
     * <p>
     * This method realizes the singleton design pattern by providing the one single INSTANCE of the
     * {@code ConfigHolder} class.
     * </p>
     * 
     * @return The one INSTANCE of {@code ConfigHolder}.
     */
    public static ConfigHolder getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * <p>
     * Configs for Palladian are held in the config field.
     * </p>
     */
    private PropertiesConfiguration config = null;

    /**
     * <p>
     * This class is a singleton and therefore the constructor is private.
     * </p>
     */
    private ConfigHolder() {
        FileInputStream propertiesInput = null;
        try {
            List<String> configCandidates = new ArrayList<String>();
            String palladianHomeEnvironment = System.getenv(PALLADIAN_HOME_ENV);
            if (palladianHomeEnvironment != null) {
                configCandidates.add(palladianHomeEnvironment + File.separatorChar + CONFIG_PATH);
            }
            try {
                String classpathConfig = ResourceHelper.getResourcePath(CONFIG_NAME);
                configCandidates.add(classpathConfig);
            } catch (FileNotFoundException e) {
                LOGGER.trace("No configuration file in classpath.");
            }
//            URL configResource = ConfigHolder.class.getResource("/" + CONFIG_NAME);
//            if (configResource != null) {
//                configCandidates.add(configResource.getFile());
//            }
            configCandidates.add(CONFIG_PATH);
            LOGGER.debug("config candidates : " + configCandidates);
            
            File configFile = null;
            for (String candidate : configCandidates) {
                configFile = new File(candidate);
                break;
            }

            if (configFile.exists()) {
                LOGGER.debug("Loaded 'palladian.properties' from: " + configFile.getAbsolutePath());
            } else {
//                throw new IllegalStateException(
//                        "No Palladian configuration file available. Please put on named palladian.properties in a folder called 'config' either on your classpath, a folder identified by the environment variable PALLADIAN_HOME or in the location you are running Palladian from.");
                LOGGER.warn("No Palladian configuration file available. Please put one named palladian.properties in a folder called 'config' either on your classpath, a folder identified by the environment variable PALLADIAN_HOME or in the location you are running Palladian from.");
            }

            final PropertiesConfiguration properties = new PropertiesConfiguration(configFile);
            if (properties.isEmpty()) {
//                throw new IllegalStateException("Failed to parse Palladian configuration file at: "
//                        + configFile.getAbsolutePath());
                LOGGER.warn("Failed to parse Palladian configuration file at: " + configFile.getAbsolutePath());
            }

            checkPropertiesVersion(properties);
            setConfig(properties);

        } catch (ConfigurationException e) {
            throw new IllegalStateException("Palladian configuration under " + CONFIG_PATH
                    + " could not be loaded completely.", e);
        } finally {
            FileHelper.close(propertiesInput);
        }
    }

    /**
     * <p>
     * Prints a warning if the currently loaded version of the properties file is not correct.
     * </p>
     * 
     * @param properties The properties loaded from the current 'palladian.properties' file.
     */
    private void checkPropertiesVersion(final PropertiesConfiguration properties) {
        if (properties.containsKey("config.version")) {
            int fileVersion = properties.getInt("config.version");
            if (fileVersion != VERSION) {
                LOGGER.warn("the palladian.properties file is outdated, it is version " + fileVersion
                        + " but the latest version is " + VERSION + ", please consider updating");
            }
        } else {
            LOGGER.warn("the palladian.properties file is outdated, it has no 'config.version' field, please consider updating");
        }
    }

    /**
     * @return
     */
    public PropertiesConfiguration getConfig() {
        return this.config;
    }

//    /**
//     * <p>
//     * Return the value of the field with the specified field name.
//     * </p>
//     *
//     * @param fieldName
//     *            The name of the field for which a value should be retrieved.
//     * @return The value of the field as Object since we don't know the type.
//     */
//    public Object getField(final String fieldName) {
//        return this.config.getProperty(fieldName);
//    }

    /**
     * <p>
     * Sets the configuration of this INSTANCE of the framework to a new value. Old configuration properties are
     * overwritten.
     * </p>
     * 
     * @param config The new configuration this config holder shall hold.
     */
    private void setConfig(final PropertiesConfiguration properties) {
        this.config = properties;
    }
}
