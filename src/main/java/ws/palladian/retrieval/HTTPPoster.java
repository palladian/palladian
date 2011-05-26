package ws.palladian.retrieval;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class HTTPPoster {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(HTTPPoster.class);

    private HttpClient client;

    public HTTPPoster() {
        client = new DefaultHttpClient();
        
        // quickfixx by Philipp
        // we set the retry behavior very aggressively (small timeouts, many retries)
        // was necessary for TagThe.net
//        HttpClientParams params = client.getParams();
//        DefaultHttpMethodRetryHandler retryhandler = new DefaultHttpMethodRetryHandler(10, false) {
//            @Override
//            public boolean retryMethod(HttpMethod method, IOException exception, int executionCount) {
//                if (exception instanceof InterruptedIOException || exception instanceof ConnectException) {
//                    // timeout reached, retry
//                    LOGGER.warn(exception.getMessage() + ", sleep for " + executionCount + " seconds");
//                    ThreadHelper.sleep(executionCount * 500);
//                    return true;
//                }
//                return super.retryMethod(method, exception, executionCount);
//            }
//        };
//        params.setParameter(HttpMethodParams.RETRY_HANDLER, retryhandler);
//        params.setParameter(HttpMethodParams.SO_TIMEOUT, 500);
//        params.setParameter(HttpMethodParams.HEAD_BODY_CHECK_TIMEOUT, 500);

    }

//    public HttpPost createPostMethod(String url) {
//        return createPostMethod(url, new HashMap<String, String>(), new NameValuePair[0]);
//    }
//
//    public HttpPost createPostMethod(String url, Map<String, String> headerFields, NameValuePair[] bodyFields) {
//
//        HttpPost method = new HttpPost(url);
//
//        for (Entry<String, String> headerField : headerFields.entrySet()) {
//            method.setRequestHeader(headerField.getKey(), headerField.getValue());
//        }
//
//        method.setRequestBody(bodyFields);
//
//        return method;
//    }

    public String handleRequest(HttpPost method) {

        String response = "";

        try {
            HttpResponse httpResponse = client.execute(method);
            int returnCode = httpResponse.getStatusLine().getStatusCode();
            if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
                LOGGER.error("The Post method is not implemented by this URI");
            } else if (returnCode == HttpStatus.SC_OK) {
                response = EntityUtils.toString(httpResponse.getEntity());
                LOGGER.debug("File post succeeded");
            } else {
                LOGGER.error("File post failed");
                LOGGER.error("Got code: " + returnCode);
                LOGGER.error("response: " + EntityUtils.toString(httpResponse.getEntity()));
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return response;
    }

    /*
     * private void saveResponse(File file, PostMethod method) throws IOException {
     * PrintWriter writer = null;
     * try {
     * BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), "UTF-8"));
     * File out = new File(output, file.getName() + ".xml");
     * writer = new PrintWriter(new BufferedWriter(new FileWriter(out)));
     * String line;
     * while ((line = reader.readLine()) != null) {
     * writer.println(line);
     * }
     * } catch (IOException e) {
     * e.printStackTrace();
     * } finally {
     * if (writer != null) {
     * try {
     * writer.close();
     * } catch (Exception ignored) {
     * }
     * }
     * }
     * }
     */

    public String postFile(File file, HttpPost method) throws IOException {
        method.setEntity(new FileEntity(file, null));
        return handleRequest(method);
    }

    public static void main(String[] args) throws IOException {

    }

}
