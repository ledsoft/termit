# TermIt

TermIt is a terminology management tool based on Semantic Web technologies.
It allows to manage vocabularies consisting of thesauri and ontologies. It can also manage documents
which use terms from the vocabularies and analyze the documents to find occurrences of these terms.

## Required Technologies

- JDK 8 (preferably Oracle)
- Apache Maven 3.x
- Apache Tomcat 9 or newer (required by Servlet API 4)


## System Architecture

The system is split into two projects, __TermIt__ is the backend, __TermIt-UI__ represents the frontend.
Both projects are built separately and can run separately.


## Technologies

This section briefly lists the main technologies and principles used (or planned to be used) in the application.

- Spring framework 5, Spring Security, Spring Data (paging, filtering)
- Jackson 2.9
- JB4JSON-LD*
- JOPA
- JUnit 5* (RT used 4), Mockito 2* (RT used 1)
- Servlet API 4* (RT used 3.0.1)
- JSON Web Tokens* (CSRF protection not necessary for JWT)
- SLF4J + Logback
- CORS* (for separate frontend)
- Java bean validation (JSR 380)*

_* Technology not used in INBAS RT_

## Implementation Notes

### Bean Discovery

We are using `basePackageClasses` instead of `basePackages` in `ComponentScan`. This is more resilient to refactoring errors 
because it uses classes instead of String-based package info. Thus, any errors are discovered during compilation.

### jsoup

Had to switch from standard Java DOM implementation to **jsoup** because DOM had sometimes trouble parsing HTML documents (`meta` tags in header).
Jsoup, on the other hand, handles HTML natively and should be able to work with XML (if need be) as well.

### Validation

The application uses JSR 380 validation API. This provides a generic, easy-to-use API for bean validation based on annotations.
Use it to verify input data. See `User` and its validation in `BaseRepositoryService`/`UserRepositoryService`.
`ValidationException` is then handled by `RestExceptionHandler` and an appropriate response is returned to the client.

### Storage

TermIt is preconfigured to run against a local RDF4J repository at `http://locahost:18188/rdf4j-server/repositories/termit`.
This can be changed by updating `config.properties`.

### SPIN Rules

In order to support the inference used by the application, new rules need to be added to RDF4J because its own RDFS rule engine does not
support OWL stuff like inverse properties (which are used in the model). Thus, when creating a new repository, a store 
with **RDFS+SPIN support** should be selected. Then, rules contained in `rdf4j-spin-rules.ttl` should be added to the repository, 
as described by the [RDF4J documentation](http://docs.rdf4j.org/programming/#_adding_rules).

## TODO
