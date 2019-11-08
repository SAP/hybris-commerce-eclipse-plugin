# Eclipse Plugin for SAP Hybris Commerce

What is it?
-----------
This Eclipse plugin makes developers more efficient by taking care of some of the common issues of developing with the SAP Hybris Commerce Suite. For example, with this plugin it's possible to have a new project workspace quickly configured with all project extensions compiling in minutes rather than hours.

[![Build Status - Master](https://travis-ci.com/mikolayek/hybris-commerce-eclipse-plugin.svg?branch=master)](https://travis-ci.com/mikolayek/hybris-commerce-eclipse-plugin)


Features
-----------
* Import platform into workspace
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

User Guide
----------
This GitHub repository is for managing the source code of the plugin.

To install the plugin visit the [Eclipse Marketplace](https://marketplace.eclipse.org/content/sap-hybris-commerce-development-tools-eclipse) and for up to date documentation, the Application Lifecycle Framework for Commerce: [SAP Hybris Commerce development tools for Eclipse](https://wiki.hybris.com/display/hybrisALF/SAP+Hybris+Commerce+development+tools+for+Eclipse).

Development
--------------

Plugin is released as a open-source project. If you want to contribute to it, can find more information about setup in [Development](docs/dev/development.md) section.


Compile and Install From Source
-------------------------------
Execute "mvn install" to create an Eclipse local update site archive in com.hybris.hyeclipse.site/target. Import it in Eclipse by going to Help -> Install New Software then click the "Add" button
