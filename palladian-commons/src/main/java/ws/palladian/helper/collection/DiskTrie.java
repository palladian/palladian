package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

/**
 * Values can be written to disk leading to massive memory savings.
 *
 * @author David Urbansky
 * @since 05-Jun-22 at 12:02
 **/
public class DiskTrie<V> extends Trie<V> {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskTrie.class);

    /**
     * If true, all the node values will be null and we have written the contents to disk in the dataFolder
     */
    private boolean dataWrittenToDisk = false;
    private File dataFolder;

    @Override
    public V put(String key, V value) {
        Validate.notEmpty(key, "key must not be empty");
        if (dataWrittenToDisk) {
            if (value == null) {
                FileHelper.delete(getSerializationPath(key));
            } else {
                try {
                    FileHelper.serialize((Serializable) value, getSerializationPath(key));
                } catch (Exception e) {
                    LOGGER.error("could not serialize " + key + " to " + dataFolder.getPath(), e);
                }
            }
            return null;
        }
        Trie<V> node = getNode(key, true);
        V oldValue = node.value;
        node.value = value;
        return oldValue;
    }

    @Override
    public V get(String key) {
        Validate.notEmpty(key, "key must not be empty");
        if (dataWrittenToDisk) {
            String serializationPath = getSerializationPath(key);
            if (FileHelper.fileExists(serializationPath)) {
                try {
                    Serializable deserialize = FileHelper.deserialize(serializationPath);
                    return (V) deserialize;
                } catch (Exception e) {
                    LOGGER.error("could not deserialize " + key + " from " + dataFolder.getPath(), e);
                }
            } else {
                return null;
            }
        }

        Trie<V> node = getNode(key);
        return node != null ? node.value : null;
    }

    public boolean isDataWrittenToDisk() {
        return dataWrittenToDisk;
    }

    private String getSerializationPath(String key) {
        String shaKey = StringHelper.sha1(key);
        String subFolder = shaKey.substring(0, 3);
        return dataFolder.getPath() + "/" + subFolder + "/node-" + shaKey + ".gz";
    }

    @Override
    public boolean clean() {
        // when data is offloaded to disk we must not remove empty nodes
        if (dataWrittenToDisk) {
            return true;
        }
        return super.clean();
    }

    private void writeValuesToDisk() throws IOException {
        writeValuesToDisk(dataFolder);
    }

    /**
     * Values can take up a tremendous amount of memory. This function allows us to write it to disk while keeping the keys in memory. That allows still for quick access while having a fraction of the memory footprint.
     *
     * @param folder The folder to which we store the data.
     */
    public void writeValuesToDisk(File folder) throws IOException {
        dataFolder = folder;
        int size = size();
        ProgressMonitor pm = new ProgressMonitor(size, 1., "Serializing values from Trie");
        Iterator<Map.Entry<String, V>> iterator = iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, V> entry = iterator.next();
            String key = entry.getKey();
            if (entry.getValue() == null) {
                continue;
            }
            try {
                FileHelper.serialize((Serializable) entry.getValue(), getSerializationPath(key));
            } catch (Exception e) {
                LOGGER.error("could not serialize " + key + " to " + folder.getPath(), e);
            }
            if (size > 10) {
                pm.incrementAndPrintProgress();
            }
        }

        // now that everything is serialized, we can remove the values from memory
        // we must not do this before because get() calls might fail while the trie is being serialized
        iterator = iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, V> entry = iterator.next();
            put(entry.getKey(), null);
        }
        dataWrittenToDisk = true;
    }
}
