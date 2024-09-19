# Eclipse Plugin for SAP Commerce

Description
-----------
This Eclipse plugin makes developers more efficient by taking care of some of the common issues of developing with the SAP Commerce Suite. For example, with this plugin it's possible to have a new project workspace quickly configured with all project extensions compiling in minutes rather than hours.

![Eclipse](https://img.shields.io/badge/Eclipse-FE7A16.svg?logo=Eclipse&logoColor=white)
[![Join the chat at https://gitter.im/SAP/commerce-eclipse-plugin](https://badges.gitter.im/SAP/commerce-eclipse-plugin.svg)](https://gitter.im/SAP/commerce-eclipse-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![REUSE status](https://api.reuse.software/badge/github.com/SAP/hybris-commerce-eclipse-plugin)](https://api.reuse.software/info/github.com/SAP/hybris-commerce-eclipse-plugin)
[![Contributor Covenant](https://img.shields.io/badge/Contributor_Covenant-1.4-4baaaa)](CODE_OF_CONDUCT.md)



Features
-----------
* [Import platform into workspace](docs/importer/importer.md)
* Synchronize extensions with the Hybris Commerce Suite platform including synchronizing libraries and classpaths managed in the platform
* Configure the runtime JVM with optimal settings to run JUnit tests
* Add the JDBC driver to the classpath (required to be able to run JUnit tests)
* Actions to build the platform, ensuring projects are refreshed after the platform has generated files
* Impex editor with type ahead and formatting functionality
* Create working sets from localextensions.xml
* Disable unnecessary extension modules
* Build individual extensions
* [Commerce Bean & Enums Generation Wizard](docs/beangen/beangen.md)
* Type System Validation

Requirements
------------
It is required to install latest Eclipse IDE for plugin. Plugin is backward compatible with IDE at least 6 months old since plugin version release date.

Contributing & Code of Conduct
--------------

Plugin is released as a open-source project. If you want to contribute to it, can find more information about setup in [Development](docs/dev/development.md) section and in [Code of Conduct](CODE_OF_CONDUCT.md)


Download & Installation
----------
This GitHub repository is for managing the source code of the plugin.

To install the plugin visit the [Eclipse Marketplace](https://marketplace.eclipse.org/content/sap-hybris-commerce-development-tools-eclipse) and for up to date documentation, the Application Lifecycle Framework for Commerce: [SAP Commerce development tools for Eclipse](https://wiki.hybris.com/display/hybrisALF/SAP+Hybris+Commerce+development+tools+for+Eclipse).

Compile and Install From Source
-------------------------------
Execute "mvn install" to create an Eclipse local update site archive in com.hybris.hyeclipse.site/target. Import it in Eclipse by going to Help -> Install New Software then click the "Add" button

Knowledge Database
--------------

Please find [Knowledge Database](docs/kb/info.md)  which describe solutions for known issues.

Licensing
--------------

Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available via the [REUSE tool](https://api.reuse.software/info/github.com/SAP/hybris-commerce-eclipse-plugin).


Deprecated Features
-------------------

Some features have been already deprecated as there are currently supported by Eclipse functionality, little usage, or just no time to maintenance. More details can be found on [Deprecation List](docs/deprecation/deprecation.md) page.

