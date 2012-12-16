package ws.palladian.helper;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.preprocessing.multimedia.ImageHandler;
import ws.palladian.preprocessing.multimedia.PdfToImageConverter;
import ws.palladian.preprocessing.multimedia.PdfToImageConverter.ImageFormat;

public class LatexToMhtmlConverter {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(LatexToMhtmlConverter.class);

    private static final String IMAGE_CHECKMARK = "R0lGODlhDAANAOMAAP///////2RkZJmZmRYWFk9PTyAgID09PS0tLQMDA319fdvb2wgICAAAAAEBAbi4uCH5BAEAAAAALAAAAAAMAA0AAAQvEMhJxaCYFEzN5ZIGSgcyAkNyAomCLcYCFAwnNKYjgExzOKNHo0E4IRqb0SLJiQAAOw==";
    private static final String NLB = "(?<![\\{\\}])";

    /** If true, pdf includes will be transformed to Base64 png data. */
    private boolean rasterPdf = true;

    /**
     * If true, we pack everything into one single HTML file with no external references. Images will be base64 encoded.
     */
    private boolean oneFile = true;

    private String writeHeader(String style, String latex) {
        StringBuilder header = new StringBuilder();

        header.append("<html><head>");
        // header.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"styles.css\" />");

        header.append("<style type=\"text/css\">").append(style).append("</style>");

        header.append("<title>").append(StringHelper.getSubstringBetween(latex, "title{", "}")).append("</title>");
        header.append("</head><body>");
        header.append("<div class=\"container\">");

        return header.toString();
    }

    private String writeFooter(String js) {
        StringBuilder footer = new StringBuilder();

        footer.append("</div>");
        footer.append("<script type=\"text/javascript\">").append(js).append("</script>");
        footer.append("</body>");
        footer.append("</html>");

        return footer.toString();
    }

    private String includeDocuments(String latex, String basePath) {

        // include referenced documents
        Matcher matcher = Pattern.compile("(?<=[^%])\\\\include\\{(.*?)\\}").matcher(latex);

        while (matcher.find()) {
            latex = latex.replace(matcher.group(), FileHelper.readFileToString(basePath + matcher.group(1) + ".tex"));
        }

        FileHelper.writeToFile(basePath + "merged.tex", latex);

        return latex;
    }

