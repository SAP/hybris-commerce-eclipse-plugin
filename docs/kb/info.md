# Knowledge Database

Document collects known issues related to plugin and solutions for them.

## JVM Compatibility

The current development version of the plugin requires Java 21 to run Eclipse.
See [JDK selection](../import-jdk.md) for the separate Commerce project and Ant JDK settings.
The following class-loading guidance and compatibility table describe historical versions.
For current Commerce patch requirements, consult SAP Help's "Third-Party Compatibility" page.

Eclipse IDE can run independently on diferent JVM version than imported SAP Commerce Cloud project. JDK version can be changed in Eclipse Setting Preference Page. 

Details how to set up JVM for Eclipse IDE can be found on page https://wiki.eclipse.org/Eclipse.ini

Information about SAP Commerce JVM compatibility is available on SAP Help page "Third-Party Compatibility"

Milestone JVM changes for SAP Commerce:

| SAP Commerce Version | Java Version | Notes |
| -------------------- | -------------|-------|
| <= 5.5.0             | Java 7 (1.7) | not supported |
| 5.5.1 - 6.7          | Java 8 (1.8) | not supported |
| 1808 - 1811          | Java 8 (1.8) | not supported |
| 1905 - 2207          | Java 11      | not supported |
| 2211                 | Java 17      | not supported |
| 2211-jdk21           | Java 21      | supported     |
