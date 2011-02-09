package tud.iir.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

public class HTTPPoster {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(HTTPPoster.class);

    private HttpClient client;

    public HTTPPoster() {
        client = new HttpClient();
        
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

    public PostMethod createPostMethod(String url) {
        return createPostMethod(url, new HashMap<String, String>(), new NameValuePair[0]);
    }

    public PostMethod createPostMethod(String url, Map<String, String> headerFields, NameValuePair[] bodyFields) {

        PostMethod method = new PostMethod(url);

        for (Entry<String, String> headerField : headerFields.entrySet()) {
            method.setRequestHeader(headerField.getKey(), headerField.getValue());
        }

        method.setRequestBody(bodyFields);

        return method;
    }

    public String handleRequest(PostMethod method) {

        String response = "";

        try {
            int returnCode = client.executeMethod(method);
            if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
                LOGGER.error("The Post method is not implemented by this URI");
            } else if (returnCode == HttpStatus.SC_OK) {
                response = method.getResponseBodyAsString();
                LOGGER.debug("File post succeeded");
            } else {
                LOGGER.error("File post failed");
                LOGGER.error("Got code: " + returnCode);
                LOGGER.error("response: " + method.getResponseBodyAsString());
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            method.releaseConnection();
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

    public void postFile(File file, PostMethod method) throws IOException {
        method.setRequestEntity(new FileRequestEntity(file, null));
        handleRequest(method);
    }

    public static void main(String[] args) {

    }

}
