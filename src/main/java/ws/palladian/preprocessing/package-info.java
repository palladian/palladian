/**
 * <p>
 * This package contains classes realizing a {@code ProcessingPipeline}. The pipeline is realized using the Command GoF
 * pattern. Command objects are called {@link ws.palladian.preprocessing.PipelineProcessor}s. They are executed by the
 * {@link ws.palladian.preprocessing.ProcessingPipeline}. The pipeline processes
 * {@link ws.palladian.preprocessing.PipelineDocument} objects, extracting features and modifying the documents content.
 * </p>
 * <p>
 * There are several implementations of {@code PipelineProcessor}s available but you can create your own if you need.
 * </p>
 * <p>
 * Provided processors are for example:
 * <ul>
 * <li>{@link ws.palladian.preprocessing.StopWordRemover}</li>
 * <li>{@link ws.palladian.preprocessing.WordCounter}</li>
 * <li>{@link ws.palladian.preprocessing.nlp.QuestionAnnotator}</li>
 * <li>{@link ws.palladian.preprocessing.nlp.LingPipeSentenceDetector}</li>
 * <li>{@link ws.palladian.preprocessing.nlp.OpenNLPSentenceDetector}</li>
 * </ul>
 * </p>
 */
package ws.palladian.preprocessing;