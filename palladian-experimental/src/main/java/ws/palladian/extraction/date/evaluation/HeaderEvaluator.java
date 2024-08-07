package ws.palladian.extraction.date.evaluation;

import org.w3c.dom.Document;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.getter.HeadDateGetter;
import ws.palladian.extraction.date.getter.TechniqueDateGetter;
import ws.palladian.extraction.date.getter.UrlDateGetter;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.extraction.date.rater.HeadDateRater;
import ws.palladian.extraction.date.rater.TechniqueDateRater;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.DocumentRetriever;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HeaderEvaluator {

    /**
     * @param args
     */
    public static void main(String[] args) {

        String file = "data/evaluation/daterecognition/datasets/headdataset.txt";
        //DataSetHandler.addCloumn(EvaluationHelper.HEADEVAL, "mod2", "INT", "-10");

        TechniqueDateGetter<MetaDate> dg = new HeadDateGetter();
        TechniqueDateRater<MetaDate> dr = new HeadDateRater(PageDateType.PUBLISH);

        //TestHeadDateRater testDR = new TestHeadDateRater(PageDateType.publish);

        String pub = "pub1";
        String mod = "mod1";
        int des = 3;
        switch (des) {
            case 0:
                String in = "D:/_Uni/_semester16/dataset/urlSet18.htm";
                String out = "D:/_Uni/_semester16/dataset/HeadUrlSet18.htm";
                //countHeadURls(in, out, (HeadDateGetter)dg);
                createHeadUrlList(in, out, (HeadDateGetter) dg);
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                evaluate(pub, DBExport.PUB_DATE, dg, dr, file);
                evaluate(mod, DBExport.MOD_DATE, dg, dr, file);
                break;
        }
    }

    private static <T extends ExtractedDate> void evaluate(String round, int pub_mod, TechniqueDateGetter<T> dg, TechniqueDateRater<T> dr, String file) {
        String table = EvaluationHelper.HEADEVAL;
        int rnf = 0;
        int ff = 0;
        int wnf = 0;
        int rf = 0;
        int wf = 0;
        int counter = 0;
        int compare;

        Map<String, DBExport> set = EvaluationHelper.readFile(file);
        DocumentRetriever crawler = new DocumentRetriever();

        for (Entry<String, DBExport> e : set.entrySet()) {
            T bestDate = null;
            String bestDateString = "";
            String url = e.getValue().get(DBExport.URL);

            System.out.print("get dates... ");
            StopWatch timer = new StopWatch();
            List<T> list;
            //System.out.println(url);
            if (table.equalsIgnoreCase(EvaluationHelper.CONTENTEVAL) || table.equalsIgnoreCase(EvaluationHelper.STRUCTEVAL) || table.equalsIgnoreCase(EvaluationHelper.HEADEVAL)) {
                String path = e.getValue().get(DBExport.PATH);
                Document document = crawler.getWebDocument(path);
                list = dg.getDates(document);
            } else {
                list = dg.getDates(url);
            }

            timer.stop();
            timer.getElapsedTimeString(true);
            CollectionHelper.removeNulls(list);

            if (list.size() > 0) {

                List<T> filteredDates = DateExtractionHelper.filterFullDate(list);
                filteredDates = DateExtractionHelper.filterByRange(filteredDates);

                if (dg instanceof UrlDateGetter) {
                    filteredDates = DateExtractionHelper.filterByRange(list);
                }

                if (filteredDates.size() > 0) {

                    //System.out.print("rate dates... ");
                    //System.out.print("best date... ");
                    RatedDate<T> temp = dr.getBest(filteredDates);
                    if (temp != null) {
                        bestDateString = temp.getDate().getNormalizedDateString(true);
                    }
                }
            }
            //System.out.println("compare...");

            compare = EvaluationHelper.compareDate(bestDate, e.getValue(), pub_mod);
            ExtractedDate date;
            String dbExportDateString;
            if (pub_mod == DBExport.PUB_DATE) {
                date = DateParser.findDate(e.getValue().getPubDate());
                dbExportDateString = " - pubDate:";
            } else {
                date = DateParser.findDate(e.getValue().getModDate());
                dbExportDateString = " - modDate:";
            }

            if (date != null) {
                dbExportDateString += date.getNormalizedDateString();
            }

            //System.out.print(compare + " bestDate:" + bestDateString + dbExportDateString);

            switch (compare) {
                case EvaluationHelper.AFW:
                    wf++;
                    System.out.println(url);
                    System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
                    //System.out.println("-------------------------------------------------------");
                    break;
                case EvaluationHelper.ANF:
                    System.out.println(url);
                    System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
                    //System.out.println("-------------------------------------------------------");
                    wnf++;
                    break;
                case EvaluationHelper.AWD:
                    System.out.println(url);
                    System.out.println(compare + " bestDate:" + bestDateString + dbExportDateString);
                    //System.out.println("-------------------------------------------------------");
                    ff++;
                    break;
                case EvaluationHelper.ARD:
                    rnf++;
                    break;
                case EvaluationHelper.AFR:
                    rf++;
                    break;

            }

            counter++;

            //System.out.println();
            System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
            System.out.println("---------------------------------------------------------------------");

        }
        //System.out.println("all: " + counter + " RF: " + rf + " RNF: " + rnf + " WF: " + wf + " FF: " + ff + " WNF: " + wnf);
    }

    private static void countHeadURls(String in, String out, HeadDateGetter dg) {
        Map<String, DBExport> set = EvaluationHelper.readFile(in);
        ArrayList<DBExport> headSet = new ArrayList<DBExport>();
        DocumentRetriever c = new DocumentRetriever();
        int index = 0;
        for (Entry<String, DBExport> e : set.entrySet()) {
            System.out.println(index + ": " + e.getKey());
            List<MetaDate> dates = dg.getDates(c.getWebDocument(e.getValue().get(DBExport.PATH)));
            if (dates.size() != 0) {
                headSet.add(e.getValue());
            }
            index++;
        }

        try {
            File file = new File(out);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(
                    "url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date \n");
            for (int i = 0; i < headSet.size(); i++) {
                bw.write(headSet.get(i).toString() + "\n");
            }
            bw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createHeadUrlList(String in, String out, HeadDateGetter dg) {
        ArrayList<String> headSet = new ArrayList<String>();
        try {

            File file = new File(in);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            DocumentRetriever c = new DocumentRetriever();
            int lineindex = 0;
            while ((line = br.readLine()) != null) {
                if (lineindex > 1) {
                    int indexStart = line.indexOf('>');
                    int indexEnd = line.indexOf("</a>");

                    if (indexStart > -1 && indexEnd > -1) {
                        String url = line.substring(indexStart + 1, indexEnd);
                        System.out.println(lineindex + ": " + url);
                        List<MetaDate> dates = dg.getDates(c.getWebDocument(url));
                        if (!dates.isEmpty()) {
                            System.out.println("+");
                            headSet.add(line);
                        }
                    }
                }

                lineindex++;
            }
            br.close();
            fr.close();
        } catch (IOException e) {

        }

        try {
            File file = new File(out);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);

            for (int i = 0; i < headSet.size(); i++) {
                bw.write(headSet.get(i) + "\n");
            }

            bw.close();
            fw.close();
        } catch (Exception e) {

        }
    }

    private static void mergeUrlsets(String in1, String in2, String out) {

        Map<String, DBExport> set1 = EvaluationHelper.readFile(in1);
        Map<String, DBExport> set2 = EvaluationHelper.readFile(in2);
        System.out.println(set1.size());
        System.out.println(set2.size());
        Map<String, DBExport> merged = new HashMap<String, DBExport>();
        merged.putAll(set1);
        merged.putAll(set2);
        String separator = EvaluationHelper.SEPARATOR;
        File file = new File(out);
        try {
            FileWriter outw = new FileWriter(file, false);
            BufferedWriter bw = new BufferedWriter(outw);
            bw.write(
                    "url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date \n");
            DocumentRetriever c = new DocumentRetriever();
            for (Entry<String, DBExport> e : merged.entrySet()) {
                HeadDateGetter dg = new HeadDateGetter();

                List<MetaDate> headDates = dg.getDates(c.getWebDocument(e.getValue().get(DBExport.PATH)));
                System.out.println(headDates.size());
                if (headDates.size() > 0) {

                    String write = e.getValue().getUrl() + separator + e.getValue().getFilePath() + separator + e.getValue().getPubDate() + separator + String.valueOf(
                            e.getValue().isPubSureness()) + separator + e.getValue().getModDate() + separator + String.valueOf(e.getValue().isModSureness()) + separator
                            + e.getValue().getGoogleDate() + separator + e.getValue().getHakiaDate() + separator + e.getValue().getAskDate() + separator + e.getValue()
                            .getLastModDate() + separator + e.getValue().getDateDate() + separator + e.getValue().getActDate();
                    bw.write(write + "\n");

                    System.out.println(write);
                }
            }
            bw.close();
            outw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
