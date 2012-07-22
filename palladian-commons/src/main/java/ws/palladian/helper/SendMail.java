package ws.palladian.helper;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

/**
 * <p>
 * Send mail via SMTP server.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public class SendMail {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SendMail.class);

    private String smtpHost;
    private int smtpPort;
    private String smtpUser;
    private Authenticator authenticator;

    private static final int DEFAULT_SMTP_PORT = 25;

    /**
     * <p>
     * Constructor specifying SMTP server and login data.
     * </p>
     * 
     * @param smtpHost
     * @param smtpPort
     * @param smtpUser
     * @param smtpPass
     */
    public SendMail(String smtpHost, int smtpPort, String smtpUser, String smtpPass) {
        super();
        // TODO check for validity
        init(smtpHost, smtpPort, smtpUser, smtpPass);
    }

    /**
     * <p>
     * Constructor that takes configuration from {@link Configuration}, it must supply the following parameters:
     * </p>
     * <ul>
     * <li>sendMail.smtpHost</li>
     * <li>sendMail.smtpUser</li>
     * <li>sendMail.smtpPass</li>
     * <li>sendMail.smtpPort (optional)</li>
     * </ul>
     * @param config The {@link Configuration} providing the specified parameters.
     */
    public SendMail(Configuration config) {
        String smtpHost = config.getString("sendMail.smtpHost");
        int smtpPort = config.getInt("sendMail.smtpPort", DEFAULT_SMTP_PORT);
        String smtpUser = config.getString("sendMail.smtpUser");
        String smtpPass = config.getString("sendMail.smtpPass");
        init(smtpHost, smtpPort, smtpUser, smtpPass);
    }

    private void init(String smtpHost, int smtpPort, final String smtpUser, final String smtpPass) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUser = smtpUser;
        authenticator = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPass);
            };
        };
        LOGGER.info("new SendMail " + smtpHost + ";" + smtpPort + ";" + smtpUser + ";" + smtpPass);
    }

    /**
     * <p>
     * Send a new mail to multiple recipients.
     * </p>
     * 
     * @param sender
     * @param recipients
     * @param subject
     * @param text
     * @return
     */
    public boolean send(String sender, List<String> recipients, String subject, String text) {

        Properties props = new Properties();
        props.put("mail.smtp.submitter", smtpUser);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));

        boolean success = false;

        Session mailSession = Session.getDefaultInstance(props, authenticator);
        Message simpleMessage = new MimeMessage(mailSession);

        try {

            simpleMessage.setFrom(new InternetAddress(sender));
            for (String recipient : recipients) {
                simpleMessage.addRecipient(RecipientType.TO, new InternetAddress(recipient));
            }
            simpleMessage.setSubject(subject);
            simpleMessage.setText(text);

            Transport.send(simpleMessage);

            LOGGER.info("successfully sent mail to " + recipients);
            success = true;

        } catch (AddressException e) {
            LOGGER.error(e);
        } catch (MessagingException e) {
            LOGGER.error(e);
        }

        return success;

    }

    /**
     * <p>
     * Send a new mail.
     * </p>
     * 
     * @param sender
     * @param recipient
     * @param subject
     * @param text
     * @return <code>true</code>, if mail was sent successfully.
     */
    public boolean send(String sender, String recipient, String subject, String text) {
        return send(sender, Arrays.asList(recipient), subject, text);
    }

//    /**
//     * <p>
//     * main method with command line interface.
//     * </p>
//     * 
//     * @param args
//     */
//    @SuppressWarnings("static-access")
//    public static void main(String[] args) {
//
//        CommandLineParser parser = new BasicParser();
//
//        Options options = new Options();
//        options.addOption(OptionBuilder.withLongOpt("smtpHost").hasArg().withType(String.class).isRequired().create());
//        options.addOption(OptionBuilder.withLongOpt("smtpPort").hasArg().withType(Number.class).isRequired().create());
//        options.addOption(OptionBuilder.withLongOpt("smtpUser").hasArg().withType(String.class).isRequired().create());
//        options.addOption(OptionBuilder.withLongOpt("smtpPass").hasArg().withType(String.class).isRequired().create());
//
//        options.addOption(OptionBuilder.withLongOpt("from").hasArg().withType(String.class).isRequired().create());
//        options.addOption(OptionBuilder.withLongOpt("to").hasArg().withType(String.class).isRequired().create());
//        options.addOption(OptionBuilder.withLongOpt("subject").hasArg().withType(String.class).isRequired().create());
//        options.addOption(OptionBuilder.withLongOpt("text").hasArg().withType(String.class).isRequired().create());
//
//        try {
//            CommandLine commandLine = parser.parse(options, args);
//
//            String smtpHost = commandLine.getOptionValue("smtpHost");
//            int smtpPort = ((Number) commandLine.getParsedOptionValue("smtpPort")).intValue();
//            String smtpUser = commandLine.getOptionValue("smtpUser");
//            String smtpPass = commandLine.getOptionValue("smtpPass");
//
//            String from = commandLine.getOptionValue("from");
//            String to = commandLine.getOptionValue("to");
//            String subject = commandLine.getOptionValue("subject");
//            String text = commandLine.getOptionValue("text");
//
//            SendMail sendMail = new SendMail(smtpHost, smtpPort, smtpUser, smtpPass);
//            sendMail.send(from, to, subject, text);
//        } catch (ParseException e) {
//            new HelpFormatter().printHelp(SendMail.class.getName(), options);
//        }
//
//    }

}
