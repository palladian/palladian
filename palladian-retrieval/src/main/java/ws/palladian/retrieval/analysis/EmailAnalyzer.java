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
     * by parsing the address with NameAPI (when an API key is set): the first given name is the first name, further given
     * names are the middle name, and initials (j.smith) are not used as a name. The image is the Gravatar avatar of that
     * exact address, or null when it has none. The username is the Gravatar username, or null. Every lookup uses a
     * {@value #QUICK_TIMEOUT_MS} ms timeout.
     */
    public PersonProfile getProfile(String emailAddress) {
        PersonProfile personProfile = new PersonProfile();
        emailAddress = emailAddress.trim().toLowerCase();
        personProfile.setEmail(emailAddress);

        String md5 = gravatarHash(emailAddress);
        String profileUrl = "https://www.gravatar.com/" + md5 + ".json";
        JsonObject gravatarResponse = null;
        HttpRetriever quickHttpRetriever = createQuickHttpRetriever();

        try {
            gravatarResponse = new DocumentRetriever(quickHttpRetriever).getJsonObject(profileUrl);
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
                    JsonObject bestMatch = nameMatches.tryGetJsonObject(0);
                    String[] givenNames = splitGivenNames(bestMatch.tryGetJsonArray("givenNames"));
                    String lastName = bestMatch.tryQueryString("surnames[0]/name");
                    fillMissingNames(personProfile, givenNames[0], givenNames[1], lastName);
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
        fillMissingNames(profile, firstName, null, lastName);
    }

    /** Like {@link #fillMissingNames(PersonProfile, String, String)}, plus a middle name that repeats neither of the others. */
    static void fillMissingNames(PersonProfile profile, String firstName, String middleName, String lastName) {
        if (StringHelper.nullOrEmpty(profile.getFirstName()) && !StringHelper.nullOrEmpty(firstName)) {
            profile.setFirstName(firstName);
        }
        if (StringHelper.nullOrEmpty(profile.getLastName()) && !StringHelper.nullOrEmpty(lastName) && !lastName.equalsIgnoreCase(profile.getFirstName())) {
            profile.setLastName(lastName);
        }
        if (StringHelper.nullOrEmpty(profile.getMiddleName()) && !StringHelper.nullOrEmpty(middleName) && !middleName.equalsIgnoreCase(profile.getFirstName())
                && !middleName.equalsIgnoreCase(profile.getLastName())) {
            profile.setMiddleName(middleName);
        }
    }

    /**
     * NameAPI's given names, initials left out: the first one is the first name, the rest (joined with spaces) the
     * middle name. Either is null when there is none.
     */
    static String[] splitGivenNames(JsonArray givenNames) {
        String firstName = null;
        StringBuilder middleName = new StringBuilder();
        if (givenNames != null) {
            for (int i = 0; i < givenNames.size(); i++) {
                JsonObject givenName = givenNames.tryGetJsonObject(i);
                String name = givenName == null ? null : givenName.tryGetString("name");
                if (StringHelper.nullOrEmpty(name) || "INITIAL".equals(givenName.tryGetString("nameType"))) {
                    continue;
                }
                if (firstName == null) {
                    firstName = name.trim();
                } else {
                    middleName.append(middleName.length() == 0 ? "" : " ").append(name.trim());
                }
            }
        }
        return new String[]{firstName, middleName.length() == 0 ? null : middleName.toString()};
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
        return verify(email, apiKey, new DocumentRetriever());
    }

    /**
     * Verify an email, giving up after {@code timeoutMs} for connecting and for waiting on the answer (a slow mail server
     * makes QuickEmailVerification slow).
     *
     * @return the verification, or null when it failed or timed out.
     */
    public static EmailVerificationResult verify(String email, String apiKey, int timeoutMs) {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        httpRetriever.setConnectionTimeout(timeoutMs);
        httpRetriever.setSocketTimeout(timeoutMs);
        httpRetriever.setConnectionTimeoutRedirects(timeoutMs);
        httpRetriever.setSocketTimeoutRedirects(timeoutMs);
        return verify(email, apiKey, new DocumentRetriever(httpRetriever));
    }

    private static EmailVerificationResult verify(String email, String apiKey, DocumentRetriever documentRetriever) {
        if (email.isEmpty()) {
            return null;
        }
        JsonObject response = documentRetriever.tryGetJsonObject(
                "http://api.quickemailverification.com/v1/verify?email=" + UrlHelper.encodeParameter(email) + "&apikey=" + apiKey);
        if (response == null || "false".equals(response.tryGetString("success")) || response.tryGetString("result") == null) {
            return null;
        }

        return new EmailVerificationResult(response);
    }

    public static void main(String[] args) {
        EmailVerificationResult verify = EmailAnalyzer.verify("email", "apiKey");
        System.out.println(verify);
    }
}
