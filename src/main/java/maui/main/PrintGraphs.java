package maui.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.text.CaseFolder;

import weka.core.Utils;

public class PrintGraphs {

	public static void computeRelatedness(Collection<Article> topics) {

		double relatedness = 0;
		for (Article a : topics) {
			for (Article c : topics) {
				if (!c.equals(a)) {
					try {
						relatedness = a.getRelatednessTo(c);
						if (relatedness > 0) {
							System.out.println(a.getTitle()
									+ " and "
									+ c.getTitle()
									+ "\t"
									+ Utils
											.doubleToString(relatedness * 100,
													2));
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void computeGraph(HashMap<Article, Integer> topics,
			String root, String outputFile) {
		FileOutputStream out;
		PrintWriter printer;
		try {
			
			System.out.println("Printing into " + outputFile);
			
			out = new FileOutputStream(outputFile);
			printer = new PrintWriter(out);

			printer.print("graph G {\n");

			printer.print("graph [root=\"" + root
					+ "\", outputorder=\"depthfirst\"];\n");

			HashSet<String> done = new HashSet<String>();
			double relatedness = 0;
			for (Article a : topics.keySet()) {
				int count = topics.get(a).intValue();
				if (count < 1) {
					printer.print("\"" + a.getTitle() + "\" [fontsize=22];\n");
				} else if (count < 3) {
					printer
							.print("\"" + a.getTitle()
									+ "\" [fontsize = 18];\n");
				} else if (count < 6) {
					printer
							.print("\"" + a.getTitle()
									+ "\" [fontsize = 14];\n");
				} else {
					printer
							.print("\"" + a.getTitle()
									+ "\" [fontsize = 12];\n");
				}

				for (Article c : topics.keySet()) {
					if (!c.equals(a)) {
						try {
							relatedness = a.getRelatednessTo(c);
							String relation = "\"" + a.getTitle() + "\" -- \""
									+ c.getTitle();
							String relation2 = "\"" + c.getTitle() + "\" -- \""
									+ a.getTitle();

							if (!done.contains(relation2)
									&& !done.contains(relation)) {
								done.add(relation2);
								done.add(relation);

								if (relatedness < 0.2) {
									printer.print(relation
											+ "\"[style=invis];\n");
								} else {
									printer.print(relation
											+ "\" [penwidth = \""
											+ (int) (relatedness * 10 - 0.2)
											+ "\"];\n");
								}
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
			printer.print("}\n");
			printer.close();
			out.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Creates GraphViz files for all key files in a directory 
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// location of the Wikipedia data
		Wikipedia wikipedia = new Wikipedia("localhost", "enwiki_20090306",
				"root", null);

		// location of the directory with the keyphrase files
		String inputDir = "/Users/alyona/Documents/PHD/chapters_txt/";

		String line;

		File directory = new File(inputDir);
		for (File file : directory.listFiles()) {
			if (file.getName().endsWith("key")) {
				String out = file.getAbsolutePath();
				out = out.replace(".key", ".gv");

				HashMap<Article, Integer> topics = new HashMap<Article, Integer>();

				InputStreamReader inputStreamReader = new InputStreamReader(
						new FileInputStream(file), "ISO-8859-1");

				BufferedReader input = new BufferedReader(inputStreamReader);
				int i = 0;
				String root = "";
				while ((line = input.readLine()) != null) {
					line = line.trim();
					Article article = wikipedia.getArticleByTitle(line);
					if (article == null) {
						article = wikipedia.getMostLikelyArticle(line,
								new CaseFolder());
					}
					if (article != null) {
						if (root == "") {
							root = article.getTitle();
						}
						topics.put(article, new Integer(i));
					} else {
						System.out.println("Couldn't find article for " + line + " in " + file);
					}
					i++;
				}
				input.close();
				
				// Just to print out the relatedness information
				computeRelatedness(topics.keySet());
				
				// To generate the graph:
				computeGraph(topics, root, out);
			}
		}
	}

}
