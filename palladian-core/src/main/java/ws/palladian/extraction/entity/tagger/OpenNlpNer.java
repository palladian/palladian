package ws.palladian.extraction.entity.tagger;

import static ws.palladian.core.AnnotationFilters.range;
import static ws.palladian.extraction.entity.TaggingFormat.COLUMN;
import static ws.palladian.helper.collection.CollectionHelper.filterList;
import static ws.palladian.helper.functional.Filters.NONE;
import static ws.palladian.helper.functional.Filters.fileExtension;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.CollectionObjectStream;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.core.AnnotationFilters;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * This class wraps the <a href="https://opennlp.apache.org">OpenNLP</a> Named Entity Recognizer which uses a maximum
 * entropy approach.
 * 
 * <p>
 * The following models exist already for this recognizer:
 * <ul>
 * <li>Date
 * <li>Location
 * <li>Money
 * <li>Organization
 * <li>Percentage
 * <li>Person
 * <li>Time
 * </ul>
 * 
 * @see <a href="https://opennlp.apache.org/documentation/1.5.3/manual/opennlp.html#tools.namefind">Apache OpenNLP
 *      Developer Documentation: Name Finder</a>
 * @see <a href="http://opennlp.sourceforge.net/models-1.5/">OpenNLP Tools Models</a>
 * @author David Urbansky
 * @author Philipp Katz
 */
