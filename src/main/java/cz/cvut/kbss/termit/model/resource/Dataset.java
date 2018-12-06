package cz.cvut.kbss.termit.model.resource;

import cz.cvut.kbss.jopa.model.annotations.EntityListeners;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.termit.service.ProvenanceGenerator;
import cz.cvut.kbss.termit.util.Vocabulary;

@OWLClass(iri = Vocabulary.s_c_dataset)
@EntityListeners(ProvenanceGenerator.class)
public class Dataset extends Resource {
}
