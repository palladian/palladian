package tud.iir.classification.controlledtagging;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetFilter;
import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StringHelper;

public class Datasetwriter {

    public static void writeFromFAO() {

        // final String PATH = "/Users/pk/Desktop/fao780";
        final String PATH = "/home/pk/Desktop/fao780";

        File[] files = FileHelper.getFiles(PATH, ".txt");
        for (File file : files) {
            String textFile = file.getName();
            String keyFile = textFile.replace(".txt", ".key");
            // System.out.println(keyFile);

            String text = FileHelper.readFileToString(PATH + "/" + textFile);
            text = StringHelper.removeNonAsciiCharacters(text);
            text = text.replace("#", " ");
            text = text.replace("\n", " ");

            List<String> keywords = FileHelper.readFileToArray(PATH + "/" + keyFile);

            String line = text + "#" + StringUtils.join(keywords, "#") + "\n";
            System.out.println(textFile);

            try {
                FileHelper.appendFile("fao.txt", line);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

        }

    }

    public static void writeFromFAONew() {

        // final String PATH = "/Users/pk/Desktop/fao780";
        final String PATH = "/home/pk/Desktop/fao780";

        File[] files = FileHelper.getFiles(PATH, ".key");

        for (File file : files) {

            String keyFile = file.getName();
            List<String> keywords = FileHelper.readFileToArray(PATH + "/" + keyFile);

            String line = keyFile.replace(".key", ".txt") + "#" + StringUtils.join(keywords, "#") + "\n";
            System.out.println(line);

            try {
                FileHelper.appendFile("fao_index.txt", line);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

        }

    }

    public static void main(String[] args) {

        writeFromFAONew();

        // writeFromFAO();
        // writeFromT140();

    }

    private static void writeFromT140() {
        // final StringBuilder sb = new StringBuilder();
        final Counter counter = new Counter();
        DeliciousDatasetReader reader = new DeliciousDatasetReader();

        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        filter.setMaxFileSize(600000);
        reader.setFilter(filter);

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());
                content = HTMLHelper.htmlToString(content, true);
                content = StringHelper.removeControlCharacters(content);
                content = content.replace("#", " ");
                StringBuilder sb = new StringBuilder();
                sb.append(content).append("#").append(StringUtils.join(entry.getTags().uniqueSet(), "#")).append("\n");

                try {
                    FileHelper.appendFile("tagData.txt", sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    stop();
                }

                counter.increment();
                if (counter.getCount() % 100 == 0) {
                    System.out.println(counter.getCount());
                }

            }
        };

        reader.read(callback);
    }
}
