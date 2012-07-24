package ws.palladian.extraction.date.evaluation;

import java.util.HashMap;
import java.util.Map.Entry;

import ws.palladian.extraction.date.comparators.DateExactness;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.ExtractedDate;

public class searchengineEvaluation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String file = "data/evaluation/daterecognition/datasets/finalEvaluation.txt";
		HashMap<String, DBExport> map = EvaluationHelper.readFile(file);
		DateComparator dc = new DateComparator(DateExactness.DAY);

		int googleAFRPub = 0;
		int googleARDPub = 0;
		int googleAFWPub = 0;
		int googleAWDPub = 0;
		int googleANFPub = 0;

		int hakiaAFRPub = 0;
		int hakiaARDPub = 0;
		int hakiaAFWPub = 0;
		int hakiaAWDPub = 0;
		int hakiaANFPub = 0;

		int askAFRPub = 0;
		int askARDPub = 0;
		int askAFWPub = 0;
		int askAWDPub = 0;
		int askANFPub = 0;

		int googleAFRMod = 0;
		int googleARDMod = 0;
		int googleAFWMod = 0;
		int googleAWDMod = 0;
		int googleANFMod = 0;

		int hakiaAFRMod = 0;
		int hakiaARDMod = 0;
		int hakiaAFWMod = 0;
		int hakiaAWDMod = 0;
		int hakiaANFMod = 0;

		int askAFRMod = 0;
		int askARDMod = 0;
		int askAFWMod = 0;
		int askAWDMod = 0;
		int askANFMod = 0;

		int i=0;
		
		for (Entry<String, DBExport> e : map.entrySet()) {
			System.out.println(i++);
			ExtractedDate pubDate = DateGetterHelper.findDate(e.getValue()
					.getPubDate());
			ExtractedDate modDate = DateGetterHelper.findDate(e.getValue()
					.getModDate());

			ExtractedDate googleDate = DateGetterHelper.findDate(e.getValue()
					.getGoogleDate());
			ExtractedDate hakiaDate = DateGetterHelper.findDate(e.getValue()
					.getHakiaDate());
			ExtractedDate askDate = DateGetterHelper.findDate(e.getValue()
					.getAskDate());

			// Google PubDate
			if (pubDate == null) {
				if (googleDate == null) {
					googleARDPub++;
				} else {
					googleAWDPub++;
				}
			} else {
				if (googleDate == null) {
					googleANFPub++;
				} else {
					int compare = dc.compare(pubDate, googleDate);
					if (compare == 0) {
						googleAFRPub++;
					} else {
						googleAFWPub++;
					}
				}
			}

			// Google ModDate
			if (modDate == null) {
				if (googleDate == null) {
					googleARDMod++;
				} else {
					googleAWDMod++;
				}
			} else {
				if (googleDate == null) {
					googleANFMod++;
				} else {
					int compare = dc.compare(modDate, googleDate);
					if (compare == 0) {
						googleAFRMod++;
					} else {
						googleAFWMod++;
					}
				}
			}

			// Hakia PubDate
			if (pubDate == null) {
				if (hakiaDate == null) {
					hakiaARDPub++;
				} else {
					hakiaAWDPub++;
				}
			} else {
				if (hakiaDate == null) {
					hakiaANFPub++;
				} else {
					int compare = dc.compare(pubDate, hakiaDate);
					if (compare == 0) {
						hakiaAFRPub++;
					} else {
						hakiaAFWPub++;
					}
				}
			}

			// Hakia ModDate
			if (modDate == null) {
				if (hakiaDate == null) {
					hakiaARDMod++;
				} else {
					hakiaAWDMod++;
				}
			} else {
				if (hakiaDate == null) {
					hakiaANFMod++;
				} else {
					int compare = dc.compare(modDate, hakiaDate);
					if (compare == 0) {
						hakiaAFRMod++;
					} else {
						hakiaAFWMod++;
					}
				}
			}

			// Ask PubDate
			if (pubDate == null) {
				if (askDate == null) {
					askARDPub++;
				} else {
					askAWDPub++;
				}
			} else {
				if (askDate == null) {
					askANFPub++;
				} else {
					int compare = dc.compare(pubDate, askDate);
					if (compare == 0) {
						askAFRPub++;
					} else {
						askAFWPub++;
					}
				}
			}

			// Ask ModDate
			if (modDate == null) {
				if (askDate == null) {
					askARDMod++;
				} else {
					askAWDMod++;
				}
			} else {
				if (askDate == null) {
					askANFMod++;
				} else {
					int compare = dc.compare(modDate, askDate);
					if (compare == 0) {
						askAFRMod++;
					} else {
						askAFWMod++;
					}
				}
			}
		}

		System.out.println("Google Pub:");
		System.out.println("afr: " + googleAFRPub + " ard: " + googleARDPub
				+ " afw: " + googleAFWPub + " awd: " + googleAWDPub + " anf: "
				+ googleANFPub);
		System.out.println("Google Mod:");
		System.out.println("afr: " + googleAFRMod+ " ard: " + googleARDMod
				+ " afw: " + googleAFWMod + " awd: " + googleAWDMod + " anf: "
				+ googleANFMod);
		System.out.println("Hakia Pub:");
		System.out.println("afr: " + hakiaAFRPub + " ard: " + hakiaARDPub
				+ " afw: " + hakiaAFWPub + " awd: " + hakiaAWDPub + " anf: "
				+ hakiaANFPub);
		System.out.println("Hakia Mod:");
		System.out.println("afr: " + hakiaAFRMod + " ard: " + hakiaARDMod
				+ " afw: " + hakiaAFWMod + " awd: " + hakiaAWDMod + " anf: "
				+ hakiaANFMod);
		System.out.println("Ask Pub:");
		System.out.println("afr: " + askAFRPub + " ard: " + askARDPub
				+ " afw: " + askAFWPub + " awd: " + askAWDPub + " anf: "
				+ askANFPub);
		System.out.println("Ask Mod:");
		System.out.println("afr: " + askAFRMod + " ard: " + askARDMod
				+ " afw: " + askAFWMod + " awd: " + askAWDMod + " anf: "
				+ askANFMod);

	}

}
