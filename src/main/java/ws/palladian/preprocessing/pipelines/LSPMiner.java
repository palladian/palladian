/**
 * Created on: 01.02.2012 14:03:19
 */
package ws.palladian.preprocessing.pipelines;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ws.palladian.model.LabeledSequentialPattern;
import ws.palladian.model.features.Feature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.ProcessingPipeline;
import ws.palladian.preprocessing.featureextraction.Annotation;
import ws.palladian.preprocessing.featureextraction.AnnotationFeature;
import ws.palladian.preprocessing.featureextraction.LSPAnnotator;
import ws.palladian.preprocessing.featureextraction.Tokenizer;
import ws.palladian.preprocessing.nlp.sentencedetection.AbstractSentenceDetector;
import ws.palladian.preprocessing.nlp.sentencedetection.PalladianSentenceDetector;

/**
 * <p>
 * A {@link ProcessingPipeline} for the extraction of english labeled sequential patterns for question answer detection
 * as described by Cong et al. [1] and Hong et al. [2].
 * </p>
 * <p>
 * [1] ﻿Cong, G., Wang, L., Lin, C. Y., Song, Y. I., & Sun, Y. (2008). Finding question-answer pairs from online forums.
 * Proceedings of the 31st annual international ACM SIGIR conference on Research and development in information
 * retrieval (pp. 467–474). New York, NY, USA: ACM. doi:10.1145/1390334.1390415<br />
 * [2] ﻿Hong, L., & Davison, B. D. (2009). A classification-based approach to question answering in discussion boards.
 * Proceedings of the 32nd international ACM SIGIR conference on Research and development in information retrieval (pp.
 * 171–178). New York, NY, USA: ACM. doi:http://doi.acm.org/10.1145/1571941.1571973
 * </p>
 * 
 * @author Klemens Mutmann
 * 
 */
public class LSPMiner extends ProcessingPipeline {
    /**
     * <p>
     * Keywords for Question Answer detection as reported by Hong et al [1]. These are the 5W1H words and the english
     * modal verbs as described on wikipedia [2], [3].
     * </p>
     * <p>
     * [1] ﻿Hong, L., & Davison, B. D. (2009). A classification-based approach to question answering in discussion
     * boards. Proceedings of the 32nd international ACM SIGIR conference on Research and development in information
     * retrieval (pp. 171–178). New York, NY, USA: ACM. doi:http://doi.acm.org/10.1145/1571941.1571973<br />
     * [2] https://en.wikipedia.org/wiki/5W1H <br />
     * [3] https://en.wikipedia.org/wiki/English_modal_auxiliary_verb <br />
     * </p>
     */
    private final static String[] keywords = new String[] {"where", "when", "how", "who", "what", "why", "shall",
            "should", "will", "would", "may", "might", "can", "could", "mote", "must"};

    /**
     * <p>
     * Creates a new {@link ProcessingPipeline} for {@link LabeledSequentialPattern}s. The {@code ProcessingPipeline}
     * requires a part of speech tagging model, which must be provided to the constructor.
     * </p>
     * 
     * @param pathToPartOfSpeechModel The path to the OpenNLP part of speech model required for this
     *            {@code PipelineProcessor}. Since this {@code ProcessingPipeline} only works on english texts please
     *            provide an english PoS model. You may find such models at: <a
     *            href="http://opennlp.sourceforge.net/models-1.5/">OpenNLP models</a>.
     */
    public LSPMiner(String pathToPartOfSpeechModel) {
        super();
        add(new PalladianSentenceDetector());
        add(new Tokenizer());
        add(new ws.palladian.preprocessing.featureextraction.OpenNlpPosTagger(pathToPartOfSpeechModel));
        add(new LSPAnnotator(keywords));

    }

    /**
     * <p>
     * Provides all {@code LabeledSequentialPattern}s extracted from {@code document}. This is a convenience method to
     * get the results of an {@code LSPMiner} pipeline. It should be called on {@link PipelineDocument}s that were
     * already processed by the {@code LSPMiner} pipeline.
     * </p>
     * 
     * @param document The {@code PipelineDocument} containing the extracted {@code LabeledSequentialPattern}s.
     * @return All {@code LabeledSequentialPattern}s from {@code document} or an empty {@code Collection} if no
     *         {@code LabeledSequentialPattern}s were extracted from {@code document} yet.
     */
    public static Collection<LabeledSequentialPattern> getExtractedPatterns(PipelineDocument document) {
        Collection<LabeledSequentialPattern> ret = new HashSet<LabeledSequentialPattern>();
        AnnotationFeature sentencesFeature = (AnnotationFeature)document.getFeatureVector().get(
                AbstractSentenceDetector.PROVIDED_FEATURE);
        if (sentencesFeature != null) {
            List<Annotation> sentenceAnnotations = sentencesFeature.getValue();

            for (Annotation annotation : sentenceAnnotations) {
                Feature<LabeledSequentialPattern> lspFeature = (Feature<LabeledSequentialPattern>)annotation
                        .getFeatureVector().get(LSPAnnotator.PROVIDED_FEATURE);
                if (lspFeature != null) {
                    ret.add(lspFeature.getValue());
                }
            }
        }

        return ret;
    }
}
