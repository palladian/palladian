package ws.palladian.helper;

import java.net.URL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Holds the Palladian configuration of the framework. This configuration is obtained from an external file called
 * {@value #CONFIG_NAME}, which is searched in the following places:
 * </p>
 * 
 * <ol>
 * <li>user's home directory,</li>
 * <li>current classpath,</li>
 * <li>system classpath.</li>
 * </ol>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class ConfigHolder {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigHolder.class);

    private static class SingletonHolder {
        private static final ConfigHolder INSTANCE = new ConfigHolder();
    }

    /** The name of the Palladian configuration properties file. */
    public static final String CONFIG_NAME = "palladian.properties";

    /**
     * @return The singleton instance of the {@link ConfigHolder}.
     */
    public static final ConfigHolder getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /** Configs for Palladian are held in the config field. */
    private final Configuration config;

    /** This class is a singleton and therefore the constructor is private. */
    private ConfigHolder() {
        config = loadConfig();
    }

    private static final Configuration loadConfig() {
        Configuration config;
        URL configUrl = ConfigurationUtils.locate(CONFIG_NAME);
        if (configUrl != null) {
            LOGGER.debug("Found configuration at {}", configUrl);
            try {
                config = new PropertiesConfiguration(configUrl);
            } catch (ConfigurationException e) {
                throw new IllegalStateException("Exception while loading the configuration: " + e.getMessage(), e);
            }
        } else {
            LOGGER.debug("No configuration with name {} found", CONFIG_NAME);
            config = new PropertiesConfiguration();
        }
        return config;
    }

    /**
     * @return The configuration as read from {@value #CONFIG_NAME}, or an empty configuration, never <code>null</code>.
     */
    public Configuration getConfig() {
        return this.config;
    }

}
