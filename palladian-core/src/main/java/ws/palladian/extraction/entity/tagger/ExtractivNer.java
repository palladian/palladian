package ws.palladian.extraction.entity.tagger;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotated;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * <p>
 * Named Entity Recognizer from <a href="http://extractiv.com">Extractiv</a>. The web page says, the service is no
 * longer offered, but the NER service still works.
 * </p>
 * 
 * @author Katja Pfeifer
 * @author Philipp Katz
 */
public class ExtractivNer extends NamedEntityRecognizer {

    /** The maximum number of characters allowed to send per request (actually 100,000). */
    private final int MAXIMUM_TEXT_LENGTH = 90000;

    /** The {@link HttpRetriever} is used for performing the POST requests to the API. */
    private final HttpRetriever httpRetriever;

    public ExtractivNer() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public List<Annotated> getAnnotations(String inputText) {

        List<String> sentenceChunks = NerHelper.createSentenceChunks(inputText, MAXIMUM_TEXT_LENGTH);
        if (sentenceChunks.size() > 1) {
            LOGGER.warn("Truncated text into {} chunks.", sentenceChunks.size());
        }

        List<Annotated> annotations = CollectionHelper.newArrayList();
        String response = null;

        try {

            for (String textChunk : sentenceChunks) {
                HttpResult httpResult = getHttpResult(textChunk.toString());
                response = HttpHelper.getStringContent(httpResult);

                List<Annotated> currentAnnotations = parse(response, inputText);
                annotations.addAll(currentAnnotations);
            }

        } catch (JSONException e) {
            throw new IllegalStateException("Exception while parsing the JSON response: " + e.getMessage()
                    + ", JSON was '" + response + "'", e);
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP exception while performing request: " + e.getMessage(), e);
        }

        Collections.sort(annotations);
        return annotations;
    }

    /**
     * <p>
     * Parse the JSON response, perform alignment. Package-private for unit-testing.
     * </p>
     * 
     * @param response The JSON string.
     * @param inputText The original input text.
     * @return List of {@link Annotated} objects.
     * @throws JSONException in case JSON could not be parsed.
     */
    static List<Annotated> parse(String response, String inputText) throws JSONException {
        List<Annotated> annotations = CollectionHelper.newArrayList();

        JSONObject jsonResponse = new JSONObject(response);
        JSONArray jsonEntities = jsonResponse.getJSONArray("entities");

        for (int i = 0; i < jsonEntities.length(); i++) {
            JSONObject jsonEntity = jsonEntities.getJSONObject(i);
            String type = jsonEntity.getString("type");
            String text = jsonEntity.getString("text");
            int offset = jsonEntity.getInt("offset");
            annotations.add(new Annotation(offset, text, type));
        }
        return alignContentText(annotations, inputText);
    }

    /**
     * <p>
     * Perform an alignment of the annotations to the real input text. The NER drops some characters from the text,
     * which leads to shifted annotation offsets. This method loops through all provided {@link Annotated} objects and
     * shifts the offsets if necessary.
     * </p>
     * 
     * @param annotations The {@link Annotated} objects to align.
     * @param correctContent The originally supplied text.
     * @return The correctly aligned {@link Annotated} objects.
     */
    // TODO move this to global NerHelper class; as this might be relevant for other NERs also.
    private static List<Annotated> alignContentText(List<Annotated> annotations, String correctContent) {
        List<Annotated> alignedAnnotations = CollectionHelper.newArrayList();

        // the current offset, everything behind this has already been aligned.
        int currentOffset = 0;

        for (Annotated annotation : annotations) {

            int annotationOffset = annotation.getStartPosition();

            // XXX this is slightly naive; if value of the entity would be changed by the service, we can currently not
            // identify it. During my (limited) testings, this was not an issue though. In case the position cannot be
            // determined, output a warning log message, that the annotation was dropped.
            int actualOffset = correctContent.indexOf(annotation.getValue(), currentOffset);

            if (actualOffset == -1) {
                LOGGER.warn("Could not determine actual offset of {} with offset of {} -- annotation will be dropped.",
                        annotation.getValue(), annotationOffset);
            } else if (annotationOffset != actualOffset) {
                LOGGER.debug("Changing offset of {} from {} to {}.", new Object[] {annotation.getValue(),
                        annotationOffset, actualOffset});
                Annotation correctAnnotation = new Annotation(actualOffset, annotation.getValue(), annotation.getTag());
                alignedAnnotations.add(correctAnnotation);
            } else {
                alignedAnnotations.add(annotation);
            }
            currentOffset = actualOffset + annotation.getValue().length();

        }

        return alignedAnnotations;
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {
        HttpRequest request = new HttpRequest(HttpMethod.POST, "http://rest.extractiv.com/extractiv/");
        request.addHeader("output_format", "json");
        request.addParameter("content", inputText);
        request.addParameter("output_format", "json");
        return httpRetriever.execute(request);
    }

    @Override
    public String getName() {
        return "Extractiv NER";
    }

}