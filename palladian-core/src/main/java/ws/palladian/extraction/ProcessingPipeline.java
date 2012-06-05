package ws.palladian.extraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.extraction.helper.StopWordRemover;
import ws.palladian.extraction.helper.WordCounter;
import ws.palladian.helper.StopWatch;

/**
 * <p>
 * A pipeline handling information processing components implemented by {@link PipelineProcessor}s to process
 * {@link PipelineDocument}s.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @version 3.0
 * @since 0.0.8
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
    private List<PipelineProcessor<?>> pipelineProcessors;
    private List<Transition<?>> transitions;

    /**
     * <p>
     * Creates a new {@code ProcessingPipeline} without any {@code PipelineProcessor}s. Add processors using
     * {@link #add(PipelineProcessor)} to get a functional {@code ProcessingPipeline}.
     * </p>
     */
    public ProcessingPipeline() {
        pipelineProcessors = new ArrayList<PipelineProcessor<?>>();
    }

    /**
     * <p>
     * Adds a new processor for execution to this pipeline. The processor is appended as last step.
     * </p>
     * 
     * @param pipelineProcessor The new processor to add.
     */
    public final void add(PipelineProcessor<?> pipelineProcessor) {
        // Begin Convenience Code
        if (!pipelineProcessors.isEmpty()) {
            List<Port<?>> previousOutputPorts = pipelineProcessors.get(pipelineProcessors.size() - 1).getOutputPorts();
            if (!previousOutputPorts.isEmpty()) {
                Port<?> previousOutputPort = previousOutputPorts.get(0);

                Port<?> inputPort = pipelineProcessor.getInputPorts().get(0);
                if ("defaultInput".equals(inputPort.getName()) && "defaultOutput".equals(previousOutputPort.getName())) {
                    add(new Transition(previousOutputPort, inputPort));
                }
            }
        }
        // End Convenience Code

        pipelineProcessors.add(pipelineProcessor);
    }

    public final void add(Transition<?> transition) {
        transitions.add(transition);
    }

    /**
     * <p>
     * Provides the list of all {@code PipelineProcessor}s currently registered at this pipeline. The list is in the
     * same order as the processors are executed beginning from the first and ending with the last.
     * </p>
     * 
     * @return The list of registered {@code PipelineProcessor}s.
     */
    public final List<PipelineProcessor<?>> getPipelineProcessors() {
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
    // Convenience Method
    public <T> PipelineDocument<T> process(PipelineDocument<T> document) throws DocumentUnprocessableException {

        ((Port<T>)pipelineProcessors.get(0).getInputPorts().get(0)).setPipelineDocument(document);

        process();

        return (PipelineDocument<T>)pipelineProcessors.get(pipelineProcessors.size() - 1).getOutputPorts().get(0)
                .getPipelineDocument();
    }

    public void process() throws DocumentUnprocessableException {
        StopWatch stopWatch = new StopWatch();
        Collection<PipelineProcessor<?>> executableProcessors = new ArrayList<PipelineProcessor<?>>(pipelineProcessors);
        Collection<Transition<?>> executableTransitions = new ArrayList<Transition<?>>(transitions);
        Collection<PipelineProcessor<?>> executedProcessors = new ArrayList<PipelineProcessor<?>>();
        Collection<Transition<?>> executedTransitions = new ArrayList<Transition<?>>();

        do {
            executedProcessors.clear();
            executedTransitions.clear();

            for (PipelineProcessor<?> processor : executableProcessors) {
                if (processor.isExecutable()) {
                    StopWatch stopWatch2 = new StopWatch();
                    processor.process();
                    LOGGER.debug("processor " + processor + " took " + stopWatch2);
                    executedProcessors.add(processor);
                }
            }
            for (Transition<?> transition : executableTransitions) {
                if (transition.canFire()) {
                    transition.transit();
                    executedTransitions.add(transition);
                }
            }
            executableProcessors.removeAll(executedProcessors);
            executableTransitions.removeAll(executedTransitions);
        } while (!executedProcessors.isEmpty());

        LOGGER.debug("pipeline took " + stopWatch);
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

        // ProcessingPipeline pipeline = new ProcessingPipeline();
        // PipelineProcessor processor1 = new PipelineProcessor();
        // PipelineProcessor processor2 = new PipelineProcessor();
        // PipelineProcessor processor3 = new PipelineProcessor();
        // pipeline.add(processor1);
        // pipeline.add(new Transition(processor1.getOutputPorts().get(0), processor2.getInputPorts().get(1)));
        // pipeline.add(processor2);
        // pipeline.add(processor3);
    }
}
