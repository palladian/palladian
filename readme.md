# <img src="_assets/palladian-128.png" alt="" height="64px"> Palladian Toolkit

[![Run Status](https://api.shippable.com/projects/5507af415ab6cc1352a2391e/badge?branch=master)](https://app.shippable.com/bitbucket/palladian/palladian)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ws.palladian/palladian/badge.svg)](http://mvnrepository.com/artifact/ws.palladian/palladian)

What is it?
-----------

Palladian is a Java-based toolkit which provides functionality to perform typical Internet Information Retrieval tasks. It provides a collection of algorithms for text processing focused on classification, extraction of various types of information, and retrieval. The aim of Palladian is to reuse algorithms that are freely available and build upon them to drive research by providing unified interfaces. This way, new algorithms can be quickly compared to the state-of-the-art allowing other users to create more advanced programs in the future.

More information about the Palladian toolkit is available here: <http://palladian.ws/>

If you have any questions, comments, or problems, we are happy to hear from you: <mail@palladian.ws>

Download
--------

Palladian is available through Maven on “The Central Repository”. Add it to your project’s `pom.xml` in the `<dependencies>` section (following example is for `palladian-core`):

```xml
<dependency>
  <groupId>ws.palladian</groupId>
  <artifactId>palladian-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

To use the `SNAPSHOT` builds, make sure to configure your `~/.m2/settings.xml` as shown [here](https://stackoverflow.com/a/7717234).

Who made it?
------------

The Palladian Toolkit was created by David Urbansky, Klemens Muthmann, Philipp Katz; 2009 — 2018.
