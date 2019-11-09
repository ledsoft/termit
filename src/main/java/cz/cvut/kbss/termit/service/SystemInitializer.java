package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Random;

public class SystemInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(SystemInitializer.class);

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final int PASSWORD_LENGTH = 8;

    private final Configuration config;
    private final UserRepositoryService userService;
    private final PlatformTransactionManager txManager;

    @Autowired
    public SystemInitializer(Configuration config, UserRepositoryService userService,
                             PlatformTransactionManager txManager) {
        this.config = config;
        this.userService = userService;
        this.txManager = txManager;
    }

    @PostConstruct
    void initSystemAdmin() {    // Package-private for testing purposes
        final UserAccount admin = initAdminInstance();
        final String passwordPlain = generatePassword();
        admin.setPassword(passwordPlain);
        if (userService.exists(admin.getUri())) {
            LOG.info("Admin already exists.");
            return;
        }
        LOG.info("Creating application admin account.");
        final TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.execute(status -> {
            userService.persist(admin);
            return null;
        });
        LOG.info("----------------------------------------------");
        LOG.info("Admin credentials are: {}/{}", admin.getUsername(), passwordPlain);
        LOG.info("----------------------------------------------");
        final File directory = new File(config.get(ConfigParam.ADMIN_CREDENTIALS_LOCATION));
        if (!directory.exists()) {
            final boolean result = directory.mkdirs();
            if (!result) {
                LOG.error(
                        "Unable to create admin credentials directory {} or one of its ancestor directories. Admin credentials won't be saved in any file!",
                        config.get(ConfigParam.ADMIN_CREDENTIALS_LOCATION));
                return;
            }
        }
        try {
            final File credentialsFile = createHiddenFile();
            if (credentialsFile == null) {
                return;
            }
            LOG.debug("Writing admin credentials into file: {}", credentialsFile);
            Files.write(credentialsFile.toPath(),
                    Collections.singletonList(admin.getUsername() + "/" + passwordPlain),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOG.error("Unable to create admin credentials file.", e);
        }
    }

    private File createHiddenFile() throws IOException {
        final File credentialsFile = new File(config.get(ConfigParam.ADMIN_CREDENTIALS_LOCATION) + File.separator +
                config.get(ConfigParam.ADMIN_CREDENTIALS_FILE));
        final boolean result = credentialsFile.createNewFile();
        if (!result) {
            LOG.error("Unable to create admin credentials file {}. Admin credentials won't be saved in any file!",
                    config.get(ConfigParam.ADMIN_CREDENTIALS_FILE));
            return null;
        }
        // Hidden attribute on Windows
        Files.setAttribute(credentialsFile.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
        return credentialsFile;
    }

    private static UserAccount initAdminInstance() {
        final UserAccount admin = new UserAccount();
        admin.setUri(URI.create(Vocabulary.ONTOLOGY_IRI_slovnik + "/system-admin-user"));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setUsername("termit-admin@kbss.felk.cvut.cz");
        admin.addType(Vocabulary.s_c_administrator_termitu);
        return admin;
    }

    private static String generatePassword() {
        final StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        final Random random = new Random();
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            if (random.nextBoolean()) {
                sb.append(random.nextInt(10));
            } else {
                char c = LETTERS.charAt(random.nextInt(LETTERS.length()));
                sb.append(random.nextBoolean() ? c : Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }
}
