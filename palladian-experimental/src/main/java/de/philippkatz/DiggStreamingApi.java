package de.philippkatz;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * <p>
 * Quick'n'dirty sample code for accessing the Digg streaming API.
 * </p>
 * 
 * @see <a href="http://about.digg.com/blog/introducing-diggs-streaming-api">Introducing Digg's Streaming API</a>
 * @author Philipp Katz
 */
public class DiggStreamingApi {

    public static void main(String[] args) throws Exception {

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://services.digg.com/2.0/stream");
        HttpResponse httpResponse = httpClient.execute(httpGet);
        InputStream inputStream = httpResponse.getEntity().getContent();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            // TODO parse the response
        }

        // TODO add exception handling, handle reconnects if connection should break

    }

}
