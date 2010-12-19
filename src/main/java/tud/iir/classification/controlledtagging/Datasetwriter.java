package tud.iir.classification.controlledtagging;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.collections15.map.LazyMap;
import org.apache.commons.lang.StringUtils;

import tud.iir.helper.FileHelper;

public class Datasetwriter {
    
    public static void main(String[] args) {

        // createCiteULike("/Users/pk/temp/citeulike180/taggers", "/Users/pk/temp/citeulike180.txt");
        // createFAO("/Users/pk/temp/fao780", "/Users/pk/temp/fao780.txt");
        
        
    }

    public static void createCiteULike(String pathToRawFiles, String resultFile) {

        Factory<Bag<String>> factory = new Factory<Bag<String>>() {
            @Override
            public Bag<String> create() {
                return new HashBag<String>();
            }
        };
        Map<Integer, Bag<String>> documentsTags = LazyMap.decorate(new TreeMap<Integer, Bag<String>>(), factory);

        // go through all .tags files and get the tags

        File[] taggerDirectories = FileHelper.getFiles(pathToRawFiles);
        for (File file : taggerDirectories) {

            if (!file.isDirectory()) {
                continue;
            }

            File[] tagFiles = FileHelper.getFiles(file.getPath());
            for (File tagFile : tagFiles) {

                List<String> tags = FileHelper.readFileToArray(tagFile);
                Bag<String> documentTags = documentsTags.get(Integer.valueOf(tagFile.getName().replace(".tags", "")));

                for (String tag : tags) {
                    if (tag.length() > 0) {

                        // some .tag files in the dataset contain junk, 
                        // which we filter here
                        if (tag.contains("  ")) {
                            tag = tag.substring(tag.indexOf("  ") + 2, tag.length());
                        }

                        documentTags.add(tag.trim());
                    }
                }
            }
        }

        // write index file
        StringBuilder sb = new StringBuilder();

        Set<Entry<Integer, Bag<String>>> entrySet = documentsTags.entrySet();
        for (Entry<Integer, Bag<String>> entry : entrySet) {
            sb.append(entry.getKey()).append(".txt").append("#");
            sb.append(StringUtils.join(entry.getValue().uniqueSet(), "#"));
            sb.append("\n");
        }
        
        FileHelper.writeToFile(resultFile, sb);

    }


    public static void createFAO(String pathToRawFiles, String resultFile) {

        File[] files = FileHelper.getFiles(pathToRawFiles, ".key");
        StringBuilder sb = new StringBuilder();

        for (File file : files) {

            String keyFile = file.getName();
            List<String> keywords = FileHelper.readFileToArray(pathToRawFiles + "/" + keyFile);
            
            sb.append(keyFile.replace(".key", ".txt"));
            sb.append("#");
            sb.append(StringUtils.join(keywords, "#"));
            sb.append("\n");

        }
        
        FileHelper.writeToFile(resultFile, sb);

    }



//    private static void writeFromT140() {
//        // final StringBuilder sb = new StringBuilder();
//        final Counter counter = new Counter();
//        DeliciousDatasetReader reader = new DeliciousDatasetReader();
//
//        DatasetFilter filter = new DatasetFilter();
//        filter.addAllowedFiletype("html");
//        filter.setMinUsers(50);
//        filter.setMaxFileSize(600000);
//        reader.setFilter(filter);
//
//        DatasetCallback callback = new DatasetCallback() {
//
//            @Override
//            public void callback(DatasetEntry entry) {
//
//                String content = FileHelper.readFileToString(entry.getPath());
//                content = HTMLHelper.htmlToString(content, true);
//                content = StringHelper.removeControlCharacters(content);
//                content = content.replace("#", " ");
//                StringBuilder sb = new StringBuilder();
//                sb.append(content).append("#").append(StringUtils.join(entry.getTags().uniqueSet(), "#")).append("\n");
//
//                try {
//                    FileHelper.appendFile("tagData.txt", sb.toString());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    stop();
//                }
//
//                counter.increment();
//                if (counter.getCount() % 100 == 0) {
//                    System.out.println(counter.getCount());
//                }
//
//            }
//        };
//
//        reader.read(callback);
//    }
}
