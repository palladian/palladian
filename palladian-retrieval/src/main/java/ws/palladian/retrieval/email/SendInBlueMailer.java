package ws.palladian.retrieval.email;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sendinblue.ApiClient;
import sendinblue.ApiException;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.*;
import ws.palladian.helper.collection.CollectionHelper;

import javax.mail.Message;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Sending emails via SenInBlue.
 * </p>
 *
 * Created 26.10.2022
 * See https://developers.sendinblue.com/docs
 *
 * @author David Urbansky
 */
public class SendInBlueMailer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendInBlueMailer.class);

    public SendInBlueMailer(String apiKey) {
        ApiClient apiClient = Configuration.getDefaultApiClient();

        // Configure API key authorization: api-key
        ApiKeyAuth apiKeyApi = (ApiKeyAuth) apiClient.getAuthentication("api-key");
        apiKeyApi.setApiKey(apiKey);
    }

    public Map<Message.RecipientType, List<String>> buildRecipientMap(String toEmail) {
        List<String> toEmails = new ArrayList<>();
        toEmails.add(toEmail);
        Map<Message.RecipientType, List<String>> recipientMap = new HashMap<>();
        recipientMap.put(Message.RecipientType.TO, toEmails);

        return recipientMap;
    }

    public boolean sendMail(String fromEmail, String fromName, String toEmail, String subject, String textMessage, String mailBody) {
        return sendMail(fromEmail, fromName, buildRecipientMap(toEmail), subject, textMessage, mailBody, new ArrayList<>(), null);
    }

    public boolean sendMail(String fromEmail, String fromName, String toEmail, String subject, String textMessage, String mailBody, Properties params) {
        return sendMail(fromEmail, fromName, buildRecipientMap(toEmail), subject, textMessage, mailBody, new ArrayList<>(), params);
    }

    public boolean sendMail(String fromEmail, String fromName, String toEmail, String subject, String textMessage, String htmlMessage, List<File> attachmentFiles,
            Properties params) {
        return sendMail(fromEmail, fromName, buildRecipientMap(toEmail), subject, textMessage, htmlMessage, attachmentFiles, params);
    }

    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String textMessage, String htmlMessage,
            Properties params) {
        return sendMail(fromEmail, fromName, recipientMap, subject, textMessage, htmlMessage, new ArrayList<>(), null, params);
    }

    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String htmlMessage, Properties params) {
        return sendMail(fromEmail, fromName, recipientMap, subject, null, htmlMessage, new ArrayList<>(), null, params);
    }

    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String textMessage, String htmlMessage,
            List<File> attachmentFiles, Properties params) {
        return sendMail(fromEmail, fromName, recipientMap, subject, textMessage, htmlMessage, attachmentFiles, null, params);
    }

    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String textMessage, String htmlMessage,
            String replyTo, Properties params) {
        return sendMail(fromEmail, fromName, recipientMap, subject, textMessage, htmlMessage, new ArrayList<>(), replyTo, params);
    }

    // see https://developers.sendinblue.com/reference/sendtransacemail
    public boolean sendMail(String fromEmail, String fromName, Map<Message.RecipientType, List<String>> recipientMap, String subject, String textMessage, String htmlMessage,
            List<File> attachmentFiles, String replyTo, Properties params) {
        List<String> toEmails = Optional.ofNullable(recipientMap.get(Message.RecipientType.TO)).orElse(new ArrayList<>());

        toEmails = toEmails.stream().filter(tm -> !tm.equalsIgnoreCase("undefined") && tm.contains("@")).collect(Collectors.toList());

        if (toEmails.isEmpty()) {
            return false;
        }

        // emails must be unique across to, cc, and bcc otherwise sendgrid throws an error
        Set<String> uniqueEmails = new HashSet<>();

        try {
            TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
            SendSmtpEmail mail = new SendSmtpEmail();
            mail.setSubject(subject);

            SendSmtpEmailSender sendSmtpEmailSender = new SendSmtpEmailSender();
            sendSmtpEmailSender.setEmail(fromEmail);
            sendSmtpEmailSender.setName(fromName);
            mail.setSender(sendSmtpEmailSender);

            List<SendSmtpEmailTo> toList = new ArrayList<>();
            for (String toEmail : toEmails) {
                if (!uniqueEmails.add(toEmail)) {
                    continue;
                }

                SendSmtpEmailTo to = new SendSmtpEmailTo();
                to.setEmail(toEmail);
                toList.add(to);
            }
            mail.setTo(toList);

            // cc
            List<SendSmtpEmailCc> ccList = new ArrayList<>();
            for (String ccEmail : Optional.ofNullable(recipientMap.get(Message.RecipientType.CC)).orElse(new ArrayList<>())) {
                if (!uniqueEmails.add(ccEmail)) {
                    continue;
                }

                SendSmtpEmailCc to = new SendSmtpEmailCc();
                to.setEmail(ccEmail);
                ccList.add(to);
            }
            if (!ccList.isEmpty()) {
                mail.setCc(ccList);
            }

            // bcc
            List<SendSmtpEmailBcc> bccList = new ArrayList<>();
            for (String bccEmail : Optional.ofNullable(recipientMap.get(Message.RecipientType.BCC)).orElse(new ArrayList<>())) {
                if (!uniqueEmails.add(bccEmail)) {
                    continue;
                }
                SendSmtpEmailBcc to = new SendSmtpEmailBcc();
                to.setEmail(bccEmail);
                bccList.add(to);
            }
            if (!bccList.isEmpty()) {
                mail.setBcc(bccList);
            }

            SendSmtpEmailReplyTo sendSmtpEmailReplyTo = new SendSmtpEmailReplyTo();
            if (replyTo == null) {
                replyTo = fromEmail;
                sendSmtpEmailReplyTo.setName(fromName);
            }
            sendSmtpEmailReplyTo.setEmail(replyTo);
            mail.setReplyTo(sendSmtpEmailReplyTo);

            if (textMessage != null) {
                mail.setTextContent(textMessage);
            }
            if (htmlMessage != null) {
                mail.setHtmlContent(htmlMessage);
            }

            for (File attachment : attachmentFiles) {
                SendSmtpEmailAttachment attachmentItem = new SendSmtpEmailAttachment();

                attachmentItem.setContent(IOUtils.toByteArray(new FileInputStream(attachment)));
                attachmentItem.setName(attachment.getName());
                mail.addAttachmentItem(attachmentItem);
            }

            if (params != null) {
                mail.setParams(params);
            }

            try {
                apiInstance.sendTransacEmail(mail);
            } catch (ApiException e) {
                e.printStackTrace();
                LOGGER.error("sendinblue could not send mail: " + e.getMessage(), ", to Emails: " + CollectionHelper.joinReadable(toEmails));
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("sendinblue could not send mail: " + ex.getMessage(), ", to Emails: " + CollectionHelper.joinReadable(toEmails));
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        Properties params = new Properties();
        params.setProperty("name", "Sam");
        params.setProperty("greet_word", "Hey ho");
        SendInBlueMailer mailer = new SendInBlueMailer("YOUR_API_KEY");
        mailer.sendMail("mail@palladian.ai", "Palladian", "Sam@gmail.com", "Test Email", "This is a test message.",
                "{{params.greet_word}} {{params.name}}, is a <b>test message</b>", params);
    }
}
