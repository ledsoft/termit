#!/usr/bin/env bash
RDF4J_ENDPOINT=http://onto.fel.cvut.cz:7200/repositories/termit-stage
DIR="/home/kremep1/fel/projects/17mvcr/semanticky-slovnik-pojmu/model/IPR Praha/"
IFS=

function transform() {
    VOC_IRI=$1
    PREFIX=$2

    URL="$RDF4J_ENDPOINT/rdf-graphs/service?graph=$VOC_IRI"
    curl -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-model.ttl" $URL
    curl -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-glosář.ttl" $URL
    curl -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-slovník.ttl" $URL

    FILE=tmp.rq
    cp transform-1.rq $FILE
    sed -i -e "s!__VOC_IRI__!$VOC_IRI!g" $FILE
    curl -X POST -H "Content-type: application/sparql-update" -T $FILE $RDF4J_ENDPOINT/statements

    cp transform-2.rq $FILE
    sed -i -e "s!__VOC_IRI__!$VOC_IRI!g" $FILE
    curl -X POST -H "Content-type: application/sparql-update" -T $FILE $RDF4J_ENDPOINT/statements

    cp transform-3.rq $FILE
    sed -i -e "s!__VOC_IRI__!$VOC_IRI!g" $FILE
    curl -X POST -H "Content-type: application/sparql-update" -T $FILE $RDF4J_ENDPOINT/statements
}

# transform http://onto.fel.cvut.cz/ontologies/slovnik/legislativni-sbirka-2006-183 l-sgov-183-2006
# transform http://onto.fel.cvut.cz/ontologies/slovnik/legislativni-sbirka-2006-500 l-sgov-500-2006
# transform http://onto.fel.cvut.cz/ontologies/slovnik/legislativni-sbirka-2006-501 l-sgov-501-2006
# transform http://onto.fel.cvut.cz/ontologies/slovnik/datovy-psp-2016 psp-2016
# transform http://onto.fel.cvut.cz/ontologies/slovnik/datovy-mpp-3.4 mpp-3.4