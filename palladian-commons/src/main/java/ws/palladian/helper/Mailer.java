package ws.palladian.helper;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

/**
 * <p>
 * The mailer can be used to send emails.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class Mailer {

    /** The logger for this class. */
    public static final Logger LOGGER = Logger.getLogger(Mailer.class);

    private Session session;

    public Mailer() {

    }

    /**
     * <p>
     * Initialize a mailer with a properties object that has to hold the following information:
     * <ul>
     * <li>mail.smtp.host</li>
     * <li>mail.smtp.socketFactory.port</li>
     * <li>mail.smtp.socketFactory.class</li>
     * <li>mail.smtp.auth</li>
     * <li>mail.smtp.port</li>
     * </ul>
     * </p>
     * 
     * @param props The properties.
     */
    public Mailer(Properties props, final String username, final String password) {
        initialize(props, username, password);
    }

    protected void initialize(Properties props, final String username, final String password) {
        session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public void sendMail(String fromAddress, String toAddress, String subject, String messageString) {
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);
            message.setText(messageString);

            Transport.send(message);
        } catch (AddressException e) {
            LOGGER.error(e.getMessage());
        } catch (MessagingException e) {
            LOGGER.error(e.getMessage());
        }
    }

}