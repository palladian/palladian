package ws.palladian.persistence;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>A simple CSV-based file database. This is more convenient for simple tasks and processing small or medium-sized files than using an RDBS.</p>
 * @author David Urbansky
 *
 */
public class CsvDb {

	private List<String> rows = new ArrayList<String>();
	private String separator = ";";
	
	public CsvDb(String csvFilePath, String separator) {
		
		rows = FileHelper.readFileToArray(csvFilePath);
		this.separator = separator;
		
	}
	
	public List<String> getRowsWhere(int column, String value) {
		
		List<String> response = new ArrayList<String>();
		
		for (String row : rows) {
			String[] parts = row.split(separator);
			if (parts[column].equals(value)) {
				response.add(row);
			}
		}
		
		return response;
	}
	
	public List<String> getColumn(int column) {
		
		List<String> response = new ArrayList<String>();
		
		for (String row : rows) {
			String[] parts = row.split(separator);
			response.add(parts[column]);
		}
		
		return response;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CsvDb csvDb = new CsvDb("data/temp/seedsPlain.txt",";");
		List<String> rows = csvDb.getRowsWhere(0, "3");
		
		CollectionHelper.print(rows);
		
		CollectionHelper.print(MathHelper.randomSample(rows, 3));
		CollectionHelper.print(MathHelper.randomSample(rows, 3));
		CollectionHelper.print(MathHelper.randomSample(rows, 3));
		
	}

}
