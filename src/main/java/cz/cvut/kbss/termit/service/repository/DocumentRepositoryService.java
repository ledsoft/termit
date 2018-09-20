package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.persistence.dao.DocumentDao;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
public class DocumentRepositoryService extends BaseRepositoryService<Document> {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentRepositoryService.class);

    private final DocumentDao dao;

    private final Configuration config;

    @Autowired
    public DocumentRepositoryService(DocumentDao dao, Configuration config, Validator validator) {
        super(validator);
        this.dao = dao;
        this.config = config;
    }

    @Override
    protected GenericDao<Document> getPrimaryDao() {
        return dao;
    }

    /**
     * Resolves the actual file stored on the file system which is represented by the specified {@code file} in the
     * specified logical {@code document}.
     *
     * @param document Document containing the file. Used for path resolution
     * @param file     File representing the file system item
     * @return IO file handle
     * @throws NotFoundException If the file cannot be found
     */
    public java.io.File resolveFile(Document document, File file) {
        final String path =
                config.get(ConfigParam.FILE_STORAGE) + java.io.File.separator + document.getFileDirectoryName() +
                        java.io.File.separator + file.getFileName();
        final java.io.File result = new java.io.File(path);
        if (!result.exists()) {
            LOG.error("File {} not found at location {}.", file, path);
            throw new NotFoundException("File " + file + " from document " + document + " not found on file system.");
        }
        return result;
    }
}
