package ws.palladian.retrieval.feeds.evaluation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;

public final class URLCleaner {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		FileInputStream input = new FileInputStream(args[0]);
		FileOutputStream output = new FileOutputStream(args[1]);
		List<String> urls = IOUtils.readLines(input);
		String start = "INSERT INTO feeds (feedUrl) VALUES (\"";
		String end = "\");";
		for(String url:urls) {
			IOUtils.write(start+url+end+"\n",output);
		}
		IOUtils.closeQuietly(input);
		IOUtils.closeQuietly(output);
	}

}
