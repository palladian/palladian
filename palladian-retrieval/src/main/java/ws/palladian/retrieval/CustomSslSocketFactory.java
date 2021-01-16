package ws.palladian.retrieval;

import org.apache.http.HttpHost;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Adapted from @see https://issues.apache.org/jira/browse/HTTPCLIENT-1522?focusedCommentId=14324923&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-14324923
 */
class CustomSslSocketFactory extends SSLSocketFactory {

    private final SocketConfig socketConfig;
    private final String ENABLE_SNI = "__enable_sni__";

    /*
     * Implement any constructor you need for your particular application -
     * SSLConnectionSocketFactory has many variants
     */
    public CustomSslSocketFactory(final SocketConfig sc, final SSLContext sslContext, final X509HostnameVerifier verifier) {
        super(sslContext, verifier);
        this.socketConfig = sc;
    }

    /*
     * Socket initialisation code is from DefaultHttpClientConnectionOperator::connect
     * Unfortunately we need this to create a new socket on a retry.
     */
    private Socket createSocketCustom(HttpContext context) throws IOException {
        Socket sock = super.createSocket(context); // FIXME help <- context (enable sni) is ignored in create socket super
        sock.setSoTimeout(socketConfig.getSoTimeout());
        sock.setReuseAddress(socketConfig.isSoReuseAddress());
        sock.setTcpNoDelay(socketConfig.isTcpNoDelay());
        sock.setKeepAlive(socketConfig.isSoKeepAlive());
        final int linger = socketConfig.getSoLinger();
        if (linger >= 0) {
            sock.setSoLinger(true, linger);
        }
        return sock;
    }

    private Socket connectSocketSni(
            final int connectTimeout,
            final Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            HttpContext context,
            final boolean enableSni) throws IOException {
        try {
            if (context == null) {
                context = new BasicHttpContext();
            }
            return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        } catch (SSLHandshakeException e) {
            if (enableSni && e.getMessage() != null && e.getMessage().contains("unrecognized_name")) {
//                System.out.println("Server received saw wrong SNI host, retrying without SNI, host: " + host);
                context.setAttribute(ENABLE_SNI, false);
                // We need to create a new socket to retry (the first one is closed after IOException)
                // Is there a clean way to do this?
                return connectSocketSni(connectTimeout, createSocketCustom(context), host, remoteAddress, localAddress, context, false);
            } else {
                throw e;
            }
        }
    }

    @Override
    public Socket connectSocket(
            final int connectTimeout,
            final Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpContext context) throws IOException {
//        context.setAttribute(ENABLE_SNI, true);
        return this.connectSocketSni(connectTimeout, socket, host, remoteAddress, localAddress, context, true);
    }

    @Override
    public Socket createLayeredSocket(
            final Socket socket,
            final String target,
            final int port,
            final HttpContext context) throws IOException {
    	boolean enableSni = true;
    	if (context != null) {
    		Object enableSniValue = context.getAttribute(ENABLE_SNI);
    		if (enableSniValue instanceof Boolean) {
    			enableSni = ((Boolean)enableSniValue).booleanValue();
    		}
    	}
        return super.createLayeredSocket(socket, enableSni ? target : "", port, context);
    }
}