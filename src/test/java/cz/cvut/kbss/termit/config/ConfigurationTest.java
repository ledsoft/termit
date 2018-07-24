package cz.cvut.kbss.termit.config;

import cz.cvut.kbss.ontodriver.sesame.SesameDataSource;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

    private MockEnvironment env;

    private Configuration sut;

    @BeforeEach
    void setUp() {
        this.env = new MockEnvironment();
        this.sut = new Configuration(env);
    }

    @Test
    void getReturnsMatchingPropertyValue() {
        final String driver = SesameDataSource.class.getName();
        env.setProperty(ConfigParam.DRIVER.toString(), driver);
        assertEquals(driver, sut.get(ConfigParam.DRIVER));
    }

    @Test
    void getReturnsPredefinedDefaultValueWhenItExists() {
        assertEquals(Constants.DEFAULT_LANGUAGE, sut.get(ConfigParam.LANGUAGE));
    }

    @Test
    void getThrowsIllegalStateExceptionWhenPropertyIsNotConfiguredAndHasNoDefault() {
        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> sut.get(ConfigParam.DRIVER));
        assertEquals("Value of key \'" + ConfigParam.DRIVER + "\' not configured.", ex.getMessage());
    }

    @Test
    void getWithDefaultReturnsMatchingPropertyValue() {
        final String driver = SesameDataSource.class.getName();
        env.setProperty(ConfigParam.DRIVER.toString(), driver);
        assertEquals(driver, sut.get(ConfigParam.DRIVER, "default"));
    }

    @Test
    void getWithDefaultReturnsDefaultValueWhenPropertyIsNotConfigured() {
        final String driver = SesameDataSource.class.getName();
        assertEquals(driver, sut.get(ConfigParam.DRIVER, driver));
    }

    @Test
    void containsReturnsTrueWhenPropertyIsConfigured() {
        final String driver = SesameDataSource.class.getName();
        env.setProperty(ConfigParam.DRIVER.toString(), driver);
        assertTrue(sut.contains(ConfigParam.DRIVER));
    }

    @Test
    void containsReturnsFalseWhenPropertyIsNotConfigured() {
        assertFalse(sut.contains(ConfigParam.DRIVER));
    }

    @Test
    void isReturnsTrueWhenPropertyIsSetToTrue() {
        // We're abusing the DRIVER parameter here a little, since it normally expects a string, not a boolean
        env.setProperty(ConfigParam.DRIVER.toString(), Boolean.TRUE.toString());
        assertTrue(sut.is(ConfigParam.DRIVER));
    }

    @Test
    void isReturnsFalseWhenPropertyIsNotConfigured() {
        assertFalse(sut.is(ConfigParam.DRIVER));
    }
}