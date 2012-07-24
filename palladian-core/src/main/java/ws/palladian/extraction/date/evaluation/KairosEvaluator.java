package ws.palladian.extraction.date.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.WebPageDateEvaluator;
import ws.palladian.extraction.date.comparators.DateExactness;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.getter.ContentDateGetter;
import ws.palladian.extraction.date.getter.MetaDateGetter;
import ws.palladian.extraction.date.getter.TechniqueDateGetter;
import ws.palladian.extraction.date.getter.UrlDateGetter;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.extraction.date.rater.TechniqueDateRater;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;
import ws.palladian.helper.date.dates.UrlDate;
import ws.palladian.retrieval.DocumentRetriever;

public class KairosEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TechniqueDateGetter<ContentDate> dg = new ContentDateGetter();
		TechniqueDateRater<ContentDate> pub_dr = new ContentDateRater(
				PageDateType.publish);
		TechniqueDateRater<ContentDate> mod_dr = new ContentDateRater(
				PageDateType.last_modified);

		String file = "data/evaluation/daterecognition/datasets/finalEvaluation.txt";
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		 evaluate(PageDateType.publish, dg, pub_dr, file, false);
		long pubTime = stopwatch.getElapsedTime();
		System.out.println("pubTime: " + pubTime);
		stopwatch.stop();
		stopwatch.start();
		 evaluate( PageDateType.last_modified, dg, mod_dr, file, false);
		long modTime = stopwatch.getElapsedTime();
		System.out.println("Time incl. DB zugriff - pub: " + pubTime
				+ " - mod: " + modTime);
	}

	private static void countUrlHeadDates(String file) {
		HashMap<String, DBExport> set = EvaluationHelper.readFile(file);

		UrlDateGetter urlDateGetter = new UrlDateGetter();
		MetaDateGetter metaDateGetter = new MetaDateGetter();

		int cntAllUrlDates = 0;
		int cntPubUrlDates = 0;
		int cntModUrlDates = 0;

		int cntAllMetaDates = 0;
		int cntPubMetaDates = 0;
		int cntModMetaDates = 0;

		int cntAll = 0;

		DateComparator dc = new DateComparator(DateExactness.DAY);

		for (Entry<String, DBExport> e : set.entrySet()) {
			System.out.println(cntAll++);
			urlDateGetter.setUrl(e.getValue().getUrl());
			ArrayList<UrlDate> urlDates = urlDateGetter.getDates();

			DocumentRetriever dr = new DocumentRetriever();
			Document doc = dr.getWebDocument(e.getValue().getFilePath());
			metaDateGetter.setDocument(doc);
			ArrayList<MetaDate> metaDates = metaDateGetter.getDates();

			ExtractedDate pubDate = DateGetterHelper.findDate(e.getValue()
					.getPubDate());
			ExtractedDate modDate = DateGetterHelper.findDate(e.getValue()
					.getModDate());

			if (metaDates != null && metaDates.size() > 0) {
				for (ExtractedDate metaDate : metaDates) {
					cntAllMetaDates++;
					if (pubDate != null
							&& dc.compare(metaDate, pubDate) == 0) {
						cntPubMetaDates++;
					}
					if (modDate != null
							&& dc.compare(metaDate, modDate) == 0) {
						cntModMetaDates++;
					}
				}
			}

			if (urlDates != null && urlDates.size() > 0
					&& urlDates.get(0) != null
					&& (pubDate != null || modDate != null)
					&& urlDates.get(0).getExactness().getValue() >= 3) {
				cntAllUrlDates++;
				System.out.println(cntAllUrlDates);
				if (pubDate != null
						&& dc.compare(urlDates.get(0), pubDate) == 0) {
					cntPubUrlDates++;
				}
				if (pubDate != null
						&& dc.compare(urlDates.get(0), pubDate) != 0) {
					System.out
							.println(pubDate.getNormalizedDateString() + " - "
									+ urlDates.get(0).getNormalizedDateString());
				}
				if (modDate != null
						&& dc.compare(urlDates.get(0), modDate) == 0) {
					cntModUrlDates++;
				}
			}

		}

		System.out.println("Url: " + cntAllUrlDates + " pub: " + cntPubUrlDates
				+ " mod: " + cntModUrlDates);
		System.out.println("Meta: " + cntAllMetaDates + " pub: "
				+ cntPubMetaDates + " mod: " + cntModMetaDates);

	}

	private static ArrayList<MetaDate> getHttpDates(DBExport dbExport) {
		ArrayList<MetaDate> dates = new ArrayList<MetaDate>();
		String headerDate = dbExport.get(DBExport.HEADER_DATE);
		String headerLastMod = dbExport.get(DBExport.HEADER_LAST);

		ExtractedDate headerExtrDate = DateGetterHelper.findDate(headerDate);
		// MetaDate headerHttpDate = DateConverter.convert(headerExtrDate,
		//		DateType.MetaDate);
		MetaDate headerHttpDate = new MetaDate(headerExtrDate);
		if (headerHttpDate != null) {
			headerHttpDate.setKeyword("date");
		}
		ExtractedDate headerExtrLastMod = DateGetterHelper
				.findDate(headerLastMod);
		//MetaDate headerHttpLastMod = DateConverter.convert(headerExtrLastMod,
		//		DateType.MetaDate);
		MetaDate headerHttpLastMod = new MetaDate(headerExtrLastMod);
		if (headerHttpLastMod != null) {
			headerHttpLastMod.setKeyword("last-modified");
		}
		dates.add(headerHttpDate);
		dates.add(headerHttpLastMod);

		return dates;

	}

	private static ExtractedDate getDownloadedDate(DBExport dbExport) {
		return DateGetterHelper.findDate(dbExport.get(DBExport.ACTUAL_DATE));
	}

	public static <T> void evaluate(PageDateType pub_mod,	TechniqueDateGetter<ContentDate> dg,
			TechniqueDateRater<ContentDate> dr, String file, boolean writeRate) {
		int ard = 0;
		int awd = 0;
		int anf = 0;
		int afr = 0;
		int afw = 0;
		int counter = 0;
		int compare;

		HashMap<String, DBExport> set = EvaluationHelper.readFile(file);
		DocumentRetriever crawler = new DocumentRetriever();

		StopWatch timer = new StopWatch();
		long time = 0;

		for (Entry<String, DBExport> e : set.entrySet()) {

			ExtractedDate date;
			T bestDate;
			String dbExportDateString;
			WebPageDateEvaluator wp = new WebPageDateEvaluator();

			String url = e.getValue().get(DBExport.URL);
			String path = e.getValue().get(DBExport.PATH);
			Document document = crawler.getWebDocument(path);

			String bestDateString = "";
			String rate = "-1";
			String dbDateString;

			System.out.println(url);

			timer.start();
			wp.setUrl(url);
			wp.setDocument(document);
			wp.setPubMod(pub_mod);
			wp.evaluate();
			bestDate = (T) wp.getBestRatedDate();
			time += timer.getElapsedTime();
			System.out.print("get dates... ");
			if (bestDate != null) {
				bestDateString = ((ExtractedDate) bestDate).getDateString();
				rate = String.valueOf(((ExtractedDate) bestDate).getRate());
			}

			System.out.println("compare...");

			if (pub_mod.equals(PageDateType.publish)) {
				compare = EvaluationHelper.compareDate(bestDate, e.getValue(),
						DBExport.PUB_DATE);
				date = DateGetterHelper.findDate(e.getValue().getPubDate());
				dbDateString = e.getValue().getPubDate();

				dbExportDateString = " - pubDate:";
			} else {
				compare = EvaluationHelper.compareDate(bestDate, e.getValue(),
						DBExport.MOD_DATE);
				date = DateGetterHelper.findDate(e.getValue().getModDate());
				dbDateString = e.getValue().getModDate();
				dbExportDateString = " - modDate:";
			}

			if (date != null) {
				dbExportDateString += date.getNormalizedDateString();
			}

			System.out.print(compare + " bestDate:" + bestDateString + " ("
					+ rate + ")" + dbExportDateString);

			switch (compare) {
			case EvaluationHelper.AFW:
				afw++;
				break;
			case EvaluationHelper.ANF:
				anf++;
				break;
			case EvaluationHelper.AWD:
				awd++;
				break;
			case EvaluationHelper.ARD:
				ard++;
				break;
			case EvaluationHelper.AFR:
				afr++;
				break;

			}

			counter++;

			System.out.println();
			System.out.println("all: " + counter + " afr: " + afr + " ard: "
					+ ard + " afw: " + afw + " awd: " + awd + " anf: " + anf);
			System.out
					.println("---------------------------------------------------------------------");
			System.out.println("time: " + time);
		}
		System.out.println("all: " + counter + " afr: " + afr + " ard: " + ard
				+ " afw: " + afw + " awd: " + awd + " anf: " + anf);
		System.out.println("final time: " + time);

	}

	

}
