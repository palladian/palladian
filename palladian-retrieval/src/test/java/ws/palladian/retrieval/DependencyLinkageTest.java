package ws.palladian.retrieval;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * Exercises libraries that Maven resolves from more than one path, where a version mix compiles fine but fails at
 * runtime with {@link NoSuchMethodError} or {@link NoSuchFieldError}.
 */
public class DependencyLinkageTest {

    /**
     * OkHttp 4 needs okio 3, but sib-api-v3-sdk declares okio 1.14.0, which won until palladian-retrieval declared
     * okio itself. OkHttp then failed on first use ({@code NoSuchFieldError: ByteString.Companion}). DeepAiApi,
     * QwantSearcher and SendInBlueMailer go through OkHttp.
     */
    @Test
    public void testOkHttpRequest() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = "okhttp works".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        });
        server.start();
        OkHttpClient client = null;
        try {
            client = new OkHttpClient();
            Request request = new Request.Builder().url("http://localhost:" + server.getAddress().getPort() + "/").build();
            try (Response response = client.newCall(request).execute()) {
                assertEquals(200, response.code());
                assertEquals("okhttp works", response.body().string());
            }
        } finally {
            server.stop(0);
            if (client != null) {
                client.dispatcher().executorService().shutdown();
                client.connectionPool().evictAll();
            }
        }
    }

    /**
     * commons-compress 1.28.0 (via webdrivermanager, which unpacks driver archives with it) needs commons-io 2.20+, but
     * palladian-commons pinned 2.14.0, so writing a tar header failed ({@code NoSuchMethodError: FileTimes.toUnixTime}).
     */
    @Test
    public void testTarRoundTrip() throws IOException {
        byte[] content = "driver".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream tar = new ByteArrayOutputStream();
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(tar)) {
            TarArchiveEntry entry = new TarArchiveEntry("geckodriver");
            entry.setSize(content.length);
            out.putArchiveEntry(entry);
            out.write(content);
            out.closeArchiveEntry();
        }
        try (TarArchiveInputStream in = new TarArchiveInputStream(new ByteArrayInputStream(tar.toByteArray()))) {
            TarArchiveEntry entry = in.getNextEntry();
            assertEquals("geckodriver", entry.getName());
            assertEquals("driver", new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
        }
    }
}
