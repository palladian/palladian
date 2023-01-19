package ws.palladian.core.dataset.csv;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.AbstractDatasetWriter;
import ws.palladian.core.dataset.DatasetAppender;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;

import java.io.*;

import static ws.palladian.helper.io.FileHelper.DEFAULT_ENCODING;

public class CsvDatasetWriter extends AbstractDatasetWriter {

    private static final class CsvDatasetAppender extends AbstractDatasetAppender {
        private final FeatureInformation featureInformation;
        private final boolean writeCategory;
        private final char fieldSeparator;

        CsvDatasetAppender(Writer writer, FeatureInformation featureInformation, boolean writeCategory, char fieldSeparator) {
            super(writer);
            this.featureInformation = featureInformation;
            this.writeCategory = writeCategory;
            this.fieldSeparator = fieldSeparator;
        }

        @Override
        public void append(Instance instance) {
            StringBuilder line = new StringBuilder();
            int featureCount = 0;
            for (FeatureInformationEntry infoEntry : featureInformation) {
                if (featureCount++ > 0) {
                    line.append(fieldSeparator);
                }
                Value value = instance.getVector().get(infoEntry.getName());
                if (value != NullValue.NULL) {
                    line.append(value.toString());
                }

            }
            if (writeCategory) {
                line.append(fieldSeparator).append(instance.getCategory());
            }
            writeLine(line);
        }

        void writeHeader() {
            StringBuilder line = new StringBuilder();
            int headerCount = 0;
            for (FeatureInformationEntry infoEntry : featureInformation) {
                if (headerCount++ > 0) {
                    line.append(fieldSeparator);
                }
                line.append(infoEntry.getName());
            }
            if (writeCategory) {
                line.append(fieldSeparator).append("targetClass");
            }
            writeLine(line);
        }

    }

    private final CsvDatasetWriterConfig config;

    /**
     * Create a new {@link CsvDatasetWriter} with the given destination file.
     *
     * @param outputCsv The destination file.
     */
    public CsvDatasetWriter(File outputCsv) {
        this(outputCsv, false, true);
    }

    /**
     * Create a new {@link CsvDatasetWriter} with the given destination file.
     *
     * @param outputCsv     The destination file.
     * @param overwrite     <code>true</code> to overwrite, in case the file already
     *                      exists. If the file exists and this value is
     *                      <code>false</code>, an exception will be thrown.
     * @param writeCategory <code>true</code> to write the category column,
     *                      <code>false</code> to skip.
     */
    public CsvDatasetWriter(File outputCsv, boolean overwrite, boolean writeCategory) {
        this(CsvDatasetWriterConfig.filePath(outputCsv).overwrite(overwrite).writeCategory(writeCategory).createConfig());
    }

    public CsvDatasetWriter(CsvDatasetWriterConfig config) {
        if (config.getOutputCsv().exists()) {
            if (config.isOverwrite()) {
                if (!config.getOutputCsv().delete()) {
                    throw new IllegalStateException(config.getOutputCsv() + " already exists and cannot be deleted");
                }
            } else {
                throw new IllegalArgumentException(config.getOutputCsv() + " already exists");
            }
        }
        this.config = config;
    }

    @Override
    public DatasetAppender write(FeatureInformation featureInformation) {
        try {

            Writer writer = new BufferedWriter(new OutputStreamWriter(config.getOutputStream(), DEFAULT_ENCODING));

            CsvDatasetAppender appender = new CsvDatasetAppender(writer, featureInformation, config.isWriteCategory(), config.getFieldSeparator());
            appender.writeHeader();
            return appender;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
