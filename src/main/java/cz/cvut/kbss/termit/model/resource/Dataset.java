package cz.cvut.kbss.termit.model.resource;

import cz.cvut.kbss.jopa.model.annotations.EntityListeners;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.asset.provenance.ProvenanceManager;
import cz.cvut.kbss.termit.util.Vocabulary;

@OWLClass(iri = Vocabulary.s_c_dataset)
@EntityListeners(ProvenanceManager.class)
@JsonLdAttributeOrder({"uri", "label", "description", "author", "lastEditor"})
public class Dataset extends Resource {
}
