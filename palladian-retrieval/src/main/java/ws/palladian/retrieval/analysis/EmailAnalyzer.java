package ws.palladian.retrieval.analysis;

import org.apache.commons.codec.digest.DigestUtils;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import java.util.Optional;

public class EmailAnalyzer {
    public PersonProfile getProfile(String emailAddress) {
        PersonProfile personProfile = new PersonProfile();
        emailAddress = emailAddress.trim().toLowerCase();
        personProfile.setEmail(emailAddress);

        String md5 = DigestUtils.md5Hex(emailAddress).toLowerCase();
        String profileUrl = "https://www.gravatar.com/" + md5 + ".json";
        JsonObject gravatarResponse = null;

        try {
            gravatarResponse = new DocumentRetriever().getJsonObject(profileUrl);
        } catch (Exception e) {
            // ccl
        }

        if (gravatarResponse != null) {
            JsonObject firstEntry = gravatarResponse.tryQueryJsonObject("entry[0]");

            personProfile.setUsername(firstEntry.tryQueryString("preferredUsername"));

            String firstName = firstEntry.tryQueryString("name/givenName");
            if (firstName != null) {
                personProfile.setFirstName(firstName);
            }
            String lastName = firstEntry.tryQueryString("name/familyName");
            if (lastName != null) {
                personProfile.setLastName(lastName);
            }
            String formattedName = firstEntry.tryQueryString("name/formatted");
            if (formattedName != null) {
                personProfile.setFullName(formattedName);
            }

            String thumbnailUrl = firstEntry.tryQueryString("thumbnailUrl");
            personProfile.setImageUrl(thumbnailUrl);

            // System.out.println(gravatarResponse.toString(2));
        } else {
            try {
                // must be fast or forget about it
                HttpRetriever quickHttpRetriever = HttpRetrieverFactory.getHttpRetriever();
                quickHttpRetriever.setConnectionTimeout(500);
                quickHttpRetriever.setSocketTimeout(500);
                quickHttpRetriever.setConnectionTimeoutRedirects(500);
                quickHttpRetriever.setSocketTimeoutRedirects(500);
                DocumentRetriever quickRetriever = new DocumentRetriever(quickHttpRetriever);

                JsonObject parsedEmailJson = quickRetriever.getJsonObject(
                        "http://api.nameapi.org/rest/v5.3/email/emailnameparser?apiKey=7aa3665e0091c46d78249c7ca9883cbf-user1&emailAddress=" + UrlHelper.encodeParameter(
                                emailAddress));

                JsonArray nameMatches = Optional.ofNullable(parsedEmailJson.tryGetJsonArray("nameMatches")).orElse(new JsonArray());
                if (!nameMatches.isEmpty()) {
                    String firstName = nameMatches.tryGetJsonObject(0).tryQueryString("givenNames[0]/name");
                    String firstNameType = nameMatches.tryGetJsonObject(0).tryQueryString("givenNames[0]/nameType");
                    if (firstNameType != null && firstNameType.equals("INITIAL")) {
                        firstName = null;
                    }
                    String lastName = nameMatches.tryGetJsonObject(0).tryQueryString("surnames[0]/name");
                    personProfile.setFirstName(firstName);
                    personProfile.setLastName(lastName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return personProfile;
    }

    /**
     * Verify an email.
     * http://docs.quickemailverification.com/email-verification-api/verify-an-email-address
     *
     * @param email  The email to verify.
     * @param apiKey The api key for quickemailverification.com.
     */
    public static EmailVerificationResult verify(String email, String apiKey) {
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
