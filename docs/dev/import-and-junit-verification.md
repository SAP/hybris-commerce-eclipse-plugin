# Import and JUnit regression verification

## Automated checks

Run `make test` with Java 21. Tests execute inside Equinox:

If an IDE also compiles into `target/classes`, pause its automatic build or run
verification in a separate checkout. Concurrent IDE and Maven compilation can
replace valid classes with partial-build output.

- `com.hybris.hyeclipse.tests`: library-entry selection, Ant command construction,
  complete stdout/stderr collection, process failures, cancellation and child cleanup,
  output-reader cleanup, import sequencing, autobuild restoration and platform tracking.
- `com.hybris.hyeclipse.junit.tests`: extension classpath ordering and a separate Java
  process that loads compiled classes and resources from the resolved launch classpath.

Import lifecycle tests replace Ant and Commerce import stages; they do not substitute
for importing a real SAP Commerce installation. The JVM classpath fixture does not
start Commerce or connect to its database.

## Eclipse smoke tests

Use an Eclipse runtime with these plugins installed and a built Commerce platform.

1. Import the platform with source attachment and the desired configuration options.
   Confirm projects appear, options take effect, sources resolve and platform-dependent
   commands become available without restarting Eclipse.
2. Repeat with workspace autobuild initially enabled, then disabled. Confirm the
   original setting remains unchanged after success, failure and cancellation.
3. Cause the dependency update to fail. Confirm an error is reported, existing projects
   are not removed, and source attachment does not run.
4. Cancel a long dependency update. Confirm its shell and Ant process stop and no
   `hyeclipse-ant-output` threads remain.
5. Close and reopen the platform project. Confirm platform-dependent tooling recovers.
6. Select an integration test and use **Run As > Hybris JUnit 4**, then debug it. Confirm
   dependencies, resources and database drivers resolve on Java 21 without reflective
   system-classloader access. Also test a method selection and a referenced extension.

Repeat import smoke tests on Windows: the command must call `setantenv.bat` before
`ant updateMavenDependencies`. Unix uses `setantenv.sh` and the same Ant target.

The shell executable can be changed in **Preferences > SAP Commerce, development
tools for Eclipse > Shell executable for platform imports**. The Unix default is
`/bin/bash`; Windows defaults to `ComSpec` or `cmd.exe`. Specify an executable path,
not a command with arguments. Unix alternatives must support `--login -c` and sourcing
`setantenv.sh`; for example, `/bin/zsh`.

Existing launch configurations without the classpath provider should be recreated
using **Run As > Hybris JUnit 4**. The shortcut deliberately does not reuse ordinary
JUnit launches or legacy configurations missing the provider.
