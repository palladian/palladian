package ws.palladian.helper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.*;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Send mail via SMTP server.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class Mailer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Mailer.class);

    private final Properties properties;

    private final Authenticator authenticator;

    /**
     * <p>
     * Create a new SendMail. The {@link Properties} configuration should usually provide the following information:
     * <ul>
     * <li>mail.smtp.host</li>
     * <li>mail.smtp.socketFactory.port</li>
     * <li>mail.smtp.socketFactory.class</li>
     * <li>mail.smtp.auth</li>
     * <li>mail.smtp.port</li>
     * </ul>
     * </p>
     * 
     * @param properties {@link Properties} object supplying the configuration, not <code>null</code>.
     * @param username The username, not <code>null</code> or empty.
     * @param password The password, not <code>null</code> or empty.
     */
    public Mailer(Properties properties, final String username, final String password) {
        Validate.notNull(properties, "properties must not be null");
        Validate.notEmpty(username, "username must not be empty");
        Validate.notEmpty(password, "password must not be empty");

        this.properties = properties;
        authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
    }

    public boolean sendMail(String senderAddress, String senderName, Map<RecipientType, List<String>> recipients, String subject, String content, Address replyTo) {
        ArrayList<Address> replyToAddresses = new ArrayList<>();
        replyToAddresses.add(replyTo);
        return sendMail(senderAddress, senderName, recipients, subject, content, true, replyToAddresses, new ArrayList<File>());
    }

    public boolean sendMail(String senderAddress, String senderName, Map<RecipientType, List<String>> recipients, String subject, String content) {
        return sendMail(senderAddress, senderName, recipients, subject, content, true, new ArrayList<Address>(), new ArrayList<File>());
    }

    /**
     * <p>
     * Send a new (html) mail to multiple recipients.
     * </p>
     * 
     * @param senderAddress The address of the sender, not <code>null</code> or empty.
     * @param senderName The name of the sender, not <code>null</code> or empty.
     * @param recipients The list of recipients, not <code>null</code> or empty.
     * @param subject The subject of the mail, not <code>null</code>.
     * @param content The (html) content of the mail, not <code>null</code>.
     * @param isHtml If true, the content will be sent as html.
     * @param replyToAddresses Addresses to reply to.
     * @return <code>true</code>, if mail was sent successfully, <code>false</code> otherwise.
     */
    public boolean sendMail(String senderAddress, String senderName, Map<RecipientType, List<String>> recipients, String subject, String content, boolean isHtml,
            List<Address> replyToAddresses, List<File> attachments) {
        Validate.notEmpty(senderAddress, "sender must not be empty");
        Validate.notEmpty(senderName, "sender must not be empty");
        Validate.notEmpty(recipients, "recipients must not be empty");
        Validate.notNull(subject, "subject must not be null");
        Validate.notNull(content, "content must not be null");

        boolean success = false;

        Session mailSession = Session.getDefaultInstance(properties, authenticator);
        Message simpleMessage = new MimeMessage(mailSession);

        try {

            simpleMessage.setFrom(new InternetAddress(senderAddress, senderName));
            for (Entry<RecipientType, List<String>> recipient : recipients.entrySet()) {
                for (String address : recipient.getValue()) {
                    simpleMessage.addRecipient(recipient.getKey(), new InternetAddress(address));
                }
            }
            simpleMessage.setSubject(subject);
            if (!replyToAddresses.isEmpty()) {
                simpleMessage.setReplyTo(replyToAddresses.toArray(new Address[replyToAddresses.size()]));
            }

            if (attachments.isEmpty()) {
                if (isHtml) {
                    simpleMessage.setContent(content, "text/html");
                } else {
                    simpleMessage.setText(content);
                }
            } else {
                // create the message part
                BodyPart messageBodyPart = new MimeBodyPart();

                // Now set the actual message
                if (isHtml) {
                    messageBodyPart.setContent(content, "text/html");
                } else {
                    messageBodyPart.setText(content);
                }

                Multipart multipart = new MimeMultipart();

                // set text message part
                multipart.addBodyPart(messageBodyPart);

                // add attachments
                for (File attachment : attachments) {
                    messageBodyPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(attachment);
                    messageBodyPart.setDataHandler(new DataHandler(source));
                    messageBodyPart.setFileName(attachment.getName());
                    multipart.addBodyPart(messageBodyPart);
                }

                // set the complete message parts
                simpleMessage.setContent(multipart);
            }

            Transport.send(simpleMessage);

            LOGGER.debug("Successfully sent mail to {} ", recipients);
            success = true;

        } catch (MessagingException | UnsupportedEncodingException e) {
            LOGGER.error("Exception while sending: {}", e);
        }

        return success;
    }

    /**
     * <p>
     * Send a new plain text mail to multiple recipients.
     * </p>
     * 
     * @param senderEmail The sender email address, not <code>null</code> or empty.
     * @param recipients The list of recipients, not <code>null</code> or empty.
     * @param subject The subject of the mail, not <code>null</code>.
     * @param text The text content of the mail, not <code>null</code>.
     * @return <code>true</code>, if mail was sent successfully, <code>false</code> otherwise.
     */
    public boolean sendMail(String senderEmail, List<String> recipients, String subject, String text) {
        Map<RecipientType, List<String>> recipientMap = new HashMap<>();
        recipientMap.put(RecipientType.TO, recipients);
        return sendMail(senderEmail, senderEmail, recipientMap, subject, text, false, new ArrayList<Address>(), new ArrayList<File>());
    }

    /**
     * <p>
     * Send a new mail to multiple recipients.
     * </p>
     * 
     * @param sender The sender, not <code>null</code> or empty.
     * @param recipient The recipient, not <code>null</code> or empty.
     * @param subject The subject of the mail, not <code>null</code>.
     * @param text The text content of the mail, not <code>null</code>.
     * @return <code>true</code>, if mail was sent successfully, <code>false</code> otherwise.
     */
    public boolean sendMail(String sender, String recipient, String subject, String text) {
        Validate.notEmpty(sender, "sender must not be empty");
        Validate.notEmpty(recipient, "recipient must not be empty");
        Validate.notNull(subject, "subject must not be null");
        Validate.notNull(text, "text must not be null");

        return sendMail(sender, Arrays.asList(recipient), subject, text);
    }

}
