# Data Theorem Mobile App Security Plugin

This plugin allows customers to upload PreProd mobile binaries directly to Data Theorem for security scanning, as part of your CI/CD pipeline.

## Using the plugin in your Jenkins instance

To use the plugin, you need a Data Theorem account with a subscription.
To find your API Key you can follow: https://datatheorem.github.io/PortalApi/upload_api.html

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

## Deploy a release

Before deploying the project, ensure you have the permission rights
https://github.com/jenkins-infra/repository-permissions-updater/

Then execute the following commands:

Prepare for new deployment

    $ mvn release:prepare

Ensure that the generate release.properties file is referring to the right repository:

    git@github.com/jenkinsci/datatheorem-mobile-app-security-plugin.git

Perform the release:

    $ mvn release:perform


If there is an issue with the deployment, please refer to the jenkins manual deployment guide:
https://www.jenkins.io/doc/developer/publishing/releasing-manually/

## Testing the release in production

Wait for the jenkins release to be available . It may take up to 4hours.
Once the plugin is available, you should see it at: https://plugins.jenkins.io/datatheorem-mobile-app-security/#releases

Then create a whole new jenkins environment with docker

    $ docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:2.316-jdk11

Install the plugin and uses it to deploy a mobile application