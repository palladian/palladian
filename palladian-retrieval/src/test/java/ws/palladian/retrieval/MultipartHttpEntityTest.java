package ws.palladian.retrieval;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

public class MultipartHttpEntityTest {

	private static final byte[] RANDOM_DATA;

	static {
		RANDOM_DATA = new byte[16];
		new Random().nextBytes(RANDOM_DATA);
	}

	@Test
	public void test_createMultipartHttpEntity() throws HttpException, FileNotFoundException, JsonException {

		// FIXME it shouldn't be necessary to add this manually
		String fixme = "\r\n";

		MultipartHttpEntity.Builder entityBuilder = new MultipartHttpEntity.Builder();
		entityBuilder.addPart(new StringHttpEntity("{\"foo\": \"bar\"}" + fixme, "application/json"), "json", null);
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

	static void dumpStream(InputStream inputStream) {
		try (Scanner s = new Scanner(inputStream)) {
			s.useDelimiter("\\A");
			if (s.hasNext()) {
				System.out.println(s.next());
			}
		}
	}

}
