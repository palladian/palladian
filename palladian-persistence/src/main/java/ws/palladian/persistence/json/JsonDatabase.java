package ws.palladian.persistence.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * A simple JSON database. No 16MB file limits that mongo db has.
 * </p>
 *
 * @author David Urbansky
 * @since 28.06.2020
 */
public class JsonDatabase {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDatabase.class);

    private String rootPath;

    // index entry => value => [file paths]
    // e.g. test-collection-source => wikipedia => [filepath1, filepath2]
    private final Map<String, Map<String, List<String>>> indexMap = new HashMap<>();
    private final Map<String, List<String>> indexedFieldsForCollection = new HashMap<>();

    private final int numSubdirectories;

    public enum Action {
        ADD, MERGE, NOTHING
    }

    public JsonDatabase(String path, int numSubdirectories) {
        this(path, new HashMap<>(), numSubdirectories);
    }

    public JsonDatabase(String path, Map<String, List<String>> collectionFieldIndexMap, int numSubdirectories) {
        this.rootPath = path;
        this.numSubdirectories = numSubdirectories;
        if (!rootPath.endsWith("/")) {
            rootPath += "/";
        }
        FileHelper.createDirectory(rootPath);

        for (Map.Entry<String, List<String>> stringCollectionEntry : collectionFieldIndexMap.entrySet()) {
            String collectionName = stringCollectionEntry.getKey();
            Collection<String> fieldNames = stringCollectionEntry.getValue();
            indexedFieldsForCollection.computeIfAbsent(collectionName, k -> new ArrayList<>());
            indexedFieldsForCollection.get(collectionName).addAll(fieldNames);
        }

        this.loadIndexes();
    }

    private synchronized void loadIndexes() {
        // read all indexes
        List<File> indexFiles = new ArrayList<>();
        File folder = new File(rootPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] collectionDirectories = folder.listFiles();
            for (File collectionDirectory : collectionDirectories) {
                File[] collectionIndexFiles = FileHelper.getFiles(rootPath + "/" + collectionDirectory.getName(), "_idx-");
                indexFiles.addAll(Arrays.asList(collectionIndexFiles));
            }
        }
        for (File indexFile : indexFiles) {
            String text = FileHelper.tryReadFileToStringNoReplacement(indexFile);
            JsonObject indexJson = JsonObject.tryParse(text);
            Map<String, List<String>> indexContentMap = new HashMap<>();
            for (Map.Entry<String, Object> stringObjectEntry : indexJson.entrySet()) {
                String key = stringObjectEntry.getKey();
                List pathList = (List) stringObjectEntry.getValue();
                indexContentMap.put(key, pathList);
            }
            String indexName = indexFile.getName().replace(".json", "");
            String indexedField = indexName.replace("_idx-", "");
            String collectionName = indexFile.getParentFile().getName();

            indexedFieldsForCollection.computeIfAbsent(collectionName, k -> new ArrayList<>());
            indexedFieldsForCollection.get(collectionName).add(indexedField);
            indexMap.put(collectionName + indexName, indexContentMap);
        }
    }

    public boolean add(String collectionName, JsonObject jsonObject) {
        String id;
        if (!jsonObject.containsKey("_id")) {
            UUID uuid = UUID.randomUUID();
            id = uuid.toString();
            jsonObject.put("_id", id);
        } else {
            id = jsonObject.tryGetString("_id");
        }
        String fileName = id + ".json";

        String folderedPath = getFolderedPath(fileName);
        String filePath = rootPath + collectionName + "/" + folderedPath;

        boolean writeSuccess = FileHelper.writeToFile(filePath, jsonObject.toString(2));

        // update index
        updateIndex(collectionName, jsonObject, fileName);

        return writeSuccess;
    }

    /**
     * Directories with millions of files become hard to read and write to. Therefore, there is an option to create "random" subfolders to distribute the files.
     */
    private String getFolderedPath(String filename) {
        if (numSubdirectories < 2) {
            return filename;
        }
        return FileHelper.getFolderedPath(filename, numSubdirectories);
    }

    public synchronized Action upsert(String collectionName, JsonObject jsonDocument) {
        // try finding the document by id
        JsonObject existingDocument = getById(collectionName, jsonDocument.tryGetString("_id"));

        // if we don't find any, we add, otherwise we merge
        // but only if the new one can contribute something
        if (existingDocument == null) {
            add(collectionName, jsonDocument);
            return Action.ADD;
        } else {
            for (String key : jsonDocument.keySet()) {
                // only add fields that are not in the existing document
                if (!existingDocument.containsKey(key)) {
                    existingDocument.put(key, jsonDocument.get(key));
                } else {
                    // do a first level merge, that is combine all array and object values from the first level
                    if (existingDocument.get(key) instanceof Collection && jsonDocument.get(key) instanceof Collection) {
                        Set existingValues = new LinkedHashSet(existingDocument.tryGetJsonArray(key));
                        Set newValues = new LinkedHashSet(jsonDocument.tryGetJsonArray(key));
                        existingValues.addAll(newValues);
                        existingDocument.put(key, new JsonArray(existingValues));
                    } else if (existingDocument.get(key) instanceof JsonObject && jsonDocument.get(key) instanceof JsonObject) {
                        JsonObject existingValue = existingDocument.tryGetJsonObject(key);
                        JsonObject newValue = (JsonObject) jsonDocument.get(key);
                        existingValue.putAll(newValue);
                    }
                }
            }

            add(collectionName, existingDocument);
            return Action.MERGE;
        }
    }

    public JsonObject getOne(String collection, String field, String value) {
        return CollectionHelper.getFirst(get(collection, field, value));
    }

    public List<JsonObject> get(String collection, String field, String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        // check if we have an index on the field
        Map<String, List<String>> indexContent = indexMap.get(collection + "_idx-" + field);
        if (indexContent != null) {
            List<String> objects = indexContent.get(value);
            if (objects == null) {
                return Collections.emptyList();
            }
            List<JsonObject> jsonObjects = new ArrayList<>();
            for (int i = 0; i < objects.size(); i++) {
                String filePath = objects.get(i);
                JsonObject obj = JsonObject.tryParse(FileHelper.tryReadFileToStringNoReplacement(new File(rootPath + collection + "/" + getFolderedPath(filePath))));
                if (obj != null) {
                    jsonObjects.add(obj);
                }
            }
            return jsonObjects;
        }

        return Collections.emptyList();
    }

    public boolean delete(String collectionName, String id) {
        String fileName = id + ".json";
        String filePath = rootPath + collectionName + "/" + getFolderedPath(fileName);
        return FileHelper.delete(filePath);
    }

    public int countCollectionEntries(String collection) {
        int count = 0;
        File folder = new File(rootPath + collection);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            for (File file : files) {
                // count files within directories (if folders are used)
                if (file.isDirectory()) {
                    count += FileHelper.getFiles(rootPath + collection + "/" + file.getName()).length;
                } else {
                    // count files if no folders are used
                    count++;
                }
            }
        }
        return count;
    }

    private List<File> getFiles(String collection) {
        List<File> files = new ArrayList<>();
        File folder = new File(rootPath + collection);
        if (folder.exists() && folder.isDirectory()) {
            File[] folderFiles = folder.listFiles();
            ProgressMonitor progressMonitor = new ProgressMonitor(folderFiles.length, 10., "Getting files in " + collection);
            for (File file : folderFiles) {
                progressMonitor.incrementAndPrintProgress();
                if (file.isDirectory()) {
                    files.addAll(Arrays.asList(FileHelper.getFiles(rootPath + collection + "/" + file.getName())));
                } else {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public JsonDbIterator<File> getAllFiles(String collection) {
        return getAllFiles(collection, 0);
    }

    public JsonDbIterator<File> getAllFiles(String collection, int startIndex) {
        final List<File> collectionFiles = getFiles(collection).stream().filter(f -> !f.getName().startsWith("_idx")).collect(Collectors.toList());

        JsonDbIterator<File> jsonDbIterator = new JsonDbIterator<>() {
            @Override
            public int getTotalCount() {
                return collectionFiles.size();
            }

            @Override
            public boolean hasNext() {
                return collectionFiles.size() > index.get();
            }

            @Override
            public File next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return collectionFiles.get(index.getAndIncrement());
            }
        };
        jsonDbIterator.setIndex(startIndex);
        jsonDbIterator.setTotalCount(collectionFiles.size());

        return jsonDbIterator;
    }

    public JsonDbIterator<JsonObject> getAll(String collection) {
        return getAll(collection, 0);
    }

    public JsonDbIterator<JsonObject> getAll(String collection, int startIndex) {
        final List<File> collectionFiles = getFiles(collection);

        JsonDbIterator<JsonObject> jsonDbIterator = new JsonDbIterator<>() {
            @Override
            public int getTotalCount() {
                return collectionFiles.size();
            }

            @Override
            public boolean hasNext() {
                return collectionFiles.size() > index.get();
            }

            @Override
            public JsonObject next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                File collectionFile = collectionFiles.get(index.getAndIncrement());
                if (collectionFile == null) {
                    throw new NullPointerException("null json object when parsing json from file");
                }
                String text = FileHelper.tryReadFileToStringNoReplacement(collectionFile);
                JsonObject jso = JsonObject.tryParse(text);
                if (jso == null) {
                    throw new NullPointerException("null json object when parsing json from file: " + collectionFile.getName());
                }
                return jso;
            }
        };
        jsonDbIterator.setIndex(startIndex);
        jsonDbIterator.setTotalCount(collectionFiles.size());

        return jsonDbIterator;
    }

    public JsonDbIterator<JsonObject> getAll(String collection, String field, String value) {
        Map<String, List<String>> indexContent = indexMap.get(collection + "_idx-" + field);
        final List<String> collectionFiles = new ArrayList<>(indexContent.get(value)); // need to make a copy otherwise it may grow when updating the index

        JsonDbIterator<JsonObject> jsonDbIterator = new JsonDbIterator<>() {
            @Override
            public boolean hasNext() {
                return collectionFiles.size() > index.get();
            }

            @Override
            public JsonObject next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                String collectionFilePath = collectionFiles.get(index.getAndIncrement());
                if (collectionFilePath == null) {
                    throw new NullPointerException("null json object when parsing json from file");
                }
                String text = FileHelper.tryReadFileToStringNoReplacement(new File(rootPath + collection + "/" + getFolderedPath(collectionFilePath)));
                JsonObject jso = JsonObject.tryParse(text);
                if (jso == null) {
                    throw new NullPointerException("null json object when parsing json from file: " + rootPath + collection + "/" + getFolderedPath(collectionFilePath));
                }
                return jso;
            }
        };
        jsonDbIterator.setIndex(0);
        jsonDbIterator.setTotalCount(collectionFiles.size());

        return jsonDbIterator;
    }

    public JsonObject getById(String collection, String id) {
        return JsonObject.tryParse(FileHelper.tryReadFileToStringNoReplacement(new File(rootPath + collection + "/" + getFolderedPath(id + ".json"))));
    }

    public JsonObject getOrCreateById(String collection, String id) {
        JsonObject jsonObject = getById(collection, id);
        if (jsonObject == null) {
            jsonObject = new JsonObject();
        }
        return jsonObject;
    }

    public boolean exists(String collection, String id) {
        return new File(rootPath + collection + "/" + getFolderedPath(id + ".json")).exists();
    }

    public synchronized void rebuildInitializedIndexes() {
        for (Map.Entry<String, List<String>> collectionKeysEntry : indexedFieldsForCollection.entrySet()) {
            for (String field : collectionKeysEntry.getValue()) {
                createIndex(collectionKeysEntry.getKey(), field, false);
            }
        }
    }

    public synchronized void createIndex(String collection, String field) {
        createIndex(collection, field, true);
    }

    public synchronized void createIndex(String collection, String field, boolean reloadIndexes) {
        String indexFilePath = rootPath + collection + "/_idx-" + field + ".json";
        JsonObject indexJson = new JsonObject();
        final List<File> files = getFiles(collection);

        ProgressMonitor pm = new ProgressMonitor(files.size(), 10.0, "Creating Index " + field);
        for (File file : files) {
            String text = FileHelper.tryReadFileToStringNoReplacement(file);
            JsonObject jso = JsonObject.tryParse(text);

            pm.incrementAndPrintProgress();

            if (jso == null) {
                LOGGER.warn("null json when creating index, file: " + file.getName());
                continue;
            }
            Object indexField = jso.get(field);
            if (indexField == null) {
                continue;
            }
            if (indexField instanceof JsonArray) {
                JsonArray values = (JsonArray) indexField;
                for (int i = 0; i < values.size(); i++) {
                    String key = values.tryGetString(i);

                    JsonArray filePaths = Optional.ofNullable(indexJson.tryGetJsonArray(key)).orElse(new JsonArray());
                    filePaths.add(file.getName());
                    indexJson.put(key, filePaths);
                }
            } else {
                String key = String.valueOf(indexField);

                JsonArray filePaths = Optional.ofNullable(indexJson.tryGetJsonArray(key)).orElse(new JsonArray());
                filePaths.add(file.getName());
                indexJson.put(key, filePaths);
            }
        }

        FileHelper.writeToFile(indexFilePath, indexJson.toString(2));

        if (reloadIndexes) {
            this.loadIndexes();
        }
    }

    private synchronized void updateIndex(String collection, JsonObject jsonObject, String filePath) {
        for (String indexedField : indexedFieldsForCollection.getOrDefault(collection, Collections.emptyList())) {
            List<String> valuesInObject = new ArrayList<>();
            Object value = jsonObject.get(indexedField);
            if (value instanceof Collection) {
                Collection values = (Collection) value;
                for (Object v : values) {
                    valuesInObject.add((String) v);
                }
            } else {
                valuesInObject.add(String.valueOf(value));
            }
            for (String v : valuesInObject) {
                String indexName = collection + "_idx-" + indexedField;
                Map<String, List<String>> indexContent = indexMap.computeIfAbsent(indexName, k -> new HashMap<>());
                List<String> strings = indexContent.computeIfAbsent(v, k -> new ArrayList<>());
                strings.add(filePath);
            }
        }

        // XXX write index?
        //        writeIndex();
    }

    public void writeIndex() {
        for (Map.Entry<String, Map<String, List<String>>> stringMapEntry : indexMap.entrySet()) {
            String collection = stringMapEntry.getKey();
            for (Map.Entry<String, List<String>> collectionEntry : stringMapEntry.getValue().entrySet()) {
                String indexFilePath = rootPath + collection + "/_idx-" + collectionEntry.getKey() + ".json";
                JsonObject indexJson = new JsonObject(stringMapEntry.getValue());
                FileHelper.writeToFile(indexFilePath, indexJson.toString(2));
            }
        }
    }

    public static void main(String[] args) {
        //        JsonDatabase db = new JsonDatabase("data/rawdb");
        //        JsonObject jsonObject = db.get("objects", "object_id", "999250");
        //        System.out.println(jsonObject);
    }
}