public class OpenNlpNer extends TrainableNamedEntityRecognizer implements ClassifyingTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenNlpNer.class);

    private static final TrainingParameters TRAIN_PARAMS = TrainingParameters.defaultParams();

    private static final AdaptiveFeatureGenerator FEATURE_GENERATOR = null;

    private static final Map<String, Object> RESOURCES = Collections.emptyMap();

    private final Tokenizer tokenizer;

    private final SentenceDetector sentenceDetector;

    private List<TokenNameFinderModel> nameFinderModels;

    /**
     * Create a new {@link OpenNlpNer}. The NER requires a tokenizer and a sentence detector in order to work properly.
     * 
     * @param tokenizer The tokenizer to use, not <code>null</code>.
     * @param sentenceDetector The sentence detector to use, not <code>null</code>.
     */
    public OpenNlpNer(Tokenizer tokenizer, SentenceDetector sentenceDetector) {
        Validate.notNull(tokenizer, "tokenizer must not be null");
        Validate.notNull(sentenceDetector, "sentenceDetector must not be null");
        this.tokenizer = tokenizer;
        this.sentenceDetector = sentenceDetector;
    }

    /**
     * <p>
     * Load the models for the entity recognizer. All files in the specified directory with the file name extension
     * ".bin" are considered as OpenNLP {@link TokenNameFinderModel}s.
     * 
     * @param configModelFilePath The path to the folder where the models lie.
     */
    @Override
    public boolean loadModel(String configModelFilePath) {
        Validate.notNull(configModelFilePath, "configModelFilePath must not be null");
        File modelDirectory = new File(configModelFilePath);
        Validate.isTrue(modelDirectory.isDirectory(), "Model file path must be an existing directory.");

        List<File> modelFiles = FileHelper.getFiles(new File(configModelFilePath), fileExtension(".bin"), NONE);
        Validate.isTrue(modelFiles.size() > 0, "Model file path must at least provide one .bin model.");

        this.nameFinderModels = CollectionHelper.newArrayList();
        for (File modelFile : modelFiles) {
            LOGGER.info("Loading {}", modelFile);
            try {
                this.nameFinderModels.add(new TokenNameFinderModel(modelFile));
            } catch (InvalidFormatException e) {
                throw new IllegalStateException("InvalidFormatException when trying to load " + modelFile);
            } catch (IOException e) {
                throw new IllegalStateException("IOException when trying to load " + modelFile);
            }
        }

        LOGGER.info("{} models successfully loaded", modelFiles.size());
        return true;
    }

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String inputText) {
        if (nameFinderModels == null || nameFinderModels.isEmpty()) {
            throw new IllegalStateException("No model available; make sure to load an existing model.");
        }
        // Map to collect all classifications for a given span; this is necessary, because individual entity types are
        // classified separately, and more than one entity classifier might tag an entity occurrence. We use the
        // probability values provided by the name finders to weight the individual type assignments.
        Map<Pair<Integer, Integer>, CategoryEntriesBuilder> collectedAnnotations = LazyMap
                .create(new Factory<CategoryEntriesBuilder>() {
                    @Override
                    public CategoryEntriesBuilder create() {
                        return new CategoryEntriesBuilder();
                    }
                });
        Span[] sentences = sentenceDetector.sentPosDetect(inputText);
        for (Span sentence : sentences) {
            String sentenceString = sentence.getCoveredText(inputText).toString();
            int sentenceOffset = sentence.getStart();
            Span[] tokenSpans = tokenizer.tokenizePos(sentenceString);
            String[] tokenStrings = Span.spansToStrings(tokenSpans, sentenceString);
            for (TokenNameFinderModel nameFinderModel : nameFinderModels) {
                NameFinderME nameFinder = new NameFinderME(nameFinderModel);
                Span[] nameSpans = nameFinder.find(tokenStrings);
                double[] probs = nameFinder.probs(nameSpans);
                for (int i = 0; i < nameSpans.length; i++) {
                    Span nameSpan = nameSpans[i];
                    int nameStart = sentenceOffset + tokenSpans[nameSpan.getStart()].getStart();
                    int nameEnd = sentenceOffset + tokenSpans[nameSpan.getEnd() - 1].getEnd();
                    collectedAnnotations.get(Pair.of(nameStart, nameEnd)).add(nameSpan.getType(), probs[i]);
                }
            }
        }
        Annotations<ClassifiedAnnotation> annotations = new Annotations<ClassifiedAnnotation>();
        for (Entry<Pair<Integer, Integer>, CategoryEntriesBuilder> entry : collectedAnnotations.entrySet()) {
            int startOffset = entry.getKey().getLeft();
            int endOffset = entry.getKey().getRight();
            String value = inputText.substring(startOffset, endOffset);
            CategoryEntries categoryEntries = entry.getValue().create();
            annotations.add(new ClassifiedAnnotation(startOffset, value, categoryEntries));
        }
        annotations.sort();
        return annotations;
    }

    @Override
    public String getModelFileEnding() {
        return "bin";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean oneModelPerConcept() {
        return true;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        // OpenNLP creates one model file for each trained tag, so for the supplied model file path, a directory will be
        // created, which contains all those files.
        File modelDirectory = new File(modelFilePath);
        if (!modelDirectory.isDirectory() && !modelDirectory.mkdirs()) {
            throw new IllegalArgumentException("Directory " + modelFilePath + " could not be created.");
        }

        Annotations<Annotation> annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);
        String text = FileFormatParser.getText(trainingFilePath, COLUMN);

        // need to train for each type; so collect all occurring annotation types
        Set<String> types = CollectionHelper.convertSet(annotations, Annotation.TAG_CONVERTER);
        LOGGER.info("Training for types: {}", types);

        for (final String type : types) {
            LOGGER.debug("Training {}", type);
            List<Annotation> currentAnnotations = filterList(annotations, AnnotationFilters.tag(type));
            List<NameSample> nameSamples = CollectionHelper.newArrayList();
            Span[] sentences = sentenceDetector.sentPosDetect(text);
            for (Span sentence : sentences) {
                String sentenceString = sentence.getCoveredText(text).toString();
                int sentenceStart = sentence.getStart();
                int sentenceEnd = sentence.getEnd();
                Span[] tokenSpans = tokenizer.tokenizePos(sentenceString);
                String[] tokens = Span.spansToStrings(tokenSpans, sentenceString);
                List<Annotation> inSentence = filterList(currentAnnotations, range(sentenceStart, sentenceEnd));
                if (inSentence.size() > 0) {
                    Span[] spans = getSpans(sentenceStart, inSentence, tokenSpans);
                    nameSamples.add(new NameSample(tokens, spans, true));
                }
            }
            ObjectStream<NameSample> stream = new CollectionObjectStream<NameSample>(nameSamples);
            BufferedOutputStream modelOut = null;
            try {
                TokenNameFinderModel model = NameFinderME.train("en", type, stream, TRAIN_PARAMS, FEATURE_GENERATOR,
                        RESOURCES);
                File modelFile = new File(modelDirectory, "openNLP_" + type + ".bin");
                modelOut = new BufferedOutputStream(new FileOutputStream(modelFile));
                model.serialize(modelOut);
            } catch (IOException e) {
                throw new IllegalStateException("IOException during training", e);
            } finally {
                try {
                    stream.close();
                } catch (IOException ignore) {
                }
                FileHelper.close(modelOut);
            }
        }
        return true;
    }

    /**
     * Transform the training annotations to an array of OpenNLP spans.
     * 
     * @param sentenceOffset The character offset of the current sentence.
     * @param annotations The training annotations.
     * @param tokenSpans The tokens of the current sentence.
     * @return An array of spans representing the annotated and tagged entities.
     */
    private static Span[] getSpans(int sentenceOffset, List<Annotation> annotations, Span[] tokenSpans) {
        List<Span> spans = CollectionHelper.newArrayList();
        for (int idx = 0; idx < annotations.size(); idx++) {
            Annotation annotation = annotations.get(idx);
            int start = -1;
            int end = -1;
            for (int i = 0; i < tokenSpans.length; i++) {
                if (sentenceOffset + tokenSpans[i].getStart() == annotation.getStartPosition()) {
                    start = i;
                }
                if (sentenceOffset + tokenSpans[i].getEnd() == annotation.getEndPosition()) {
                    end = i + 1;
                }
            }
            if (start == -1 || end == -1) {
                // FIXME I currently don't know, why the end position is sometimes -1
                LOGGER.warn("Could not properly align {} (start={}, end={})", annotation, start, end);
            } else {
                spans.add(new Span(start, end, annotation.getTag()));
            }
        }
        return spans.toArray(new Span[spans.size()]);
    }

    @Override
    public String getName() {
        return "OpenNLP NER";
    }

}
