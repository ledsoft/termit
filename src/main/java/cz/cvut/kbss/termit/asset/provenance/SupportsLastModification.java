package cz.cvut.kbss.termit.asset.provenance;

/**
 * Indicates that last modification date of assets is tracked by this class.
 */
public interface SupportsLastModification {

    /**
     * Gets timestamp of the last modification of assets managed by this class.
     *
     * @return Timestamp of last modification in millis since epoch
     */
    long getLastModified();

    default void refreshLastModified() {}
}
