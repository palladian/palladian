package ws.palladian.retrieval.parser;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.persistence.ParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parser for HTML documents using Jsoup.
 *
 * @author David Urbansky
 */
public final class JsoupParser extends BaseDocumentParser {

    private static final Map<String, String> NAMESPACE_MAP = new HashMap<>();
    private static final Pattern NAMESPACE_REGEX = PatternHelper.compileOrGet("^[a-zA-Z_][a-zA-Z0-9_.-]*$");

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
            if (encoding == null) { // make sure content-type meta tag is handled correctly
                String documentEncoding = getDocumentEncoding(Jsoup.parse(xml));
                if (documentEncoding != null && !documentEncoding.equalsIgnoreCase(StandardCharsets.UTF_8.name())) {
                    inputSource.setEncoding(documentEncoding);
                    xml = byteArrayToString(inputBytes, documentEncoding);
                }
            }
            return parse(xml);
        } catch (IOException e) {
            throw new ParserException(e);
        }
    }

    @Override
    public Document parse(String xml) {
        org.jsoup.nodes.Document parse = Jsoup.parse(xml);
        // unknown namespaces can cause problems with the documents (e.g. cloning or serializing), this is somehow handled by ValidatorNuParser out of the box
        addNamespacesToHtml(parse);
        return W3CDom.convert(parse);
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
        Set<String> attrs = new HashSet<>();
        for (Element el : document.getAllElements()) {
            attrs.add(el.tag().getName());
            for (Attribute attribute : el.attributes()) {
                attrs.add(attribute.getKey());
            }
        }

        for (String attr : attrs) {
            if (!attr.contains(":")) {
                continue;
            }
            String[] split = attr.split(":");
            if (split.length != 2) {
                continue;
            }
            String key = split[0];
            if (key.equals("xmlns") || key.equals("xml")) {
                continue;
            }
            if (!NAMESPACE_REGEX.matcher(key).matches()) {
                continue;
            }
            namespaces.put("xmlns:" + key, Optional.ofNullable(NAMESPACE_MAP.get(key)).orElse("http://www.w3.org/1999/xhtml" + key));
        }
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (html.attr(entry.getKey()).isEmpty()) {
                html.attr(entry.getKey(), entry.getValue());
            }
        }

        // no matter what, we want the xhtml namespace otherwise the xpath helper won't work
        html.attr("xmlns", "http://www.w3.org/1999/xhtml");
    }
}
