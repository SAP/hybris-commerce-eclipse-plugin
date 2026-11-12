# Shared Commerce system configuration

`com.hybris.hyeclipse.commons.config.SystemConfig` replaces the SAP bootstrap
configuration class for importer, type-system and beans configuration consumers.
IDE type/attribute lookups now use the generated-model repository described in
`type-system-repository.md`. Source-built TSV also uses this shared configuration;
see `tsv-source-migration.md`.

## Access from another plugin

Declare `com.hybris.hyeclipse.commons` in `Require-Bundle`, then obtain the repository:

```java
SystemConfigRepository repository =
        com.hybris.hyeclipse.commons.Activator.getDefault().getSystemConfigRepository();
Optional<SystemConfig> config = repository.find(platformHome);
```

Run the first lookup in a background Job because it can read from disk. Lookups
are keyed by canonical platform-home path and return cached read-only snapshots.
During an import, lookups expose its provisional snapshot through a scoped
`beginImport()` handle; closing the handle removes it without writing to disk.
Multiple platforms can be stored; the existing IDE selection still chooses one
active platform through the `platform_home` workspace preference.

## Import and persistence

The import job parses Ant output and validates mandatory platform identity,
version, configuration directory and extension information before modifying
projects. It also reads extension descriptors for package roots, HMC and
Backoffice metadata. All plugins can use a provisional snapshot while importing;
the shared repository publishes the committed snapshot after project import.

The snapshot is saved before auto-build resumes, not at IDE shutdown. Failed or
cancelled project imports do not replace persisted snapshots. A persistence
failure is reported as an import error and prevents success follow-up work.
Workspace project changes themselves are not rolled back.

Files live in the commons bundle's Eclipse state location:

```text
<workspace>/.metadata/.plugins/com.hybris.hyeclipse.commons/system-configs/<platform-id>.xml
```

The XML uses a versioned JDK properties format, independent of Java object
serialization. Effective directory fields, properties, ordered extensions,
dependencies and descriptor metadata are stored. The extension lookup index is
rebuilt on loading. Writes use a temporary sibling file and atomic replacement
where the filesystem supports it.

On reopening a workspace, the first lookup restores the snapshot without running
Ant or reparsing extension descriptors. Missing snapshots return `Optional.empty()`;
corrupt or unsupported snapshots raise `CoreException`. A deleted platform is
not returned from the cache. Reimport repairs missing/corrupt data and refreshes
snapshots after Commerce configuration changes; automatic freshness detection is
not implemented. Existing workspaces need one reimport to create their snapshot.

This is local metadata, not a credential store. Do not add secrets to the captured
Ant properties. No shutdown hook or separate serialization dependency is needed.

## Verification

`make test` compiles the plugin reactor and runs the OSGi tests, including restart
round trips, platform/workspace isolation, corrupt/unsupported snapshots, removal,
descriptor metadata, persistence failures and import cancellation ordering.
