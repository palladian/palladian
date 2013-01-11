package ws.palladian.experimental;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import prefuse.data.Table;
import prefuse.data.io.CSVTableReader;
import prefuse.data.io.DataIOException;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Transforms a 2D Matrix representation with three columns in a 3D representation. This is required for 3D graphics in
 * the feed paper. The input needs to be a CSV file <big>with</big> headers. The output is a CSV Matrix. An example is
 * shown below.
 * </p>
 * <p>
 * Example:
 * 
 * <pre>
 * real	predicted	count
 * 1	1		1
 * 1	2		1
 * 2	1		1
 * 3	2		1
 * 4	3		1
 * 5	4		2
 * 5	5		2
 * 6	6		2
 * 6	7		2
 * 7	6		1
 * 7	7		3
 * 8	8		3
 * 9	8		1
 * 9	9		1
 * 10	10		2
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * 	    1	2	3	4	5	6	7	8	9	10
 * 1	1	1
 * 2	1
 * 3		1
 * 4			1
 * 5				2	2
 * 6						2	2
 * 7						1	3
 * 8								3
 * 9								1	1
 * 10										2
 * </pre>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * 
 */
public final class MatrixCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatrixCreator.class);

    /**
     * <p>
     * The entry point to the program.
     * </p>
     * 
     * @param args
     *            The first parameter is the input the second the output file.
     *            The output file is created if it is not yet available. The
     *            third parameter is an optional symbol that is used as
     *            separator in the input. If ommitted the default is ",".
     * @throws DataIOException
     *             If input file can not be accessed or is not valid.
     * @throws IOException
     *             If output can not be written.
     * @throws FileNotFoundException
     *             if the output file exists but is a directory rather than a
     *             regular file, does not exist but cannot be created, or cannot
     *             be opened for any other reason.
     */
    public static void main(String[] args) throws DataIOException,
	    FileNotFoundException, IOException {
	CSVTableReader csvReader = new CSVTableReader();
	Table table = null;
        String cellSeperator = ",";
	if (args.length == 3) {
            cellSeperator = args[2];
	    String input = FileHelper.readFileToString(args[0]);
            String cleanedInput = input.replace(cellSeperator, ",");

	    LOGGER.info(cleanedInput);
	    ByteArrayInputStream cleanedInputStream = new ByteArrayInputStream(
		    cleanedInput.getBytes());
	    table = csvReader.readTable(cleanedInputStream);
	} else {
	    table = csvReader.readTable(args[0]);
	}

	Table matrix = new Table();
	for (int i = 0; i < table.getRowCount(); i++) {
	    LOGGER.info("Processing input line: " + i);
	    Integer rowIndex = (Integer) table.get(i, 0);
	    Integer columnIndex = (Integer) table.get(i, 1);
	    Integer value = (Integer) table.get(i, 2);

	    if (matrix.getRowCount() < rowIndex) {
		for (int j = matrix.getRowCount(); j < rowIndex; j++) {
		    LOGGER.info("Adding new Row: " + j);
		    matrix.addRow();
		}
	    }

	    if (matrix.getColumnCount() < columnIndex) {
		for (int k = matrix.getColumnCount(); k < columnIndex; k++) {
		    LOGGER.info("Adding new Column: " + k);
		    matrix.addColumn(String.valueOf(k), String.class, "");
		}
	    }

	    matrix.setString(rowIndex.intValue() - 1,
		    columnIndex.intValue() - 1, value.toString());
	}

	StringBuffer output = new StringBuffer();
	for (int i = 0; i < matrix.getColumnCount(); i++) {
            output.append(cellSeperator + (i + 1));
	}
	output.append("\n");

	for (int j = 0; j < matrix.getRowCount(); j++) {
	    output.append(j + 1);
	    for (int k = 0; k < matrix.getColumnCount(); k++) {
		Object value = matrix.get(j, k);
                output.append(cellSeperator + value);
	    }
	    output.append("\n");
	}
	FileHelper.writeToFile(args[1], output);
    }

}
