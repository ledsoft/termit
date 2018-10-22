package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.service.language.LanguageService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

class LanguageServiceTest extends BaseServiceTestRunner {

    @Autowired
    private LanguageService sut;

    @Test
    void getTypesForBasicLanguage(){
        List<Term> result = sut.getTypesForLang("en");
        assertEquals(10,result.size());
    }
}