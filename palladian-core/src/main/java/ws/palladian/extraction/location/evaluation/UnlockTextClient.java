package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * Test client for Unlock. As the service works asynchronous and takes a long time for processing, there are several
 * steps:
 * <ol>
 * <li>Create clean texts from the annotated dataset using {@link #writeCleanTexts(String, String)}</li>
 * <li>Upload the clean texts to a web server, so that Unlock can access them.</li>
 * <li>Set up a user account at Unlock using {@link #registerUser()}</li>
 * <li>Create a new batch job, using {@link #addDocuments(String, List)}</li>
 * <li>... wait ...</li>
 * <li>Check the status using {@link #printBatchStatus(String)}</li>
 * <li>Fetch the batch result using {@link #fetchBatchResult(String, String)}</li>
 * <li>Delete the batch using {@link #deleteBatch(String)}</li>
 * <li>Use the {@link UnlockTextMockExtractor} to evaluate.</li>
 * </ol>
 * 
 * @see <a href="http://unlock.edina.ac.uk/texts/api">API Documentation</a>
 * @author Philipp Katz
 */
class UnlockTextClient {

    private static final String BASE_URL = "http://unlock.edina.ac.uk/text-api/users/";

    private static final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    private final String username;

    private final String password;

    /**
     * <p>
     * Create a new Unlock client. If the username/password have not been used before, call {@link #registerUser()}
     * after init.
     * </p>
     * 
     * @param username The username.
     * @param password The password.
     */
    public UnlockTextClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * <p>
     * Create a user account with the credentials specified in the constructor.
     * </p>
     */
    public void registerUser() {
        try {
            HttpRequest createUserRequest = new HttpRequest(HttpMethod.POST, BASE_URL + username);
            createUserRequest.addHeader("accept", "application/json");
            createUserRequest.addHeader("Authorization", password);
            HttpResult createUserResult = retriever.execute(createUserRequest);
            System.out.println(createUserResult.getStringContent());
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * <p>
     * Create a new job with the documents from the specified URL.
     * </p>
     * 
     * @param jobName The name of the job, used to access it later.
     * @param baseUrl The directory, where the texts are available on the web for Unlock to retrieve them.
     * @param textPath The local path to the directory, used for obtaining the file names.
     */
    public void addDocuments(String jobName, String baseUrl, File textsPath) {
        List<String> documentUrls = CollectionHelper.newArrayList();
        File[] files = FileHelper.getFiles(textsPath.getPath());
        for (File file : files) {
            if (file.getName().startsWith("text")) {
                documentUrls.add(String.format("%s/%s", baseUrl, file.getName()));
            }
        }
        try {
            String postUrl = BASE_URL + username + "/batchjobs/" + jobName;
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(postUrl);
            post.setHeader("accept", "application/json");
            post.setHeader("Authorization", password);
            post.setHeader("Content-Type", "application/json");

            StringBuilder requestContent = new StringBuilder();
            requestContent.append("{\"Texts\":[");
            boolean first = true;
            for (String documentUrl : documentUrls) {
                if (first) {
                    first = false;
                } else {
                    requestContent.append(',');
                }
                requestContent.append('\n');
                requestContent.append("   {\"src\":\"" + documentUrl + "\"}");
            }
            requestContent.append("\n]}");
            post.setEntity(new StringEntity(requestContent.toString()));
            HttpResponse response = client.execute(post);
            String result = EntityUtils.toString(response.getEntity());
            System.out.println(result);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * <p>
     * Print the current status of a batch.
     * </p>
     * 
     * @param batchName The name of the batch.
     */
    public void printBatchStatus(String batchName) {
        try {
            int completeCount = 0;
            JsonObject batchStatus = getBatchStatus(batchName);
            JsonArray textsArray = batchStatus.getJsonArray("Texts");
            for (Object textObject : textsArray) {
                JsonObject jsonTextObject = (JsonObject)textObject;
                String source = jsonTextObject.getString("src");
                String status = jsonTextObject.getString("status");
                System.out.println(source + " : " + status);
                if ("complete".equals(status)) {
                    completeCount++;
                }
            }
            System.out.println("\n\nCompleted " + completeCount + "/" + textsArray.size());
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        } catch (JsonException e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonObject getBatchStatus(String batchName) throws HttpException, JsonException {
        String requestUrl = BASE_URL + username + "/batchjobs/" + batchName;
        HttpRequest getStatusRequest = new HttpRequest(HttpMethod.GET, requestUrl);
        getStatusRequest.addHeader("accept", "application/json");
        getStatusRequest.addHeader("Authorization", password);
        HttpResult getStatusResult = retriever.execute(getStatusRequest);
        return new JsonObject(getStatusResult.getStringContent());
    }

    private String getText(String batchName, String requestUrl) throws HttpException {
        HttpRequest getTextRequest = new HttpRequest(HttpMethod.GET, requestUrl);
        getTextRequest.addHeader("accept", "application/json");
        getTextRequest.addHeader("Authorization", password);
        HttpResult getStatusResult = retriever.execute(getTextRequest);
        return getStatusResult.getStringContent();
    }

    /**
     * <p>
     * Retrieve all the results of a batch.
     * </p>
     * 
     * @param batchName The name of the batch.
     * @param outputDir The path to the directory where to store the result JSON files.
     */
    public void fetchBatchResult(String batchName, File outputDir) {
        try {
            JsonObject batchStatus = getBatchStatus(batchName);
            JsonArray textsArray = batchStatus.getJsonArray("Texts");
            ProgressMonitor monitor = new ProgressMonitor(textsArray.size(), 1);
            for (Object textObject : textsArray) {
                JsonObject jsonTextObject = (JsonObject)textObject;
                String source = jsonTextObject.getString("src");
                String resourceUri = jsonTextObject.getString("resource-uri");
                String text = getText(batchName, resourceUri + ".json");
                File outputFile = new File(outputDir, source.substring(source.lastIndexOf("/"))
                        .replace(".txt", ".json"));
                FileHelper.writeToFile(outputFile.getPath(), text);
                monitor.incrementAndPrintProgress();
            }
        } catch (JsonException e) {
            throw new IllegalStateException(e);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * <p>
     * Delete a batch.
     * </p>
     * 
     * @param batchName Name of the batch to delete.
     */
    public void deleteBatch(String batchName) {
        try {
            String postUrl = BASE_URL + username + "/batchjobs/" + batchName;
            HttpClient client = new DefaultHttpClient();
            HttpDelete delete = new HttpDelete(postUrl);
            delete.setHeader("accept", "application/json");
            delete.setHeader("Authorization", password);
            HttpResponse response = client.execute(delete);
            String result = EntityUtils.toString(response.getEntity());
            System.out.println(result);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void writeCleanTexts(String inputDir, String outputDir) {
        File[] files = FileHelper.getFiles(inputDir);
        for (File file : files) {
            String taggedText = FileHelper.readFileToString(file);
            String strippedText = HtmlHelper.stripHtmlTags(taggedText);
            FileHelper.writeToFile(new File(new File(outputDir), file.getName()).getPath(), strippedText);
        }
    }

    public static void main(String[] args) throws Exception {

        String username = "palladian-test-user";
        String password = "opabonand";

        UnlockTextClient client = new UnlockTextClient(username, password);
        // client.registerUser();

        // client.addDocuments("LGL", "http://palladian.ws/tempLocationTest", new
        // File("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-cleanTexts"));
        // client.printBatchStatus("LGL");
        // client.fetchBatchResult("LGL", new File("/Users/pk/Desktop/UnlockTextResults"));
        client.deleteBatch("LGL");
    }

}
