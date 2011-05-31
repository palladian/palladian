package ws.palladian.retrieval;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.FileHelper;

/**
 * This is just out-sourced code from the {@link DocumentRetriever} and not yet working!
 * 
 * @author Philipp Katz
 *
 */
public class ProxySwitcher {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ProxySwitcher.class);
    
    // /////////////////////////////////////////////////////////
    // /////////////////// proxy settings /////////////////////
    // /////////////////////////////////////////////////////////


    /** Number of request before switching to another proxy, -1 means never switch. */
    private int switchProxyRequests = -1;

    /** List of proxies to choose from. */
    private LinkedList<Proxy> proxyList = new LinkedList<Proxy>();
    
    private Proxy currentProxy = null;

    /** Number of requests sent with currently used proxy. */
    private int proxyRequests = 0;
    
    private DocumentRetriever dr = null;

    
    
    
    @SuppressWarnings("unchecked")
    public ProxySwitcher() {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        setSwitchProxyRequests(config.getInt("documentRetriever.switchProxyRequests", switchProxyRequests));
        setProxyList(config.getList("documentRetriever.proxyList", proxyList));
    }
    
    
    /**
     * Check whether to change the proxy and do it if needed. If a proxy is not working, remove it from the list. If we
     * have no working proxies left, fall back into normal mode.
     * 
     * @param force force the proxy change, no matter if the specified number of request for the switch has already been
     *            reached.
     */
    private void checkChangeProxy(boolean force) {
        if (switchProxyRequests > -1 && (force || proxyRequests == switchProxyRequests /*|| dr.getProxy() == null*/)) {
            if (force) {
                LOGGER.debug("force-change proxy");
            }
            boolean continueChecking = true;
            do {
                changeProxy();
                if (checkProxy()) {
                    continueChecking = false;
                } else {

                    // proxy is not working; remove it from the list
                    LOGGER.warn("proxy " + currentProxy + " is not working, removing from the list.");
                    proxyList.remove(currentProxy);
                    LOGGER.debug("# proxies in list: " + proxyList.size() + " : " + proxyList);

                    // if there are no more proxies left, go to normal mode without proxies.
                    if (proxyList.isEmpty()) {
                        LOGGER.error("no more working proxies, falling back to normal mode.");
                        continueChecking = false;
                        // dr.setProxy(null);
                        setSwitchProxyRequests(-1);
                    }
                }
            } while (continueChecking);
            proxyRequests = 0;
        }
    }
    
    /**
     * Number of requests after the proxy is changed.
     * 
     * @param switchProxyRequests number of requests for proxy change. Must be greater than 1 or -1 which means: change
     *            never.
     */
    public void setSwitchProxyRequests(int switchProxyRequests) {
        if (switchProxyRequests == 0) {
            throw new IllegalArgumentException();
        }
        this.switchProxyRequests = switchProxyRequests;
    }

    public int getSwitchProxyRequests() {
        return switchProxyRequests;
    }

    /**
     * Add an entry to the proxy list. The entry must be formatted as "HOST:PORT".
     * 
     * @param proxyEntry The proxy to add.
     */
    public void addToProxyList(String proxyEntry) {
        String[] proxySetting = proxyEntry.split(":");
        String host = proxySetting[0];
        int port = Integer.parseInt(proxySetting[1]);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        if (!proxyList.contains(proxy)) {
            proxyList.add(proxy);
        }
    }
    

    /**
     * Set a list of proxies. Each entry must be formatted as "HOST:PORT".
     * 
     * @param proxyList The list of proxies.
     */
    public void setProxyList(List<String> proxyList) {
        this.proxyList = new LinkedList<Proxy>();
        for (String proxy : proxyList) {
            addToProxyList(proxy);
        }
    }

    public List<Proxy> getProxyList() {
        return proxyList;
    }

    /**
     * Cycle the proxies, taking the first item from the queue and adding it to the end.
     */
    public void changeProxy() {
        Proxy selectedProxy = proxyList.poll();
        if (selectedProxy != null) {
            dr.setProxy(selectedProxy);
            proxyList.add(selectedProxy);
            LOGGER.debug("changed proxy to " + selectedProxy.address());
        }
    }

    /**
     * Check whether the curretly set proxy is working.
     * 
     * @return <tt>True</tt>, if proxy returns result, <tt>false</tt> otherwise.
     */
    public boolean checkProxy() {
        boolean result;
        InputStream is = null;
        try {
            // try to download from Google, if downloading fails we get IOException
            //is = dr.downloadInputStream("http://www.sourceforge.com");
            // TODO check is not working like this
            dr.getWebDocument("http://www.sourceforge.com");
            if (currentProxy != null) {
                LOGGER.debug("proxy " + currentProxy.address() + " is working.");
            }
            result = true;
        } finally {
            FileHelper.close(is);
        }
        return result;
    }



}
