package cz.cvut.kbss.termit.model.util;

/**
 * Interface implemented by assets supporting storage of data on file system.
 */
public interface SupportsStorage {

    /**
     * Gets name of the directory where files related to this instance are stored.
     * <p>
     * The name consists of normalized name of this asset, appended with hash code of this document's URI.
     * <p>
     * Note that the full directory path consists of the configured storage directory ({@link
     * cz.cvut.kbss.termit.util.ConfigParam#FILE_STORAGE}) to which the asset-specific directory name is appended.
     *
     * @return Asset-specific directory name
     */
    String getDirectoryName();
}
