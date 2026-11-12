# Shared Ant and project JDK selection

The existing workspace import preference, `antJdk`, now controls both Ant and
newly imported Java projects. It stores Eclipse's **composite VM identifier**, not
only a Java version or filesystem path, so installations with the same Java
version remain distinguishable. An empty selection uses the workspace default
JDK. JDKs must be registered under **Java > Installed JREs** and contain `javac`.

## Import behaviour

`ImportJob` captures the selected VM and validates its compiler level before Ant
or workspace project changes. The captured VM is passed to both `ShellImporter`
and `Importer`; changing the preference while Ant is running does not switch the
JDK halfway through the import. The JDK installation home is checked again after
Ant, so an installation edited during Ant execution does not silently produce
inconsistent project settings.

Each newly imported Java project gets:

- A JRE container referencing the captured registered VM.
- Project-specific compiler compliance, source and target levels derived from
  that JDK. VM version metadata is used first, with the JDK `release` file as a
  fallback for VM types that do not expose a version.
- Preserved non-JRE classpath entries and runtime-container access rules,
  attributes and exported flag. Duplicate JRE references are consolidated.

This happens even when **Fix classpaths** is disabled. The old platform-version
logic that globally forced Java 8 compiler options was removed. Workspace default
VM and workspace compiler options are not changed by import or the update job.

Existing projects keep their configured runtime during optional missing-runtime
repair; a missing runtime is repaired using the saved preference. Consequently,
declining a workspace-wide update does not silently replace existing SDK choices
during that repair phase.

## Preference changes

The importer bundle listens for committed changes to `antJdk`. Saving the same
value or changing an unrelated preference does not prompt. When the preference
uses the workspace default, a change to that default VM also prompts; initial VM
registration does not.

JDK/version validation runs in a background Job. A confirmation question then
runs on the display thread:

- **Yes:** schedule a user-visible Job to update every Java project in the
  workspace, including unrelated projects. Closed Java projects are temporarily
  opened and reclosed. Non-Java projects are left unchanged. Per-project failures
  are aggregated and cancellation is supported.
- **No:** the plugin does not update existing project settings. Future imports
  use the newly saved selection. Eclipse's own default-VM containers still follow
  its workspace default according to normal JDT behaviour.

Listeners are removed and pending work is cancelled when the importer bundle
stops. Superseded preparation callbacks do not display stale questions.

Imports and confirmed updates share a reentrant operation lock. The update Job
waits for imports **before** acquiring its workspace scheduling rule; it does not
hold the workspace rule while waiting for Ant or child Smart Import Jobs. This
prevents deadlocks and ensures a confirmed change during import is applied after
project creation completes. Already applied project changes are not rolled back
if a later project fails or the operation is cancelled.

## Verification

Headless Equinox tests cover the original wrong-runtime/global-Java-8 regressions,
captured Ant/importer selection, the actual post-Smart-Import configuration seam, runtime metadata,
workspace/default preservation, closed/non-Java projects, unchanged preference
events and cancellation. Smart Import itself requires an Eclipse workbench and
cannot run in the headless test container. GUI acceptance should confirm the Yes/No popup under
the import preference page and inspect **Java Build Path > Libraries** plus
**Java Compiler** on imported projects.
