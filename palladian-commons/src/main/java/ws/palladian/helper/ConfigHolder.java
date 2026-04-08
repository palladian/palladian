package ws.palladian.helper;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileLocatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigHolder.class);

    private static class SingletonHolder {
        private static final ConfigHolder INSTANCE = new ConfigHolder();
    }

    /** The name of the Palladian configuration properties file. */
    public static final String CONFIG_NAME = "palladian.properties";

    /**
     * @return The singleton instance of the {@link ConfigHolder}.
     */
    public static ConfigHolder getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /** Configs for Palladian are held in the config field. */
    private final Configuration config;

    /** This class is a singleton and therefore the constructor is private. */
    private ConfigHolder() {
        config = loadConfig();
    }

    private static Configuration loadConfig() {
        Configuration config;
        URL configUrl = FileLocatorUtils.locate(FileLocatorUtils.fileLocator().fileName(CONFIG_NAME).create());
        if (configUrl != null) {
            LOGGER.debug("Found configuration at {}", configUrl);
            try {
                FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class).configure(
                        new Parameters().properties().setURL(configUrl));
                config = builder.getConfiguration();
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
