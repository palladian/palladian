package com.newsseecr.xperimental.wikipedia;

import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;

public class Test {
    public static void main(String[] args) throws Exception {
        // configure the database connection parameters
        DatabaseConfiguration dbConfig = new DatabaseConfiguration();
        dbConfig.setHost("localhost");
        dbConfig.setDatabase("JWPL");
        dbConfig.setUser("root");
        dbConfig.setPassword("root");
        dbConfig.setLanguage(Language.english);
        
        // Create a new German wikipedia.
        Wikipedia wiki = new Wikipedia(dbConfig);
        
        // Get the page with title "Hello world".
        // May throw an exception, if the page does not exist.
        Page page = wiki.getPage("Hello world");
        System.out.println(page.getText());
    }

}
