package com.newsseecr.xperimental.wikipedia;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TextBufferHandler extends DefaultHandler {
    
    private boolean catchText = false;
    private StringBuilder buffer = new StringBuilder();
    
    @Override
    public final void characters(char[] ch, int start, int length)  throws SAXException {
        if (catchText) {
            buffer.append(ch, start, length);
        }
    }
    
    public void startCatching() {
        catchText = true;
    }
    
    public String getText() {
        try {
            return buffer.toString();
        } finally {
            buffer = new StringBuilder();
            catchText = false;
        }
    }

}
