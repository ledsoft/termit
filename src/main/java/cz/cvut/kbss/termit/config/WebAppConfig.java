package cz.cvut.kbss.termit.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.jsonld.ConfigParam;
import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.jsonld.jackson.JsonLdModule;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.AdjustedUriTemplateProxyServlet;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ServletWrappingController;

import static cz.cvut.kbss.termit.util.ConfigParam.REPOSITORY_URL;

@Configuration
@EnableWebMvc
@EnableAsync
@Import({RestConfig.class, SecurityConfig.class})
public class WebAppConfig implements WebMvcConfigurer {

    @Autowired
    private cz.cvut.kbss.termit.util.Configuration config;

    @Bean(name = "multipartResolver")
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Bean(name = "objectMapper")
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean(name = "jsonLdMapper")
    public ObjectMapper jsonLdObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final JsonLdModule jsonLdModule = new JsonLdModule();
        jsonLdModule.configure(ConfigParam.SCAN_PACKAGE, "cz.cvut.kbss.termit");
        mapper.registerModule(jsonLdModule);
        return mapper;
    }

    /**
     * Register the proxy for SPARQL endpoint.
     * @return Returns the ServletWrappingController for the SPARQL endpoint.
     */
    @Bean(name = "sparqlEndpointProxyServlet")
    public ServletWrappingController sparqlEndpointController() throws Exception {
        ServletWrappingController controller = new ServletWrappingController();
        controller.setServletClass(AdjustedUriTemplateProxyServlet.class);
        controller.setBeanName("sparqlEndpointProxyServlet");
        final Properties p = new Properties();
        p.setProperty("targetUri",config.get(REPOSITORY_URL));
        p.setProperty("log","false");
        controller.setInitParameters(p);
        controller.afterPropertiesSet();
        return controller;
    }

    /**
     * @return Returns the SimpleUrlHandlerMapping.
     */
    @Bean
    public SimpleUrlHandlerMapping sparqlQueryControllerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        Properties urlProperties = new Properties();
        urlProperties.put("/query", "sparqlEndpointProxyServlet");
        mapping.setMappings(urlProperties);
        return mapping;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(createJsonLdMessageConverter());
        converters.add(createDefaultMessageConverter());
        final StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(Charset.forName(
                Constants.UTF_8_ENCODING));
        converters.add(stringConverter);
        converters.add(new ResourceHttpMessageConverter());
    }

    private HttpMessageConverter<?> createJsonLdMessageConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
                jsonLdObjectMapper());
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.valueOf(JsonLd.MEDIA_TYPE)));
        return converter;
    }

    private HttpMessageConverter<?> createDefaultMessageConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer matcher) {
        matcher.setUseSuffixPatternMatch(false);
    }
}
