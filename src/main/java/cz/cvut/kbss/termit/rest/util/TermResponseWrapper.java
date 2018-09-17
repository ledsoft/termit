package cz.cvut.kbss.termit.rest.util;

import cz.cvut.kbss.termit.model.Term;

import java.util.List;

public class TermResponseWrapper {

    private List<Term> terms;
    private List<String> errors;

    public TermResponseWrapper(List<Term> terms, List<String> errors) {
        this.terms = terms;
        this.errors = errors;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public List<String> getErrors() {
        return errors;
    }
}
