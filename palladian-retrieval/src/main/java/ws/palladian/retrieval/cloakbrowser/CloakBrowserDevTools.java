package ws.palladian.retrieval.cloakbrowser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Talks to the CloakBrowser container's Chrome directly over the DevTools protocol, without a WebDriver session.
 * <p>
 * A session's tab has to be closable when its session is not: sessions are mostly replaced because they are wedged,
 * and a close sent through a wedged session's chromedriver hangs and fails. That left 206 tabs (10 cores, 18 GB) in
 * the shared browser on skycraft. The container's proxy (cloakserve) serves only {@code /json/version},
 * {@code /json/list} and the DevTools websockets, so tabs are closed with {@code Target.closeTarget} over the
 * browser-level websocket.
 */
final class CloakBrowserDevTools {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloakBrowserDevTools.class);

    /** Bound for one call: closing tabs runs on the replace and shutdown paths, which must not hang. */
    static final Duration TIMEOUT = Duration.ofSeconds(5);

    /** Chrome's answer to closing a target that is already gone; for the caller that is as good as closed. */
    private static final String NO_SUCH_TARGET = "No target with given id";

    private CloakBrowserDevTools() {
        // static helpers
    }

    /** Ids of the browser's open tabs ("page" targets, so no iframes or workers), within {@link #TIMEOUT}. */
    static Set<String> listPageTargetIds(String debuggerAddress) throws IOException, InterruptedException {
        return parsePageTargetIds(get(debuggerAddress, "/json/list", System.nanoTime() + TIMEOUT.toNanos()));
    }

    /**
     * Close tabs by target id over the browser's DevTools websocket, all within one {@link #TIMEOUT}. Never throws: a
     * failure is logged and the tabs it did not get to are simply not in the result.
     *
     * @return the ids that are closed now, including those that were already gone.
     */
    static Set<String> closeTargets(String debuggerAddress, Collection<String> targetIds) {
        Set<String> closed = new LinkedHashSet<>();
        if (targetIds.isEmpty()) {
            return closed;
        }
        WebSocket webSocket = null;
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        try {
            String browserUrl = parseWebSocketDebuggerUrl(get(debuggerAddress, "/json/version", deadline));
            Responses responses = new Responses();
            webSocket = HttpClient.newHttpClient().newWebSocketBuilder().connectTimeout(TIMEOUT).buildAsync(URI.create(browserUrl), responses)
                    .get(remainingMillis(deadline), TimeUnit.MILLISECONDS);
            int id = 0;
            for (String targetId : targetIds) {
                CompletableFuture<JsonObject> response = responses.expect(++id);
                webSocket.sendText(closeTargetCommand(id, targetId), true).get(remainingMillis(deadline), TimeUnit.MILLISECONDS);
                if (isClosedOrGone(response.get(remainingMillis(deadline), TimeUnit.MILLISECONDS))) {
                    closed.add(targetId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.warn("CloakBrowser: could not close tab(s) {} over DevTools at {} ({} of {} closed): {}", targetIds, debuggerAddress, closed.size(),
                    targetIds.size(), e.toString());
        } finally {
            if (webSocket != null) {
                webSocket.abort();
            }
        }
        return closed;
    }

    private static long remainingMillis(long deadlineNanos) throws TimeoutException {
        long remaining = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
        if (remaining <= 0) {
            throw new TimeoutException("no answer within " + TIMEOUT);
        }
        return remaining;
    }

    /**
     * GET within the deadline, body included: {@code HttpRequest.timeout} stops counting at the response headers, so a
     * browser that stalls mid-body would block the caller for good.
     */
    private static String get(String debuggerAddress, String path, long deadlineNanos) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + debuggerAddress + path)).GET().build();
        CompletableFuture<HttpResponse<String>> pending = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(TIMEOUT).build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            HttpResponse<String> response = pending.get(remainingMillis(deadlineNanos), TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) {
                throw new IOException(path + " answered HTTP " + response.statusCode());
            }
            return response.body();
        } catch (ExecutionException e) {
            throw new IOException(path + " failed: " + e.getCause(), e.getCause());
        } catch (TimeoutException e) {
            throw new IOException(path + " did not answer within " + TIMEOUT, e);
        } finally {
            pending.cancel(true);
        }
    }

    /** Package-private + pure so it is unit-testable without a browser. */
    static String closeTargetCommand(int id, String targetId) {
        JsonObject params = new JsonObject();
        params.put("targetId", targetId);
        JsonObject command = new JsonObject();
        command.put("id", id);
        command.put("method", "Target.closeTarget");
        command.put("params", params);
        return command.toString();
    }

    /** Package-private + pure so it is unit-testable without a browser. */
    static boolean isClosedOrGone(JsonObject response) {
        JsonObject result = response.tryGetJsonObject("result");
        if (result != null) {
            // Chrome answers {"success": true}; newer versions send an empty result
            return !Boolean.FALSE.equals(result.tryGetBoolean("success"));
        }
        JsonObject error = response.tryGetJsonObject("error");
        String message = error == null ? null : error.tryGetString("message");
        return message != null && message.contains(NO_SUCH_TARGET);
    }

    /** Package-private + pure so it is unit-testable without a browser. */
    static String parseWebSocketDebuggerUrl(String versionJson) throws IOException {
        try {
            String url = new JsonObject(versionJson).tryGetString("webSocketDebuggerUrl");
            if (url == null || url.isEmpty()) {
                throw new IOException("/json/version has no webSocketDebuggerUrl");
            }
            return url;
        } catch (JsonException e) {
            throw new IOException("/json/version is not JSON", e);
        }
    }

    /** Package-private + pure so it is unit-testable without a browser. */
    static Set<String> parsePageTargetIds(String listJson) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        try {
            JsonArray targets = new JsonArray(listJson);
            for (int i = 0; i < targets.size(); i++) {
                JsonObject target = targets.tryGetJsonObject(i);
                if (target != null && "page".equals(target.tryGetString("type")) && target.tryGetString("id") != null) {
                    ids.add(target.tryGetString("id"));
                }
            }
        } catch (JsonException e) {
            throw new IOException("/json/list is not JSON", e);
        }
        return ids;
    }

    /** Matches answers to commands by id; events (no id) are dropped. */
    private static final class Responses implements WebSocket.Listener {
        private final Map<Integer, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
        private final StringBuilder message = new StringBuilder();

        CompletableFuture<JsonObject> expect(int id) {
            return pending.computeIfAbsent(id, k -> new CompletableFuture<>());
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            message.append(data);
            if (last) {
                String text = message.toString();
                message.setLength(0);
                try {
                    JsonObject json = new JsonObject(text);
                    Integer id = json.tryGetInt("id");
                    if (id != null) {
                        expect(id).complete(json);
                    }
                } catch (JsonException e) {
                    LOGGER.debug("CloakBrowser: unreadable DevTools message {}", text);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            failPending(new IOException("DevTools websocket closed: " + statusCode + " " + reason));
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            failPending(error);
        }

        private void failPending(Throwable error) {
            for (CompletableFuture<JsonObject> response : List.copyOf(pending.values())) {
                response.completeExceptionally(error);
            }
        }
    }
}
