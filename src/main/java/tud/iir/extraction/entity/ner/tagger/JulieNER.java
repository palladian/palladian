package tud.iir.extraction.entity.ner.tagger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import opennlp.maxent.EventStream;
import opennlp.maxent.GISModel;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.PooledGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.tools.lang.english.NameFinder;
import opennlp.tools.namefind.NameFinderEventStream;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.julielab.jnet.tagger.JNETException;
import de.julielab.jnet.tagger.NETagger;
import de.julielab.jnet.tagger.Sentence;
import de.julielab.jnet.tagger.Tags;
import de.julielab.jnet.utils.FormatConverter;
import de.julielab.jnet.utils.Utils;

import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.FileFormatParser;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.TaggingFormat;
import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;

public class JulieNER extends NamedEntityRecognizer {

	public JulieNER() {
		setName("Julie NER");
	}

	public void demo() {
		String inputText = "Microsoft Inc. is a company which was founded by Bill Gates many years ago. The company's headquarters are close to Seattle in the USA.";
		demo(inputText);
	}

	public void demo(String inputText) {
		System.out
				.println(tag(
						inputText,
						"data/models/opennlp/openNLP_organization.bin.gz,data/models/opennlp/openNLP_person.bin.gz,data/models/opennlp/openNLP_location.bin.gz"));
	}

	@Override
	public Annotations getAnnotations(String inputText,
			String configModelFilePath) {

		Annotations annotations = new Annotations();

		FileHelper.writeToFile("julieInputText.txt", inputText);
		FileFormatParser.textToColumn("julieInputText.txt","julieInputTextColumn.txt"," ");
		FileFormatParser.columnToSlash("julieInputTextColumn.txt", "julieTrainingSlash.txt", " ", "|O|");
		
		File testDataFile = new File("julieTrainingSlash.txt");
		File modelFile = new File(configModelFilePath);
	
		// TODO assign confidence values for predicted labels (see JNET documentation)
		boolean showSegmentConfidence = false;
		
		ArrayList<String> ppdTestData = Utils.readFile(testDataFile);
		ArrayList<Sentence> sentences = new ArrayList<Sentence>();

		NETagger tagger = new NETagger();

		try {
			tagger.readModel(modelFile.toString());
		} catch (Exception e) {
			LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
		}

		for (String ppdSentence : ppdTestData) {
			try {
				sentences.add(tagger.PPDtoUnits(ppdSentence));
			} catch (JNETException e) {
				LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
			}
		}
		try {
			// tagger.readModel(modelFile.toString());
			File outFile = new File("juliePredictionOutput.txt");
			Utils.writeFile(outFile, tagger.predictIOB(sentences,showSegmentConfidence));
			annotations = FileFormatParser.getAnnotationsFromColumn(outFile.getPath());
		} catch (Exception e) {
			LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
		}
		
		CollectionHelper.print(annotations);

		return annotations;
	}

	/**
	 * Create a file containing all entity types from the training file.
	 * 
	 * @param trainingFilePath
	 * @return
	 */
	private File createTagsFile(String trainingFilePath) {

		Set<String> tags = FileFormatParser.getTagsFromColumnFile(
				trainingFilePath, " ");

		StringBuilder tagsFile = new StringBuilder();
		for (String tag : tags) {
			tagsFile.append(tag).append("\n");
		}
		if (!tags.contains("O")) {
			tagsFile.append("O").append("\n");
		}

		FileHelper.writeToFile("julieTags.txt", tagsFile);

		return new File("julieTags.txt");
	}

	@Override
	public boolean train(String trainingFilePath, String modelFilePath) {
		return train(trainingFilePath, modelFilePath, "");
	}

