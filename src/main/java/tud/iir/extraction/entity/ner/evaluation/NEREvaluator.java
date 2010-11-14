package tud.iir.extraction.entity.ner.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.classification.page.evaluation.Dataset;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.tagger.JulieNER;
import tud.iir.extraction.entity.ner.tagger.LingPipeNER;
import tud.iir.extraction.entity.ner.tagger.StanfordNER;
import tud.iir.extraction.entity.ner.tagger.TUDNER;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;

public class NEREvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(NEREvaluator.class);
    
    private List<NamedEntityRecognizer> nerList;
    private List<Dataset> datasets;

    private final String evaluationFolder = "data/temp/nerEvaluation/";

    public NEREvaluator() {
        new File(evaluationFolder).mkdirs();
    }

    private void trainAllNERs(Dataset datasetTraining) {

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

        JulieNER jn = new JulieNER();
        // nerList.add(jn);

        LingPipeNER ln = new LingPipeNER();
        // nerList.add(ln);

        StanfordNER sn = new StanfordNER();
        nerList.add(sn);

        TUDNER tn = new TUDNER();
        // nerList.add(tn);

        List<Dataset> datasets = new ArrayList<Dataset>();
        Dataset dataset = new Dataset();
        dataset.setFirstFieldLink(true);
        dataset.setPath("data/datasets/ner/www_test/index_split2.txt");
        datasets.add(dataset);

        Dataset datasetTraining = new Dataset();
        datasetTraining.setFirstFieldLink(true);
        datasetTraining.setPath("data/datasets/ner/www_test/index_split1.txt");

        NEREvaluator evaluator = new NEREvaluator();
        evaluator.setNerList(nerList);
        evaluator.trainAllNERs(datasetTraining);
        evaluator.setDatasets(datasets);
        evaluator.runEvaluation();

    }

}