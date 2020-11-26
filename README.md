<img src="http://wcm.io/images/favicon-16@2x.png"/> Maven NodeJS Proxy
======
[![Build Status](https://travis-ci.com/wcm-io-devops/maven-nodejs-proxy.png?branch=develop)](https://travis-ci.com/wcm-io-devops/maven-nodejs-proxy)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm.devops.maven/io.wcm.devops.maven.nodejs-proxy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm.devops.maven/io.wcm.devops.maven.nodejs-proxy)

Maven proxy to download NodeJS binaries as Maven artifacts.

This is a Maven Artifact Proxy for NodeJS binaries located at: https://nodejs.org/dist. Every call to this repository is routed directly to this URL, so it should not be used directly as Maven Repository, but cached by your own Maven Artifact Manager.

Steps to build and start the proxy:

- Go to maven-nodejs-proxy directory
- Build server with `mvn clean install`
- Start server with<br/>
`java -jar target/io.wcm.devops.maven.nodejs-proxy-<version>.jar server config.yml`
- Go to [http://localhost:8080](http://localhost:8080) for further instructions

---

A public instance of this proxy is available at: https://maven-nodejs-proxy.pvtool.org/

A Ansible role for this proxy is available at:
https://github.com/wcm-io-devops/ansible-maven-nodejs-proxy
