package ws.palladian.extraction.location.experimental;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * <p>
 * Clean context CSV files created by {@link WikipediaEntityContextMiner}, remove all entries which have a total (all
 * types) occurrence count below a specified threshold.
 * </p>
 * 
 * @author Philipp Katz
 */
class WikipediaEntityContextPruner {

    static void prune(File inputPath, final int minTotalCount) throws IOException {
        // File inputPath = new File("/Users/pk/Desktop/WikipediaContexts");
        File outputPath = new File(inputPath, "pruned");
        outputPath.mkdirs();

        File[] files = FileHelper.getFiles(inputPath.getPath(), ".csv");
        for (File file : files) {
            File outputFile = new File(outputPath, file.getName());
            final Writer[] writer = new Writer[1];
            final int[] counter = new int[1];
            try {
                writer[0] = new BufferedWriter(new FileWriter(outputFile));
                FileHelper.performActionOnEveryLine(file.getPath(), new LineAction() {
                    @Override
                    public void performAction(String line, int lineNumber) {
                        if (lineNumber == 0) {
                            return;
                        }
                        String[] split = line.split("###");
                        int sum = 0;
                        for (int i = 1; i < split.length; i++) {
                            try {
                                sum += Integer.valueOf(split[i]);
                            } catch (NumberFormatException e) {
                                System.out.println("Error at line " + lineNumber + "(" + line + ")");
                                return;
                            }
                        }
                        if (sum > minTotalCount) {
                            try {
                                writer[0].append(line).append('\n');
                                counter[0]++;
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    }
                });
                System.out.println("Wrote " + counter[0] + " entries for " + file);
            } finally {
                FileHelper.close(writer);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        prune(new File("/Users/pk/Desktop/WikipediaContexts"), 100);
    }

}
