package ws.palladian.retrieval.analysis;

import ws.palladian.helper.nlp.StringHelper;

import java.util.Arrays;

public class PersonProfile {
    private String firstName;
    private String middleName;
    private String lastName;
    private String username;
    private String email;
    private String imageUrl;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (firstName != null) {
            firstName = StringHelper.upperCaseFirstLetter(firstName);
        }
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        if (middleName != null) {
            middleName = StringHelper.upperCaseFirstLetter(middleName);
        }
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (lastName != null) {
            lastName = StringHelper.upperCaseFirstLetter(lastName);
        }
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFullName() {
        String fullName = "";
        if (firstName != null) {
            fullName = firstName;
        }
        if (middleName != null) {
            fullName += " " + middleName;
        }
        if (lastName != null) {
            fullName += " " + lastName;
        }

        fullName = StringHelper.clean(fullName);

        return fullName;
    }

    /**
     * Fill the name parts from a formatted name: one word is the first name, two are first and last name, more are first
     * name, middle name (all words in between) and last name. A part that is already set is kept.
     */
    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return;
        }
        String[] parts = fullName.trim().split("\\s+");
        if (firstName == null) {
            setFirstName(parts[0]);
        }
        if (parts.length >= 2 && lastName == null) {
            setLastName(parts[parts.length - 1]);
        }
        if (parts.length >= 3 && middleName == null) {
            setMiddleName(String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1)));
        }
    }

    @Override
    public String toString() {
        return "PersonProfile{" + "firstName='" + firstName + '\'' + ", middleName='" + middleName + '\'' + ", lastName='" + lastName + '\'' + ", username='" + username + '\''
                + ", email='" + email + '\'' + ", imageUrl='" + imageUrl + '\'' + '}';
    }
}
