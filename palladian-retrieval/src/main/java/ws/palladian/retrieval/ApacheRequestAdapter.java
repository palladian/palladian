package ws.palladian.retrieval;



import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.net.URI;
import java.util.Map.Entry;

final class ApacheRequestAdapter extends HttpUriRequestBase implements HttpUriRequest {

    private final HttpRequest2 adapted;

    ApacheRequestAdapter(HttpRequest2 adapted) {
        super(adapted.getMethod().toString(), URI.create(adapted.getUrl()));
        this.adapted = adapted;
        for (Entry<String, String> header : adapted.getHeaders().entrySet()) {
            setHeader(header.getKey(), header.getValue());
        }
        HttpEntity entity = adapted.getEntity();
        if (entity != null) {
            ContentType contentType = null;
            String cleanContentType = entity.getContentType();
            if (cleanContentType != null) {
                String[] split = cleanContentType.split(";");
                cleanContentType = split[0];
                if (split.length > 1) {
                    NameValuePair[] params = new NameValuePair[split.length - 1];
                    for (int i = 1; i < split.length; i++) {
                        String[] kv = split[i].split("=");
                        params[i - 1] = new BasicNameValuePair(kv[0], kv[1]);
                    }
                    contentType = ContentType.create(cleanContentType, params);
                } else {
                    contentType = ContentType.create(cleanContentType);
                }
            }

            setEntity(new InputStreamEntity(entity.getInputStream(), entity.length(), contentType));
        }
    }

    @Override
    public String getMethod() {
        return adapted.getMethod().toString();
    }

}
