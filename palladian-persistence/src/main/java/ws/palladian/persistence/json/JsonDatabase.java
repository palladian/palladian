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

    public JsonDatabase(String path) {
        this(path, new HashMap<>());
    }

    public JsonDatabase(String path, Map<String, List<String>> collectionFieldIndexMap) {
        this.rootPath = path;
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

    /**
     * Open the database for some collections. This creates default indexes on these collections for upserts to work.
     */
    public JsonDatabase(String path, String... collectionName) {
        this.rootPath = path;
        if (!rootPath.endsWith("/")) {
            rootPath += "/";
        }
        FileHelper.createDirectory(rootPath);
        for (String collection : collectionName) {
            createIndex(collection, "_id");
        }
        this.loadIndexes();
    }

    private synchronized void loadIndexes() {
        // read all indexes
        File[] indexFiles = FileHelper.getFilesRecursive(rootPath, "_idx-");
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
        String filePath = rootPath + collectionName + "/" + fileName;
        boolean writeSuccess = FileHelper.writeToFile(filePath, jsonObject.toString(2));

        // update index
        updateIndex(collectionName, jsonObject, fileName);

        return writeSuccess;
    }

    //    public synchronized boolean upsert(String collectionName, JsonObject jsonObject) {
    //        if (jsonObject.containsKey("_id")) {
    //            return upsert(collectionName, jsonObject);
    //        }
    //        return add(collectionName, jsonObject);
    //    }

    public synchronized boolean upsert(String collectionName, JsonObject jsonDocument) {
        return add(collectionName, jsonDocument);
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
                jsonObjects.add(JsonObject.tryParse(FileHelper.tryReadFileToStringNoReplacement(new File(rootPath + collection + "/" + filePath))));
            }
            return jsonObjects;
        }

        return Collections.emptyList();
    }

    public boolean delete(String collectionName, String id) {
        String fileName = id + ".json";
        String filePath = rootPath + collectionName + "/" + fileName;
        return FileHelper.delete(filePath);
    }

    public int countCollectionEntries(String collection) {
        return FileHelper.getFiles(rootPath + collection).length;
    }

    public JsonDbIterator<File> getAllFiles(String collection) {
        return getAllFiles(collection, 0);
    }

    public JsonDbIterator<File> getAllFiles(String collection, int startIndex) {
        final List<File> collectionFiles = Arrays.stream(FileHelper.getFiles(rootPath + collection)).filter(f -> !f.getName().startsWith("_idx")).collect(Collectors.toList());

        JsonDbIterator<File> jsonDbIterator = new JsonDbIterator<File>() {
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

        return jsonDbIterator;
    }

    public JsonDbIterator<JsonObject> getAll(String collection) {
        return getAll(collection, 0);
    }

    public JsonDbIterator<JsonObject> getAll(String collection, int startIndex) {
        final List<File> collectionFiles = Arrays.stream(FileHelper.getFiles(rootPath + collection)).filter(f -> !f.getName().startsWith("_idx")).collect(Collectors.toList());

        JsonDbIterator<JsonObject> jsonDbIterator = new JsonDbIterator<JsonObject>() {
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
                    return null;
                }
                String text = FileHelper.tryReadFileToStringNoReplacement(collectionFile);
                return JsonObject.tryParse(text);
            }
        };
        jsonDbIterator.setIndex(startIndex);

        return jsonDbIterator;
    }

    public JsonDbIterator<JsonObject> getAll(String collection, String field, String value) {
        Map<String, List<String>> indexContent = indexMap.get(collection + "_idx-" + field);
        final List<String> collectionFiles = indexContent.get(value);

        JsonDbIterator<JsonObject> jsonDbIterator = new JsonDbIterator<JsonObject>() {
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
                    return null;
                }
                String text = FileHelper.tryReadFileToStringNoReplacement(new File(rootPath + collection + "/" + collectionFilePath));
                return JsonObject.tryParse(text);
            }
        };
        jsonDbIterator.setIndex(0);
        if (collectionFiles != null) {
            jsonDbIterator.setTotalCount(collectionFiles.size());
        } else {
            jsonDbIterator.setTotalCount(0);
        }

        return jsonDbIterator;
    }

    //    public boolean exists(String collection, String field, String value) {
    //        // check if we have an index on the field
    //        Map<String, List<String>> indexContent = indexMap.get(collection + "-idx-" + field);
    //        if (indexContent != null) {
    //            return indexContent.get(value) != null;
    //        }
    //
    //        return false;
    //    }

    //    public JsonObject get(String collection, String id) {
    //        return get(collection, "_id", id);
    //    }

    public JsonObject getById(String collection, String id) {
        return JsonObject.tryParse(FileHelper.tryReadFileToStringNoReplacement(new File(rootPath + collection + "/" + id + ".json")));
    }

    //    public List<JsonObject> get(String collection, String field, String value) {
    //        List<JsonObject> objs = new ArrayList<>();
    //
    //        // check if we have an index on the field
    //        List<String> indexJsonPaths = indexMap.get(collection + "_idx-" + field);
    //        if (indexJsonPaths != null) {
    //            for (String indexJsonPath : indexJsonPaths) {
    //                objs.add(JsonObject.tryParse(FileHelper.tryReadFileToString(rootPath + collection + "/" + indexJsonPath)));
    //            }
    //        }
    //
    //        return objs;
    //    }

    public synchronized void createIndex(String collection, String field) {
        String indexFilePath = rootPath + collection + "/_idx-" + field + ".json";
        JsonObject indexJson = new JsonObject();
        final List<File> files = Arrays.stream(FileHelper.getFiles(rootPath + collection)).filter(f -> !f.getName().startsWith("_idx")).collect(Collectors.toList());

        ProgressMonitor pm = new ProgressMonitor(files.size(), 1.0, "Creating Index " + field);
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

        this.loadIndexes();
    }

    private void updateIndex(String collection, JsonObject jsonObject, String filePath) {
        for (String indexedField : indexedFieldsForCollection.get(collection)) {
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
