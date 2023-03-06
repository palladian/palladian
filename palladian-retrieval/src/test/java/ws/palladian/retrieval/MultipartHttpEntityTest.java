package ws.palladian.retrieval;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Scanner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MultipartHttpEntityTest {

    private static final byte[] RANDOM_DATA;
    private static final String BOUNDARY = "xoxoxoxoxoxoxoxo";

    static {
        RANDOM_DATA = new byte[16];
        new Random().nextBytes(RANDOM_DATA);
    }

    @Test
    public void test_HttpEntityPart_content() {
        MultipartHttpEntity.HttpEntityPart entityPart = new MultipartHttpEntity.HttpEntityPart(new StringHttpEntity("foo", "text/plain"), "text", null);
        String entityString = streamToString(entityPart.getInputStream(BOUNDARY));
        String[] lines = entityString.split("\r\n");
        assertEquals("must contain 6 lines separated with CRLF", 6, lines.length);
        assertEquals("line 0 must be empty", "", lines[0]);
        assertEquals("line 1 must contain --boundary", "--" + BOUNDARY, lines[1]);
        assertEquals("line 2 must contain Content-Disposition", "Content-Disposition: form-data; name=\"text\"", lines[2]);
        assertEquals("line 3 must contain Content-Type", "Content-Type: text/plain", lines[3]);
        assertEquals("line 4 must be empty", "", lines[4]);
        assertEquals("line 5 must be entity content", "foo", lines[5]);
    }

    @Test
    public void test_HttpEntityPart_length() throws IOException {
        MultipartHttpEntity.HttpEntityPart entityPart = new MultipartHttpEntity.HttpEntityPart(new StringHttpEntity("foo", "text/plain"), "text", null);

        long length = entityPart.length(BOUNDARY);
        long streamLength = getStreamLength(entityPart.getInputStream(BOUNDARY));

        assertEquals(".length() must be the actual InputStream length", streamLength, length);
    }

    // TODO this accesses the network -- move this into "integration testing" phase in the future
    @Test
    public void test_createMultipartHttpEntity() throws HttpException, JsonException {

        MultipartHttpEntity.Builder entityBuilder = new MultipartHttpEntity.Builder();
        entityBuilder.addPart(new StringHttpEntity("{\"foo\": \"bar\"}", "application/json"), "json", null);
        entityBuilder.addPart(new InputStreamHttpEntity(RANDOM_DATA, "application/octet-stream"), "data", "test.dat");
        MultipartHttpEntity httpEntity = entityBuilder.create();

        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.POST, "https://postman-echo.com/post");
        requestBuilder.setEntity(httpEntity);

        // dumpStream(httpEntity.getInputStream());

        HttpResult httpResult = HttpRetrieverFactory.getHttpRetriever().execute(requestBuilder.create());

        JsonObject json = new JsonObject(httpResult.getStringContent());
        String jsonString = json.getJsonObject("form").getString("json");

        assertEquals("{\"foo\": \"bar\"}", jsonString);

        String fileBase64 = json.getJsonObject("files").getString("test.dat");
        String trimmedFileBase64 = fileBase64.substring(fileBase64.indexOf(",") + 1);
        assertArrayEquals(RANDOM_DATA, Base64.decodeBase64(trimmedFileBase64));

    }

    static String streamToString(InputStream inputStream) {
        try (Scanner s = new Scanner(inputStream)) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : null;
        }
    }

    static long getStreamLength(InputStream inputStream) throws IOException {
        long length = 0;
        while (inputStream.read() != -1) {
            length++;
        }
        return length;
    }

}
