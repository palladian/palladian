package ws.palladian.extraction.location.evaluation;

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
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * TODO currently just a parser for the JSON data. Not trivial to integrate, as text files need to be present on a web
 * server and processing takes time.
 * 
 * @see <a href="http://unlock.edina.ac.uk/texts/api">API Documentation</a>
 * @author Philipp Katz
 */
class UnlockTextClient {

    private static final String BASE_URL = "http://unlock.edina.ac.uk/text-api/users/";

    private static final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    private final String username;

    private final String password;

    public UnlockTextClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

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

    public void addDocuments(String jobName, List<String> documentUrls) {
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

    public void getTextStatus(String batchName, String documentName) {
        try {
            String requestUrl = BASE_URL + username + "/batchjobs/" + batchName + "/" + documentName;
            HttpRequest getStatusRequest = new HttpRequest(HttpMethod.GET, requestUrl);
            getStatusRequest.addHeader("accept", "application/json");
            getStatusRequest.addHeader("Authorization", password);
            HttpResult getStatusResult = retriever.execute(getStatusRequest);
            // System.out.println(HttpHelper.getStringContent(getStatusResult));
            JSONObject jsonObject = new JSONObject(getStatusResult.getStringContent());
            String output = documentName + ": ";
            if (jsonObject.has("status-code")) {
                output += jsonObject.getString("status-code") + "; " + jsonObject.optString("message");
            } else if (jsonObject.has("output")) {
                output += "done.";
            } else {
                output += "?";
            }
            System.out.println(output);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getText(String batchName, String documentName) {
        try {
            String requestUrl = BASE_URL + username + "/batchjobs/" + batchName + "/" + documentName + ".json";
            HttpRequest getTextRequest = new HttpRequest(HttpMethod.GET, requestUrl);
            getTextRequest.addHeader("accept", "application/json");
            getTextRequest.addHeader("Authorization", password);
            HttpResult getStatusResult = retriever.execute(getTextRequest);
            return getStatusResult.getStringContent();
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        }
    }

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



    public static void main(String[] args) throws Exception {

        String username = "palladian-test-user";
        String password = "opabonand";
        UnlockTextClient unlockTextEvaluator = new UnlockTextClient(username, password);
        int numFiles = 152;

        // unlockTextEvaluator.registerUser();

        // List<String> documentUrls = CollectionHelper.newArrayList();
        // for (int i = 1; i <= numFiles; i++) {
        // documentUrls.add(String.format("http://palladian.ws/tudLocEvaluation/text%s.txt", i));
        // }
        // unlockTextEvaluator.addDocuments("palladian-test-evaluation", documentUrls);

        // ... wait ...

        // for (int i = 1; i <= numFiles; i++) {
        // unlockTextEvaluator.getTextStatus("palladian-test-evaluation", "palladian-test-evaluation" + i);
        // }

        for (int i = 1; i <= numFiles; i++) {
            String text = unlockTextEvaluator.getText("palladian-test-evaluation", "palladian-test-evaluation" + i);
            FileHelper.writeToFile("/Users/pk/Desktop/LocationLab/UnlockTextResults/text" + i + ".json", text);
        }

        // unlockTextEvaluator.deleteBatch("palladian-test-evaluation");
    }

}
