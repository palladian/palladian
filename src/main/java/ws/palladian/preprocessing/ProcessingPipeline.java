package ws.palladian.preprocessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.helper.StopWatch;

/**
 * <p>
 * A pipeline handling information processing components implemented by {@link PipelineProcessor}s to process
 * {@link PipelineDocument}s.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 */
public class ProcessingPipeline implements Serializable {

    /**
     * <p>
     * Unique number used to identify serialized versions of this object. This value change only but each time the
     * serialized schema of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -6173687204106619909L;

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ProcessingPipeline.class);

    /**
     * <p>
     * The processors this pipeline will execute as ordered by this list from the first to the last.
     * </p>
     */
    private List<PipelineProcessor> pipelineProcessors;

    /**
     * <p>
     * Creates a new {@code ProcessingPipeline} without any {@code PipelineProcessor}s. Add processors using
     * {@link #add(PipelineProcessor)} to get a functional {@code ProcessingPipeline}.
     * </p>
     */
    public ProcessingPipeline() {
        pipelineProcessors = new ArrayList<PipelineProcessor>();
    }

    /**
     * <p>
     * Adds a new processor for execution to this pipeline. The processor is appended as last step.
     * </p>
     * 
     * @param pipelineProcessor The new processor to add.
     */
    public final void add(PipelineProcessor pipelineProcessor) {
        pipelineProcessors.add(pipelineProcessor);
    }

    /**
     * <p>
     * Provides the list of all {@code PipelineProcessor}s currently registered at this pipeline. The list is in the
     * same order as the processors are executed beginning from the first and ending with the last.
     * </p>
     * 
     * @return The list of registered {@code PipelineProcessor}s.
     */
    public final List<PipelineProcessor> getPipelineProcessors() {
        return pipelineProcessors;
    }

    /**
     * <p>
     * Starts processing of the provided document in this pipeline, running it through all currently registered
     * processors.
     * </p>
     * 
     * @param document The document to process.
     * @return The processed document is returned. This should be the same instance as provided. However this is not
     *         guaranteed. The returned document contains all features and modified representations created by the
     *         pipeline.
     */
    public PipelineDocument process(PipelineDocument document) {

        StopWatch stopWatch = new StopWatch();

        for (PipelineProcessor processor : pipelineProcessors) {
            StopWatch stopWatch2 = new StopWatch();
            processor.process(document);
            LOGGER.debug("processor " + processor + " took " + stopWatch2);
        }

        LOGGER.debug("pipeline took " + stopWatch);

        return document;
    }

    /**
     * <p>
     * Convenience main method showing how the code works.
     * </p>
     * 
     * @param args No arguments are accepted by this main method.
     */
    public static void main(String[] args) {

        // /////////////////// example usage /////////////////////

        // the usage of the stop word remover will change the classification outcome
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new StopWordRemover());
        pipeline.add(new WordCounter());

        DictionaryClassifier classifier = new DictionaryClassifier();
        ClassificationTypeSetting cts = new ClassificationTypeSetting();
        cts.setClassificationType(ClassificationTypeSetting.TAG);
        classifier.setClassificationTypeSetting(cts);
        classifier.setProcessingPipeline(pipeline);

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        UniversalInstance instance = new UniversalInstance(instances);
        instance.setTextFeature("Das ist ein deutscher Text, ein Mann steht vor einem Gebäude und lacht.");
        instance.setInstanceCategory("German");
        classifier.train(instance);

        instance = new UniversalInstance(instances);
        instance.setTextFeature("This is English, a man stands in front of a building and laughs.");
        instance.setInstanceCategory("English");
        classifier.train(instance);

        LOGGER.info(classifier.classify("This is a sample text, whether you believe it or not."));
    }
}
