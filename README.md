# TermIt

TermIt is a terminology management tool based on Semantic Web technologies.
It allows to manage vocabularies consisting of thesauri and ontologies. It can also manage documents
which use terms from the vocabularies and analyze the documents to find occurrences of these terms.

## Terminology

#### Asset

An **asset** is an object of one of the main domain types managed by the system - _Resource_, _Term_ or _Vocabulary_.

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

### REST Method Annotations

Started to switch to HTTP method-specific annotation shortcuts, e.g., instead of `@RequestMapping(method=RequestMethod.GET)`,
we should use `@GetMapping`. It should make the REST controller code a bit more concise.

### jsoup

Had to switch from standard Java DOM implementation to **jsoup** because DOM had sometimes trouble parsing HTML documents (`meta` tags in header).
Jsoup, on the other hand, handles HTML natively and should be able to work with XML (if need be) as well.

### Validation

The application uses JSR 380 validation API. This provides a generic, easy-to-use API for bean validation based on annotations.
Use it to verify input data. See `User` and its validation in `BaseRepositoryService`/`UserRepositoryService`.
`ValidationException` is then handled by `RestExceptionHandler` and an appropriate response is returned to the client.

### Storage

TermIt is preconfigured to run against a local GraphDB repository at `http://locahost:7200/repositories/termit`.
This can be changed by updating `config.properties`.

### GraphDB Ruleset

_Applies when running against a GraphDB repository._

In order to support inference used by the application, a custom ruleset has to be specified for the TermIt repository. This
ruleset is available in `rulesets/rulest-termit-graphdb.pie`.

### RDF4J SPIN Rules

_Applies when running against an RDF4J repository._

In order to support the inference used by the application, new rules need to be added to RDF4J because its own RDFS rule engine does not
support OWL stuff like inverse properties (which are used in the model). Thus, when creating a new repository, a store 
with **RDFS+SPIN support** should be selected (if proper search should be supported as well, a **RDFS+SPIN with Lucene support** should be selected). 
Then, rules contained in `rulesets/rules-termit-spin.ttl` should be added to the repository, 
as described by the [RDF4J documentation](http://docs.rdf4j.org/programming/#_adding_rules).

### Loading Ontologies into Repository

TermIt needs the repository to provide some inference. Besides loading the appropriate rulesets (see above), it is also
necessary to load the ontological models into the repository.

First model to load is the TermIt model itself. It can be found in `ontology/termit-model.ttl`. The other necessary model
is the _popis dat_ model. It is available online at 
[http://onto.fel.cvut.cz/ontologies/slovnik/agendovy/popis-dat/model](http://onto.fel.cvut.cz/ontologies/slovnik/agendovy/popis-dat/model).
With these two models loaded, the repository should provide the inference services required by TermIt.


### User vs UserAccount
`User` is a domain class used for domain functions, mostly for resource provenance (author, last editor). It does not support password.
 `UserAccount` is used for security-related functions and supports password. Most parts of the application **should** use
 `User`.

### JMX

A JMX bean called `AppAdminBean` was added to the application. Currently, it supports invalidation of application caches.
The bean's name is set during Maven build. In case multiple deployments of TermIt are running on the same application server,
it is necessary to provide different names for it. A Maven property with default value _DEV_ was introduced for it. To specify
a different value, pass a command line parameter to Maven, so the build call might look as follows:

`mvn clean package -B -P production "-Ddeployment=DEV"`

### Fulltext Search

Fulltext search currently supports multiple types of implementation:

* Simple substring matching on term and vocabulary label _(default)_
* RDF4J with Lucene SAIL
* GraphDB with Lucene connector using czech analyzer

Each implementation has its own search query which is loaded and used by `SearchDao`. In order for the more advanced implementations
for Lucene to work, a corresponding Maven profile (**graphdb**, **rdf4j**) has to be selected. This inserts the correct query into the resulting
artifact during build. If none of the profiles is selected, the default search is used.

Note that in case of GraphDB, a corresponding Lucene connector has to be created as well.

### RDFS Inference in Tests

The test in-memory repository is configured to be a SPIN SAIL with RDFS inferencing engine. Thus, basically all the inference features available
in production are available in tests as well. However, the repository is by default left empty (without the model or SPIN rules) to facilitate test
performance (inference in RDF4J is really slow). To load the TermIt model into the repository and thus enable RDFS inference, call the `enableRdfsInference`
method available on both `BaseDaoTestRunner` and `BaseServiceTestRunner`. SPIN rules are currently not loaded as they don't seem to be used by any tests.

## Ontology

The ontology on which TermIt is based can be found in the `ontology` folder. For proper inference functionality, `termit-model.ttl` and the 
_popis-dat_ ontology model (http://onto.fel.cvut.cz/ontologies/slovnik/agendovy/popis-dat/model) need to be loaded into the repository 
used by TermIt.

## Monitoring

We are using [JavaMelody](https://github.com/javamelody/javamelody) for monitoring the application and its usage. The data are available
on the `/monitoring` endpoint and are secured using _basic_ authentication, see `SecurityConstants` for credentials.

## Documentation

TermIt REST API is documented on [SwaggerHub](https://app.swaggerhub.com/apis/ledvima1/TermIt/) under the appropriate version.
