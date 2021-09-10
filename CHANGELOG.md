CHANGELOG
=========


version 2.3.0
-------------
* Change: Update jenkins minimum version to 2.222.4 (2020-05-24)
* Updated: Vulnerable Dependencies were updated
* New: Add optional field: release_type. It will default to PRE_PROD


version 2.2.1
-------------
* Change: Various parameters have been changed to optionals

version 2.2.0
-------------
* New: Add a feature to send application credentials to Data Theorem

version 2.1.0
-------------
* New: Add a feature to allow the building node to send the build directly to Data Theorem

version 2.0.3
-------------
* New: Add a new retry mechanism to the jenkins plugin

* Change: Update jenkins minimum version to 2.164.3 (2019-05-09)

* Change: Upgrade java dependencies:
    * credentials lib upgraded to 2.1.19
    * credential binding library upgraded to 1.23
    * common-codec library upgraded to 1.13

version 2.0.2
-------------
* Change: Move documentation to github

version 2.0.1
-------------
* Change: Add more debug logs

version 2.0.0
-------------
* New: The jenkins plugin can now update a generated source map file with obfuscated apk

version 1.4.0
-------------
* Change: The proxy password is now stored as a secret

version 1.3.0
-------------
* New: The jenkins plugin is now DSL compatible

version 1.2.0
-------------
* New: The jenkins plugin can now be configured to hit a specified proxy

version 1.1.0
-------------
* New: The jenkins plugin can now find files built using an external secondary agent

version 1.0.3
-------------
* Patch: Fix how the `Authorization` header is built.

version 1.0.2
-------------
* Patch: Change information about the plugin

version 1.0.1
-------------
* Patch: Update jenkins tests

version 1.0.0
-------------
* Initial implementation

