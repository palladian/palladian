package ws.palladian.retrieval.apiwrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;

public class WSW {

    private Document document = null;

    public WSW(String wswPath) {
        parseWSW(wswPath);
    }

    public ArrayList<Integer> getWebServiceIDs(int profileID) {
        ArrayList<Integer> wsids = new ArrayList<Integer>();

        List<Node> ws = XPathHelper.getNodes(document.getLastChild(), ".//profiles/profile[@id=" + profileID
                + "]/webservices/ws");

        for (Node wsn : ws) {
            try {
                wsids.add(Integer.valueOf(wsn.getAttributes().getNamedItem("id").getTextContent()));
            } catch (NumberFormatException e) {
            }
        }

        return wsids;
    }

    public String createQueryURL(int webServiceID, HashSet<ParameterBinding> parameterBindings) {

        String queryURL = "";

        Node ws = XPathHelper.getNode(document, ".//webservices/webservice[@id=" + webServiceID + "]");

        queryURL += XPathHelper.getNode(ws, "endpoint").getTextContent();

        Node rp = XPathHelper.getNode(ws, "./requestParameters");
        for (ParameterBinding pb : parameterBindings) {
            Node pn = XPathHelper.getNode(rp, "./parameter[@term='" + pb.term + "']");
            if (pn != null) {
                queryURL += "&" + pn.getTextContent() + "=" + pb.value;
            }
        }

        return queryURL;
    }

    private void parseWSW(String wswPath) {

        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(wswPath);
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // public void call(String wswPath, int profileID, HashSet<ParameterBinding>
    // parameterBindings) {

    public OutputObject callProfile(int profileID, HashSet<ParameterBinding> parameterBindings) throws JSONException {

        OutputObject oo = new OutputObject();

        ArrayList<Integer> wsids = getWebServiceIDs(profileID);

        for (Integer wsid : wsids) {
            OutputObject ob = callWebService(wsid, parameterBindings);
            oo.parameterBindings.addAll(ob.parameterBindings);
        }

        return oo;
    }

    public OutputObject callWebService(int webServiceID, HashSet<ParameterBinding> parameterBindings) throws JSONException {

        OutputObject oo = new OutputObject();

        String url = createQueryURL(webServiceID, parameterBindings);

        DocumentRetriever c = new DocumentRetriever();
        JSONObject jsonOBJ = c.getJSONObject(url);

        HashSet<ParameterBinding> outputParameterBindings = new HashSet<ParameterBinding>();

        List<Node> rps = XPathHelper.getNodes(document, ".//webservices/webservice[@id=" + webServiceID
                + "]/responseParameters/parameter");

        for (Node rn : rps) {
            String term = rn.getAttributes().getNamedItem("term").getTextContent();
            String[] types = rn.getAttributes().getNamedItem("type").getTextContent().split("\\.");
            String[] parameter = rn.getTextContent().split("\\.");
            Object lastObject = jsonOBJ;
            String lastType = "o";
            String value = "";
            for (int i = 0; i < types.length; i++) {

                if (types[i].equalsIgnoreCase("o")) {

                    if (lastType.equalsIgnoreCase("o")) {
                        lastObject = ((JSONObject) lastObject).getJSONObject(parameter[i]);
                    } else {
                        // lastObject =
                        // ((JSONArray)lastObject).getJSONObject(1);
                    }

                    lastType = "o";

                } else if (types[i].equalsIgnoreCase("a")) {

                    lastObject = ((JSONObject) lastObject).getJSONArray(parameter[i]);
                    lastType = "a";

                } else if (types[i].equalsIgnoreCase("v")) {

                    if (lastType.equalsIgnoreCase("a")) {
                        JSONArray ja = ((JSONArray) lastObject);
                        for (int j = 0; j < ja.length(); j++) {
                            value = ja.getJSONObject(j).getString(parameter[i]);
                            ParameterBinding opb = new ParameterBinding(term, value);
                            outputParameterBindings.add(opb);
                        }
                    }

                    lastType = "v";
                }

            }

        }

        oo.parameterBindings = outputParameterBindings;

        return oo;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            WSW wsw = new WSW("config/wsw.xml");
            HashSet<ParameterBinding> parameterBindings = new HashSet<ParameterBinding>();
            parameterBindings.add(new ParameterBinding("query", "John Hiatt"));
            OutputObject oo = wsw.callProfile(1, parameterBindings);

            CollectionHelper.print(oo.parameterBindings);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}