# How to transform Modelio models to TermIt repository

This procedure applies to the commit d21a984044579afbf66b21db357d7166419bfddd of OntoUmlModelio Plugin that can be found at https://kbss.felk.cvut.cz/gitblit/r/ontouml-modelio

1. In Modelio, select a vocabulary and in the contextual menu choose 'OntoUml+ Module' and execute the 'Export Vocabulary to Compact OWL'.
2. Check the console to see if the transformation was successful. If yes, you should see in console an output similar to
    <pre>
    23:06:32.187 INFO  o.m.api.impl     - OntoUmlModule - [SGoV]  	- saving glossary
    23:06:32.187 INFO  o.m.api.impl     - OntoUmlModule - 	- SOME_PATH/VOCABULARY_PREFIX-glosář.ttl
    23:06:32.199 INFO  o.m.api.impl     - OntoUmlModule - [SGoV]  	- saving model
    23:06:32.199 INFO  o.m.api.impl     - OntoUmlModule - 	- SOME_PATH/VOCABULARY_PREFIX-model.ttl
    23:06:32.200 INFO  o.m.api.impl     - OntoUmlModule - [SGoV]  	- saving vocabulary
    23:06:32.201 INFO  o.m.api.impl     - OntoUmlModule - 	- SOME_PATH/VOCABULARY_PREFIX-slovník.ttl
    </pre>

3. Edit the <code>transform.sh</code> script and adjust the variables <code>RDF4J_ENDPOINT</code> and <code>DIR</code> accordingly. Also, uncomment/change
4. Edit the <code>transform.sh</code> script  and select vocabularies you want to transform. E.g. <code>transform http://onto.fel.cvut.cz/ontologies/slovnik/datovy-mpp-3.4 mpp-3.4</code>. The first argument to <code>transform</code> represents the context to which the vocabulary should be uploaded. This context must be the same as the IRI of the vocabulary without the <code>/slovnik</code> suffix.
5. Before running the script, make sure that the contexts you want to post the data to are empty to prevent mixup of the data.
6. Run the script
