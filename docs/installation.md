# Installation and Eclipse update site

Install the plugin through the
[Eclipse Marketplace](https://marketplace.eclipse.org/content/sap-hybris-commerce-development-tools-eclipse)
or use the hosted Eclipse p2 repository:

```text
https://sap.github.io/hybris-commerce-eclipse-plugin/updates/latest/
```

## Install from the update site

1. In Eclipse, select **Help → Install New Software…**.
2. Choose **Add…**, name the repository **SAP Commerce development tools**, and paste the URL above.
3. Select **SAP Commerce, development tools for Eclipse** and follow the installation wizard.
4. Review the license and any trust/signature prompts, then restart Eclipse when requested.

Keep **Contact all update sites during install to find required software** enabled
so Eclipse can resolve dependencies from its own repositories.
To update an existing installation, use **Help → Check for Updates** with this repository enabled.

## What is published

The documentation version selector changes documentation pages only. The installer
URL above remains stable and is independent of selected documentation releases.

The hosted repository mirrors the committed `updatesite/latest` repository on `master`.
That path currently points to `updatesite/neon`; the directory name is historical and
does not by itself determine Eclipse compatibility. Consult the release notes and
the install wizard for the requirements of the version offered by the repository.

This is not a snapshot of the current Java sources. A source change or documentation
deployment does not produce a new plugin release. New versions are published only
when maintainers update the committed p2 repository and merge it into `master`.
The repository metadata determines the available plugin version.

GitHub Pages serves the p2 metadata, feature JARs, and plugin JARs without modifying
their bytes. Hosting does not sign artifacts or change their existing signatures.
See the [release process](dev/development.md#releasing-a-new-version)
for signing requirements.

The update URL is for Eclipse's installer, not a browsable documentation page.
If it is not yet accessible, the Pages workflow must first be deployed from `master`.
