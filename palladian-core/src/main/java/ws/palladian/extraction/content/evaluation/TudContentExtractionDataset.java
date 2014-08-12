package ws.palladian.extraction.content.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public final class TudContentExtractionDataset implements ContentExtractionDataset {

    private final List<File> txtFiles;
    private final Map<String, String> fileUrlMapping;

    public TudContentExtractionDataset(File tudDatasetDirectory) {
        Validate.notNull(tudDatasetDirectory, "tudDatasetDirectory must not be null");
        if (!tudDatasetDirectory.isDirectory()) {
            throw new IllegalArgumentException(tudDatasetDirectory + " is not a directory.");
        }
        this.fileUrlMapping = readFileUrlMapping(tudDatasetDirectory);
        this.txtFiles = FileHelper.getFiles(tudDatasetDirectory, Filters.fileExtension(".txt"));
    }

    private static Map<String, String> readFileUrlMapping(File tudDatasetDirectory) {
        final Map<String, String> mapping = CollectionHelper.newHashMap();
        File csvFile = new File(tudDatasetDirectory, "___index.csv");
        if (!csvFile.isFile()) {
            throw new IllegalStateException(csvFile + " does not exist.");
        }
        FileHelper.performActionOnEveryLine(csvFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split(";");
                if (split.length == 4) {
                    String pageId = split[0];
                    String url = split[1];
                    mapping.put(pageId, url);
                }
            }
        });
        return mapping;
    }

    @Override
    public Iterator<ContentExtractionPage> iterator() {
        return new AbstractIterator<ContentExtractionPage>() {
            Iterator<File> iterator = txtFiles.iterator();

            @Override
            protected ContentExtractionPage getNext() throws Finished {
                if (iterator.hasNext()) {
                    final File txtFile = iterator.next();
                    return new ContentExtractionPage() {

                        @Override
                        public File getHtmlFile() {
                            return new File(txtFile.getAbsolutePath().replace(".txt", ".html"));
                        }

                        @Override
                        public String getExpectedText() {
                            try {
                                return FileHelper.readFileToString(txtFile);
                            } catch (IOException e) {
                                throw new IllegalStateException(
                                        "Could not read "
                                                + txtFile
                                                + ", make sure, that there is a .txt file for each .html file in the directory.");
                            }
                        }

                        @Override
                        public String toString() {
                            return getHtmlFile().getPath();
                        }

                        @Override
                        public String getUrl() {
                            return fileUrlMapping.get(txtFile.getName().replace(".txt", ""));
                        }
                    };
                }
                throw FINISHED;
            }
        };
    }

    @Override
    public int size() {
        return txtFiles.size();
    }

    @Override
    public String toString() {
        return "TudContentExtractionDataset";
    }

}
