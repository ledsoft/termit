package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;

import java.io.Serializable;
import java.net.URI;

@OWLClass(iri = cz.cvut.kbss.termit.util.Vocabulary.s_c_vocabulary)
public class Vocabulary implements Serializable {

    @Id
    private URI uri;

    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String name;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_has_glossary, cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Glossary glossary;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_has_model, cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Model model;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Glossary getGlossary() {
        return glossary;
    }

    public void setGlossary(Glossary glossary) {
        this.glossary = glossary;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "Vocabulary{" +
                "uri=" + uri +
                ", name='" + name + '\'' +
                ", glossary=" + glossary +
                '}';
    }
}
