package ws.palladian.helper.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created on 18.11.2019
 */
public class CsvWriter implements Closeable {
    public static CSVFormat getCSVFormat() {
        return CSVFormat.newFormat(';').withQuoteMode(QuoteMode.NON_NUMERIC).withQuote('"').withRecordSeparator("\n");
    }

    private FileWriter fileWriter;
    private BufferedWriter out;
    private CSVPrinter csvPrinter;
    private String outputFilePath;

    public CsvWriter(String outputFilePath) {
        try {
            this.outputFilePath = outputFilePath;
            this.fileWriter = new FileWriter(outputFilePath);
            this.out = new BufferedWriter(fileWriter);
            this.csvPrinter = new CSVPrinter(out, CsvWriter.getCSVFormat());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CsvWriter(Appendable out) throws IOException {
        this.csvPrinter = new CSVPrinter(out, CsvWriter.getCSVFormat());
    }

    public String getFilePath() {
        return outputFilePath;
    }

    public boolean tryAddRow(Iterable<?> values) {
        try {
            addRow(values);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean tryAddRow(Object... values) {
        try {
            addRow(values);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void addRow(Iterable<?> values) throws IOException {
        csvPrinter.printRecord(values);
    }

    public void addRow(Object... values) throws IOException {
        csvPrinter.printRecord(values);
    }

    public boolean tryAddRows(List<List<String>> rows) {
        try {
            addRows(rows);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void addRows(List<List<String>> rows) throws IOException {
        csvPrinter.printRecords(rows);
    }

    public boolean tryAddColumn(Object value) {
        try {
            addColumn(value);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void addColumn(Object value) throws IOException {
        csvPrinter.print(value);
    }

    public boolean tryNewRow() {
        try {
            newRow();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void newRow() throws IOException {
        csvPrinter.println();
    }

    @Override
    public void close(){
        try {
            csvPrinter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.close(csvPrinter, out, fileWriter);
        }
    }
}
