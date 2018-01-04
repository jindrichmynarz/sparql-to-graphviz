# sparql-to-graphviz

When you find unknown data in a SPARQL endpoint, do you also go:

```sparql
SELECT DISTINCT ?class
WHERE {
  [] a ?class .
}
```

Or:

```sparql
SELECT DISTINCT ?property
WHERE {
  [] ?property [] .
}
```

Yeah, me too.

This tools helps you to explore an unfamiliar dataset in a SPARQL endpoint, optionally restricted to a named graph. It produces a class diagram in the [DOT language](http://www.graphviz.org/doc/info/lang.html), which can be turned into images by [Graphviz](http://www.graphviz.org). The class diagram shows an *empirical schema* of the explored dataset. The schema reflects the structure of instance data in terms of its vocabularies. 

If you want a more feature-rich and interactive visualization, give [LODSight](http://lod2-dev.vse.cz/lodsight-v2) a try.

Warning: The tool can also hurt your feelings by showing what a mess your data is. 

## Usage

Compile using [Leiningen](http://leiningen.org): 

```sh
git clone https://github.com/jindrichmynarz/sparql-to-graphviz.git
cd sparql-to-graphviz
lein bin
```

Observe the offered parameters:

```sh
target/sparql_to_graphviz --help
```

Find out what is hiding in your local RDF store:

```sh
target/sparql_to_graphviz -e http://localhost:8890/sparql |
  neato -Tsvg -Gepsilon=.001 -o localhost.svg
```

Give it some time, while it runs a boatload of SPARQL queries. When it finishes, `localhost.svg` should contain something beautiful. Exporting to SVG also allows you to make the visualization fancier using vector graphics tools.

For example, the resulting mess can look like this:

![Example diagram](https://github.com/jindrichmynarz/sparql-to-graphviz/blob/master/resources/vvz.png)

## Caveats

* If there are no instantiated classes, the program fails.
* Classes identified with blank nodes are ignored. 

## License

Copyright © 2017 Jindřich Mynarz

Distributed under the Eclipse Public License version 1.0.
