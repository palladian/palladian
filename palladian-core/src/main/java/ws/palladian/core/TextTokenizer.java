package ws.palladian.core;

import java.util.Iterator;

public interface TextTokenizer {

    Iterator<Token> iterateTokens(String text);

}
