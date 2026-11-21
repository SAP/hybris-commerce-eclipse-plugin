# ImpEx language server

Non-UI LSP4J bundle, connected in-process through the preview bundle's LSP4E
provider. Full document synchronization, UTF-16 positions, document/range formatting,
completion, and definition navigation are advertised. The server analyzes open
snapshots and host-supplied metadata; it does not load files, connect to Commerce,
or execute scripts.

## Completion and navigation policy

- Context-specific operation/type names, inherited attributes, nested/collection
  references, explicit type paths, header/column modifiers, boolean/collection-mode
  values, and earlier document-local macro definitions.
- Proposals replace only the current token, including its suffix when typing in
  the middle. At most 200 proposals are returned; truncated lists are marked
  incomplete so a refined prefix can request a narrower result.
- Reference types come from indexed accessor Java types; ambiguous model names
  are not resolved by guessing or stripping a `Model` suffix.
- `TypeSystemProvider` is the host boundary. The Eclipse adapter reuses immutable
  `TypeSystemSnapshot` data and caches its DTO projection until the snapshot changes.
  Source accessibility and URI are resolved by the host when navigation is requested.
- The current shared index supports one project named `platform`. Its standard
  `bin/platform` layout scopes the wider Commerce tree; nonstandard layouts scope
  only the platform project's own directory. Unindexed roots, external extensions,
  closed projects, and ambiguous workspace aliases receive no indexed proposals.
  Do not treat this as full multi-platform indexing support.
- Unknown types and missing indexes retain syntax/modifier/macro proposals without
  speculative attributes or false validation errors. Comments/scripts do not get
  type/macro assistance. Quoted/continued or macro-expanded headers remain unsupported
  for type-context resolution.
- Metadata lookups run on the bounded analysis worker. Document or index changes
  during a request discard obsolete proposals; shutdown cancels pending requests.

## Formatting policy

- Align header/data cells with ASCII spaces at CSV boundaries. This fixed policy
  does not use tabs, regardless of `insertSpaces`/`tabSize` formatting options.
- Retain quoted contents, doubled quotes, empty/trailing cells, record ordering,
  comments, scripts, macro definitions, line endings, and final-newline state.
- Skip an entire block if a data/header record is multiline, backslash-continued,
  malformed, has an unsupported record, or has a different cell count from the
  header. Virtual/pos-dependent layouts may therefore be left unchanged.
- Range formatting affects only complete header blocks contained in the selection.
- Bounds: 4 Mi UTF-16 code units per input/output document, 100,000 logical records,
  512 cells per record, and 256 UTF-16 code units per aligned cell. Oversized/unsafe
  input produces no edits, rather than attempting potentially destructive recovery.
- Language analysis uses a single background worker with at most eight queued requests.
  Closed, superseded, or cancelled requests cannot supply edits for old snapshots.
  LSP4E also rejects results when the editor document's modification stamp changed.

Parser rules follow `docs/dev/impex/impex-docs.md`, not RFC 4180: quotes only start
at cell boundaries, doubled quotes are literal, and backslashes do not escape quotes.
The scanner groups continued/multiline records for safety, but does not implement
the full import engine or header execution semantics.

Macro hover, symbols, diagnostics, and UI/upgrade checks are subsequent slices.
See [the migration plan](../docs/dev/impex/editor-replacement-plan.md).
