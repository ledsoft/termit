package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.PropertyMockingApplicationContextInitializer;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration(initializers = PropertyMockingApplicationContextInitializer.class)
class SystemInitializerTest extends BaseServiceTestRunner {

    private static final URI ADMIN_URI = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/system-admin-user");

    @Autowired
    private Environment environment;

    @Autowired
    private Configuration config;

    @Autowired
    private UserRepositoryService userService;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminCredentialsDir;

    private SystemInitializer sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        // Randomize admin credentials folder
        this.adminCredentialsDir =
                System.getProperty("java.io.tmpdir") + File.separator + Integer.toString(Generator.randomInt(0, 10000));
        ((MockEnvironment) environment)
                .setProperty(ConfigParam.ADMIN_CREDENTIALS_LOCATION.toString(), adminCredentialsDir);
        this.sut = new SystemInitializer(config, userService, txManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        final File dir = new File(adminCredentialsDir);
        if (dir.listFiles() != null) {
            for (File child : dir.listFiles()) {
                Files.deleteIfExists(child.toPath());
            }
        }
        Files.deleteIfExists(dir.toPath());
    }

    @Test
    void persistsSystemAdminWhenHeDoesNotExist() {
        sut.initSystemAdmin();
        assertNotNull(em.find(User.class, ADMIN_URI));
    }

    @Test
    void doesNotCreateNewAdminWhenOneAlreadyExists() {
        sut.initSystemAdmin();
        final User admin = em.find(User.class, ADMIN_URI);
        sut.initSystemAdmin();
        final User result = em.find(User.class, ADMIN_URI);
        // We know that password is generated, so the same password means no new instance was created
        assertEquals(admin.getPassword(), result.getPassword());
    }

    @Test
    void savesAdminLoginCredentialsIntoHiddenFileInUserHome() throws Exception {
        sut.initSystemAdmin();
        final User admin = em.find(User.class, ADMIN_URI);
        final String home = environment.getProperty(ConfigParam.ADMIN_CREDENTIALS_LOCATION.toString());
        final File credentialsFile = new File(home + File.separator + Constants.ADMIN_CREDENTIALS_FILE);
        assertTrue(credentialsFile.exists());
        assertTrue(credentialsFile.isHidden());
        final List<String> lines = Files.lines(credentialsFile.toPath()).collect(Collectors.toList());
        assertThat(lines.get(0), containsString(admin.getUsername() + "/"));
        final String password = lines.get(0).substring(lines.get(0).indexOf('/') + 1);
        assertTrue(passwordEncoder.matches(password, admin.getPassword()));
    }
}