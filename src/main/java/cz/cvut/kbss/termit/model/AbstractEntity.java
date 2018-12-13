package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.Id;
import cz.cvut.kbss.jopa.model.annotations.MappedSuperclass;
import cz.cvut.kbss.termit.model.util.HasIdentifier;

import java.io.Serializable;
import java.net.URI;

@MappedSuperclass
public abstract class AbstractEntity implements HasIdentifier, Serializable {

    @Id(generated = true)
    private URI uri;

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return "uri=<" + uri + '>';
    }
}
