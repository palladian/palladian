package ws.palladian.retrieval.email;

import com.sendgrid.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonObject;

import javax.mail.Message;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * Sending emails via SendGrid.
 * </p>
 *
 * @created 24.01.2018
 * @author David Urbansky
 */
public class SendGridMailer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendGridMailer.class);

    private final Set<String> mailCategories = new HashSet<>();
    private String recipientName = null;

    // avoid abuse
    public static int maxEmailsPerDay = 2000;

    /**
     * The number of sent emails in a sliding 24 hour window, by timestamps.
     */
    private static final List<Long> sentEmails = new ArrayList<>();

    private final String apiKey;

    public SendGridMailer(String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<Message.RecipientType, List<String>> buildRecipientMap(String toEmail) {
        List<String> toEmails = new ArrayList<>();
        toEmails.add(toEmail);
        Map<Message.RecipientType, List<String>> recipientMap = new HashMap<>();
        recipientMap.put(Message.RecipientType.TO, toEmails);

        return recipientMap;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public boolean sendMail(String fromEmail, String fromName, String toEmail, String subject, String textMessage, String mailBody) {
        return sendMail(fromEmail, fromName, buildRecipientMap(toEmail), subject, textMessage, mailBody, new ArrayList<>());
    }

    public boolean sendMail(String fromEmail, String fromName, String toEmail, String subject, String textMessage, String htmlMessage, List<File> attachmentFiles) {
        return sendMail(fromEmail, fromName, buildRecipientMap(toEmail), subject, textMessage, htmlMessage, attachmentFiles);
    }

    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String textMessage, String htmlMessage) {
        return sendMail(fromEmail, fromName, recipientMap, subject, textMessage, htmlMessage, new ArrayList<>(), null);
    }

    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String textMessage, String htmlMessage,
                            List<File> attachmentFiles) {
        return sendMail(fromEmail, fromName, recipientMap, subject, textMessage, htmlMessage, attachmentFiles, null);
    }

    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String textMessage, String htmlMessage, String replyTo) {
        return sendMail(fromEmail, fromName, recipientMap, subject, textMessage, htmlMessage, new ArrayList<>(), replyTo);
    }

    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String textMessage, String htmlMessage,
                            List<File> attachmentFiles, String replyTo) {
        List<String> toEmails = Optional.ofNullable(recipientMap.get(Message.RecipientType.TO)).orElse(new ArrayList<>());

        toEmails = toEmails.stream().filter(tm -> !tm.equalsIgnoreCase("undefined") && tm.contains("@")).collect(Collectors.toList());

        if (toEmails.isEmpty()) {
            return false;
        }

        long yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        sentEmails.removeIf(ts -> ts < yesterday);
        if (sentEmails.size() > maxEmailsPerDay) {
            LOGGER.error("email could not be sent because quota limit of " + maxEmailsPerDay + " emails per day was reached");
            return false;
        }

        // emails must be unique across to, cc, and bcc otherwise sendgrid throws an error
        Set<String> uniqueEmails = new HashSet<>();

        try {
            Email from = new Email(fromEmail);
            from.setName(fromName);

            // // recipients
            // to
            Personalization personalization = new Personalization();
            for (String toEmail : toEmails) {
                if (!uniqueEmails.add(toEmail)) {
                    continue;
                }
                Email to = new Email(toEmail);
                if (recipientName != null) {
                    to.setName(recipientName);
                }
                personalization.addTo(to);
            }

            // cc
            for (String ccEmail : Optional.ofNullable(recipientMap.get(Message.RecipientType.CC)).orElse(new ArrayList<>())) {
                if (!uniqueEmails.add(ccEmail)) {
                    continue;
                }
                Email cc = new Email(ccEmail);
                personalization.addCc(cc);
            }

            // bcc
            for (String bccEmail : Optional.ofNullable(recipientMap.get(Message.RecipientType.BCC)).orElse(new ArrayList<>())) {
                if (!uniqueEmails.add(bccEmail)) {
                    continue;
                }
                Email bcc = new Email(bccEmail);
                personalization.addBcc(bcc);
            }

            com.sendgrid.Mail mail = new com.sendgrid.Mail();
            mail.setFrom(from);
            mail.setSubject(subject);

            if (replyTo != null) {
                mail.setReplyTo(new Email(replyTo));
            }

            mail.addPersonalization(personalization);
            if (textMessage != null) {
                mail.addContent(new Content("text/plain", textMessage));
            }
            if (htmlMessage != null) {
                mail.addContent(new Content("text/html", htmlMessage));
            }

            int c = 0;
            for (File attachment : attachmentFiles) {
                Attachments attachments = new Attachments();

                Base64 x = new Base64();
                String encodedString = x.encodeAsString(IOUtils.toByteArray(new FileInputStream(attachment)));
                attachments.setContent(encodedString);
                attachments.setType("application/pdf");
                attachments.setFilename(attachment.getName());
                attachments.setDisposition("attachment");
                attachments.setContentId("PDF File " + (c++));
                mail.addAttachments(attachments);
            }

            mailCategories.forEach(mail::addCategory);

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);

            // add email to sent, remove timestamps older than 24 hours from list
            sentEmails.add(System.currentTimeMillis());
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("send grid could not send mail: " + ex.getMessage(), ", to Emails: " + CollectionHelper.joinReadable(toEmails));
            return false;
        }

        return true;
    }

    /**
     * Add a contact to a
     * See https://sendgrid.com/docs/API_Reference/api_v3.html
     */
    public boolean addContact(String email, String firstName, String lastName, String... listId) {
        SendGrid sendGrid = new SendGrid(apiKey);
        Request request = new Request();

        JsonObject requestObject = new JsonObject();

        requestObject.put("list_ids", Arrays.asList(listId));
        JsonArray contacts = new JsonArray();
        JsonObject contact = new JsonObject();
        contact.put("email", email);
        if (firstName != null) {
            contact.put("first_name", firstName);
        }
        if (lastName != null) {
            contact.put("last_name", lastName);
        }
        contacts.add(contact);
        requestObject.put("contacts", contacts);

        request.setMethod(Method.PUT);
        request.setEndpoint("marketing/contacts");
        request.setBody(requestObject.toString());
        try {
            sendGrid.api(request);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public JsonObject getListDetails(String listId) {
        SendGrid sendGrid = new SendGrid(apiKey);
        Request request = new Request();

        request.setMethod(Method.GET);
        request.setEndpoint("marketing/lists/" + listId);
        try {
            Response api = sendGrid.api(request);
            return new JsonObject(api.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return new JsonObject();
        }
    }

    public boolean addCategory(String category) {
        return this.mailCategories.add(category);
    }

}
