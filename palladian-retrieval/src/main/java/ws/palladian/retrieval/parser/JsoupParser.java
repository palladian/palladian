package ws.palladian.retrieval.parser;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import ws.palladian.persistence.ParserException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser for HTML documents using Jsoup.
 *
 * @author David Urbansky
 */
public final class JsoupParser extends BaseDocumentParser {

    private static final Map<String, String> NAMESPACE_MAP = new HashMap<>();

    static {
        NAMESPACE_MAP.put("xlink", "http://www.w3.org/1999/xlink");
    }

    protected JsoupParser() {
        // instances should be created by the factory.
    }

    @Override
    public Document parse(InputSource inputSource) throws ParserException {
        try {
            byte[] inputBytes = inputStreamToByteArray(inputSource.getByteStream());
            String encoding = inputSource.getEncoding();
            String xml = byteArrayToString(inputBytes, encoding);
            org.jsoup.nodes.Document parse = Jsoup.parse(xml);
            if (encoding == null) { // make sure content-type meta tag is handled correctly
                String documentEncoding = getDocumentEncoding(parse);
                if (documentEncoding != null && !documentEncoding.equalsIgnoreCase(StandardCharsets.UTF_8.name())) {
                    inputSource.setEncoding(documentEncoding);
                    xml = byteArrayToString(inputBytes, documentEncoding);
                    parse = Jsoup.parse(xml);
                }
            }
            // unknown namespaces can cause problems with the documents (e.g. cloning or serializing), this is somehow handled by ValidatorNuParser out of the box
            addNamespacesToHtml(parse);
            return W3CDom.convert(parse);
        } catch (IOException e) {
            throw new ParserException(e);
        }
    }

    private static String getDocumentEncoding(org.jsoup.nodes.Document document) {
        Element encodingMeta = document.selectFirst("meta[http-equiv=\"content-type\"]");
        if (encodingMeta == null) {
            return null;
        }
        String content = encodingMeta.attr("content");
        if (!content.isEmpty()) {
            String[] parts = content.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("charset=")) {
                    String encoding = part.trim().substring("charset=".length());
                    if (isSupportedCharset(encoding)) {
                        return encoding;
                    }
                }
            }
        }
        return null;
    }

    private static String byteArrayToString(byte[] byteArray, String encoding) throws UnsupportedEncodingException {
        encoding = Optional.ofNullable(encoding).orElse(StandardCharsets.UTF_8.name());
        return new String(byteArray, encoding);
    }

    private static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private static void addNamespacesToHtml(org.jsoup.nodes.Document document) {
        Element html = document.selectFirst("html");
        if (html == null) {
            return;
        }
        Map<String, String> namespaces = new HashMap<>();
        Set<String> attrs = Collections.synchronizedSet(new HashSet<>());
        document.getAllElements().parallelStream().forEach(el -> {
            attrs.add(el.tag().getName());
            for (Attribute attribute : el.attributes()) {
                attrs.add(attribute.getKey());
            }
        });
        for (String attr : attrs) {
            if (!attr.contains(":")) {
                continue;
            }
            String key = attr.split(":")[0];
            if (key.equals("xmlns") || key.equals("xml")) {
                continue;
            }
            namespaces.put("xmlns:" + key, Optional.ofNullable(NAMESPACE_MAP.get(key)).orElse("http://palladian.ws/2024/" + key));
        }
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (html.attr(entry.getKey()).isEmpty()) {
                html.attr(entry.getKey(), entry.getValue());
            }
        }
    }
}
