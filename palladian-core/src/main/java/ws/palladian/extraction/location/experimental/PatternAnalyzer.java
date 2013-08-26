package ws.palladian.extraction.location.experimental;

import java.io.File;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.location.ContextClassifier;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

public class PatternAnalyzer {

    public static enum Direction {
        LEFT, RIGHT
    }

    static CountMatrix<String> getPatterns(File inputFile, final Direction direction, final int size) {
        final CountMatrix<String> countMatrix = CountMatrix.create();
        final int numLines = FileHelper.getNumberOfLines(inputFile);
        final ProgressMonitor monitor = new ProgressMonitor(numLines, 1);
        FileHelper.performActionOnEveryLine(inputFile, new LineAction() {
            StringBuilder buffer = new StringBuilder();

            @Override
            public void performAction(String line, int lineNumber) {
                monitor.incrementAndPrintProgress();
                if (line.startsWith("=-DOCSTART-")) {
                    String text = buffer.toString();
                    text = StringHelper.replaceProtectedSpace(text);
                    text = StringHelper.normalizeQuotes(text);
                    String cleanText = HtmlHelper.stripHtmlTags(text);
                    Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromXmlText(text);
                    for (ContextAnnotation annotation : annotations) {
                        String tagName = annotation.getTag();
                        // String context = getLeftContext(annotation, cleanText, 1);
                        String context;
                        if (direction == Direction.LEFT) {
                            context = ContextClassifier.getLeftContext(annotation, cleanText, size);
                        } else {
                            context = ContextClassifier.getRightContext(annotation, cleanText, size);
                        }
                        boolean noTab = context != null && !context.contains("\t"); // XXX because of CSV issues
                        if (context != null && context.length() > 0 && noTab) {
                            countMatrix.add(tagName, new String(context).toLowerCase());
                        }
                    }
                    buffer = new StringBuilder();
                } else {
                    buffer.append(line).append('\n');
                }
            }
        });
        String fileName = FileHelper.getFileName(inputFile.getName()) + "_" + size + "_" + direction + ".tsv";
        FileHelper.writeToFile(fileName, countMatrix.toString());
        return countMatrix;
    }

    static void createProximity(File inputFile) {
        final CountMatrix<String> countMatrix = CountMatrix.create();
        FileHelper.performActionOnEveryLine(inputFile, new LineAction() {
            String[] headers;

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.trim().split("\\t");
                if (lineNumber == 0) {
                    headers = split;
                    return;
                }
                String[] tokens = split[0].split("\\s");
                for (int i = 1; i < split.length; i++) {
                    Integer count = "null".equals(split[i]) ? 0 : Integer.valueOf(split[i]);
                    for (String token : tokens) {
                        countMatrix.add(headers[i - 1], StringHelper.trim(token), count);
                    }
                }
            }
        });
        String fileName = FileHelper.getFileName(inputFile.getName()) + "_proximity.tsv";
        FileHelper.writeToFile(fileName, countMatrix.toString());
    }



    public static void main(String[] args) throws Exception {
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.LEFT, 1);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.LEFT, 2);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.LEFT, 3);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.RIGHT, 1);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.RIGHT, 2);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.RIGHT, 3);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.LEFT, 4);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.LEFT, 5);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.RIGHT, 4);
        // getPatterns(new File("/Users/pk/Desktop/allCleansed.xml"), Direction.RIGHT, 5);
        // createProximity(new File("allCleansed_2_LEFT.tsv"));
        // createProximity(new File("allCleansed_2_RIGHT.tsv"));
        // createProximity(new File("allCleansed_3_LEFT.tsv"));
        // createProximity(new File("allCleansed_3_RIGHT.tsv"));
        // createProximity(new File("allCleansed_4_LEFT.tsv"));
        // createProximity(new File("allCleansed_4_RIGHT.tsv"));
        createProximity(new File("allCleansed_5_LEFT.tsv"));
        createProximity(new File("allCleansed_5_RIGHT.tsv"));
    }

}
