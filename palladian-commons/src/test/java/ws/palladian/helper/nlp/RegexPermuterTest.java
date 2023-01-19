package ws.palladian.helper.nlp;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.Collection;

import static org.hamcrest.core.Is.is;

/**
 * <p>Created by David Urbansky on 25.10.2016.</p>
 *
 * @author David Urbansky
 */
public class RegexPermuterTest {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testPermute() throws Exception {
        Collection<String> permute = RegexPermuter.permute("(a|b)(d|e)");
        collector.checkThat(permute, CoreMatchers.hasItems("ad", "ae", "bd", "be"));

        permute = RegexPermuter.permute("(a|b)?(d|e)");
        collector.checkThat(permute, CoreMatchers.hasItems("ad", "ae", "bd", "be", "d", "e"));
        collector.checkThat(permute.size(), is(6));

        permute = RegexPermuter.permute("(a|b)?(d|ds|e)");
        collector.checkThat(permute, CoreMatchers.hasItems("ad", "ae", "bd", "be", "d", "e"));
        collector.checkThat(permute.size(), is(9));

        permute = RegexPermuter.permute("(how|what) (is|are) (you|strange)");
        collector.checkThat(permute, CoreMatchers.hasItems("what is strange", "how are you", "how is strange"));
        collector.checkThat(permute.size(), is(8));
    }

}