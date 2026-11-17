#  Development

## Documentation website

The documentation site uses MkDocs with Material. `README.md` supplies the homepage;
the remaining pages and images come from `docs/`. Do not maintain a separate `docs/index.md`.
Navigation and site settings are in the root `mkdocs.yml`.

From the repository root, use Python 3.13 and an isolated environment:

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
python -m pip install -r tools/docs/requirements.txt
python -m unittest discover -s tools/docs -p 'test_*.py'
python -m mkdocs serve --strict
```

Open the local URL printed by MkDocs. README and documentation changes trigger a rebuild.
For a production build and internal link/image validation, run:

```bash
python -m mkdocs build --strict
```

The generated `site/` directory and virtual environment are ignored by Git.
Operational files such as Sonar scripts and Docker configuration are not published.
External links are not checked by the strict build.

### GitHub Pages deployment

In repository **Settings → Pages**, select **GitHub Actions** as the source.
The Documentation and Eclipse update site workflow validates pull requests without deploying them and publishes
documentation or `updatesite/` changes pushed to `master`. To rebuild manually, run the workflow from
the Actions tab with `master` selected. Other branches cannot deploy.

The public site is https://sap.github.io/hybris-commerce-eclipse-plugin/.
The same deployment also hosts the Eclipse p2 repository at
https://sap.github.io/hybris-commerce-eclipse-plugin/updates/latest/.

### Eclipse p2 hosting

The post-build hook copies the committed `updatesite/latest` repository (currently a
symlink to `updatesite/neon`) into `site/updates/latest/` as ordinary files.
It validates compressed metadata consistency, referenced artifacts, sizes, and available
SHA-256/SHA-512 checksums before publishing. Artifact bytes and signatures are preserved.
Finder files and other files not referenced by the repository are excluded.

Documentation and p2 files are uploaded as one Pages artifact. Do not add a second
Pages deployment workflow: deploying only one of them would remove the other.
No Java build runs during Pages publication; updating Java source does not publish
an unsigned snapshot. Follow the release steps below to update the committed repository.

To preview the repository locally, run `python -m mkdocs serve --strict` and use
the printed local base URL followed by `updates/latest/` in Eclipse's installer.
Run the documentation test command above to verify the combined artifact before merging.

Material for MkDocs receives critical fixes and security updates until May 5, 2027.
Reassess the theme/toolchain before that date; Markdown sources remain independent of it.

## Continuous Integration

folder contains docker configuration for sonarqube installation.
To execute sonar check it is required to run docker installation first

```bash
docker-compose
```

 or by running shell script in this directory

```bash
./start-sonar.sh
```

After running that website will be available on default sonar port [localhost:9000](http://localhost:9000)

## Running Quality Check

To check plugin by sonar, run maven task:

```bash
mvn clean verify sonar:sonar
```

Results will be available on your local website

## Java Upgrades with OpenRewrite

[OpenRewrite](https://docs.openrewrite.org/) is used for automated refactoring and source migrations when upgrading target Java versions or applying modern Java recipes across the codebase.

### Running OpenRewrite via Maven

The `rewrite-maven-plugin` can be invoked directly on-demand using the Maven wrapper without modifying `pom.xml`.

#### 1. Dry Run (Preview Changes)

To preview proposed refactorings and generate a patch without modifying files on disk:

```bash
./mvnw org.openrewrite.maven:rewrite-maven-plugin:dryRun \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:RELEASE \
  -Drewrite.activeRecipes=org.openrewrite.java.migrate.UpgradeToJava21 \
  -Drewrite.exclusions="**/com.hybris.hyeclipse.emf.beans/**"
```

The dry run prints a summary of modified files and writes a patch file to `target/site/rewrite/rewrite.patch`.

#### 2. Apply Changes

To apply recipes directly to the source code:

```bash
./mvnw org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:RELEASE \
  -Drewrite.activeRecipes=org.openrewrite.java.migrate.UpgradeToJava21 \
  -Drewrite.exclusions="**/com.hybris.hyeclipse.emf.beans/**"
