package ws.palladian.extraction.date;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public class DatasetConverter {
    public static void main(String[] args) {
        FileHelper.performActionOnEveryLine("/Users/pk/Desktop/mgregor/Diplomarbeit/evaluation/dbBackup/all.csv", new LineAction() {
            
            @Override
            public void performAction(String line, int lineNumber) {
                line = line.replace("\"", "");
                String[] split = line.split(";");
                int length = split.length;
                if (length != 31) {
                    System.err.println(length);
                    System.exit(0);
                }
                String result = "";
                for (int i = 3; i < split.length; i++) {
                    String modSplit = split[i];
                    if (modSplit.contains(" ")) {
                        modSplit = "'"+modSplit+"'";
                     }
                    result = result + modSplit + ";";
                }
                String className = split[2]; // 1 is pub, 2 is mod
                result += className;
                FileHelper.appendFile("/Users/pk/Desktop/dates_mod.csv", result + '\n');
            }
        });
    }

}
