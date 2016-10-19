
![ArangoDB-Logo](https://docs.arangodb.com/assets/arangodb_logo_2016_inverted.png)

# arangodb-java-driver

2.7: [![Build Status](https://secure.travis-ci.org/arangodb/arangodb-java-driver.svg?branch=2.7)](https://travis-ci.org/arangodb/arangodb-java-driver) 
3.0: [![Build Status](https://secure.travis-ci.org/arangodb/arangodb-java-driver.svg?branch=3.0)](https://travis-ci.org/arangodb/arangodb-java-driver) 
3.1: [![Build Status](https://secure.travis-ci.org/arangodb/arangodb-java-driver.svg?branch=3.1)](https://travis-ci.org/arangodb/arangodb-java-driver) 
4.0: [![Build Status](https://secure.travis-ci.org/arangodb/arangodb-java-driver.svg?branch=4.0)](https://travis-ci.org/arangodb/arangodb-java-driver) 
master: [![Build Status](https://secure.travis-ci.org/arangodb/arangodb-java-driver.svg?branch=master)](https://travis-ci.org/arangodb/arangodb-java-driver)

## Supported versions

<table>
<tr><th>arangodb-java-driver</th><th>ArangoDB</th><th>network protocol</th><th>Java version</th></tr>
<tr><td>4.0.x</td><td>3.1.x</td><td>VelocyStream</td><td>1.6+</td></tr>
<tr><td>3.1.x</td><td>3.1.x</td><td>HTTP</td><td>1.6+</td></tr>
<tr><td>3.0.x</td><td>3.0.x</td><td>HTTP</td><td>1.6+</td></tr>
<tr><td>2.7.4</td><td>2.7.x and 2.8.x</td><td>HTTP</td><td>1.6+</td></tr>
</table>

##Server Configuration

To use the driver version 4.0.0 and above, you need to run ArangoDB server with an endpoint using VelocyStream protocol. (see [documentation](https://docs.arangodb.com/current/Manual/Administration/Configuration/Endpoint.html#server-endpoints))

```
unix> ./arangod --server.endpoint vpp+tcp://127.0.0.1:8529
                --server.endpoint vpp+ssl://127.0.0.1:8530
                --ssl.keyfile server.pem /tmp/vocbase
```

Note: The web interface needs only endpoint tcp. If you want to use both, the driver and the web interface you have to use both endpoints.

```
unix> ./arangod --server.endpoint vpp+tcp://127.0.0.1:8529
                --server.endpoint vpp+ssl://127.0.0.1:8530
                --server.endpoint tcp://127.0.0.1:8531
                --ssl.keyfile server.pem /tmp/vocbase
```

## Maven

To add the driver to your project with maven, add the following code to your pom.xml
(please use a driver with a version number compatible to your ArangoDB server's version):

ArangoDB 3.1.X
```XML
<dependencies>
  <dependency>
    <groupId>com.arangodb</groupId>
    <artifactId>arangodb-java-driver</artifactId>
    <version>4.0.0</version>
  </dependency>
	....
</dependencies>
```

If you want to test with a snapshot version (e.g. 4.0.0-SNAPSHOT), add the staging repository of oss.sonatype.org to your pom.xml:

```XML
<repositories>
  <repository>
    <id>arangodb-snapshots</id>
    <url>https://oss.sonatype.org/content/groups/staging</url>
  </repository>
</repositories>
```

## Compile java driver

```
mvn clean install -DskipTests=true -Dgpg.skip=true -Dmaven.javadoc.skip=true -B
```	

# Learn more
* [ArangoDB](https://www.arangodb.com/)
* [ChangeLog](ChangeLog)
* [Documentation](docs/documentation.md)
* [Examples](src/test/java/com/arangodb/example)
* [Tutorial](https://www.arangodb.com/tutorial-java/)
* [JavaDoc](http://arangodb.github.io/arangodb-java-driver/javadoc-4_0/index.html)
