#!/usr/bin/env bash
RDF4J_ENDPOINT=http://onto.fel.cvut.cz:7200/repositories/termit-dev
DIR="/home/michal/modelio/workspace/IPR Praha/"
IFS=
FILE=tmp.rq
CLEAR_INITIAL_DATA=false

function runUpdateQuery() {
    cp $1 $FILE
    sed -i -e "s!__VOC_IRI__!$2!g" $FILE
    curl --netrc-file .netrc -X POST -H "Content-type: application/sparql-update" -T $FILE $RDF4J_ENDPOINT/statements
}

function transform() {
    VOC_IRI=$1
    PREFIX=$2

    if $CLEAR_INITIAL_DATA; then
	    runUpdateQuery transform-clear.rq $VOC_IRI
    fi

    curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-model.ttl" -G --data-urlencode "graph=$VOC_IRI" $RDF4J_ENDPOINT/rdf-graphs/service
    curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-glosář.ttl" -G --data-urlencode "graph=$VOC_IRI" $RDF4J_ENDPOINT/rdf-graphs/service
    curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-slovník.ttl" -G --data-urlencode "graph=$VOC_IRI" $RDF4J_ENDPOINT/rdf-graphs/service
    curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$DIR/$PREFIX-diagram.ttl" -G --data-urlencode "graph=$VOC_IRI" $RDF4J_ENDPOINT/rdf-graphs/service

    runUpdateQuery transform-2.rq $VOC_IRI
    runUpdateQuery transform-4.rq $VOC_IRI
    runUpdateQuery transform-1.rq $VOC_IRI
    runUpdateQuery transform-5.rq $VOC_IRI
    runUpdateQuery transform-diagram.rq $VOC_IRI
}

URL="$RDF4J_ENDPOINT/rdf-graphs/service?graph=http://onto.fel.cvut.cz/ontologies/termit"
TERMIT=$(pwd)
curl --netrc-file .netrc -X POST -H "Content-type: text/turtle" -T "$TERMIT/../../../../ontology/termit-model.ttl" $URL

transform http://onto.fel.cvut.cz/ontologies/legislativní/sbírka/183/2006 l-sgov-183-2006
transform http://onto.fel.cvut.cz/ontologies/legislativní/sbírka/500/2006 l-sgov-500-2006
transform http://onto.fel.cvut.cz/ontologies/legislativní/sbírka/501/2006 l-sgov-501-2006
transform http://onto.fel.cvut.cz/ontologies/slovník/datový/psp-2016 psp-2016
transform http://onto.fel.cvut.cz/ontologies/slovník/datový/mpp-3.5-np mpp-3.5-np
