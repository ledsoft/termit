package cz.cvut.kbss.termit.config;

import cz.cvut.kbss.termit.rest.Rest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {Rest.class})
public class RestConfig {
}
