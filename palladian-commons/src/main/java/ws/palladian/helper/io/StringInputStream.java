package ws.palladian.helper.io;

import java.io.IOException;
import java.io.InputStream;

public class StringInputStream extends InputStream {

    private StringBuilder string = null;
    int currentIndex = 0;

    public StringInputStream(String text) {
        string = new StringBuilder(text);
    }

    public void write(int b) throws IOException {
        this.string.append((char) b);
    }

    @Override
    public int read() throws IOException {
        if (currentIndex < string.length()) {
            return string.charAt(currentIndex++);
        }
        return -1;
    }

}