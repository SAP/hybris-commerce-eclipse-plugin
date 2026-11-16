#  Development

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

### 4. Update In-Repo Update Site (if maintained)
Sync the built p2 repository into `updatesite/neon`:
```bash
rm -rf updatesite/neon/*
cp -R com.hybris.hyeclipse.site/target/repository/* updatesite/neon/
```

### 5. Commit and Tag Release
```bash
git add pom.xml */pom.xml */META-INF/MANIFEST.MF */feature.xml updatesite/neon/
git commit -m "chore(release): bump version to VERSION.RELEASE"
git tag -a vVERSION -m "Release vVERSION"   # e.g., git tag -a v22.9.0 -m "Release v22.9.0"
git push origin release/VERSION --tags
```

### 6. Create Pull Request and Merge to Master
1. Open a pull request from `release/VERSION` into `master` on GitHub.
2. Ensure GitHub Actions CI passes.
3. Merge the pull request.

### 7. Publish GitHub Release on github.com
1. Go to the GitHub repository: **Releases -> Draft a new release** (or use the GitHub CLI `gh`).
2. Select the existing tag (e.g. `v22.9.0`).
3. Set the release title: `SAP Commerce Cloud - Eclipse Plugin v22.9.0`.
4. Generate release notes automatically or paste changelog entries.
5. Attach the built update-site ZIP distribution archive:
   - File: `com.hybris.hyeclipse.site/target/com.hybris.hyeclipse.site-VERSION.RELEASE.zip`
6. Click **Publish release**.

### 8. Prepare Next Development Iteration
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
