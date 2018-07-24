package cz.cvut.kbss.termit.config;

import cz.cvut.kbss.termit.util.ConfigParam;
import org.springframework.core.env.Environment;

import java.util.Objects;

/**
 * Represents application-wide configuration.
 */
public class Configuration {

    private final Environment environment;

    public Configuration(Environment environment) {
        this.environment = environment;
    }

    /**
     * Gets the value of the specified parameter.
     *
     * @param param Parameter
     * @return Value of the parameter
     * @throws IllegalStateException If the parameter value is not set
     */
    public String get(ConfigParam param) {
        Objects.requireNonNull(param);
        if (!environment.containsProperty(param.toString())) {
            throw new IllegalStateException("Value of key " + param + " not configured.");
        }
        return environment.getProperty(param.toString());
    }

    /**
     * Gets the value of the specified parameter.
     *
     * @param param        Parameter
     * @param defaultValue Value to return if the parameter is not set
     * @return Value of the parameter or the default value
     */
    public String get(ConfigParam param, String defaultValue) {
        Objects.requireNonNull(param);
        return environment.getProperty(param.toString(), defaultValue);
    }

    /**
     * Checks whether the configuration contains the specified parameter.
     *
     * @param param Parameter
     * @return Whether the specified parameter is configured
     */
    public boolean contains(ConfigParam param) {
        Objects.requireNonNull(param);
        return environment.containsProperty(param.toString());
    }

    /**
     * Checks whether the specified parameter is set to {@code true}.
     * <p>
     * If the parameter value is not configured, {@code false} is returned.
     *
     * @param param Parameter
     * @return Boolean value of the parameter
     */
    public boolean is(ConfigParam param) {
        Objects.requireNonNull(param);
        return Boolean.parseBoolean(get(param, Boolean.FALSE.toString()));
    }
}
