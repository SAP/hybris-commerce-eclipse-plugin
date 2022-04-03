# SAP Commerce Project Importer

Project Importer functionality aims to import SAP Commerce platform with extensions as Eclipse projects and solve dependency rules based on `project.xml` file.

![Project Import Wizard](imgs/importing-p.gif)

## Features

Importer contains set of features and options available for developers. Most of them have been listed below.

### Project Import Wizard

Plugin provides new option available in IDE standard Import wizard option. Option is available in menu: `File -> Import...`.
There you can find category `SAP` and there `Import SAP Platform`.

Next wizard step shows list of checkboxes available during import and available directory field. It is required to point valid `hybris/bin/platform` folder for your project.



# Roadmap

That is default approach with importing projects for SAP Commerce till version 1808. With that version Gradle support has been introduced as an experimental functionality and it is possible to use that option to play with.

That approach is supported by most IDEs on market.

Project Importer functionality will be supported and fixes will be provided, but implementation will be focused on other areas of SAP Commerce plugin.


# Added HotSwap

It is possible to set up HotSwap to have similar to JRebel functionality.

https://www.linkedin.com/pulse/hotswap-hybris-dcevm-free-open-source-50-alternative-jrebel-agarwal

```bash
curl -s https://api.github.com/repos/HotswapProjects/HotswapAgent/releases/latest | grep "browser_download_url" | cut -d : -f 2,3 | tr -d \"
```

