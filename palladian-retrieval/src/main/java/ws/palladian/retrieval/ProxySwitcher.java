package ws.palladian.retrieval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * <p>
 * This class allows to switch through a list of different proxies when using the {@link DocumentRetriever}. This makes
 * it possible to harvest pages, which do not want to be harvested :) . A list of proxies can be found in the link
 * below.
 * </p>
 * 
 * @see <a href="http://www.proxy-list.org/en/index.php">Proxy List</a>
 * @author David Urbansky
 * @author Philipp Katz
 */
public class ProxySwitcher implements HttpHook {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ProxySwitcher.class);

    /** The URL to use for testing the proxies. */
    private static final String CHECK_URL = "http://www.sourceforge.com";

    /** List of proxies to choose from. */
    private List<String> proxies;

    /** Number of request before switching to another proxy, -1 means never switch. */
    private int switchRequests;

    /** Number of requests sent with currently used proxy. */
    private int proxyRequests;

    /** Index of the currently selected proxy. */
    private int proxyIndex;

    /** The DocumentRetriever which is only used for checking the proxies. */
    private HttpRetriever testRetriever;

    /**
     * <p>
     * Create a new {@link ProxySwitcher} with the supplied proxy {@link Collection}, switching at the specified number
     * of requests.
     * </p>
     * 
     * @param proxies The proxies to use.
     * @param switchRequests The number of requests after the switch is performed.
     */
    public ProxySwitcher(Collection<String> proxies, int switchRequests) {
        setup(proxies, switchRequests);
    }

    private void setup(Collection<String> proxies, int switchRequests) {
        this.proxies = new ArrayList<String>(proxies);
        this.switchRequests = switchRequests;
        this.proxyRequests = 0;
        this.proxyIndex = 0;

        this.testRetriever = new HttpRetriever();

        // use low timeouts, since we do not want slow proxies
        testRetriever.setConnectionTimeout(3000);
        testRetriever.setSocketTimeout(3000);
    }

    /**
     * <p>
     * Get the next proxy by cycling through the list.
     * </p>
     * 
     * @return
     */
    private void nextProxy() {
        proxyIndex = ++proxyIndex % proxies.size();
    }

    /**
     * <p>
     * Get the currently active proxy.
     * </p>
     * 
     * @return
     */
    private String getCurrentProxy() {
        return proxies.get(proxyIndex);
    }

    /**
     * <p>
     * Check, whether the supplied proxy is working by performing a test request.
     * </p>
     * 
     * @param proxy
     * @return
     */
    private boolean checkProxy(String proxy) {
        boolean success = false;

        try {
            testRetriever.setProxy(proxy);
            HttpResult result = testRetriever.httpGet(CHECK_URL);
            if (result.getContent().length > 0) {
                success = true;
            }
        } catch (HttpException e) {
            // yes, we want to ignore this!
        }

        LOGGER.debug("proxy " + proxy + " is working : " + success);
        return success;
    }

    @Override
    public void beforeRequest(String url, HttpRetriever retriever) throws HttpException {

        if (proxyRequests % switchRequests == 0) {

            boolean continueChecking = true;
            String proxy = null;
            int checks = 0;

            while (continueChecking) {
                nextProxy();
                proxy = getCurrentProxy();
                continueChecking = !checkProxy(proxy);
                if (++checks == proxies.size()) {
                    throw new HttpException("no (more) working proxies");
                }
            }

            retriever.setProxy(proxy);
            proxyRequests = 0;

        }
    }

    @Override
    public void afterRequest(HttpResult result, HttpRetriever documentRetriever) throws HttpException {
        proxyRequests++;
    }

    /**
     * <p>
     * Check all proxies which have been supplied and optionally remove those, which are not working.
     * </p>
     * 
     * @param iterations the number of iterations for checking.
     * @param removeNonWorking <code>true</code>, to remove those proxies which were not working from the list.
     */
    public void checkAllProxies(int iterations, boolean removeNonWorking) {

        Set<String> fails = new HashSet<String>();

        for (int i = 0; i < iterations; i++) {
            LOGGER.info("# iteration : " + i);
            for (int j = 0; j < proxies.size(); j++) {

                String proxy = getCurrentProxy();
                boolean working = checkProxy(proxy);
                if (!working) {
                    fails.add(proxy);
                }
                nextProxy();

            }
        }

        LOGGER.info("# proxies with failures " + fails.size());

        if (removeNonWorking) {
            proxies.removeAll(fails);
            LOGGER.info("removed non-working proxies; " + proxies.size() + " proxies left");
        }
    }

    public static void main(String[] args) {

        List<String> proxies = new ArrayList<String>();
        proxies.add("85.214.149.158:80");
        proxies.add("193.198.184.5:80");
        proxies.add("124.193.109.13:80");
        proxies.add("221.130.162.244:80");
        proxies.add("119.47.91.6:808");
        proxies.add("195.145.22.46:80");
        proxies.add("201.39.174.82:3128");
        proxies.add("67.184.161.238:8080");
        proxies.add("200.77.252.162:3128");
        proxies.add("201.219.17.23:3128");
        proxies.add("189.214.74.139:80");
        proxies.add("80.86.254.41:8080");
        proxies.add("91.121.218.169:8000");
        proxies.add("95.154.98.152:80");
        proxies.add("209.97.203.60:8080");
        proxies.add("220.118.19.148:8000");
        proxies.add("201.16.64.24:3128");
        proxies.add("200.119.12.198:3128");
        proxies.add("62.108.161.141:8080");

        // create a ProxySwitcher which changes the Proxy after every 3rd request
        ProxySwitcher proxySwitcher = new ProxySwitcher(proxies, 3);

        // check all proxies from the list by sending three requests,
        // remove proxies which are not working
        proxySwitcher.checkAllProxies(3, true);

        // use the instance of this DocumentRetriever with cycling proxies
        HttpRetriever retriever = new HttpRetriever();
        retriever.setHttpHook(proxySwitcher);

    }

}