package ws.palladian.retrieval.ip;

/**
 * Information lookup for a given IP address; mainly focused on geo data.
 * 
 * @author pk
 */
public interface IpLookup {
	/**
	 * Look up the given IP address (or hostname).
	 * 
	 * @param ip
	 *            The IP address.
	 * @return The result, or <code>null</code> in case the given IP or hostname
	 *         could not be found.
	 * @throws IpLookupException
	 *             In case of a failure.
	 */
	IpLookupResult lookup(String ip) throws IpLookupException;
}
