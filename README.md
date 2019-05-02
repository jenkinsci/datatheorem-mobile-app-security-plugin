# Data Theorem Mobile App Security Plugin

This plugin allows you to upload PreProd mobile binaries directly to Data Theorem for security scanning, as part of your CI/CD pipeline.

## Installing the plugin in your Jenkins instance

To install the plugin, you need a Data Theorem account with a subscription. **A quick-start guide is available here :** [User guide](USERGUIDE.md)

## Local development

- Install Java
- Install official java JDK 8
- Install maven
- At the root of the project run a new Jenkins project with the plugin included using `$ mvn hpi:run`

## Manually testing the plugin

- At the root of the project run the plugin using `$ mvn hpi:run`
- Connect to the Jenkins webpage at http://localhost:8080/jenkins
- From the global configuration page install credentials and credentials plugins
- Create a new job and add a post build action named : "Upload Build To Data Theorem"

## Launch the tests

    mvn clean test

## Updating version

You should update the version variable from the SendBuildAction and from pom.xml when releasing a new plugin version