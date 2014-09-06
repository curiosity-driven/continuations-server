Continuations server
---------------

This is a proof-of-concept Java servlet that implements a simple generic form-based flow using JavaScript interpreter Rhino which supports continuations.

Download [Maven](http://maven.apache.org/) and execute this from the project's directory:

    mvn jetty:run

Then open your web browser and go to http://localhost:8080/script

For more details see https://curiosity-driven.org/continuations#web-applications