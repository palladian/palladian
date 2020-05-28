package ws.palladian.retrieval.email;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.*;

/**
 * Implementation of the Sendy API.
 *
 * @author David Urbansky
 * @see <a href="https://sendy.co/api">https://sendy.co/api</a>
 */
public class SendyMailer {
    private String apiLocation;
    private String apiKey;

    public SendyMailer(String apiLocation, String apiKey) {
        if (!apiLocation.endsWith("/")) {
            apiLocation += "/";
        }
        this.apiLocation = apiLocation;
        this.apiKey = apiKey;
    }

    public boolean trySubscribe(EmailContact emailContact) {
        try {
            return subscribe(emailContact);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean subscribe(EmailContact emailContact) throws HttpException {
        String apiUrl = apiLocation + "subscribe";

        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.POST, apiUrl);
        FormEncodedHttpEntity.Builder entityBuilder = new FormEncodedHttpEntity.Builder();
        entityBuilder.addData("api_key", apiKey);
        entityBuilder.addData("email", emailContact.getEmail());
        entityBuilder.addData("list", emailContact.getList());
        if (emailContact.getName() != null) {
            entityBuilder.addData("name", emailContact.getName());
        }
        if (emailContact.getCountryCode() != null) {
            entityBuilder.addData("country", emailContact.getCountryCode());
        }
        if (emailContact.getIpAddress() != null) {
            entityBuilder.addData("ipaddress", emailContact.getIpAddress());
        }
        if (emailContact.getReferrer() != null && UrlHelper.isValidUrl(emailContact.getReferrer())) {
            entityBuilder.addData("referrer", emailContact.getReferrer());
        }
        entityBuilder.addData("gdpr", String.valueOf(emailContact.isGdpr()));
        entityBuilder.addData("silent", String.valueOf(emailContact.isSilent()));
        entityBuilder.addData("boolean", "true");

        requestBuilder.setEntity(entityBuilder.create());

        HttpResult result = httpRetriever.execute(requestBuilder.create());
        String responseText = StringHelper.clean(result.getStringContent()).toLowerCase();
        // System.out.println("Result: " + responseText);
        if (result.getStatusCode() > 300 || !responseText.equals("1")) {
            throw new RuntimeException("Could not subscribe contact " + emailContact + ", responseText: " + responseText);
        }

        return true;
    }

    public boolean tryUnsubscribe(EmailContact emailContact) {
        try {
            return unsubscribe(emailContact);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean unsubscribe(EmailContact emailContact) throws HttpException {
        String apiUrl = apiLocation + "unsubscribe";
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.POST, apiUrl);
        FormEncodedHttpEntity.Builder entityBuilder = new FormEncodedHttpEntity.Builder();
        entityBuilder.addData("api_key", apiKey);
        entityBuilder.addData("email", emailContact.getEmail());
        entityBuilder.addData("list", emailContact.getList());
        entityBuilder.addData("boolean", "true");
        requestBuilder.setEntity(entityBuilder.create());
        HttpResult result = httpRetriever.execute(requestBuilder.create());
        String responseText = StringHelper.clean(result.getStringContent()).toLowerCase();
        // System.out.println("Result: " + responseText);
        if (result.getStatusCode() > 300 || !responseText.equals("1")) {
            throw new RuntimeException("Could not unsubscribe contact " + emailContact + ", responseText: " + responseText);
        }
        return true;
    }

    public int getSubscriberCount(String listId) {
        String apiUrl = apiLocation + "api/subscribers/active-subscriber-count.php";

        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.POST, apiUrl);
        FormEncodedHttpEntity.Builder entityBuilder = new FormEncodedHttpEntity.Builder();
        entityBuilder.addData("api_key", apiKey);
        entityBuilder.addData("list_id", listId);

        requestBuilder.setEntity(entityBuilder.create());

        try {
            HttpResult result = httpRetriever.execute(requestBuilder.create());
            String responseText = StringHelper.clean(result.getStringContent()).toLowerCase();
            // System.out.println("Result: " + responseText);
            try {
                return Integer.valueOf(responseText);
            } catch (Exception e) {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void main(String[] args) {
        SendyMailer sendyMailer = new SendyMailer("API_LOCATION", "API_KEY");
        EmailContact emailContact = new EmailContact("mail@palladian.ai");
        emailContact.setName("Me Mine");
        emailContact.setList("LIST_ID");
        emailContact.setReferrer("https://palladian.ai");
        sendyMailer.trySubscribe(emailContact);
        System.out.println(sendyMailer.getSubscriberCount("LIST_ID"));
    }

}
