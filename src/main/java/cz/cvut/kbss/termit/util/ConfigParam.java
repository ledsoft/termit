package cz.cvut.kbss.termit.util;

/**
 * Application configuration parameters, loaded from {@code config.properties} provided on classpath.
 */
public enum ConfigParam {

    /**
     * URL of the main application repository.
     */
    REPOSITORY_URL("repository.url"),

    /**
     * OntoDriver class for the repository.
     */
    DRIVER("persistence.driver"),

    /**
     * Language used to store strings in the repository (persistence unit language).
     */
    LANGUAGE("persistence.language"),

    /**
     * Username for connecting to the application repository.
     */
    REPO_USERNAME("repository.username"),

    /**
     * Password for connecting to the application repository.
     */
    REPO_PASSWORD("repository.password"),

    /**
     * Secret key used when hashing a JWT.
     */
    JWT_SECRET_KEY("jwt.secretKey"),

    /**
     * Namespace for vocabulary identifiers.
     */
    NAMESPACE_VOCABULARY("namespace.vocabulary"),

    /**
     * Namespace for user identifiers.
     */
    NAMESPACE_USER("namespace.user"),

    /**
     * Namespace for resource identifiers.
     */
    NAMESPACE_RESOURCE("namespace.resource"),

    /**
     * Separator of Term namespace from the parent Vocabulary identifier.
     * <p>
     * Since Term identifier is given by the identifier of the Vocabulary it belongs to and its own normalized label,
     * this separator is used to (optionally) configure the Term identifier namespace.
     * <p>
     * For example, if we have a Vocabulary with IRI {@code http://www.example.org/ontologies/vocabularies/metropolitan-plan}
     * and a Term with normalized label {@code inhabited-area}, the resulting IRI will be {@code
     * http://www.example.org/ontologies/vocabularies/metropolitan-plan/SEPARATOR/inhabited-area}, where 'SEPARATOR' is
     * the value of this configuration parameter.
     * <p>
     * Defaults to {@link Constants#DEFAULT_TERM_NAMESPACE_SEPARATOR}.
     */
    TERM_NAMESPACE_SEPARATOR("namespace.term.separator"),

    /**
     * URL of the text analysis service.
     */
    TEXT_ANALYSIS_SERVICE_URL("textAnalysis.url"),

    /**
     * Specifies folder in which admin credentials are saved when his account is generated.
     *
     * @see #ADMIN_CREDENTIALS_FILE
     */
    ADMIN_CREDENTIALS_LOCATION("admin.credentialsLocation"),

    /**
     * Name of the file in which admin credentials are saved when his account is generated.
     * <p>
     * This file is stored in the {@link #ADMIN_CREDENTIALS_LOCATION}.
     *
     * @see #ADMIN_CREDENTIALS_LOCATION
     */
    ADMIN_CREDENTIALS_FILE("admin.credentialsFile"),

    /**
     * Specifies root directory in which document files are stored.
     */
    FILE_STORAGE("file.storage"),

    /**
     * Minimal match score of a term occurrence for which a term assignment should be automatically generated.
     * <p>
     * More specifically, when annotated file content is being processed, term occurrences with sufficient score will
     * cause creation of corresponding term assignments to the file.
     */
    TERM_ASSIGNMENT_MIN_SCORE("term.assignment.minScore");

    private final String parameter;

    ConfigParam(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return parameter;
    }
}