    private String latexToHtml(String latex) {
        String converted = latex;

        // strip comments
        converted = converted.replaceAll("(?<=[^\\\\])\\%.*", "");
        converted = converted.replaceAll("[\\\\]%", "%");
        converted = converted.replaceAll("\\\\begin\\{comment\\}", "<!--");
        converted = converted.replaceAll("\\\\end\\{comment\\}", "-->");

        // special commands
        converted = converted.replaceAll("\\\\leq", "&lt;=");
        converted = converted.replaceAll("\\\\geq", "&gt;>");
        converted = converted.replaceAll("~", "&nbsp;");

        // headings
        converted = converted.replaceAll("\\\\chapter\\{(.*?)\\}", "<h1>$1</h1>");
        converted = converted.replaceAll("\\\\section\\{(.*?)\\}", "<h2>$1</h2>");
        converted = converted.replaceAll("\\\\subsection\\{(.*?)\\}", "<h3>$1</h3>");
        converted = converted.replaceAll("\\\\subsubsection\\{(.*?)\\}", "<h4>$1</h4>");
        converted = converted.replaceAll("\\\\paragraph\\{(.*?)\\}", "<h5>$1</h5>");
        converted = converted.replaceAll("\\\\subparagraph\\{(.*?)\\}", "<h6>$1</h6>");

        // symbols
        converted = converted.replaceAll("\\$?\\\\checkmark\\$?", "<img src=\"data:image/gif;base64," + IMAGE_CHECKMARK
                + "\" />");

        // structure
        converted = converted.replaceAll(NLB + "\\\\begin\\{itemize\\}", "<ul>");
        converted = converted.replaceAll(NLB + "\\\\end\\{itemize\\}", "</ul>");
        converted = converted.replaceAll(NLB + "\\\\begin\\{description\\}", "<ul>");
        converted = converted.replaceAll(NLB + "\\\\end\\{description\\}", "</ul>");
        converted = converted.replaceAll(NLB + "\\\\begin\\{enumerate\\}", "<ol>");
        converted = converted.replaceAll(NLB + "\\\\end\\{enumerate\\}", "</ol>");
        converted = converted.replaceAll(NLB + "\\\\item", "<li>");
        
        converted = converted.replaceAll("\\\\begin\\{verbatim\\}", "<pre>");
        converted = converted.replaceAll("\\\\end\\{verbatim\\}", "</pre>");

        // URLs
        converted = converted.replaceAll("\\\\url\\{(.*?)\\}", "<a href=\"$1\">$1</a>");
        converted = converted.replaceAll("\\\\href\\{(.*?)\\}\\{(.*?)\\}", "<a href=\"$1\">$2</a>");
        converted = converted.replaceAll("\\\\label\\{(.*?)\\}", "<a name=\"$1\"></a>");
        converted = converted.replaceAll("([^\\s]+?)(\\s)?\\\\ref\\{(.*?)\\}", "<a href=\"#$3\">$1 $3</a>$2");

        // codelisting
        converted = converted.replaceAll("\\\\begin\\{codelisting\\}", "<pre>");
        converted = converted.replaceAll("\\\\end\\{codelisting\\}", "</pre>");

        // styling
        converted = converted.replaceAll("\\\\textbf\\{(.*?)\\}", "<span class=\"textbf\">$1</span>");
        converted = converted.replaceAll("\\\\textit\\{(.*?)\\}", "<span class=\"textit\">$1</span>");
        converted = converted.replaceAll("\\\\texttt\\{(.*?)\\}", "<span class=\"texttt\">$1</span>");
        converted = converted.replaceAll("\\\\emph\\{(.*?)\\}", "<span class=\"emph\">$1</span>");
        converted = converted.replaceAll("\\\\verb.(.*?).\\s", "<span class=\"verb\">$1</span> ");

        // citations
        converted = converted.replaceAll("\\\\cite(p|t)?\\{(.*?)\\}", "(<span class=\"citation\">$2</span>)");

        // footnote
        converted = converted.replaceAll("([^\\s]+)\\\\footnote\\{(.*?)\\}",
                        "<span class=\"footnote\" onmouseover=\"showFootnote(this);\" onmouseout=\"hideFootnote(this);\">$1</span><span class=\"footnoteText\">$2</span>");

        // images TODO use fancy zoom http://www.dfc-e.com/metiers/multimedia/opensource/jquery-fancyzoom/
        // converted = converted.replaceAll(
        // "\\\\begin\\{figure\\}.*?\\\\includegraphics.*?\\{(.*?)\\}.*\\\\end\\{figure\\}", "<img src=\"$1\" />");
        // pdf embeds TODO: http://pdfobject.com/instructions.php
        converted = Pattern
                .compile(
                        "\\\\begin\\{figure\\}(\\[.*?\\])?(.{0,60}?)\\\\includegraphics[^{]*?\\{([^.]{1,50}?\\.pdf)\\}(.*?)\\\\end\\{figure\\}",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE)
                .matcher(converted)
                .replaceAll(
                        "$2\n<div class=\"pdfContainer\"><object type=\"application/pdf\" data=\"$3#zoom=85&scrollbar=0&toolbar=0&navpanes=0\" class=\"pdfObject\"></object></div>\n$4");

        converted = Pattern
                .compile(
                        "\\\\begin\\{figure\\}.{0,60}?\\\\includegraphics[^{]*?\\{([^.]{1,50}?\\.(png|jpg|gif))\\}.*?(<a.*?a>).*?\\\\end\\{figure\\}",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(converted)
                .replaceAll("$3\n<div class=\"imgContainer\"><img src=\"$1\" class=\"figure\"/></div>");

        // tables
        converted = Pattern
                .compile("\\\\begin\\{table\\}(\\[.*?\\])?(.*?)(?=\\\\begin\\{tab)", Pattern.DOTALL | Pattern.MULTILINE)
                .matcher(converted).replaceAll("$2\n<table cellpadding=\"0\" cellmargin=\"0\" class=\"tablesorter\">");
        converted = processTables(converted);
        converted = Pattern.compile("(?<=\\<\\/tbody\\>)(.*?)\\\\end\\{table\\}", Pattern.DOTALL | Pattern.MULTILINE)
                .matcher(converted).replaceAll("</table>\n$1");

        // equations (wrap for js replacement)
        converted = Pattern
                .compile("\\\\begin\\{equation\\}.*?(<a.*?a>\n)(.*?)\\\\end\\{equation\\}",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(converted)
                .replaceAll("$1\n<div class=\"equation\" lang=\"latex\">\n $2</div>");

        // captions
        converted = converted.replaceAll("\\\\caption\\{(.*?)\\}", "<div class=\"caption\">$1</div>");

        // paragraphs
        converted = converted.replaceAll("(?<=h[1-6].)\n\\s?(.{20,}?)(?=\n\n)", "\n<p>$1</p>\n");
        converted = converted.replaceAll("\n\n\\s?(.{20,}?)(?=\n\n)", "\n<p>$1</p>\n");

        // quotes
        converted = converted.replaceAll("``(.*?)''", "&#8220;$1&#8221;");

        // delete all other lines beginning with \ or {\
        converted = Pattern.compile("^\\\\.*", Pattern.MULTILINE).matcher(converted).replaceAll("");
        converted = Pattern.compile("^\\{+\\\\.*?\\}+", Pattern.MULTILINE).matcher(converted).replaceAll("");

        return converted;
    }

    private String processTables(String converted) {

        Matcher matcher = Pattern.compile("\\\\begin\\{tabular.?\\}(.*?)\\\\end\\{tabular.?\\}",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(converted);

        while (matcher.find()) {

            StringBuilder table = new StringBuilder();

            String tableText = matcher.group();
            String[] lines = tableText.split("\n");

            boolean tableHeadWritten = false;

            for (String line : lines) {

                line = line.trim();

                if (line.startsWith("\\") || line.isEmpty()) {
                    continue;
                }

                if (!tableHeadWritten) {
                    table.append("<thead>\n");
                }

                table.append("<tr>\n");

                String[] tds = line.split("&");
                for (String td : tds) {
                    td = td.replace("\\", "").trim();
                    String name = "td";
                    if (!tableHeadWritten) {
                        name = "th";
                    }
                    table.append("\t<").append(name).append(">");
                    table.append(td);
                    table.append("</").append(name).append(">\n");
                }

                table.append("</tr>\n");

                if (!tableHeadWritten) {
                    table.append("</thead>\n<tbody>");
                    tableHeadWritten = true;
                }

            }

            table.append("</tbody>\n");

            converted = converted.replace(tableText, table.toString());
        }

        return converted;
    }

    private String replaceImages(String html, String basePath) {
        List<String> matches = StringHelper.getRegexpMatches("(?<=img src\\=\").*?(?=\")", html);
        // List<String> matches = StringHelper.getRegexpMatches("img.*?\"", html);
        for (String url : matches) {
            System.out.println(basePath + url);
            BufferedImage image = ImageHandler.load(basePath + url);

            try {
                ByteArrayOutputStream byteaOutput = new ByteArrayOutputStream();
                Base64OutputStream base64Output = new Base64OutputStream(byteaOutput);
                // ImageIO.write(image, "JPG", base64Output);
                ImageIO.write(image, ImageFormat.PNG.toString(), base64Output);
                String base64 = new String(byteaOutput.toByteArray());
                base64 = StringHelper.removeControlCharacters(base64).replace(" ", "");
                System.out.println(base64);

                // html = html.replace(url, "data:image/jpeg;base64," + base64);
                html = html.replace(url, "data:image/png;base64," + base64);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        return html;
    }

    private void writeToMhtml(String html, String targetFolderPath) throws MessagingException, IOException {
        Properties props = new Properties();

        Session session = Session.getInstance(props, null);
        // construct a MIME message message
        MimeMessage message = new MimeMessage(session);
        MimeMultipart mpart = new MimeMultipart("related");

        mpart.addBodyPart(bodyPart(new StringSource("text/html", "index.html", html)));
        // mpart.addBodyPart(bodyPart(new FileDataSource("styles.css")));
        // mpart.addBodyPart(bodyPart(new FileDataSource("lady.jpg")));
        message.setContent(mpart);
        // the subject is displayed as the window title in the browser
        message.setSubject("MHTML example");
        // one can set the URL of the original page:
        message.addHeader("Content-Encoding", "UTF-8");
        message.addHeader("Content-Location", "index.html");

        // Save to example.mhtml
        FileOutputStream out = new FileOutputStream(targetFolderPath + "converted.mhtml");
        message.writeTo(out);
        out.close();
    }

    public void convert(String latexFilePath, String cssFilePath, String jsFilePath) throws MessagingException,
            IOException {

        String targetFolderPath = FileHelper.getFilePath(latexFilePath);

        String latex = FileHelper.readFileToString(latexFilePath);
        String css = FileHelper.readFileToString(cssFilePath);
        String js = FileHelper.readFileToString(jsFilePath);

        String converted = includeDocuments(latex, targetFolderPath);

        if (isRasterPdf()) {
            converted = rasterPdf(converted, targetFolderPath);
        }
        FileHelper.writeToFile(targetFolderPath + "rastered.html", converted);

        converted = latexToHtml(converted);
        if (isOneFile()) {
            converted = replaceImages(converted, targetFolderPath);
        }

        converted = writeHeader(css, latex) + converted + writeFooter(js);


        FileHelper.writeToFile(targetFolderPath + "converted.html", converted);

        writeToMhtml(converted, targetFolderPath);
    }

    private String rasterPdf(String converted, String targetFolderPath) {

        // converted = converted.replaceAll("\\\\includegraphics([^{]*?)\\{([^.]{1,50}?\\.)pdf\\}",
        // "\\\\includegraphics$1\\{$2png\\}");

        Matcher matcher = Pattern.compile("\\\\includegraphics([^{]*?)\\{([^.]{1,50}?\\.pdf)\\}").matcher(converted);
        while (matcher.find()) {
            String pdfPath = targetFolderPath + matcher.group(2);
            String targetPath = targetFolderPath + FileHelper.getFileName(pdfPath) + ".png";
            LOGGER.debug(pdfPath + " => " + targetPath);
            PdfToImageConverter.convertPdfToImage(pdfPath, targetPath);
            converted = converted.replaceAll(matcher.group(2), matcher.group(2).replace(".pdf", ".png"));
        }

        return converted;
    }

    static BodyPart bodyPart(DataSource ds) throws MessagingException {
        MimeBodyPart body = new MimeBodyPart();
        DataHandler dh = new DataHandler(ds);
        body.setDisposition("inline");
        body.setDataHandler(dh);
        body.setFileName(dh.getName());
        // the URL of the file; we set it simply to its name
        body.addHeader("Content-Location", dh.getName());
        return body;
    }

    /**
     * A simple in-memory implementation of {@link DataSource}.
     */
    static final class StringSource implements DataSource {
        private final String contentType;
        private final String name;
        private final byte[] data;

        public StringSource(String contentType, String name, String data) {
            this.contentType = contentType;
            this.data = data.getBytes();
            this.name = name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public boolean isRasterPdf() {
        return rasterPdf;
    }

    public void setRasterPdf(boolean rasterPdf) {
        this.rasterPdf = rasterPdf;
    }

    public boolean isOneFile() {
        return oneFile;
    }

    public void setOneFile(boolean oneFile) {
        this.oneFile = oneFile;
    }

    public static void main(String argv[]) throws Exception {
        LatexToMhtmlConverter converter = new LatexToMhtmlConverter();
        converter.setRasterPdf(true);
        converter.setOneFile(true);
        // converter.convert("documentation/latex2pdf/thesis/tex/main.tex",
        // "documentation/latex2pdf/thesis/tex/styles.css", "documentation/latex2pdf/thesis/tex/js.js");

        converter.convert("documentation/latex2pdf/sample.tex", "documentation/latex2pdf/styles.css",
                "documentation/latex2pdf/js.js");


        // converter.convert("documentation/book/book.tex", "documentation/book/res/styles.css",
        // "documentation/book/res/js.js");
    }

}
