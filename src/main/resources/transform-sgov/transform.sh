#!/usr/bin/env bash
RDF4J_ENDPOINT=http://onto.fel.cvut.cz:7200/repositories/termit-dev
DIR="/home/michal/modelio/workspace/IPR Praha/"
IFS=
FILE=tmp.rq

function runUpdateQuery() {
    cp $1 $FILE
    sed -i -e "s!__VOC_IRI__!$2!g" $FILE
    curl --netrc-file .netrc -X POST -H "Content-type: application/sparql-update" -T $FILE $RDF4J_ENDPOINT/statements
}

function transform() {
    VOC_IRI=$1
    PREFIX=$2

    URL="$RDF4J_ENDPOINT/rdf-graphs/service?graph=$VOC_IRI"
    curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-model.ttl" $URL
    curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-glosář.ttl" $URL
    curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-slovník.ttl" $URL
    curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-diagram.ttl" $URL

    runUpdateQuery transform-2.rq $VOC_IRI
    runUpdateQuery transform-4.rq $VOC_IRI
    runUpdateQuery transform-1.rq $VOC_IRI
    runUpdateQuery transform-3.rq $VOC_IRI
}

URL="$RDF4J_ENDPOINT/rdf-graphs/service?graph=http://onto.fel.cvut.cz/ontologies/termit"
TERMIT=$(pwd)
curl -X POST -H "Content-type: text/turtle" -T "$TERMIT/../../../../ontology/termit-model.ttl" $URL

transform http://onto.fel.cvut.cz/ontologies/slovnik/legislativni-sbirka-2006-183 l-sgov-183-2006
transform http://onto.fel.cvut.cz/ontologies/slovnik/legislativni-sbirka-2006-500 l-sgov-500-2006
transform http://onto.fel.cvut.cz/ontologies/slovnik/legislativni-sbirka-2006-501 l-sgov-501-2006
transform http://onto.fel.cvut.cz/ontologies/slovnik/datovy-psp-2016 psp-2016
transform http://onto.fel.cvut.cz/ontologies/slovnik/datovy-mpp-3.4 mpp-3.4
