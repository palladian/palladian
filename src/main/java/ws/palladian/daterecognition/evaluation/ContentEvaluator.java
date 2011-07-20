package ws.palladian.daterecognition.evaluation;

import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.searchengine.DBExport;
import ws.palladian.daterecognition.technique.ContentDateGetter;
import ws.palladian.daterecognition.technique.ContentDateRater_old;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.daterecognition.technique.TechniqueDateGetter;
import ws.palladian.daterecognition.technique.TechniqueDateRater;

public class ContentEvaluator {

    public static void main(String[] args) {

        TechniqueDateGetter<ContentDate> dg = new ContentDateGetter();
        TechniqueDateRater<ContentDate> pub_dr = new ContentDateRater_old(PageDateType.publish);
        TechniqueDateRater<ContentDate> mod_dr = new ContentDateRater_old(PageDateType.last_modified);

        String file = "data/evaluation/daterecognition/datasets/dataset.txt";
         evaluate(DBExport.PUB_DATE, dg, pub_dr, file);
         evaluate(DBExport.MOD_DATE, dg, mod_dr, file);
    }

    public static void evaluate(int pub_mod, TechniqueDateGetter<ContentDate> dg,
            TechniqueDateRater<ContentDate> dr, String file) {
        Evaluator.evaluate(pub_mod, dg, dr, file);
    }
}
