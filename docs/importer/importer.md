# SAP Commerce Project Importer

Project Importer functionality aims to import SAP Commerce platform with extensions as Eclipse projects and solve dependency rules based on ```project.xml``` file.

![Project Import Wizard](imgs/importing-p.gif)

## Features

Importer contains set of features and options available for developers. Most of them have been listed below.

### Project Import Wizard

Plugin provides new option available in IDE standard Import wizard option. Option is available in menu: ```File -> Import...```.
There you can find category ```SAP Hybris [y]``` (yeap.. We still love to use *that* old name) and there ```Import SAP Hybris Platform```.

Next wizard step shows list of checkboxes available during import and available directory field. It is required to point valid ```hybris/bin/platform``` folder for your project.



# Roadmap

That is default approach with importing projects for SAP Commerce till version 1808. With that version Gradle support has been introduced as an experimental functionality and it is possible to use that option to play with.

That approach is supported by most IDEs on market.

Project Importer functionality will be supported and fixes will be provided, but implementation will be focused on other areas of SAP Commerce plugin.
 No newline at end of file
