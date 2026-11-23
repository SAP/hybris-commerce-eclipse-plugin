# Documentation and plugin versioning

Documentation follows the plugin's Git history and release tags. There is no
independent documentation version number and no release-version selector on the
current MkDocs site.

## Version sources

| Item | Source of truth | Example |
| --- | --- | --- |
| Development version | Root and module `pom.xml` files | `22.11.1-SNAPSHOT` |
| Development bundle/feature version | `META-INF/MANIFEST.MF` and `feature.xml` | `22.11.1.qualifier` |
| Release version | Synchronized Maven, bundle, and feature metadata | `22.11.1.RELEASE` |
| Release tag | Git tag identifying the release source and documentation | `v22.11.1` |
| Installed/distributed version | p2 metadata and release assets | Consult the installed feature or release notes |
| Published documentation | Latest successful documentation deployment from `master` | May include unreleased changes |

Examples illustrate the naming convention; they do not assign the next release.
Maintain the existing three-component version sequence. Do not derive a plugin
version automatically from today's date, the Java version, or the Eclipse target.

Use the Tycho Versions plugin described in
[the release procedure](development.md#2-set-the-new-release-version) to update
Maven, bundle, and feature versions together. Do not change individual versions
with manual search-and-replace. Build qualifiers are not separate documentation
releases.

## When to update documentation

- Update behavior, setup, and migration documentation in the same change as the
  implementation. Include user-visible changes in `CHANGELOG.md`.
- Mark features not yet present in a published artifact as **Unreleased**. State
  **Available since X.Y.Z** only after that release has been published.
- Documentation corrections alone do not require a plugin version bump or a new
  p2 repository. They can be published from `master` through the existing Pages workflow.
- For compatibility-affecting changes, document removed APIs/bundles, replacement
  behavior, user actions, and known limitations. The maintainer chooses the next
  plugin version; the numeric version alone is not a compatibility guarantee.

For example, retiring the legacy ImpEx editor requires release notes explaining
the new default editor, possible reassociation/manual uninstallation, and any
remaining grammar or indexing limitations. Do not describe manual checks as
completed unless they were actually performed.

## Release documentation checklist

Before creating the release tag:

1. Apply the selected plugin release version using the release procedure.
2. Review `README.md`, installation requirements, changed feature guides, and
   migration notes against the actual release candidate—not just current source.
3. Convert applicable **Unreleased** changelog entries into the release section;
   retain genuinely unreleased work under **Unreleased**.
4. Validate documentation using the commands in
   [Documentation website](development.md#documentation-website), including the
   strict MkDocs build and published-resource tests.
5. Include matching documentation and changelog changes in the source commit
   referenced by the release tag. Publish the artifacts using the
   [release procedure](development.md#releasing-a-new-version).

After publication, prepare the next development `-SNAPSHOT` in a separate change.
Do not move or recreate an existing release tag to incorporate later documentation
fixes. Put corrections on the development branch and, when necessary, a dedicated
release-maintenance branch or a new release.

## Read documentation for an installed release

The public MkDocs site represents the latest deployed `master` documentation,
not necessarily the version offered by `updates/latest/`. Documentation deployment
does not build or release Java bundles; p2 publication uses the committed repository.
See [What is published](../installation.md#what-is-published).

For version-specific source documentation, select the release tag on GitHub and
browse `README.md` and `docs/` in the repository that published the release. A fork's
tag may not exist in the upstream repository. Locally, inspect tagged files without
changing the working tree:

```bash
VERSION=22.11.0  # replace with the installed release's tag version
git show "v${VERSION}:README.md"
git show "v${VERSION}:docs/installation.md"
```

Some older tags predate particular documentation pages. Use the README, changelog,
and documentation files that exist at that tag. Check the current site's corrections
and migration notes separately; do not treat newer instructions as evidence that an
older binary supports the same features.
