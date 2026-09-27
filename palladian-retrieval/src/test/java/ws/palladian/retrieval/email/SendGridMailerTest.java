package ws.palladian.retrieval.email;

import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import org.junit.Test;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Custom headers on {@link SendGridMailer}, checked on the JSON body SendGrid receives. No network: {@code decorate}
 * is the step {@code sendMail} runs on the {@link Mail} right before posting it.
 */
public class SendGridMailerTest {

    private static final String UNSUBSCRIBE_URL = "<https://api.example.com/unsubscribe?u=42&c=drip&t=abc123>";

    private static JsonObject requestBody(SendGridMailer mailer) throws IOException {
        Mail mail = new Mail();
        mailer.decorate(mail);
        JsonObject json = JsonObject.tryParse(mail.build());
        assertNotNull("the mail serializes to JSON", json);
        return json;
    }

    @Test
    public void headersReachTheRequestBodyVerbatim() throws IOException {
        SendGridMailer mailer = new SendGridMailer("no-key-needed");
        mailer.addHeader("List-Unsubscribe", UNSUBSCRIBE_URL);
        mailer.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");

        JsonObject headers = requestBody(mailer).tryGetJsonObject("headers");
        assertNotNull("a headers object is sent", headers);
        assertEquals("the ampersands in the URL are not escaped", UNSUBSCRIBE_URL, headers.tryGetString("List-Unsubscribe"));
        assertEquals("List-Unsubscribe=One-Click", headers.tryGetString("List-Unsubscribe-Post"));
        assertEquals(2, headers.size());
    }

    @Test
    public void categoriesStillReachTheRequestBody() throws IOException {
        SendGridMailer mailer = new SendGridMailer("no-key-needed");
        mailer.addCategory("linkflare");
        mailer.addHeader("List-Unsubscribe", UNSUBSCRIBE_URL);

        JsonArray categories = requestBody(mailer).tryGetJsonArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.size());
        assertEquals("linkflare", categories.tryGetString(0));
    }

    /** A mailer without headers sends no headers object at all, which is what every existing caller gets. */
    @Test
    public void noHeadersMeansNoHeadersObject() throws IOException {
        assertNull(requestBody(new SendGridMailer("no-key-needed")).tryGetJsonObject("headers"));
    }

    /** Headers belong to one mailer instance, like categories. Nothing leaks into the next mail. */
    @Test
    public void headersAreScopedToTheInstance() throws IOException {
        new SendGridMailer("no-key-needed").addHeader("List-Unsubscribe", UNSUBSCRIBE_URL);
        assertNull(requestBody(new SendGridMailer("no-key-needed")).tryGetJsonObject("headers"));
    }

    @Test
    public void theLastValueForANameWins() throws IOException {
        SendGridMailer mailer = new SendGridMailer("no-key-needed");
        mailer.addHeader("List-Unsubscribe", "<https://old.example.com>");
        mailer.addHeader("List-Unsubscribe", UNSUBSCRIBE_URL);
        assertEquals(UNSUBSCRIBE_URL, requestBody(mailer).tryGetJsonObject("headers").tryGetString("List-Unsubscribe"));
    }

    /** A line break in a header would let whoever supplies the value append headers of their own (a Bcc, say). */
    @Test
    public void lineBreaksAreRefused() {
        SendGridMailer mailer = new SendGridMailer("no-key-needed");
        assertRefused(mailer, "List-Unsubscribe", "<https://example.com>\r\nBcc: someone@example.com");
        assertRefused(mailer, "List-Unsubscribe", "<https://example.com>\nBcc: someone@example.com");
        assertRefused(mailer, "X-Evil\r\nBcc", "someone@example.com");
    }

    @Test
    public void blankNamesAndNullValuesAreRefused() throws IOException {
        SendGridMailer mailer = new SendGridMailer("no-key-needed");
        assertRefused(mailer, null, "value");
        assertRefused(mailer, " ", "value");
        assertRefused(mailer, "List-Unsubscribe", null);
        assertNull("a refused header is not half-added", requestBody(mailer).tryGetJsonObject("headers"));
    }

    /** SendGrid refuses the whole mail over a malformed header name, so the name is refused where it is added. */
    @Test
    public void malformedHeaderNamesAreRefused() throws IOException {
        SendGridMailer mailer = new SendGridMailer("no-key-needed");
        assertRefused(mailer, "List-Unsubscribe:", "<https://example.com>");
        assertRefused(mailer, "List Unsubscribe", "<https://example.com>");
        assertRefused(mailer, " List-Unsubscribe", "<https://example.com>");
        assertRefused(mailer, "X-Ümlaut", "value");
        assertNull(requestBody(mailer).tryGetJsonObject("headers"));
    }

    /** The client returns error responses instead of throwing, so only a 2xx may count as sent. */
    @Test
    public void onlyA2xxIsAnAcceptedMail() {
        assertTrue(SendGridMailer.isAccepted(new Response(202, "", new HashMap<>())));
        assertTrue(SendGridMailer.isAccepted(new Response(200, "", new HashMap<>())));
        assertFalse(SendGridMailer.isAccepted(new Response(400, "{\"errors\":[]}", new HashMap<>())));
        assertFalse(SendGridMailer.isAccepted(new Response(401, "", new HashMap<>())));
        assertFalse(SendGridMailer.isAccepted(new Response(413, "", new HashMap<>())));
        assertFalse(SendGridMailer.isAccepted(new Response(500, "", new HashMap<>())));
        assertFalse(SendGridMailer.isAccepted(null));
    }

    private static void assertRefused(SendGridMailer mailer, String name, String value) {
        try {
            mailer.addHeader(name, value);
            fail("expected addHeader(" + name + ", " + value + ") to be refused");
        } catch (IllegalArgumentException expected) {
            // refused
        }
    }
}
