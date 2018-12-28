package cz.cvut.kbss.termit.environment;

import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Collection;
import java.util.Random;

public class Generator {

    private static Random random = new Random();

    private Generator() {
        throw new AssertionError();
    }

    /**
     * Generates a (pseudo) random URI, usable for test individuals.
     *
     * @return Random URI
     */
    public static URI generateUri() {
        return URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/randomInstance" + randomInt());
    }

    /**
     * Generates a (pseudo-)random integer between the specified lower and upper bounds.
     *
     * @param lowerBound Lower bound, inclusive
     * @param upperBound Upper bound, exclusive
     * @return Randomly generated integer
     */
    public static int randomInt(int lowerBound, int upperBound) {
        int rand;
        do {
            rand = random.nextInt(upperBound);
        } while (rand < lowerBound);
        return rand;
    }

    /**
     * Generates a (pseudo) random integer.
     * <p>
     * This version has no bounds (aside from the integer range), so the returned number may be negative or zero.
     *
     * @return Randomly generated integer
     * @see #randomInt(int, int)
     */
    public static int randomInt() {
        return random.nextInt();
    }

    /**
     * Generates a (pseudo)random index of an element in the collection.
     * <p>
     * I.e. the returned number is in the interval <0, col.size()).
     *
     * @param col The collection
     * @return Random index
     */
    public static int randomIndex(Collection<?> col) {
        assert col != null;
        assert !col.isEmpty();
        return random.nextInt(col.size());
    }

    /**
     * Generates a (pseudo)random index of an element in the array.
     * <p>
     * I.e. the returned number is in the interval <0, arr.length).
     *
     * @param arr The array
     * @return Random index
     */
    public static int randomIndex(Object[] arr) {
        assert arr != null;
        assert arr.length > 0;
        return random.nextInt(arr.length);
    }

    /**
     * Generators a (pseudo) random boolean.
     *
     * @return Random boolean
     */
    public static boolean randomBoolean() {
        return random.nextBoolean();
    }

    /**
     * Creates a random instance of {@link User}.
     * <p>
     * The instance has no identifier set.
     *
     * @return New {@code User} instance
     * @see #generateUserWithId()
     */
    public static User generateUser() {
        final User user = new User();
        user.setFirstName("Firstname" + randomInt());
        user.setLastName("Lastname" + randomInt());
        user.setUsername("user" + randomInt() + "@kbss.felk.cvut.cz");
        return user;
    }

    /**
     * Creates a random instance of {@link User} with a generated identifier.
     * <p>
     * The presence of identifier is the only difference between this method and {@link #generateUser()}.
     *
     * @return New {@code User} instance
     */
    public static User generateUserWithId() {
        final User user = generateUser();
        user.setUri(Generator.generateUri());
        return user;
    }

    /**
     * Generates a random {@link UserAccount} instance, initialized with first name, last name, username and
     * identifier.
     *
     * @return A new {@code UserAccount} instance
     */
    public static UserAccount generateUserAccount() {
        final UserAccount account = new UserAccount();
        account.setFirstName("FirstName" + randomInt());
        account.setLastName("LastName" + randomInt());
        account.setUsername("user" + randomInt() + "@kbss.felk.cvut.cz");
        account.setUri(Generator.generateUri());
        return account;
    }

    /**
     * Generates a random {@link UserAccount} instance, initialized with first name, last name, username, password and
     * identifier.
     *
     * @return A new {@code UserAccount} instance
     * @see #generateUserAccount()
     */
    public static UserAccount generateUserAccountWithPassword() {
        final UserAccount account = generateUserAccount();
        account.setPassword("Pass" + randomInt(0, 10000));
        return account;
    }

    /**
     * Generates a {@link cz.cvut.kbss.termit.model.Vocabulary} instance with a name, an empty glossary and a model.
     *
     * @return New {@code Vocabulary} instance
     */
    public static cz.cvut.kbss.termit.model.Vocabulary generateVocabulary() {
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = new cz.cvut.kbss.termit.model.Vocabulary();
        vocabulary.setGlossary(new Glossary());
        vocabulary.setModel(new Model());
        vocabulary.setName("Vocabulary" + randomInt());
        return vocabulary;
    }

    public static Term generateTerm() {
        final Term term = new Term();
        term.setLabel("Term" + randomInt());
        term.setComment("Comment" + randomInt());
        return term;
    }

    public static Term generateTermWithId() {
        final Term term = generateTerm();
        term.setUri(Generator.generateUri());
        return term;
    }

    public static Resource generateResource() {
        final Resource resource = new Resource();
        resource.setName("Resource " + randomInt());
        resource.setDescription("Resource description ");
        return resource;
    }

    public static Resource generateResourceWithId() {
        final Resource resource = generateResource();
        resource.setUri(Generator.generateUri());
        return resource;
    }

    public static Target generateTargetWithId() {
        final Target target = new Target();
        target.setUri(Generator.generateUri());
        return target;
    }

    public static TermAssignment generateTermAssignmentWithId() {
        final TermAssignment termAssignment = new TermAssignment();
        termAssignment.setUri(Generator.generateUri());
        return termAssignment;
    }
}
