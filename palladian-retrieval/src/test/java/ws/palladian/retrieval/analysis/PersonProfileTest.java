package ws.palladian.retrieval.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PersonProfileTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void oneWordIsTheFirstName() {
        PersonProfile profile = new PersonProfile();
        profile.setFullName("karanveer");
        collector.checkThat(profile.getFirstName(), is("Karanveer"));
        collector.checkThat(profile.getLastName(), nullValue());
        collector.checkThat(profile.getFullName(), is("Karanveer"));
    }

    @Test
    public void twoWordsAreFirstAndLastName() {
        PersonProfile profile = new PersonProfile();
        profile.setFullName("Max  Test");
        collector.checkThat(profile.getFirstName(), is("Max"));
        collector.checkThat(profile.getMiddleName(), nullValue());
        collector.checkThat(profile.getLastName(), is("Test"));
        collector.checkThat(profile.getFullName(), is("Max Test"));
    }

    @Test
    public void wordsBetweenFirstAndLastAreTheMiddleName() {
        PersonProfile profile = new PersonProfile();
        profile.setFullName("Anna Maria Luise Schmidt");
        collector.checkThat(profile.getFirstName(), is("Anna"));
        collector.checkThat(profile.getMiddleName(), is("Maria Luise"));
        collector.checkThat(profile.getLastName(), is("Schmidt"));
    }

    @Test
    public void partsAlreadySetAreKept() {
        PersonProfile profile = new PersonProfile();
        profile.setFirstName("Dave");
        profile.setFullName("David Urbansky");
        collector.checkThat(profile.getFirstName(), is("Dave"));
        collector.checkThat(profile.getLastName(), is("Urbansky"));
    }

    @Test
    public void emptyFullNameChangesNothing() {
        PersonProfile profile = new PersonProfile();
        profile.setFullName(null);
        profile.setFullName("   ");
        collector.checkThat(profile.getFirstName(), nullValue());
        collector.checkThat(profile.getLastName(), nullValue());
        collector.checkThat(profile.getFullName(), is(""));
    }
}
