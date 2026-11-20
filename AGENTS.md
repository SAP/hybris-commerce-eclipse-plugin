# AGENTS.md

Guidance for AI coding agents working in this repository. Follow nearby code patterns and keep changes focused.

## Project

SAP Commerce Cloud Eclipse plugin: a multi-module Maven/Tycho project of OSGi bundles, targeting Java 21 and Eclipse 2026-09. The root `pom.xml` is the source of truth for modules and build configuration.

- Production bundles: `com.hybris.hyeclipse.*`
- Installable feature: `com.hybris.hyeclipse.feature`
- p2 update site: `com.hybris.hyeclipse.site`
- OSGi tests: `com.hybris.hyeclipse.tests` and `com.hybris.hyeclipse.junit.tests`
- Generated EMF model: `com.hybris.hyeclipse.emf.beans` — do not edit generated code by hand.

Bundles contain `META-INF/MANIFEST.MF`, `plugin.xml` where needed, and `build.properties`. Source is generally in `src/`; some bundles have `src-platform-dependent/`. Bundle dependencies are declared in `Require-Bundle` in the manifest, not in Maven dependencies.

## Build and test

Use Java 21 and Maven 3.9.15 or newer (the Maven wrapper is preferred):

```bash
make build                 # clean install
make test                  # clean verify, including OSGi tests
make package               # p2 site package
make sign-release          # signed release build; requires the private GPG key
make verify-signatures     # verify the signed site ZIP
```

Tests run in Tycho's Equinox/OSGi runtime; do not run them as plain JUnit tests. Test sources are in `com.hybris.hyeclipse.tests/src/`. See `Makefile` for targets and `docs/dev/development.md` for release and signing instructions. Never commit private keys or passphrases; `sappubkey.asc` is public.

## Implementation guidance

- Match the conventions of the file and surrounding module; prefer small, focused changes.
- Java 21, no preview features. Use explicit types; avoid `var`, records, text blocks, pattern-matching `instanceof`, and switch expressions for consistency with this codebase.
- Use explicit imports, sorted alphabetically in the existing groups; remove unused imports. Do not use wildcard imports.
- Use NLS message bundles for user-visible strings (`*Messages.java` / `*Messages.properties`); use `%key` references in `plugin.xml`. Keep exception messages in English.
- Use `com.hybris.hyeclipse.commons.Log` for logging; do not use `System.out`.
- Keep SWT/UI work on the display thread. Run long operations in an Eclipse `Job` and report progress.
- Add tests for new or changed behavior where practical. Public/protected APIs need Javadoc; add comments for non-obvious logic, not obvious code.
- Do not modify generated EMF implementation code. Change the model and regenerate it instead.

## Release and version changes

Follow the single detailed procedure in [`docs/dev/development.md`](docs/dev/development.md#releasing-a-new-version). It covers version bumps, signed release builds, signature verification, update-site snapshots, tags, and GitHub assets. For a normal version change, use the Tycho Versions plugin so `pom.xml`, `MANIFEST.MF`, and `feature.xml` stay synchronized.

## Repository-specific cautions

- `Constants.PLATFROM` and `ClasspathChagedListener` are existing misspellings; do not rename without updating all references.
- The macOS `aarch64` target environment is required for Tycho Surefire on Apple Silicon.
