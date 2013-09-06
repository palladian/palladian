/**
 * Created on: 23.10.2012 11:16:17
 */
package ws.palladian.extraction.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.InputPort;
import ws.palladian.processing.OutputPort;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Writes the {@link FeatureVector} of the processed document to a CSV file.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class CsvWriter extends AbstractPipelineProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvWriter.class);

    private final List<String> featurePaths;
    private final String csvFilePath;

    public CsvWriter(String csvFilePath, Collection<String> featurePaths) {
        super(new InputPort[] {new InputPort(DEFAULT_INPUT_PORT_IDENTIFIER)}, new OutputPort[0]);

        this.featurePaths = new ArrayList<String>(featurePaths);
        this.csvFilePath = csvFilePath;
        StringBuffer header = new StringBuffer("");
        for (String featurePath : this.featurePaths) {
            header.append("\"" + featurePath + "\",");
        }
        header.replace(header.length() - 1, header.length(), "\n");

        boolean success = FileHelper.writeToFile(csvFilePath, header);
        if (!success) {
            throw new IllegalStateException("Error writing to \"" + csvFilePath);
        }
    }

    public CsvWriter(String csvFilePath, String... featurePaths) throws IOException {
        this(csvFilePath, Arrays.asList(featurePaths));
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        StringBuffer dataLine = new StringBuffer("");
        PipelineDocument<?> document = getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER).poll();
        for (String featurePath : featurePaths) {
            Feature<?> feature = document.getFeatureVector().get(featurePath);
            if (feature == null) {
                // if (feature == null) {
                LOGGER.warn("Unable to find feature for feature path: " + featurePath);
                dataLine.append("?,");
            } else {
                // XXX only take the first feature currently
                Object featureValue = feature.getValue();
                dataLine.append(featureValue + ",");
            }
        }
        dataLine.replace(dataLine.length() - 1, dataLine.length(), "\n");
        boolean success = FileHelper.appendFile(csvFilePath, dataLine);
        if (!success) {
            throw new DocumentUnprocessableException("Error appending file \"" + csvFilePath + "\"");
        }
    }

}
