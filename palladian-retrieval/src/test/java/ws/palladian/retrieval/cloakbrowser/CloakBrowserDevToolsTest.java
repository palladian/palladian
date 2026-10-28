package ws.palladian.retrieval.cloakbrowser;

import org.junit.Test;
import ws.palladian.persistence.json.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Closing a tab over the browser's DevTools websocket, the path that works while the tab's own session is wedged. The
 * end-to-end tests run against {@link FakeDevTools}, a minimal stand-in for cloakserve: {@code /json/version},
 * {@code /json/list} and one browser websocket.
 */
public class CloakBrowserDevToolsTest {

    private static final String TAB = "C1C14BB2826C48B04F16A66A5D28310E";
    private static final String OTHER_TAB = "EA29EA3F492D0CF0D1F149CA6C1772F6";

    @Test
    public void closeCommandAddressesTheTarget() throws Exception {
        JsonObject command = new JsonObject(CloakBrowserDevTools.closeTargetCommand(7, TAB));
        assertEquals(Integer.valueOf(7), command.tryGetInt("id"));
        assertEquals("Target.closeTarget", command.tryGetString("method"));
        assertEquals(TAB, command.tryGetJsonObject("params").tryGetString("targetId"));
    }

    @Test
    public void readsChromesAnswers() throws Exception {
        assertTrue(CloakBrowserDevTools.isClosedOrGone(new JsonObject("{\"id\":1,\"result\":{\"success\":true}}")));
        assertTrue(CloakBrowserDevTools.isClosedOrGone(new JsonObject("{\"id\":1,\"result\":{}}")));
        assertTrue(CloakBrowserDevTools.isClosedOrGone(new JsonObject("{\"id\":1,\"error\":{\"code\":-32602,\"message\":\"No target with given id found\"}}")));
        assertFalse(CloakBrowserDevTools.isClosedOrGone(new JsonObject("{\"id\":1,\"result\":{\"success\":false}}")));
        assertFalse(CloakBrowserDevTools.isClosedOrGone(new JsonObject("{\"id\":1,\"error\":{\"code\":-32000,\"message\":\"Not allowed\"}}")));
    }

    @Test
    public void listsOnlyTabs() throws Exception {
        String list = "[{\"id\":\"" + TAB + "\",\"type\":\"page\"},{\"id\":\"F1\",\"type\":\"iframe\"},{\"id\":\"W1\",\"type\":\"service_worker\"},"
                + "{\"id\":\"" + OTHER_TAB + "\",\"type\":\"page\"}]";
        assertEquals(List.of(TAB, OTHER_TAB), List.copyOf(CloakBrowserDevTools.parsePageTargetIds(list)));
    }

    @Test(expected = IOException.class)
    public void aVersionWithoutWebSocketUrlIsAnError() throws IOException {
        CloakBrowserDevTools.parseWebSocketDebuggerUrl("{\"Browser\":\"Chrome/146.0.7680.177\"}");
    }

    @Test
    public void closesTabsOverTheBrowserWebSocket() throws Exception {
        try (FakeDevTools devTools = new FakeDevTools(command -> "{\"id\":" + command.tryGetInt("id") + ",\"result\":{}}")) {
            Set<String> closed = CloakBrowserDevTools.closeTargets(devTools.address(), List.of(TAB, OTHER_TAB));
            assertEquals(Set.of(TAB, OTHER_TAB), closed);
            assertEquals(2, devTools.commands.size());
            assertEquals(TAB, devTools.commands.get(0).tryGetJsonObject("params").tryGetString("targetId"));
        }
    }

    @Test
    public void aTabThatIsAlreadyGoneCountsAsClosed() throws Exception {
        try (FakeDevTools devTools = new FakeDevTools(
                command -> "{\"id\":" + command.tryGetInt("id") + ",\"error\":{\"code\":-32602,\"message\":\"No target with given id found\"}}")) {
            assertEquals(Set.of(TAB), CloakBrowserDevTools.closeTargets(devTools.address(), List.of(TAB)));
        }
    }

    /** A browser that never answers costs the bounded timeout, not a hung replace. */
    @Test(timeout = 15_000)
    public void aBrowserThatDoesNotAnswerClosesNothing() throws Exception {
        try (FakeDevTools devTools = new FakeDevTools(command -> null)) {
            assertTrue(CloakBrowserDevTools.closeTargets(devTools.address(), List.of(TAB)).isEmpty());
            assertEquals("the command went out and the answer was waited for", 1, devTools.commands.size());
        }
    }

