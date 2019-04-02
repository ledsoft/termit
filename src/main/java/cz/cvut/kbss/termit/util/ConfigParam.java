/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
     * Namespace for document identifiers.
     *
     * @deprecated This should be superseded by {@link #NAMESPACE_RESOURCE}
     */
    @Deprecated
    NAMESPACE_DOCUMENT("namespace.document"),

    /**
     * Namespace for resource identifiers.
     */
    NAMESPACE_RESOURCE("namespace.resource"),

    /**
     * URL of the text analysis service.
     */
    TEXT_ANALYSIS_SERVICE_URL("textAnalysis.url"),

    /**
     * Specifies folder in which admin credentials are stored when his account is generated.
     */
    ADMIN_CREDENTIALS_LOCATION("admin.credentialsLocation"),

    /**
     * Specifies root directory in which document files are stored.
     */
    FILE_STORAGE("file.storage");

    private final String parameter;

    ConfigParam(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return parameter;
    }
}
