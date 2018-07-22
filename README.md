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
- JSON Web Tokens*
- SLF4J + Logback
- CSRF* (was disabled in RT because of Liferay), CORS* (for separate frontend)

_* Technology not used in INBAS RT_

## Implementation Notes

We are using `basePackageClasses` instead of `basePackages` in `ComponentScan`. This is more resilient to refactoring errors 
because it uses classes instead of String-based package info. Thus, any errors are discovered during compilation.

## TODO

- __CONSIDER__: OAuth for authentication
