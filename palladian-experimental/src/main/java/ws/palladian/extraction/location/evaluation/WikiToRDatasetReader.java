package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

/**
 * Parser for the WikToR dataset from
 * <a href="https://github.com/milangritta/WhatsMissingInGeoparsing">What's
 * Missing In Geoparsing?</a>.
 *
 * @author pk
 *
 */
public class WikiToRDatasetReader {

    private static final class Span {
        final int start;
        final int end;

        Span(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final class WikiToRHandler extends DefaultHandler {

        final Consumer<LocationDocument> consumer;

        StringBuilder buffer = new StringBuilder();
        Integer pageNumber;
        // String pageTitle;
        String toponymName;
        String text;
        List<Span> toponymIndices = new ArrayList<>();
        // String url;
        Double lat;
        Double lon;
        // String feature;
        // String country;
        Integer start;
        Integer end;

        WikiToRHandler(Consumer<LocationDocument> consumer) {
            this.consumer = Objects.requireNonNull(consumer);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (qName.equals("page")) {
                pageNumber = Integer.parseInt(attributes.getValue("number"));
            }
            clearBuffer();
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("page")) {
                GeoCoordinate coordinate = new ImmutableGeoCoordinate(lat, lon);
                Location location = new ImmutableLocation(coordinate.hashCode(), toponymName, LocationType.UNDETERMINED,
                        coordinate, 0l);
                List<LocationAnnotation> annotations = toponymIndices.stream()
                        .map(s -> new LocationAnnotation(s.start, text.substring(s.start, s.end), location))
                        .collect(Collectors.toList());
                LocationDocument locationDocument = new LocationDocument("page_" + pageNumber, text, annotations,
                        location);
                consumer.accept(locationDocument);
                clearAll();
            } else if (qName.equals("pageTitle")) {
                // pageTitle = getBuffer();
            } else if (qName.equals("toponymName")) {
                toponymName = getBuffer();
            } else if (qName.equals("text")) {
                text = getBuffer();
            } else if (qName.equals("start")) {
                start = Integer.parseInt(getBuffer());
            } else if (qName.equals("end")) {
                end = Integer.parseInt(getBuffer());
            } else if (qName.equals("toponym")) {
                toponymIndices.add(new Span(start, end));
            } else if (qName.equals("url")) {
                // url = getBuffer();
            } else if (qName.equals("lat")) {
                lat = Double.parseDouble(getBuffer());
            } else if (qName.equals("lon")) {
                lon = Double.parseDouble(getBuffer());
            } else if (qName.equals("feature")) {
                // feature = getBuffer();
            } else if (qName.equals("country")) {
                // country = getBuffer();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            buffer.append(ch, start, length);
        }

        private void clearAll() {
            pageNumber = null;
            // pageTitle = null;
            toponymName = null;
            text = null;
            toponymIndices = new ArrayList<>();
            // url = null;
            lat = null;
            lon = null;
            // feature = null;
            // country = null;
            start = null;
            end = null;
            clearBuffer();
        }

        private String getBuffer() {
            try {
                return buffer.toString();
            } finally {
                clearBuffer();
            }
        }

        private void clearBuffer() {
            buffer = new StringBuilder();
        }

    }

    public static void parse(File inputFile, Consumer<LocationDocument> action)
            throws ParserConfigurationException, SAXException, IOException {
        Objects.requireNonNull(inputFile, "inputFile was null");
        Objects.requireNonNull(action, "action was null");
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        DefaultHandler handler = new WikiToRHandler(action::accept);
        parser.parse(inputFile, handler);
    }

}
