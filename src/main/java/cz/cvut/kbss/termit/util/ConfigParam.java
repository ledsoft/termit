package cz.cvut.kbss.termit.util;

/**
 * Application configuration parameters, loaded from {@code config.properties} provided on classpath.
 */
public enum ConfigParam {

    /**
     * URL of the main application repository.
     */
    REPOSITORY_URL("repositoryUrl"),

    /**
     * OntoDriver class for the repository.
     */
    DRIVER("driver"),

    /**
     * Language used to store texts in the repository (persistence unit language).
     */
    LANGUAGE("language"),

    /**
     * Username for connecting to the application repository.
     */
    REPO_USERNAME("repositoryUsername"),

    /**
     * Password for connecting to the application repository.
     */
    REPO_PASSWORD("repositoryPassword");

    private final String parameter;

    ConfigParam(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return parameter;
    }
}
