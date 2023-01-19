package ws.palladian.retrieval.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.core.Is.is;

public class EmailAnalyzerTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void getProfile() {
        PersonProfile profile = new EmailAnalyzer().getProfile("DAVID.urbansky@semknox.com");
        collector.checkThat(profile.getFirstName(), is("David"));
        collector.checkThat(profile.getLastName(), is("Urbansky"));
        collector.checkThat(profile.getEmail(), is("david.urbansky@semknox.com"));
        collector.checkThat(profile.getUsername(), is("ddsky"));
        collector.checkThat(profile.getImageUrl(), is("https://secure.gravatar.com/avatar/e61fff419c2ddf685b1520e768d33e40"));

        profile = new EmailAnalyzer().getProfile("david.urbansky@anotherwebsite.com");
        //collector.checkThat(profile.getFirstName(), is("David"));
        //collector.checkThat(profile.getLastName(), is("Urbansky"));
    }
}