	public boolean train(String trainingFilePath, String modelFilePath,
			String configFilePath) {

		FileFormatParser.columnToSlash(trainingFilePath, "julieTraining.txt", " ", "|");
		
		File trainFile = new File("julieTraining.txt");
		File tagsFile = createTagsFile(trainingFilePath);

		File featureConfigFile = null;
		if (configFilePath.length() == 0) {
			featureConfigFile = new File(configFilePath);
		}

		ArrayList<String> ppdSentences = Utils.readFile(trainFile);
		ArrayList<Sentence> sentences = new ArrayList<Sentence>();
		Tags tags = new Tags(tagsFile.toString());

		NETagger tagger;
		if (featureConfigFile != null) {
			tagger = new NETagger(featureConfigFile);
		} else {
			tagger = new NETagger();
		}
		for (String ppdSentence : ppdSentences) {
			try {
				sentences.add(tagger.PPDtoUnits(ppdSentence));
			} catch (JNETException e) {
				e.printStackTrace();
			}
		}
		tagger.train(sentences, tags);
		tagger.writeModel(modelFilePath);

		return true;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {

		JulieNER tagger = new JulieNER();

		if (args.length > 0) {

			Options options = new Options();
			options.addOption(OptionBuilder.withLongOpt("mode")
					.withDescription("whether to tag or train a model")
					.create());

			OptionGroup modeOptionGroup = new OptionGroup();
			modeOptionGroup.addOption(OptionBuilder.withArgName("tg")
					.withLongOpt("tag").withDescription("tag a text").create());
			modeOptionGroup.addOption(OptionBuilder.withArgName("tr")
					.withLongOpt("train").withDescription("train a model")
					.create());
			modeOptionGroup.addOption(OptionBuilder.withArgName("ev")
					.withLongOpt("evaluate")
					.withDescription("evaluate a model").create());
			modeOptionGroup.addOption(OptionBuilder.withArgName("dm")
					.withLongOpt("demo")
					.withDescription("demo mode of the tagger").create());
			modeOptionGroup.setRequired(true);
			options.addOptionGroup(modeOptionGroup);

			options.addOption(OptionBuilder
					.withLongOpt("trainingFile")
					.withDescription(
							"the path and name of the training file for the tagger (only if mode = train)")
					.hasArg().withArgName("text").withType(String.class)
					.create());

			options.addOption(OptionBuilder
					.withLongOpt("testFile")
					.withDescription(
							"the path and name of the test file for evaluating the tagger (only if mode = evaluate)")
					.hasArg().withArgName("text").withType(String.class)
					.create());

			options.addOption(OptionBuilder
					.withLongOpt("configFile")
					.withDescription(
							"the path and name of the config file for the tagger")
					.hasArg().withArgName("text").withType(String.class)
					.create());

			options.addOption(OptionBuilder
					.withLongOpt("inputText")
					.withDescription(
							"the text that should be tagged (only if mode = tag)")
					.hasArg().withArgName("text").withType(String.class)
					.create());

			options.addOption(OptionBuilder
					.withLongOpt("outputFile")
					.withDescription(
							"the path and name of the file where the tagged text should be saved to")
					.hasArg().withArgName("text").withType(String.class)
					.create());

			HelpFormatter formatter = new HelpFormatter();

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = null;
			try {
				cmd = parser.parse(options, args);

				if (cmd.hasOption("tag")) {

					String taggedText = tagger.tag(
							cmd.getOptionValue("inputText"),
							cmd.getOptionValue("configFile"));

					if (cmd.hasOption("outputFile")) {
						FileHelper.writeToFile(
								cmd.getOptionValue("outputFile"), taggedText);
					} else {
						System.out
								.println("No output file given so tagged text will be printed to the console:");
						System.out.println(taggedText);
					}

				} else if (cmd.hasOption("train")) {

					tagger.train(cmd.getOptionValue("trainingFile"),
							cmd.getOptionValue("configFile"));

				} else if (cmd.hasOption("evaluate")) {

					EvaluationResult evResult = tagger
							.evaluate(cmd.getOptionValue("trainingFile"),
									cmd.getOptionValue("configFile"),
									TaggingFormat.XML);
					System.out.println(evResult);

				} else if (cmd.hasOption("demo")) {

					tagger.demo(cmd.getOptionValue("inputText"));

				}

			} catch (ParseException e) {
				LOGGER.debug("Command line arguments could not be parsed!");
				formatter.printHelp("OpenNLPNER", options);
			}

		}
		
		// // HOW TO USE (some functions require the models in
		// data/models/juliener) ////
		// // train
		// tagger.train("data/datasets/ner/sample/trainingPhoneXML.xml",
		// "data/models/opennlp/openNLP_phone.bin.gz");

		// // tag
		String taggedText = tagger.tag("Point mutations	have the potential to activate the K-ras gene if they occur in the critical coding sequences. John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone. The iphone 4 is modern. Seattle is a rainy city.", "data/models/pennbio_variations.mod.gz");
		System.out.println(taggedText);

		// // demo
		// tagger.demo();

		// // evaluate
		// System.out
		// .println(
		// tagger
		// .evaluate(
		// "data/datasets/ner/sample/testingXML.xml",
		// "data/models/opennlp/openNLP_organization.bin.gz,data/models/opennlp/openNLP_person.bin.gz,data/models/opennlp/openNLP_location.bin.gz",
		// TaggingFormat.XML));

	}

}
