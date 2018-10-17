package cz.cvut.kbss.termit.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Application-wide constants.
 */
public class Constants {

    /**
     * URL path to the application's REST API.
     */
    public static final String REST_MAPPING_PATH = "/rest";

    /**
     * UTF-8 encoding identifier.
     */
    public static final String UTF_8_ENCODING = "UTF-8";

    /**
     * Temporary location where uploaded files will be stored.
     */
    public static final String UPLOADED_FILE_LOCATION = "/tmp/";

    /**
     * Max uploaded file size. Currently 10MB.
     */
    public static final long MAX_UPLOADED_FILE_SIZE = 10L * 1024 * 1024;

    /**
     * Total request size containing Multi part. 20MB.
     */
    public static final long MAX_UPLOAD_REQUEST_SIZE = 20L * 1024 * 1024;

    /**
     * Size threshold after which files will be written to disk.
     */
    public static final int UPLOADED_FILE_SIZE_THRESHOLD = 0;

    /**
     * Version of this application.
     * <p>
     * Injected by Maven during production build.
     */
    public static final String VERSION = "$VERSION$";

    /**
     * Default persistence unit language.
     */
    public static final String DEFAULT_LANGUAGE = "en";

    /**
     * Name of the file in which admin credentials are stored.
     */
    public static final String ADMIN_CREDENTIALS_FILE = ".termit-admin";

    /**
     * Default page specification, corresponding to a find all query with no page specification.
     * <p>
     * I.e., the request asks for the first page (number = 0) and its size is {@link Integer#MAX_VALUE}.
     */
    public static final Pageable DEFAULT_PAGE_SPEC = PageRequest.of(0, Integer.MAX_VALUE);

    /**
     * Piece of text appended to vocabulary IRI for term IRI generation.
     * <p>
     * For example, if we have a vocabulary with IRI {@code http://www.example.org/ontologies/vocabularies/metropolitan-plan}
     * and a term with label {@code Inhabited area}, the resulting IRI will be {@code
     * http://www.example.org/ontologies/vocabularies/metropolitan-plan/SEPARATOR/inhabited-area}, where 'SEPARATOR' is
     * this constant.
     */
    public static final String TERM_NAMESPACE_SEPARATOR = "/pojem";

    private Constants() {
        throw new AssertionError();
    }

    /**
     * Constants from the RDFa vocabulary.
     */
    public static final class RDFa {

        /**
         * RDFa property attribute.
         */
        public static final String PROPERTY = "property";

        /**
         * RDFa context identifier attribute.
         */
        public static final String ABOUT = "about";

        /**
         * RDFa content attribute.
         */
        public static final String CONTENT = "content";

        /**
         * RDFa type identifier attribute.
         */
        public static final String TYPE = "typeof";

        /**
         * RDFa resource identifier.
         */
        public static final String RESOURCE = "resource";

        /**
         * RDFa prefix attribute.
         */
        public static final String PREFIX = "prefix";

        private RDFa() {
            throw new AssertionError();
        }
    }
}
