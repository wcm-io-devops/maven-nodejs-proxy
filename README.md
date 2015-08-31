<img src="http://wcm.io/images/favicon-16@2x.png"/> Maven NodeJS Proxy
======
[![Build Status](https://travis-ci.org/wcm-io-devops/maven-nodejs-proxy.png?branch=develop)](https://travis-ci.org/wcm-io-devops/maven-nodejs-proxy)

Maven proxy to download NodeJS binaries as Maven artifacts.

Steps to build and start the proxy:

- Go to maven-nodejs-proxy directory
- Build server with `mvn clean install`
- Start server with `java -jar target/io.wcm.devops.maven.nodejs-proxy-<version>.jar server config.yml`
- Go to http://localhost:8080 for further instructions
