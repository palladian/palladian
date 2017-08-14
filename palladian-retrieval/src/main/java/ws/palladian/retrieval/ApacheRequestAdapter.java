package ws.palladian.retrieval;

import java.net.URI;
import java.util.Map.Entry;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;

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
            ContentType contentType = null;
            if (entity.getContentType() != null) {
                contentType = ContentType.parse(entity.getContentType());
            }
            setEntity(new InputStreamEntity(entity.getInputStream(), entity.length(), contentType));
        }
    }

    @Override
    public String getMethod() {
        return adapted.getMethod().toString();
    }

}
