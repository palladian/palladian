package ws.palladian.extraction.content.evaluation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

public final class CleanevalDataset implements ContentExtractionDataset {

    private final File cleanEvalDirectory;
    private final List<File> txtFiles;

    public CleanevalDataset(File cleanEvalDirectory) {
        Validate.notNull(cleanEvalDirectory, "cleanEvalDirectory must not be null");
        this.cleanEvalDirectory = cleanEvalDirectory;
        this.txtFiles = FileHelper.getFiles(cleanEvalDirectory, new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".txt");
            }
        });
    }

    @Override
    public Iterator<ContentExtractionDatasetItem> iterator() {
        return new AbstractIterator<ContentExtractionDatasetItem>() {
            Iterator<File> iterator = txtFiles.iterator();

            @Override
            protected ContentExtractionDatasetItem getNext() throws Finished {
                if (iterator.hasNext()) {
                    final File txtFile = iterator.next();
                    return new ContentExtractionDatasetItem() {

                        @Override
                        public File getHtmlFile() {
                            return new File(txtFile.getAbsolutePath().replace(".txt", ".html"));
                        }

                        @Override
                        public String getExpectedText() {
                            try {
                                String textContent = FileHelper.readFileToString(txtFile);
                                return cleanup(textContent);
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
                    };
                }
                throw FINISHED;
            }
        };
    }

    private static final String cleanup(String expectedText) {
        expectedText = expectedText.replaceAll("URL: [^ ]+", "");
        expectedText = expectedText.replaceAll("\\<.+?\\>", "");
        expectedText = StringHelper.replaceProtectedSpace(expectedText);
        expectedText = StringHelper.removeLineBreaks(expectedText);
        expectedText = expectedText.replaceAll("\\s+", " ");
        expectedText = expectedText.trim();
        return expectedText;
    }

    @Override
    public int size() {
        return txtFiles.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CleanevalDataset [cleanEvalDirectory=");
        builder.append(cleanEvalDirectory);
        builder.append("]");
        return builder.toString();
    }

    public static void main(String[] args) {
        CleanevalDataset dataset = new CleanevalDataset(new File("/Users/pk/Desktop/CleanEval"));
        for (ContentExtractionDatasetItem item : dataset) {
            System.out.println(item);
            System.out.println(item.getExpectedText());
            System.exit(0);
        }
    }

}
