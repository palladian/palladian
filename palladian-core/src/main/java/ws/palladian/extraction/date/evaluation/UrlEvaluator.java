package ws.palladian.extraction.date.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.getter.TechniqueDateGetter;
import ws.palladian.extraction.date.getter.UrlDateGetter;
import ws.palladian.extraction.date.rater.TechniqueDateRater;
import ws.palladian.extraction.date.rater.UrlDateRater;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.UrlDate;

public class UrlEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TechniqueDateGetter<UrlDate> dg = new UrlDateGetter();
		TechniqueDateRater<UrlDate> dr = new UrlDateRater(PageDateType.publish);

		String file = "data/evaluation/daterecognition/datasets/urldataset.txt";
		evaluate(DBExport.PUB_DATE, dg, dr, file);
		evaluate(DBExport.MOD_DATE, dg, dr, file);

	}

	private static <T extends ExtractedDate> void evaluate(int pub_mod, TechniqueDateGetter<T> dg,
			TechniqueDateRater<T> dr, String file) {

		Evaluator.evaluate(pub_mod, dg, dr, file);

	}

	private static void countUrlsWithDate(String file) {
		HashMap<String, DBExport> set = EvaluationHelper.readFile(file);
		UrlDateGetter dg = new UrlDateGetter();
		int count = 0;
		for (Entry<String, DBExport> e : set.entrySet()) {
			if (dg.getFirstDate(e.getKey()) != null) {
				System.out.println(e.getKey());
				count++;
			}
		}
		System.out.println(count);
	}

	private static void compareUrlDateFoundDate(PageDateType pub_mod,
			String file) {

		HashMap<String, DBExport> set = EvaluationHelper.readFile(file);
		UrlDateGetter dg = new UrlDateGetter();
		DateComparator dc = new DateComparator();
		int countAll = 0;
		int countTP = 0;
		int countFN = 0;
		for (Entry<String, DBExport> e : set.entrySet()) {
			ExtractedDate urlDate = dg.getFirstDate(e.getKey());
			if (urlDate != null) {
				ExtractedDate foundDate;
				if (pub_mod == PageDateType.publish) {
					foundDate = DateGetterHelper.findDate(e.getValue().get(
							DBExport.PUB_DATE));
				} else {
					foundDate = DateGetterHelper.findDate(e.getValue().get(
							DBExport.MOD_DATE));
				}
				if (foundDate != null) {
					int compare = dc.compare(urlDate, foundDate, dc
							.getCompareDepth(urlDate, foundDate));
					if (compare == 0) {
						countTP++;
					} else {
						countFN++;
						System.out.println(e.getKey());
						System.out.println("urlDate: "
								+ urlDate.getNormalizedDateString()
								+ " foundDate: "
								+ foundDate.getNormalizedDateString());
					}
				}

				countAll++;
			}
		}
		System.out.println("countAll: " + countAll + " countTP: " + countTP
				+ " countFN: " + countFN);
	}

	private static void mergeUrlsets(String in1, String in2, String out) {

		HashMap<String, DBExport> set1 = EvaluationHelper.readFile(in1);
		HashMap<String, DBExport> set2 = EvaluationHelper.readFile(in2);

		HashMap<String, DBExport> merged = new HashMap<String, DBExport>();
		merged.putAll(set1);
		merged.putAll(set2);
		String separator = EvaluationHelper.SEPARATOR;
		File file = new File(out);
		try {
			FileWriter outw = new FileWriter(file, false);
			BufferedWriter bw = new BufferedWriter(outw);
			UrlDateGetter dg = new UrlDateGetter();
			bw
					.write("url *;_;* path *;_;* pub_date *;_;* pub_sureness *;_;* mod_date *;_;* mod_sureness *;_;* google_date *;_;* hakia_date *;_;* ask_date *;_;* header_last_mod *;_;* header_date *;_;* down_date");
			for (Entry<String, DBExport> e : merged.entrySet()) {
				if (dg.getFirstDate(e.getKey()) != null) {
					String write = e.getValue().getUrl() + separator
							+ e.getValue().getFilePath() + separator
							+ e.getValue().getPubDate() + separator
							+ String.valueOf(e.getValue().isPubSureness())
							+ separator + e.getValue().getModDate() + separator
							+ String.valueOf(e.getValue().isModSureness())
							+ separator + e.getValue().getGoogleDate()
							+ separator + e.getValue().getHakiaDate()
							+ separator + e.getValue().getAskDate() + separator
							+ e.getValue().getLastModDate() + separator
							+ e.getValue().getDateDate() + separator
							+ e.getValue().getActDate();
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
