package ws.palladian.retrieval.search;

import ws.palladian.helper.UrlHelper;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * Query the SPARQL DBpedia API.
 *
 * @author David Urbansky
 * @since 19-Feb-22 at 22:28
 **/
public class DbPediaApi {
    public static JsonObject query(String query) {
        String url = "https://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=" + UrlHelper.encodeParameter(query)
                + "&format=application%2Fsparql-results%2Bjson&timeout=90000&signal_void=on&signal_unconnected=on";
        JsonObject jso = new DocumentRetriever().tryGetJsonObject(url);
        return jso;
    }

    public static void main(String[] args) {
        String query =
                "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + "PREFIX  dbo: <http://dbpedia.org/ontology/>\n" + "PREFIX  dbp: <http://dbpedia.org/property/>\n"
                        + "\n" + "SELECT ?resource ?name ?sa\n" + "WHERE {\n" + "  ?resource  rdf:type  dbo:Person;\n" + "             dbp:name ?name; \n" + "owl:sameAs ?sa.\n"
                        + "  FILTER (lang(?name) = 'en')\n" + "}\n" + "ORDER BY ASC(?name)\n" + "LIMIT 10000 OFFSET 0";
        JsonObject response = DbPediaApi.query(query);
        System.out.println(response.toString(2));
    }
}
