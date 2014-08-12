package ws.palladian.extraction.content.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Reader for the CleanEval dataset. See: '<a
 * href="http://clic.cimec.unitn.it/marco/publications/lrec2008/lrec08-cleaneval.pdf">CleanEval: a competition for
 * cleaning webpages</a>'; Marco Baroni, Francis Chantree, Adam Kilgarriff, Serge Sharoff, 2008. The dataset can be
 * downloaded from <a href="http://nlp.fi.muni.cz/~xpomikal/cleaneval/">here</a>; necessary are the files
 * 'GoldStandard.tar.gz' and 'finalrun-input.tar.gz'.
 * 
 * @author pk
 */
public final class CleanevalDataset implements ContentExtractionDataset {

    private final List<File> txtFiles;

    public CleanevalDataset(File cleanEvalDirectory) {
        Validate.notNull(cleanEvalDirectory, "cleanEvalDirectory must not be null");
        this.txtFiles = FileHelper.getFiles(cleanEvalDirectory, Filters.fileExtension("txt"));
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

                        @Override
                        public String getUrl() {
                            return null;
                        }
                    };
                }
                throw FINISHED;
            }
        };
    }

    private static final String cleanup(String expectedText) {
        String cleanText = expectedText.replaceAll("URL: [^\\s]+", "");
        cleanText = cleanText.replaceAll("<[^>]*>", "");
        cleanText = StringHelper.replaceProtectedSpace(cleanText);
        cleanText = StringHelper.removeLineBreaks(cleanText);
        cleanText = cleanText.replaceAll("\\s+", " ");
        cleanText = cleanText.trim();
        return cleanText;
    }

    @Override
    public int size() {
        return txtFiles.size();
    }

    @Override
    public String toString() {
        return "CleanEvalDataset";
    }

}
