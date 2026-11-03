package ws.palladian.retrieval.analysis;

import org.apache.commons.codec.digest.DigestUtils;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

import java.util.Optional;

public class EmailAnalyzer {
    /** Connect and read timeout for the NameAPI and Gravatar avatar calls: long enough to answer the same way every time. */
    private static final int QUICK_TIMEOUT_MS = 1500;

    private final String apiKey;

    public EmailAnalyzer() {
        this(null);
    }

    public EmailAnalyzer(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Build a profile for an email address. Names come from the public Gravatar profile of that address, gaps are filled
     * by parsing the address with NameAPI (when an API key is set). The image is the Gravatar avatar of that exact
     * address, or null when it has none. The username is the Gravatar username, or null.
     */
    public PersonProfile getProfile(String emailAddress) {
        PersonProfile personProfile = new PersonProfile();
        emailAddress = emailAddress.trim().toLowerCase();
        personProfile.setEmail(emailAddress);

        String md5 = gravatarHash(emailAddress);
        String profileUrl = "https://www.gravatar.com/" + md5 + ".json";
        JsonObject gravatarResponse = null;

        try {
            gravatarResponse = new DocumentRetriever().getJsonObject(profileUrl);
        } catch (Exception e) {
            // ccl
        }

        JsonObject firstEntry = gravatarResponse == null ? null : gravatarResponse.tryQueryJsonObject("entry[0]");
        if (firstEntry != null) {
            personProfile.setUsername(firstEntry.tryQueryString("preferredUsername"));

            String firstName = firstEntry.tryQueryString("name/givenName");
            if (firstName != null) {
                personProfile.setFirstName(firstName);
            }
            String lastName = firstEntry.tryQueryString("name/familyName");
            if (lastName != null) {
                personProfile.setLastName(lastName);
            }
            // only fills the parts givenName and familyName left open
            personProfile.setFullName(firstEntry.tryQueryString("name/formatted"));
        }

        HttpRetriever quickHttpRetriever = createQuickHttpRetriever();

        // the profile's thumbnailUrl uses the hash of the account's primary address, which looks unrelated to this one
        if (hasGravatarAvatar(quickHttpRetriever, md5)) {
            personProfile.setImageUrl(gravatarAvatarUrl(md5));
        }

        if (apiKey != null && (StringHelper.nullOrEmpty(personProfile.getFirstName()) || StringHelper.nullOrEmpty(personProfile.getLastName()))) {
            try {
                DocumentRetriever quickRetriever = new DocumentRetriever(quickHttpRetriever);

                JsonObject parsedEmailJson = quickRetriever.getJsonObject(
                        "http://api.nameapi.org/rest/v5.3/email/emailnameparser?apiKey=" + apiKey + "&emailAddress=" + UrlHelper.encodeParameter(emailAddress));

                JsonArray nameMatches = Optional.ofNullable(parsedEmailJson.tryGetJsonArray("nameMatches")).orElse(new JsonArray());
                if (!nameMatches.isEmpty()) {
                    String firstName = nameMatches.tryGetJsonObject(0).tryQueryString("givenNames[0]/name");
                    String firstNameType = nameMatches.tryGetJsonObject(0).tryQueryString("givenNames[0]/nameType");
                    if (firstNameType != null && firstNameType.equals("INITIAL")) {
                        firstName = null;
                    }
                    String lastName = nameMatches.tryGetJsonObject(0).tryQueryString("surnames[0]/name");
                    fillMissingNames(personProfile, firstName, lastName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return personProfile;
    }

    /**
     * Fill the first and last name only where the profile has none, so a part found before is never overwritten or
     * cleared. A last name that repeats the first name (a single token read both ways) is ignored.
     */
    static void fillMissingNames(PersonProfile profile, String firstName, String lastName) {
        if (StringHelper.nullOrEmpty(profile.getFirstName()) && !StringHelper.nullOrEmpty(firstName)) {
            profile.setFirstName(firstName);
        }
        if (StringHelper.nullOrEmpty(profile.getLastName()) && !StringHelper.nullOrEmpty(lastName) && !lastName.equalsIgnoreCase(profile.getFirstName())) {
            profile.setLastName(lastName);
        }
    }

    /** Gravatar's hash of an address: MD5 of the trimmed, lower-cased address. */
    static String gravatarHash(String emailAddress) {
        return DigestUtils.md5Hex(emailAddress.trim().toLowerCase()).toLowerCase();
    }

    static String gravatarAvatarUrl(String md5) {
        return "https://0.gravatar.com/avatar/" + md5;
    }

    /** Whether Gravatar has an avatar for this hash: with d=404 it answers 404 instead of a default image. */
    private static boolean hasGravatarAvatar(HttpRetriever httpRetriever, String md5) {
        try {
            HttpResult result = httpRetriever.httpHead(gravatarAvatarUrl(md5) + "?d=404&s=1");
            return result.getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static HttpRetriever createQuickHttpRetriever() {
        HttpRetriever quickHttpRetriever = HttpRetrieverFactory.getHttpRetriever();
        quickHttpRetriever.setConnectionTimeout(QUICK_TIMEOUT_MS);
        quickHttpRetriever.setSocketTimeout(QUICK_TIMEOUT_MS);
        quickHttpRetriever.setConnectionTimeoutRedirects(QUICK_TIMEOUT_MS);
        quickHttpRetriever.setSocketTimeoutRedirects(QUICK_TIMEOUT_MS);
        return quickHttpRetriever;
    }

    /**
     * Verify an email.
     * http://docs.quickemailverification.com/email-verification-api/verify-an-email-address
     *
     * @param email  The email to verify.
     * @param apiKey The api key for quickemailverification.com.
     */
    public static EmailVerificationResult verify(String email, String apiKey) {
        if (email.isEmpty()) {
            return null;
        }
        JsonObject response = new DocumentRetriever().tryGetJsonObject(
                "http://api.quickemailverification.com/v1/verify?email=" + UrlHelper.encodeParameter(email) + "&apikey=" + apiKey);
        if (response == null || response.tryGetString("success").equals("false")) {
            return null;
        }

        return new EmailVerificationResult(response);
    }

    public static void main(String[] args) {
        EmailVerificationResult verify = EmailAnalyzer.verify("email", "apiKey");
        System.out.println(verify);
    }
}
