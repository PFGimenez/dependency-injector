# A simple dependency injector

## Downloading / compiling

[![Build Status](https://travis-ci.org/PFGimenez/dependency-injector.svg?branch=master)](https://travis-ci.org/PFGimenez/dependency-injector)

You can find the latest compiled .jar here : https://github.com/PFGimenez/dependency-injector/releases/download/v1.1/dependency-injector-1.1.jar.

Otherwise, you can compile it yourself. You will need a JDK and maven.

    $ git clone https://github.com/PFGimenez/dependency-injector.git --depth 1
    $ cd dependency-injector
    $ mvn validate
    $ mvn compile
    $ mvn package
    $ mvn install

The jar file will be located in the `target` directory.
