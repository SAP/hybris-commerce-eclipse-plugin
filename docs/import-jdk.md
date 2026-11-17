# Shared Ant and project JDK selection

The existing workspace import preference, `antJdk`, now controls both Ant and
newly imported Java projects. It stores Eclipse's **composite VM identifier**, not
only a Java version or filesystem path, so installations with the same Java
version remain distinguishable. An empty selection uses the workspace default
JDK. JDKs must be registered under **Java > Installed JREs** and contain `javac`.

## Import behaviour

Each newly imported Java project gets:

- A JRE container referencing the captured registered VM.
- Project-specific compiler compliance, source and target levels derived from
  that JDK. VM version metadata is used first, with the JDK `release` file as a
  fallback for VM types that do not expose a version.
- Preserved non-JRE classpath entries and runtime-container access rules,
  attributes and exported flag. Duplicate JRE references are consolidated.

## Preference changes

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
