package ws.palladian.extraction.entity.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.tagger.OpenNlpNer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>The NerEvaluator can be used to train and evaluate several NERs on the same data.</p>
 * 
 * @author David Urbansky
 * 
 */
public class NerEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(NerEvaluator.class);

    /** A list of NERs that should be evaluated. */
    private List<NamedEntityRecognizer> nerList;

    /** A list of datasets that should be used for evaluation. */
    private List<Dataset> datasets;

    /** The path to the file where the evaluation results are written to. */
    private final String evaluationFolder = "data/temp/nerEvaluation/";

    public NerEvaluator() {
        new File(evaluationFolder).mkdirs();
    }

    public void trainAllNERs(Dataset datasetTraining) {

        for (NamedEntityRecognizer ner : getNerList()) {

            String modelPath = "data/temp/nerEvaluation/" + StringHelper.makeSafeName(ner.getName());

            if (!ner.setsModelFileEndingAutomatically()) {
                modelPath += "." + ner.getModelFileEnding();
            }

            ner.train(datasetTraining, modelPath);
            ner.loadModel(modelPath);

        }

    }

    public void runEvaluation() {

        StopWatch sw = new StopWatch();

        LOGGER.info("start evaluating " + nerList.size() + " NERs on " + datasets.size() + " datasets");

        for (Dataset dataset : getDatasets()) {

            for (NamedEntityRecognizer ner : getNerList()) {

                EvaluationResult er = ner.evaluate(dataset);

                String filePath = evaluationFolder + ner.getName() + "_evaluationResults.csv";
                FileHelper.writeToFile(filePath, er.toString());
                LOGGER.info("evaluated " + ner.getName() + " and wrote results to " + filePath);
            }

        }

        LOGGER.info("finished evaluatring NERs in " + sw.getElapsedTimeString());
    }

    public void setNerList(List<NamedEntityRecognizer> nerList) {
        this.nerList = nerList;
    }

    public List<NamedEntityRecognizer> getNerList() {
        return nerList;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        List<NamedEntityRecognizer> nerList = new ArrayList<NamedEntityRecognizer>();

        // JulieNER jn = new JulieNER();
        // nerList.add(jn);

        // LingPipeNER ln = new LingPipeNER();
        // nerList.add(ln);

        // StanfordNER sn = new StanfordNER();
        // nerList.add(sn);

        // IllinoisLbjNER in = new IllinoisLbjNER();
        // nerList.add(in);

        OpenNlpNer on = new OpenNlpNer();
        nerList.add(on);

        // PalladianNer tn = new PalladianNer();
        // nerList.add(tn);

        List<Dataset> datasets = new ArrayList<Dataset>();
        Dataset dataset = new Dataset();
        dataset.setFirstFieldLink(true);
        dataset.setPath("data/datasets/ner/www_test/index_split2.txt");
        datasets.add(dataset);

        Dataset datasetTraining = new Dataset();
        datasetTraining.setFirstFieldLink(true);
        datasetTraining.setPath("data/datasets/ner/www_test/index_split1.txt");

        NerEvaluator evaluator = new NerEvaluator();
        evaluator.setNerList(nerList);
        evaluator.trainAllNERs(datasetTraining);
        evaluator.setDatasets(datasets);
        evaluator.runEvaluation();

    }

}