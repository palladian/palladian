package ws.palladian.extraction.entity.tagger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.sentence.PalladianSentenceDetector;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;

//https://github.com/Ahwar/NER-NLP-with-ONNX-Java?tab=readme-ov-file

public class BertNer implements ClassifyingTagger, Closeable {

    // https://huggingface.co/FacebookAI/xlm-roberta-large-finetuned-conll03-english/blob/main/config.json#L12
    private static final Map<Integer, String> ID_TO_LABEL = Map.ofEntries( //
            Map.entry(0, "B-LOC"), //
            Map.entry(1, "B-MISC"), //
            Map.entry(2, "B-ORG"), //
            Map.entry(3, "I-LOC"), //
            Map.entry(4, "I-MISC"), //
            Map.entry(5, "I-ORG"), //
            Map.entry(6, "I-PER"), //
            Map.entry(7, "O") //
    );

    private final OrtEnvironment env;

    private final HuggingFaceTokenizer tokenizer;

    private final OrtSession session;

    public static BertNer loadFrom(Path modelDirectory, Path tokenizerJson) {
        Objects.requireNonNull(modelDirectory, "modelDirectory was null");
        Objects.requireNonNull(tokenizerJson, "tokenizerJson was null");
        try {
            var env = OrtEnvironment.getEnvironment();
            var session = env.createSession(modelDirectory.toString(), new OrtSession.SessionOptions());
            var tokenizer = HuggingFaceTokenizer.newInstance(tokenizerJson);
            return new BertNer(env, session, tokenizer);
        } catch (OrtException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private BertNer(OrtEnvironment env, OrtSession session, HuggingFaceTokenizer tokenizer) {
        this.env = env;
        this.session = session;
        this.tokenizer = tokenizer;
    }

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String text) {
        var sentences = new PalladianSentenceDetector(Language.ENGLISH).iterateTokens(text);
        var allSentences = CollectionHelper.newArrayList(sentences);

        // create chunks of 2,000 characters maximum, as the text length is limited
        var chunks = new ArrayList<Token>();
        Token currentToken = null;
        for (var next : allSentences) {
            if (currentToken == null) {
                currentToken = next;
            }
            if (currentToken.getValue().length() + next.getValue().length() > 2_000) {
                chunks.add(currentToken);
                currentToken = next;
            } else {
                var value = text.substring(currentToken.getStartPosition(), next.getEndPosition());
                currentToken = new ImmutableToken(currentToken.getStartPosition(), value);
            }
        }
        if (currentToken != null) {
            chunks.add(currentToken);
        }

        List<ClassifiedAnnotation> annotations = new ArrayList<>();
        for (var chunk : chunks) {
            List<ClassifiedAnnotation> result;
            try {
                result = process(chunk.getValue(), env, session, tokenizer);
            } catch (OrtException e) {
                throw new IllegalStateException(e);
            }
            for (var annotation : result) {
                int start = chunk.getStartPosition() + annotation.getStartPosition();
                annotations
                        .add(new ClassifiedAnnotation(start, annotation.getValue(), annotation.getCategoryEntries()));
            }
        }

        return annotations;
    }

    private static List<ClassifiedAnnotation> process(String text, OrtEnvironment env, OrtSession session,
            HuggingFaceTokenizer tokenizer) throws OrtException {
        var encoding = tokenizer.encode(text);

        var inputIds = encoding.getIds();
        var inputAttentionMask = encoding.getAttentionMask();

        // modify Encoded Ids according to the model requirement
        // from [input_ids] to [[input_ids]]
        long[][] newInputIds = new long[1][inputIds.length];
        System.arraycopy(inputIds, 0, newInputIds[0], 0, inputIds.length);

        // modify Attention Mask according to the model requirement
        // from [attention_mask] to [[attention_mask]]
        long[][] newAttentionMask = new long[1][inputAttentionMask.length];
        System.arraycopy(inputAttentionMask, 0, newAttentionMask[0], 0, inputAttentionMask.length);

        // create OnnxTensor
        var idsTensor = OnnxTensor.createTensor(env, newInputIds);
        var maskTensor = OnnxTensor.createTensor(env, newAttentionMask);

        // map input Tensor according to model Input
        // key is layer name, and value is value you want to pass
        var modelInputs = Map.of("input_ids", idsTensor, "attention_mask", maskTensor);

        // Running the inference on the model
        var result = session.run(modelInputs);

        // Handling the inference output
        // get output results
        var logits = (float[][][]) result.get(0).getValue();
        var charSpans = encoding.getCharTokenSpans();

        // (1) create annotations with categories and probabilities
        List<ClassifiedAnnotation> annotations = new ArrayList<>();
        for (int i = 0; i < logits[0].length; i++) {
            // start or end marker <s> and </s>
            if (charSpans[i] == null) {
                continue;
            }
            var start = charSpans[i].getStart();
            var end = charSpans[i].getEnd();
            var value = text.substring(start, end);
            var categoriesBuilder = new CategoryEntriesBuilder();
            for (int j = 0; j < logits[0][i].length; j++) {
                var logitExp = Math.exp(logits[0][i][j]);
                var category = ID_TO_LABEL.getOrDefault(j, "");
                var categoryNormalized = category.replaceAll("^\\w-", "");
                categoriesBuilder.add(categoryNormalized, logitExp);
            }
            annotations.add(new ClassifiedAnnotation(start, value, categoriesBuilder.create()));
        }

        // (2) remove 'O' annotations
        var filteredAnnotations = annotations.stream().filter(a -> !a.getTag().equals("O")).toList();

        // (3) iteratively merge annotations until there's no more changes
        var combinedAnnotations = filteredAnnotations;
        for (;;) {
            var combinedAnnotationsNew = combineAnnotations(text, combinedAnnotations);
            if (combinedAnnotations.size() == combinedAnnotationsNew.size()) {
                break;
            }
            combinedAnnotations = combinedAnnotationsNew;

        }

        return combinedAnnotations;
    }

    private static List<ClassifiedAnnotation> combineAnnotations(String text, List<ClassifiedAnnotation> annotations) {
        var result = new ArrayList<ClassifiedAnnotation>();
        ClassifiedAnnotation currentAnnotation = null;
        var categoryEntriesBuilder = new CategoryEntriesBuilder();

        for (var annotation : annotations) {
            if (currentAnnotation == null) {
                currentAnnotation = annotation;
                continue;
            }
            // previous token is directly adjacent to current one - no space
            var sameWord = currentAnnotation.getEndPosition() == annotation.getStartPosition();
            // previous token has same tag
            var sameTag = currentAnnotation.getTag().equals(annotation.getTag());
            // previous token is a different word - space between
            var newWord = currentAnnotation.getEndPosition() + 1 == annotation.getStartPosition();
            if (sameWord || (sameTag && newWord)) {
                // merge into current annotation
                var value = text.substring(currentAnnotation.getStartPosition(), annotation.getEndPosition());
                categoryEntriesBuilder.add(annotation.getCategoryEntries());
                currentAnnotation = new ClassifiedAnnotation(currentAnnotation.getStartPosition(), value,
                        categoryEntriesBuilder.create());
            } else {
                result.add(currentAnnotation);
                currentAnnotation = annotation;
                categoryEntriesBuilder = new CategoryEntriesBuilder();
                categoryEntriesBuilder.add(annotation.getCategoryEntries());
            }
        }
        if (currentAnnotation != null) {
            result.add(currentAnnotation);
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            session.close();
        } catch (OrtException e) {
            // ignore
        }
        tokenizer.close();
    }

}