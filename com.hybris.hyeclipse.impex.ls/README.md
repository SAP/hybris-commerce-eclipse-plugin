# ImpEx language server

Non-UI LSP4J bundle, connected in-process through the preview bundle's LSP4E
provider. Only full document synchronization, UTF-16 positions, document formatting,
and range formatting are currently advertised. The server analyzes open snapshots;
it does not load files, connect to Commerce, or execute scripts.

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
- Formatting uses a single background worker with at most eight queued requests.
  Closed, superseded, or cancelled requests cannot supply edits for old snapshots.
  LSP4E also rejects results when the editor document's modification stamp changed.

Parser rules follow `docs/dev/impex/impex-docs.md`, not RFC 4180: quotes only start
at cell boundaries, doubled quotes are literal, and backslashes do not escape quotes.
The scanner groups continued/multiline records for safety, but does not implement
the full import engine or header execution semantics.

Type-system completion/navigation, macro assistance, symbols, and diagnostics are
subsequent slices. See [the migration plan](../docs/dev/impex/editor-replacement-plan.md).
