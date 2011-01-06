package tud.iir.web.datasetcrawler.language;

import org.jivesoftware.smack.packet.IQ;

public class CollectaIQ extends IQ {

    private String jid;
    private String languageCode;

    public CollectaIQ(String jid, String languageCode) {
        this.jid = jid;
        this.languageCode = languageCode;
    }

    @Override
    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<pubsub xmlns=\'http://jabber.org/protocol/pubsub\'>" + "<subscribe jid=\'" + jid + "\'"
                + " node=\'search\'/>" + "<options>" + "<x xmlns=\'jabber:x:data\' type=\'submit\'>"
                + "<field var=\'FORM_TYPE\' type=\'hidden\'>"
                + "<value>http://jabber.org/protocol/pubsub#subscribe_options</value>" + "</field>"
                + "<field var=\'x-collecta#apikey\'>" + "<value>aef13a5bc659dc610cf6ad300da7994e</value>" + "</field>"
                + "<field var=\'x-collecta#query\'>" + "<value> language:" + this.languageCode + "</value>"
                + "</field>" + "</x>"
                + "</options>" + "</pubsub>");

        return buf.toString();
    }

}
