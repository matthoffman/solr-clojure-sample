Solr sample project
===================

This project uses [flux](https://github.com/mwmitchell/flux), Matt Mitchell's Solr client for Clojure. 
There are a few Solr clients in the Clojure world -- [Icarus](https://github.com/mattdeboard/Icarus), [Clojure-Solr](https://github.com/mikejs/clojure-solr), and [Solrclj](https://github.com/mlehman/solrclj) -- but Flux is the most recent and the only one that supports Solr 4.x out of the box.

It's designed as a quick way to get started, especially if you use IntelliJ IDEA and the [Cursive Clojure](http://cursiveclojure.com) plugin.

Step 0
------

If you don't have a Clojure setup that you like already, I recommend one of the following: 

1. If you're comfortable with Emacs, go ahead and use that. There's a lot of information about getting started with Emacs on [clojure-doc](http://clojure-doc.org/articles/tutorials/emacs.html). 

2. If you're not fully conversant in Emacs (and there's no shame in that), I recommend downloading the free Community Edition of [IntelliJ](http://www.jetbrains.com/idea/download/) and the [Cursive](http://cursiveclojure.com/eap.html) plugin. It's really nice. 
A lot of people also use Eclipse and [Counterclockwise](), so if you're already comfortable with Eclipse, that's a fine option. 

[Download Leiningen](http://leiningen.org), the most common Clojure build tool.

Step 1
------

Download Solr from [http://lucene.apache.org/solr/](http://lucene.apache.org/solr/) and unzip it.
Go into the `examples/` directory and run:

     java -jar start.jar

That will start a sample Solr server.  Keep that terminal open.

Step 2
------

Start up a REPL. If you're using IntelliJ, you can do that using Run -> lein repl.
From the command line, you can run "lein repl".

When you start the REPL via leiningen, it will automatically load dev/user.clj (because project.clj says to).
This loads a set of useful namespaces as well as some helpful functions for development: init, start, stop, go, and reset.
For more on the philosophy behind these, see [Stuart Sierra's blog post](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)

To get started, try running this from your REPL:

    (go)

This should connect to the example Solr server and return:

    :ready

Congratulations, you now have a running Solr client.
If for some reason you make changes that require resetting the connection, or changes to the configuration map, you can
 run:

    (reset)

This will close any existing connection, reload the namespaces, and reconnect.

Step 3
------

Experiment!

You have a few helper functions in user.clj that might be useful. "q", for example, runs queries:

    (q "*:*")

There are some commented-out forms there that will load some sample data into Solr for experimentation.

Enjoy!


On "config" and "system"
------------------------

We have a map called blast.solr/config that contains all of the configuration data for the sample app -- right now, that's just
the Solr URL and the name of the collection (both of which are set to the default values for the Solr example server),
but you can add more to that map if you'd like.
Then we have a map called `system` that contains that state of a running system -- the connection to Solr, for example.
The `system` map is created using the config map as a starting point, so it is a superset of `config`.

For development purposes, we hold on to the currently running `system` in an atom: `user/system`.
So you can run this from the REPL to see the contents:

    @system
