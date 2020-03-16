package cz.cvut.kbss.termit.persistence.dao.changetracking;

import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Objects;

/**
 * Determines repository context into which change tracking records are stored.
 */
@Component
public class ChangeTrackingContextResolver {

    private final String contextExtension;

    @Autowired
    public ChangeTrackingContextResolver(Configuration config) {
        this.contextExtension = config.get(ConfigParam.CHANGE_TRACKING_CONTEXT_EXTENSION);
    }

    /**
     * Resolves change tracking context of the specified changed asset.
     * <p>
     * In general, each vocabulary has its own change tracking context, so changes to it and all its terms are stored in
     * this context.
     *
     * @param changedAsset Asset for which change records will be generated
     * @return Identifier of the change tracking context of the specified asset
     */
    public URI resolveChangeTrackingContext(Asset changedAsset) {
        Objects.requireNonNull(changedAsset);
        if (changedAsset instanceof Vocabulary) {
            return URI.create(changedAsset.getUri().toString().concat(contextExtension));
        } else if (changedAsset instanceof Term) {
            return URI.create(((Term) changedAsset).getVocabulary().toString().concat(contextExtension));
        }
        return URI.create(changedAsset.getUri().toString().concat(contextExtension));
    }
}