    /** {@code HttpRequest.timeout} stops at the headers: a body that never finishes must still cost only the bound. */
    @Test(timeout = 15_000)
    public void aBrowserThatStallsMidAnswerCostsTheBoundNotAHang() throws Exception {
        try (ServerSocket stalling = new ServerSocket(0, 10, InetAddress.getLoopbackAddress())) {
            Thread thread = new Thread(() -> {
                while (!stalling.isClosed()) {
                    try {
                        Socket socket = stalling.accept();
                        FakeDevTools.readHeaders(socket.getInputStream());
                        socket.getOutputStream().write(
                                "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 100\r\n\r\n[{\"id\":".getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                        // and never the rest; the socket stays open until the server closes
                    } catch (IOException e) {
                        return;
                    }
                }
            }, "stalling-devtools");
            thread.setDaemon(true);
            thread.start();
            String address = "127.0.0.1:" + stalling.getLocalPort();

            assertTrue(CloakBrowserDevTools.closeTargets(address, List.of(TAB)).isEmpty());
            try {
                CloakBrowserDevTools.listPageTargetIds(address);
                fail("a stalled /json/list must fail");
            } catch (IOException expected) {
                // within the bound
            }
        }
    }

    @Test
    public void anUnreachableBrowserClosesNothingAndDoesNotThrow() {
        assertTrue(CloakBrowserDevTools.closeTargets("127.0.0.1:1", List.of(TAB)).isEmpty());
    }

    @Test
    public void nothingToCloseNeedsNoBrowser() {
        assertTrue(CloakBrowserDevTools.closeTargets("127.0.0.1:1", Collections.emptyList()).isEmpty());
    }

    @Test
    public void listsTheBrowsersTabsOverHttp() throws Exception {
        try (FakeDevTools devTools = new FakeDevTools(command -> null)) {
            assertEquals(Set.of(TAB), CloakBrowserDevTools.listPageTargetIds(devTools.address()));
        }
    }

    /**
     * Serves {@code /json/version}, {@code /json/list} (one tab, {@link #TAB}) and a browser websocket that hands every
     * command to {@code answer}, which returns the response text or {@code null} for no answer.
     */
    private static final class FakeDevTools implements Closeable {
        private static final Pattern KEY = Pattern.compile("(?im)^Sec-WebSocket-Key:\\s*(\\S+)");
        final List<JsonObject> commands = new CopyOnWriteArrayList<>();
        private final ServerSocket server;
        private final Function<JsonObject, String> answer;

        FakeDevTools(Function<JsonObject, String> answer) throws IOException {
            this.answer = answer;
            server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
            Thread thread = new Thread(this::serve, "fake-devtools");
            thread.setDaemon(true);
            thread.start();
        }

        String address() {
            return "127.0.0.1:" + server.getLocalPort();
        }

        private void serve() {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    handle(socket);
                } catch (Exception e) {
                    // the client went away or the server closed
                }
            }
        }

        private void handle(Socket socket) throws Exception {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            String request = readHeaders(in);
            Matcher key = KEY.matcher(request);
            if (key.find()) {
                String accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                        .digest((key.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
                out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + accept
                        + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.flush();
                String text;
                while ((text = readTextFrame(in)) != null) {
                    JsonObject command = new JsonObject(text);
                    commands.add(command);
                    String response = answer.apply(command);
                    if (response != null) {
                        writeTextFrame(out, response);
                    }
                }
            } else if (request.startsWith("GET /json/version")) {
                writeJson(out, "{\"Browser\":\"Chrome/146.0.7680.177\",\"webSocketDebuggerUrl\":\"ws://" + address() + "/devtools/browser/fake\"}");
            } else {
                writeJson(out, "[{\"id\":\"" + TAB + "\",\"type\":\"page\",\"url\":\"about:blank\"},{\"id\":\"F1\",\"type\":\"iframe\"}]");
            }
        }

        private static String readHeaders(InputStream in) throws IOException {
            ByteArrayOutputStream headers = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != -1) {
                headers.write(b);
                byte[] soFar = headers.toByteArray();
                int n = soFar.length;
                if (n >= 4 && soFar[n - 4] == '\r' && soFar[n - 3] == '\n' && soFar[n - 2] == '\r' && soFar[n - 1] == '\n') {
                    break;
                }
            }
            return headers.toString(StandardCharsets.US_ASCII);
        }

        private static void writeJson(OutputStream out, String json) throws IOException {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
        }

        /** One client frame (always masked); {@code null} on a close frame or end of stream. */
        private static String readTextFrame(InputStream in) throws IOException {
            int first = in.read();
            int second = in.read();
            if (first == -1 || second == -1 || (first & 0x0F) == 0x8) {
                return null;
            }
            long length = second & 0x7F;
            if (length == 126) {
                length = (in.read() << 8) | in.read();
            } else if (length == 127) {
                length = 0;
                for (int i = 0; i < 8; i++) {
                    length = (length << 8) | in.read();
                }
            }
            byte[] mask = in.readNBytes(4);
            byte[] payload = in.readNBytes((int) length);
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i % 4];
            }
            return new String(payload, StandardCharsets.UTF_8);
        }

        private static void writeTextFrame(OutputStream out, String text) throws IOException {
            byte[] payload = text.getBytes(StandardCharsets.UTF_8);
            out.write(0x81);
            if (payload.length < 126) {
                out.write(payload.length);
            } else {
                out.write(126);
                out.write(payload.length >> 8);
                out.write(payload.length & 0xFF);
            }
            out.write(payload);
            out.flush();
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}
