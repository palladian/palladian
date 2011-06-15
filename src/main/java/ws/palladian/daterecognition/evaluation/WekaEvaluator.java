package ws.palladian.daterecognition.evaluation;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.gui.explorer.Explorer;
import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.evaluation.weka.WekaClassifierEval;
import ws.palladian.daterecognition.searchengine.DataSetHandler;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.helper.date.DateComparator;

public class WekaEvaluator {
    private String db = "dateset";

    private void setDB(String db) {
        this.db = db;
    }

    private HashMap<Integer, String> allUrlMap = new HashMap<Integer, String>();
    private HashMap<String, Double> relSizeMap = new HashMap<String, Double>();
    private HashMap<Integer, ExtractedDate> allDateMap = new HashMap<Integer, ExtractedDate>();
    private HashMap<Integer, ExtractedDate> allWDMap = new HashMap<Integer, ExtractedDate>();

    /**
     * @param args
     */
    public static void main(String[] args) {

        // 0 - load Url and Serializer
        // 1 - evaluate KNIME Output
        int work = 1;

        if (work == 2) {
            Explorer.main(null);
        }
        if (work == 3) {

        }
        if (work == 0) {
            work0();
        }

        if (work == 1) {
            work1();
        }
    }

    private static void work0() {
        Classifier classifier = null;
        String classifierString = "data/wekaClassifier/classifier.model";
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(classifierString)));
            classifier = (Classifier) ois.readObject();

            WekaClassifierEval wce = new WekaClassifierEval();
            PageDateType classIndex = PageDateType.publish;
            String classAttributeName;
            Attribute classAttribute = null;
            Enumeration<Attribute> attributes;

            if (classIndex.equals(PageDateType.publish)) {
                classAttributeName = "pub";
            } else {
                classAttributeName = "mod";
            }

            BufferedReader reader;
            Instances instances = null;
            classifier = wce.getAtributeSelectedClassifier();

            // classifier = wce.getThreshold();
            try {
                reader = new BufferedReader(new FileReader("d:/wekaout/datesets/pubtrainee.arff"));
                instances = new Instances(reader);
                attributes = instances.enumerateAttributes();
                while (attributes.hasMoreElements()) {
                    Attribute attribute = attributes.nextElement();
                    if (attribute.name().equals(classAttributeName)) {
                        classAttribute = attribute;
                        break;
                    }
                }
                instances.setClass(classAttribute);
                classifier.buildClassifier(instances);
                SerializationHelper.write(classifierString, classifier);
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        File file = new File("d:/wekaout/datesets/pubtest.arff");
        BufferedReader reader;
        Instances instances = null;
        ArrayList<Integer> idList = new ArrayList<Integer>();
        try {
            reader = new BufferedReader(new FileReader(file));
            file = new File("d:/wekaout/datesets/pubTestTemp.arff");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
            String line;
            int i = 1;
            while ((line = reader.readLine()) != null) {
                if (i > 32) {
                    idList.add(Integer.valueOf(line.substring(0, 5)));
                    line = line.substring(6);
                }
                writer.write(line + "\n");
                i++;
            }
            writer.close();
            reader.close();
            reader = new BufferedReader(new FileReader(file));
            instances = new Instances(reader);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        instances.setClassIndex(1);
        Enumeration<Instance> instanceEnum = instances.enumerateInstances();
        HashMap<Integer, Double> resultMap = new HashMap<Integer, Double>();
        int i = 0;
        while (instanceEnum.hasMoreElements()) {
            Instance instance = instanceEnum.nextElement();
            int id = Integer.valueOf(instance.toString(0));
            // instance.setMissing(0);
            instance.setClassMissing();
            try {
                double[] dbl = classifier.distributionForInstance(instance);
                resultMap.put(idList.get(i), dbl[1]);
            } catch (Exception e) {
                System.out.println(classifier == null);
                e.printStackTrace();
            }
            i++;
        }

        DataSetHandler.setDB("wekaout");
        DataSetHandler.openConnection();
        for (Entry<Integer, Double> entry : resultMap.entrySet()) {
            String sqlQuery = "INSERT INTO threshold (yesPub, id) VALUES (" + entry.getValue() + ", " + entry.getKey()
                    + ")";
            try {
                DataSetHandler.st.execute(sqlQuery);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        DataSetHandler.closeConnection();
    }

    private static void work1() {
        // ths.buildClassifier(arg0)
        // ths.setClassifier()
        WekaEvaluator we = new WekaEvaluator();
        PageDateType pageDateType = PageDateType.publish;
        // PageDateType pageDateType = PageDateType.last_modified;

        String factorTable = "contentfactormaincontent";
        String table = "xfold";
        String wekaOutDB = "contentXFold10";

        // table = "weka10";
        // wekaOutDB = "wekaout";

        String file = "data/evaluation/daterecognition/datasets/trasholdX.txt";
        double lowBoundaryRelSize = 0;
        double upBoundRelSize = 1.1;
        boolean useBounds = false;
        // 253++
        for (int j = 1; j <= 10; j++) {
            for (int i = 1; i <= 1; i++) {
                System.out.println("Round: " + i);
                System.out.println();
                // table = "wekaout" + i;
                // weka2 - best set
                // table = "weka2";

                switch (i) {
                    case 1:
                        lowBoundaryRelSize = 1;
                        upBoundRelSize = 1.1;
                        break;
                    case 2:
                        lowBoundaryRelSize = 0.5;
                        upBoundRelSize = 1;
                        break;
                    case 3:
                        lowBoundaryRelSize = 0.3;
                        upBoundRelSize = 0.5;
                        break;
                    case 4:
                        lowBoundaryRelSize = 0.2;
                        upBoundRelSize = 0.3;
                        break;
                    case 5:
                        lowBoundaryRelSize = 0.1;
                        upBoundRelSize = 0.2;
                        break;
                    case 6:
                        lowBoundaryRelSize = 0.05;
                        upBoundRelSize = 0.1;
                        break;
                    case 7:
                        lowBoundaryRelSize = 0.02;
                        upBoundRelSize = 0.05;
                        break;
                    case 8:
                        lowBoundaryRelSize = 0.0;
                        upBoundRelSize = 0.02;
                        break;
                }
                // switch (i) {
                // case 1:
                // lowBoundaryRelSize = 1;
                // upBoundRelSize = 1.1;
                // break;
                // case 2:
                // lowBoundaryRelSize = 0.5;
                // upBoundRelSize =1;
                // break;
                // case 3:
                // lowBoundaryRelSize = 0.3;
                // upBoundRelSize = 0.5;
                // break;
                // case 4:
                // lowBoundaryRelSize = 0.2;
                // upBoundRelSize = 0.3;
                // break;
                // case 5:
                // lowBoundaryRelSize = 0.14;
                // upBoundRelSize = 0.2;
                // break;
                // case 6:
                // lowBoundaryRelSize = 0.1;
                // upBoundRelSize = 0.14;
                // break;
                // case 7:
                // lowBoundaryRelSize = 0.0625;
                // upBoundRelSize = 0.1;
                // break;
                // case 8:
                // lowBoundaryRelSize = 0.05;
                // upBoundRelSize = 0.0625;
                // break;
                // case 9:
                // lowBoundaryRelSize = 0.04;
                // upBoundRelSize = 0.05;
                // break;
                // case 10:
                // lowBoundaryRelSize = 0.02;
                // upBoundRelSize = 0.04;
                // break;
                // case 11:
                // lowBoundaryRelSize = 0.01;
                // upBoundRelSize = 0.02;
                // break;
                // case 12:
                // lowBoundaryRelSize = 0;
                // upBoundRelSize = 0.01;
                // break;
                // }

                // if (i == 101) {
                // lowBoundaryRelSize = 0;
                // upBoundRelSize = Math.round((1.0 / (double) (i - 1)) * 100) /
                // 100.0;
                // } else if (i == 1) {
                // lowBoundaryRelSize = Math.round((1.0 / (double) i) * 100) /
                // 100.0;
                // upBoundRelSize = 1.1;
                // } else {
                // lowBoundaryRelSize = Math.round((1.0 / (double) i) * 100) /
                // 100.0;
                // upBoundRelSize = Math.round((1.0 / (double) (i - 1)) * 100) /
                // 100.0;
                // }

                try {
                    we.evaluationOut(pageDateType, table + j, factorTable, file, wekaOutDB, useBounds,
                            lowBoundaryRelSize, upBoundRelSize);
                    // we.evaluationOut(pageDateType, table, factorTable, file,
                    // wekaOutDB, useBounds, lowBoundaryRelSize,
                    // upBoundRelSize);

                    System.out.println("lowBoundaryRelSize: " + lowBoundaryRelSize);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void evaluationOut(PageDateType pageDateType, String table, String factorTable, String file,
            String wekaOutDB, boolean useBounds, double lowBoundRS, double upBoundRS) throws SQLException {

        if (wekaOutDB != null && !wekaOutDB.equals("")) {
            setDB(wekaOutDB);
        }
        HashMap<Integer, Double> wekaMap = importWeka(pageDateType, table);

        setDB("dateset");
        HashMap<Integer, String> urlMap = importUrl(wekaMap, factorTable);

        HashMap<String, Integer> bestUrlId = findBestValue(wekaMap, urlMap, factorTable, pageDateType, useBounds,
                lowBoundRS, upBoundRS);

        double limit;
        System.out.println(" - ARD - AWD - ANF - AFR - AFW");

        double tempF1 = 0;
        double tempLimit = 0;
        int tempARD = 0;
        int tempAWD = 0;
        int tempANF = 0;
        int tempAFR = 0;
        int tempAFW = 0;

        for (int i = 0; i <= 100; i++) {
            limit = ((double) i) / 100.0;
            HashMap<String, Integer> importIdDate = importIdDate(bestUrlId, wekaMap, pageDateType, factorTable, limit);
            int ard = 0;
            int awd = 0;
            int anf = 0;
            int afr = 0;
            int afw = 0;
            /*
             * for(Entry<String, Integer> entry : importIdDate.entrySet()){
             * switch(entry.getValue()){ case DataSetHandler.ARD : ard++; break;
             * case DataSetHandler.AWD : awd++; break; case DataSetHandler.ANF :
             * anf++; break; case DataSetHandler.AFR : afr++; break; case
             * DataSetHandler.AFW : afw++; break; } }
             */

            ard = importIdDate.get("ARD");
            awd = importIdDate.get("AWD");
            anf = importIdDate.get("ANF");
            afr = importIdDate.get("AFR");
            afw = importIdDate.get("AFW");

            double precession = Math.round(((double) (afr) / (double) (awd + afr + afw)) * 1000.0) / 1000.0;
            double recall = Math.round(((double) afr / (double) (afr + anf + afw)) * 1000.0) / 1000.0;
            double f1 = (2 * precession * recall) / (precession + recall);

            System.out.print(limit + " - ");
            System.out.print(ard + " - ");
            System.out.print(awd + " - ");
            System.out.print(anf + " - ");
            System.out.print(afr + " - ");
            System.out.print(afw);
            // System.out.print(" - F1: " + f1);
            System.out.println();

            if (f1 > tempF1) {
                tempLimit = limit;
                tempF1 = f1;
                tempARD = ard;
                tempAWD = awd;
                tempANF = anf;
                tempAFR = afr;
                tempAFW = afw;
            }

            // System.out.print(accuracy + " - ");
            // System.out.println(dedection);
        }

        File f = new File(file);
        FileWriter fw;
        try {
            fw = new FileWriter(f, true);
            BufferedWriter bw = new BufferedWriter(fw);
            String line = (1 / lowBoundRS) + " - " + (1 / upBoundRS) + " - -  " + tempLimit + " - " + tempARD + " - "
                    + tempAWD + " - " + tempANF + " - " + tempAFR + " - " + tempAFW + "\n";
            bw.write(line);
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private HashMap<Integer, Double> importWeka(PageDateType pageDateType, String table) throws SQLException {
        HashMap<Integer, Double> wekaMap = new HashMap<Integer, Double>();
        String yesType = pageDateType.equals(PageDateType.publish) ? "yesPub" : "yesMod";

        DataSetHandler.setDB(this.db);
        DataSetHandler.openConnection();
        String sqlQuery = "SELECT * FROM " + table;

        ResultSet rs = DataSetHandler.st.executeQuery(sqlQuery);
        while (rs.next()) {
            int id = rs.getInt("id");
            double yesValue = rs.getDouble(yesType);
            wekaMap.put(id, yesValue);
        }
        DataSetHandler.closeConnection();
        return wekaMap;
    }

    private HashMap<Integer, String> importUrl(HashMap<Integer, Double> wekaMap, String factorTable) {
        HashMap<Integer, String> urlMap = new HashMap<Integer, String>();
        DataSetHandler.setDB(this.db);
        DataSetHandler.openConnection();

        if (allUrlMap == null || allUrlMap.size() == 0) {
            try {
                String sqlQuery = "SELECT * FROM " + factorTable;
                ResultSet rs = DataSetHandler.st.executeQuery(sqlQuery);
                while (rs.next()) {
                    allUrlMap.put(rs.getInt("id"), rs.getString("url"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        for (Entry<Integer, Double> entry : wekaMap.entrySet()) {
            String url = allUrlMap.get(entry.getKey());
            urlMap.put(entry.getKey(), url);
        }

        DataSetHandler.closeConnection();
        return urlMap;
    }

    private HashMap<String, Integer> findBestValue(HashMap<Integer, Double> wekaMap, HashMap<Integer, String> urlMap,
            String factorTable, PageDateType pageDateType, boolean useBound, double lowBoundRS, double upBoundRS) {
        HashMap<String, Integer> bestUrlId = new HashMap<String, Integer>();

        if (allDateMap == null || allDateMap.size() == 0) {
            DataSetHandler.setDB(this.db);
            DataSetHandler.openConnection();
            String sqlQuery = "SELECT * FROM " + factorTable;
            ResultSet rs;
            try {
                rs = DataSetHandler.st.executeQuery(sqlQuery);
                while (rs.next()) {
                    allDateMap.put(rs.getInt("id"), DateGetterHelper.findDate(rs.getString("date")));
                    if (useBound) {
                        relSizeMap.put(rs.getString("url"), rs.getDouble("relSize"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            DataSetHandler.closeConnection();
        }

        for (Entry<Integer, String> entry : urlMap.entrySet()) {
            String url = entry.getValue();
            if (!useBound || (lowBoundRS <= relSizeMap.get(url) && relSizeMap.get(url) < upBoundRS)) {
                int newId = entry.getKey();
                double newValue = wekaMap.get(newId);
                Integer oldId = bestUrlId.get(url);
                if (oldId == null || wekaMap.get(oldId) <= newValue) {
                    if (oldId != null && wekaMap.get(oldId) == newValue) {
                        ExtractedDate oldDate = allDateMap.get(oldId);
                        ExtractedDate newDate = allDateMap.get(newId);
                        DateComparator dc = new DateComparator();

                        int comp = dc.compare(oldDate, newDate, DateComparator.STOP_DAY);
                        if (comp > 0 && pageDateType.equals(PageDateType.publish)) {
                            bestUrlId.put(url, newId);
                            // System.out.println(oldDate + " - " + newDate);
                        } else if (comp == -1 && pageDateType.equals(PageDateType.last_modified)) {
                            bestUrlId.put(url, newId);
                            // System.out.println(oldDate + " - " + newDate);
                        }
                    } else {
                        bestUrlId.put(url, newId);
                    }
                }
            }
        }
        System.out.println("Size: " + bestUrlId.size());
        return bestUrlId;
    }

    private HashMap<String, Integer> importIdDate(HashMap<String, Integer> bestUrlId, HashMap<Integer, Double> wekaMap,
            PageDateType pageDateType, String factorTable, double limit) {
        // HashMap<String, Integer> urlIdDate = new HashMap<String, Integer>();
        HashMap<String, Integer> cntMap = new HashMap<String, Integer>();
        cntMap.put("ARD", 0);
        cntMap.put("AWD", 0);
        cntMap.put("ANF", 0);
        cntMap.put("AFR", 0);
        cntMap.put("AFW", 0);

        if (allWDMap == null || allWDMap.size() == 0) {
            DataSetHandler.setDB(this.db);
            DataSetHandler.openConnection();
            String dateType = pageDateType.equals(PageDateType.publish) ? "pubDate" : "modDate";
            String sqlQuery = "SELECT * FROM " + factorTable;
            ResultSet rs;
            try {
                rs = DataSetHandler.st.executeQuery(sqlQuery);
                while (rs.next()) {
                    allWDMap.put(rs.getInt("id"), DateGetterHelper.findDate(rs.getString(dateType)));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            DataSetHandler.closeConnection();
        }

        DateComparator dc = new DateComparator();
        for (Entry<String, Integer> entry : bestUrlId.entrySet()) {
            int id = entry.getValue();
            ExtractedDate pageDate = allWDMap.get(id);
            ExtractedDate wekaDate = null;
            if (wekaMap.get(entry.getValue()) >= limit) {
                wekaDate = allDateMap.get(id);
            }

            // Integer result;
            String classType;
            if (pageDate == null) {
                if (wekaDate == null) {
                    classType = "ARD";
                    // result = DataSetHandler.ARD;
                } else {
                    classType = "AWD";
                    // result = DataSetHandler.AWD;
                }
            } else {
                if (wekaDate == null) {
                    classType = "ANF";
                    // result = DataSetHandler.ANF;
                } else {
                    // System.out.println(pageDate.getNormalizedDateString() +
                    // " - " + wekaDate.getNormalizedDateString());
                    int compare = dc.compare(pageDate, wekaDate, dc.getCompareDepth(pageDate, wekaDate));
                    if (compare == 0) {
                        classType = "AFR";
                        // result = DataSetHandler.AFR;
                    } else {
                        classType = "AFW";
                        // result = DataSetHandler.AFW;
                    }
                }
            }
            // urlIdDate.put(entry.getKey(), result);
            Integer cnt = cntMap.get(classType);
            if (cnt == null) {
                cnt = 0;
            }
            cnt++;
            cntMap.put(classType, cnt);
        }

        // return urlIdDate;
        return cntMap;
    }

}
