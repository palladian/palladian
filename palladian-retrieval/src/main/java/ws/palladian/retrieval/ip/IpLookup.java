package ws.palladian.retrieval.ip;

/**
 * Information lookup for a given IP address; mainly focused on geo data.
 * 
 * @author pk
 */
public interface IpLookup {
	IpLookupResult lookup(String ip) throws IpLookupException;
}
