package tud.iir.web.datasetcrawler.language;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;

import tud.iir.helper.StringHelper;

public class CollectaClient implements Runnable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CollectaClient.class);

    XMPPConnection connection;
    private String languageCode;
    public FileWriter fileWriter;

    public static void main(String args[]) {
        (new Thread(new CollectaClient("en", ""))).start();
    }

    public CollectaClient(String languageCode, String directoryPath) {
        this.languageCode = languageCode;
        try {
            fileWriter = new FileWriter(directoryPath + languageCode + "/messages.txt");
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void connect() {
        connection = new XMPPConnection("guest.collecta.com");
        try {
            connection.connect();
            connection.loginAnonymously();
        } catch (XMPPException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void run() {
        this.connect();
        this.initializeIQListener();
        while (connection.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    private Packet searchRequest() {
        CollectaIQ iqStanza = new CollectaIQ(connection.getUser(), languageCode);
        iqStanza.setType(Type.SET);
        iqStanza.setFrom(connection.getUser());
        iqStanza.setPacketID("subscribe1");
        iqStanza.setTo("search.collecta.com");
        return iqStanza;
    }

    private void initializeIQListener() {

        PacketListener myListener = new PacketListener() {

            @Override
            public void processPacket(Packet arg0) {
                // if (arg0 instanceof org.jivesoftware.smack.packet.IQ) {
                // org.jivesoftware.smack.packet.IQ iq = (org.jivesoftware.smack.packet.IQ) arg0;
                // System.out.println("IQ:"+iq.toXML());
                //
                // }
                if (arg0 instanceof org.jivesoftware.smack.packet.Message) {

                    org.jivesoftware.smack.packet.Message msg = (org.jivesoftware.smack.packet.Message) arg0;
                    String content = StringHelper.getSubstringBetween(msg.toXML(), "<content>", "</content>");
                    content = StringEscapeUtils.unescapeHtml(content);

                    LOGGER.info("\nMessage:" + content);
                    try {
                        fileWriter.write(content);
                        fileWriter.write("\n");
                        fileWriter.flush();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                    }

                }
            }
        };
        connection.addPacketListener(myListener, null);
        connection.sendPacket(this.searchRequest());
        connection.sendPacket(this.searchRequest());
    }
}
