# Shared generated-model type repository

## Scope and migration inventory

The shared model lives in `com.hybris.hyeclipse.commons.typesystem`. Its `YType`
and `YAttributeDescriptor` are immutable source-derived models, **not** SAP
bootstrap classes.

The previous SAP `YTypeSystem` usages were:

| Location | Previous usage | Outcome |
| --- | --- | --- |
| `ytypesystem/Activator.java` | Runtime type loading, type/name caches, attributes and loader locations | Delegates to shared snapshots; caches/runtime loading removed |
| `ytypesystem/YTypeSystemBuilder.java` | Loaded platform classes and parsed items XML | Removed |
| `ytypesystem/ExtendedYTypeSystem.java` | Subclassed SAP type system and reflected into private fields | Removed |
| `impexed/Activator.java` | Delegated completion and hyperlink data to the old Activator | Uses commons index directly |
| ImpEx completion processors | Requested types and qualifiers through the Activator | Retain their API, now backed by generated models |
| ImpEx hyperlink detector/link | Cached type names and decoded items.xml loader strings | Reads current names and opens the generated model source |
| Beans type selection/label provider/generator | Used SAP composed types and attributes | Uses shared types and accessor Java types |
| `tsv.extended/.../PlatformServiceImplHyeclipse.java` | Returns SAP `YTypeSystem` to the TSV library | Removed in the source-built TSV migration |
| `tsv.extended/.../TSVExtendedYTypeSystem.java` | SAP subclass with private-field reflection | Removed in the source-built TSV migration |

The old bundled `tsv-extended-0.0.1-SNAPSHOT.jar` exposed SAP `YTypeSystem` in its
APIs. It is now replaced by the source-built validation engine described in
`tsv-source-migration.md`, using imported items XML for schema-only metadata.

## Source rules

- Scan Java source under the imported, open `platform` project's `bootstrap/gensrc`.
  Refresh workspace resources first so newly generated Ant files are included.
- Include types inheriting directly or indirectly from
  `de.hybris.platform.core.model.ItemModel`, including the root model if it is
  present and declares its own type code.
- Read each class's **own** `_TYPECODE`; do not invent types for helper subclasses
  that merely inherit the parent's constant.
- Collect `@de.hybris.platform.servicelayer.model.annotations.Accessor` qualifiers
  on methods and fields. Ordinary fields, method names, comments and unrelated
  annotations are not attributes.
- Resolve string literals, local/inherited/qualified constants, explicit static
  imports and string concatenation. No Commerce classes are loaded or executed.
- Merge inherited attributes from generated source, deduplicate getters/setters,
  and let a subclass override its inherited descriptor. Getter return types take
  precedence over setter parameter types, including localized getter overloads.
- Store model class, Java attribute type, declaring class and workspace source
  path/line for navigation. Type codes and attribute names have deterministic order.

This is a **generated ItemModel index**, not a full items.xml schema. Enums,
atomic/collection type definitions, relation metadata, indexes and deployments
are not independent entries. Beans preserve accessor Java types (including
generics), replacing indexed model references with corresponding `*Data` classes.
Inheritance outside the scanned generated sources contributes no attributes.

## Lifecycle and persistence

The commons startup extension activates the index on Eclipse startup, even if no
ImpEx editor is opened. A background Job restores an existing snapshot first,
then reconciles it with current sources. Import completion explicitly requests indexing. Every
workspace `POST_BUILD` event requests another scan, covering IDE full, incremental
and automatic builds. Requests are coalesced and run under a workspace scheduling
rule so scans do not race a workspace build.

Snapshots are persisted before they are published to consumers. Failed parsing,
cancelled scans or failed writes retain the previous good snapshot. Complete
replacement scans remove types and attributes whose generated sources were
removed. Closed/missing platform projects return an empty snapshot.
If the generated-source directory is unavailable, the previous snapshot is kept
until a later import/build makes the sources accessible again.

Storage uses versioned JDK XML properties under the commons workspace state:

```text
<workspace>/.metadata/.plugins/com.hybris.hyeclipse.commons/type-system/<platform-id>.xml
```

Platform identity is the canonical platform-home path. Writes use temporary files
and atomic replacement where supported. Restart restoration requires neither Ant
nor the Commerce JVM/bootstrap library. Corrupt snapshots are rebuilt from source.

## Consumer API

Declare `com.hybris.hyeclipse.commons` in `Require-Bundle`:

```java
TypeSystemSnapshot snapshot = com.hybris.hyeclipse.commons.Activator.getDefault()
        .getTypeSystemIndex().getSnapshot();
YType type = snapshot.getType("Dealer");
YAttributeDescriptor attribute = snapshot.getAttribute("Dealer", "dealer");
List<String> names = snapshot.getAllTypeNames();
List<String> qualifiers = snapshot.getAllAttributeNames("Dealer");
```

Lookups do not parse source or read files on the UI thread. Until the initial
background job finishes, the snapshot may be empty. Consumers should retrieve a
fresh snapshot for each interaction rather than indefinitely cache names.
Wizard background operations may use `awaitPendingRefresh(monitor)` before
displaying choices; never call it directly on the UI thread.

`make test` runs source parsing, inheritance, persistence, isolation, invalid-input,
cancellation and workspace-build refresh tests in Equinox.
