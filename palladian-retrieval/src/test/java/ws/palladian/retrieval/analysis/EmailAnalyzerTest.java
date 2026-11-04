package ws.palladian.retrieval.analysis;

import org.hamcrest.Matchers;
import ws.palladian.persistence.json.JsonArray;
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
        //        collector.checkThat(profile.getFirstName(), is("David"));
        //        collector.checkThat(profile.getLastName(), is("Urbansky"));
        collector.checkThat(profile.getEmail(), is("david.urbansky@semknox.com"));
        collector.checkThat(profile.getUsername(), is("ddsky"));
        // the avatar of the queried address itself, not the account's primary address (e61fff…)
        collector.checkThat(profile.getImageUrl(), is("https://0.gravatar.com/avatar/dce196cd00ed7df3bebdcb0b23a0f4db"));

        // no Gravatar for this address: no image rather than Gravatar's default picture
        collector.checkThat(new EmailAnalyzer().getProfile("nobody-xq7z1@example.com").getImageUrl(), Matchers.nullValue());

        //        profile = new EmailAnalyzer().getProfile("david.urbansky@anotherwebsite.com");
        //collector.checkThat(profile.getFirstName(), is("David"));
        //collector.checkThat(profile.getLastName(), is("Urbansky"));
    }

    @Test
    public void fillMissingNamesNeverOverwritesOrClears() {
        PersonProfile profile = new PersonProfile();
        profile.setFirstName("Karanveer");
        EmailAnalyzer.fillMissingNames(profile, null, "Singh");
        collector.checkThat(profile.getFirstName(), is("Karanveer"));
        collector.checkThat(profile.getLastName(), is("Singh"));

        EmailAnalyzer.fillMissingNames(profile, "Other", "Name");
        collector.checkThat(profile.getFirstName(), is("Karanveer"));
        collector.checkThat(profile.getLastName(), is("Singh"));
    }

    @Test
    public void fillMissingNamesIgnoresALastNameThatRepeatsTheFirstName() {
        PersonProfile profile = new PersonProfile();
        EmailAnalyzer.fillMissingNames(profile, "karanveer", "Karanveer");
        collector.checkThat(profile.getFirstName(), is("Karanveer"));
        collector.checkThat(profile.getLastName(), Matchers.nullValue());
    }

    @Test
    public void avatarUrlUsesTheHashOfTheQueriedAddress() {
        String md5 = EmailAnalyzer.gravatarHash("  DAVID.urbansky@semknox.com ");
        collector.checkThat(md5, is("dce196cd00ed7df3bebdcb0b23a0f4db"));
        collector.checkThat(EmailAnalyzer.gravatarAvatarUrl(md5), is("https://0.gravatar.com/avatar/dce196cd00ed7df3bebdcb0b23a0f4db"));
    }

    @Test
    public void givenNamesAfterTheFirstAreTheMiddleNameAndInitialsAreDropped() {
        // NameAPI's answers for john.michael.smith@, mary_ann.lee@ and j.smith@ (lower case, as it sends them)
        String[] john = EmailAnalyzer.splitGivenNames(JsonArray.tryParse("[{\"name\":\"john\",\"nameType\":\"NAME\"},{\"name\":\"michael\",\"nameType\":\"NAME\"}]"));
        collector.checkThat(john[0], is("john"));
        collector.checkThat(john[1], is("michael"));
        String[] maryAnn = EmailAnalyzer.splitGivenNames(JsonArray.tryParse("[{\"name\":\"mary ann\",\"nameType\":\"NAME\"}]"));
        collector.checkThat(maryAnn[0], is("mary ann"));
        collector.checkThat(maryAnn[1], Matchers.nullValue());
        String[] initial = EmailAnalyzer.splitGivenNames(JsonArray.tryParse("[{\"name\":\"j.\",\"nameType\":\"INITIAL\"}]"));
        collector.checkThat(initial[0], Matchers.nullValue());
        collector.checkThat(initial[1], Matchers.nullValue());
        collector.checkThat(EmailAnalyzer.splitGivenNames(null)[0], Matchers.nullValue());

        PersonProfile profile = new PersonProfile();
        EmailAnalyzer.fillMissingNames(profile, john[0], john[1], "smith");
        collector.checkThat(profile.getFullName(), is("John Michael Smith"));
    }
}
