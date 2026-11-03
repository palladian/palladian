package ws.palladian.retrieval.analysis;

import org.hamcrest.Matchers;
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
}