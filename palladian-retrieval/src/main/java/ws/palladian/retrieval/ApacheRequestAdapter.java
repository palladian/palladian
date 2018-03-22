package ws.palladian.retrieval;

import java.net.URI;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicNameValuePair;

final class ApacheRequestAdapter extends HttpEntityEnclosingRequestBase implements HttpUriRequest {

    private final HttpRequest2 adapted;

    ApacheRequestAdapter(HttpRequest2 adapted) {
        this.adapted = adapted;
        setURI(URI.create(adapted.getUrl()));
        for (Entry<String, String> header : adapted.getHeaders().entrySet()) {
            setHeader(header.getKey(), header.getValue());
        }
        HttpEntity entity = adapted.getEntity();
        if (entity != null) {
            // setEntity(new InputStreamEntity(entity.getInputStream(), entity.length()));
            String cleanContentType = entity.getContentType();
            ContentType contentType = null;
            if (cleanContentType != null) {
                String[] split = cleanContentType.split(";");
                cleanContentType = split[0];
                if (split.length > 1) {
                    NameValuePair[] params = new NameValuePair[split.length-1];
                    for (int i = 1; i < split.length; i++) {
                        String[] kv = split[i].split("=");
                        params[i-1] = new BasicNameValuePair(kv[0], kv[1]);
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