```

#### 3. Common Recipes

- **Java 21 migration:** `org.openrewrite.java.migrate.UpgradeToJava21`
- **Java 17 migration:** `org.openrewrite.java.migrate.UpgradeToJava17`
- **Java version best practices:** `org.openrewrite.java.migrate.JavaVersionBestPractices`

### Important Considerations for this Repository

1. **Exclude EMF Generated Code**:
   Do **not** apply automated transformations to `com.hybris.hyeclipse.emf.beans/src/`. This code is generated from `model/beans.ecore` and `model/beans.genmodel`. Always pass:
   ```bash
   -Drewrite.exclusions="**/com.hybris.hyeclipse.emf.beans/**"
   ```
2. **Review Code Style and Project Conventions**:
   Always inspect the diff (`git diff`) after running recipes. This repository enforces conservative Java features:
   - Do **not** use `var` (retain explicit types)
   - Do **not** use pattern matching `instanceof` (use traditional cast-after-check)
   - Do **not** use records, sealed classes, or text blocks (`"""`)
   - Preserve 4-space tab indentation and K&R braces
   Reformat modified files using `eclipse-formatter.xml` or `docs/dev/eclipse-code-style.xml`.
3. **Update OSGi and Tycho Metadata**:
   OpenRewrite only modifies Java source files. When upgrading the Java target version, manually update:
   - Root `pom.xml`: `<maven.compiler.release>`, `<source>`, `<target>`, and `<sonar.java.source>`
   - `META-INF/MANIFEST.MF` across all bundles: `Bundle-RequiredExecutionEnvironment: JavaSE-<version>` (e.g. `JavaSE-21`)
   - `Makefile`: `JAVA_HOME` auto-detection logic if bumping minimum runtime version
4. **Build and Test Verification**:
   Verify that all OSGi bundles compile and pass tests:
   ```bash
   make test
   make build
   ```

## Generating Release Notes

Release notes are generated thanks to [gren](https://github.com/github-tools/github-release-notes).


## Releasing a New Version

Follow these steps to prepare, build, and publish a new release:

### 1. Create a Release Branch
Start from the latest `master` (or target development branch):
```bash
git checkout master
git pull origin master
git checkout -b release/VERSION   # e.g., release/22.9.0
```

### 2. Set the New Release Version
Use the Tycho Versions plugin to bump all `pom.xml`, `MANIFEST.MF`, and `feature.xml` files simultaneously:
```bash
./mvnw org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=VERSION.RELEASE
# e.g., -DnewVersion=22.9.0.RELEASE
```

### 3. Build & Test Locally
Build all bundles, verify unit tests, and generate the p2 update site repository:
```bash
make test
make build
```

This local build is an unsigned candidate. Do not use its ZIP as the delivered
release artifact until the signing step below has completed.

### 4. Sign Deliverable Eclipse Artifacts
Where technically sensible, downloadable Eclipse artifacts should be signed
with an Eclipse Foundation certificate ([Eclipse Project Handbook: Signed
Artifacts](https://www.eclipse.org/projects/handbook/#resources-signing)). Sign
the plug-in and feature JARs contained in the p2 repository; signing only the
outer ZIP does not sign the installable Eclipse artifacts.

The Eclipse Foundation CBI signing service is available to authorized project
builds. Configure the
[CBI Maven `eclipse-jarsigner-plugin`](https://github.com/eclipse-cbi/cbi/wiki/Signing)
in a dedicated Maven profile (for example, `sign`) and enable that profile only
in the authorized Eclipse signing Jenkins job. The signing profile must run
before the p2 repository and release ZIP are assembled, so the p2 metadata
checksums describe the signed JARs. Keep signing credentials and service access
out of the repository and local developer builds.

Run the release build in the authorized signing environment, for example:
```bash
./mvnw clean install -Psign
```

Verify the resulting plug-in and feature JAR signatures (including the expected
Eclipse Foundation signer), then install from the signed p2 repository in a
clean Eclipse instance. Use the ZIP produced by this signed build for the
update-site snapshot and GitHub release. Do not deliver the unsigned local ZIP
if signing was required but unavailable.

> **Current setup:** This repository has no CBI signing profile or configured
> Eclipse signing Jenkins job. These must be established with the authorized
> build administrators before a signed release can be produced; the local
> `make build` alone does not sign artifacts.

### 5. Update In-Repo Update Site (if maintained)
Sync the built p2 repository into `updatesite/neon`:
```bash
rm -rf updatesite/neon/*
cp -R com.hybris.hyeclipse.site/target/repository/* updatesite/neon/
```

Use the repository generated by the signed build above.

`updatesite/latest` points to `neon`, so updating this directory also updates the
repository published by GitHub Pages. Commit the complete metadata and artifact set
together. Once merged into `master`, the Pages workflow publishes it at the stable
`updates/latest/` URL without rebuilding or re-signing it.

### 6. Commit and Tag Release
```bash
git add pom.xml */pom.xml */META-INF/MANIFEST.MF */feature.xml updatesite/neon/
git commit -m "chore(release): bump version to VERSION.RELEASE"
git tag -a vVERSION -m "Release vVERSION"   # e.g., git tag -a v22.9.0 -m "Release v22.9.0"
git push origin release/VERSION --tags
```

### 7. Create Pull Request and Merge to Master
1. Open a pull request from `release/VERSION` into `master` on GitHub.
2. Ensure GitHub Actions CI passes.
3. Merge the pull request.

### 8. Publish GitHub Release on github.com
1. Go to the GitHub repository: **Releases -> Draft a new release** (or use the GitHub CLI `gh`).
2. Select the existing tag (e.g. `v22.9.0`).
3. Set the release title: `SAP Commerce Cloud - Eclipse Plugin v22.9.0`.
4. Generate release notes automatically or paste changelog entries.
5. Attach the built update-site ZIP distribution archive:
   - File: `com.hybris.hyeclipse.site/target/com.hybris.hyeclipse.site-VERSION.RELEASE.zip`
6. Click **Publish release**.

### 9. Prepare Next Development Iteration
Bump the codebase to the next `-SNAPSHOT` version on `master`:
```bash
git checkout master
git pull origin master
./mvnw org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=NEXT_VERSION-SNAPSHOT
# e.g., -DnewVersion=22.10.0-SNAPSHOT
git add pom.xml */pom.xml */META-INF/MANIFEST.MF */feature.xml
git commit -m "chore: prepare for next development iteration NEXT_VERSION-SNAPSHOT"
git push origin master
```
