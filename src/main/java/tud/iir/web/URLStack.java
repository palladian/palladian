package tud.iir.web;

import java.util.HashSet;

/**
 * Stack of URLs. TODO replace with native Java stack?
 * 
 * @author David Urbansky
 */
public class URLStack extends HashSet<String> {

    private static final long serialVersionUID = 421506489958342483L;

    @Override
    public boolean contains(Object o) {
        String url = (String) o;
        for (String stackURL : this) {
            if (url.equals(stackURL))
                return true;
        }
        return false;
    }
}