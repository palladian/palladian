package tud.iir.classification.controlledtagging;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetFilter;
import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StringHelper;

public class Datasetwriter {

    public static void main(String[] args) {

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
