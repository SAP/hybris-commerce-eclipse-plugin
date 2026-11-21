# ImpEx editor replacement plan

Status: migration started on `feature/impex-lsp-migration`; legacy editor retained.

## Implementation progress

- Added `com.hybris.hyeclipse.impex` and its independent OSGi test bundle to
  the reactor, and the preview bundle to the installable feature.
- Registered **Open With > ImpEx Editor (Preview)** as a non-default Generic
  Editor, with an ImpEx content type and TM4E presentation reconciler.
- Moved the grammar into the production bundle as the canonical resource;
  implemented initial header/modifier/macro/data/comment/script scopes and
  cell-aware quoting. Grammar and extension-registration tests cover this slice.
- The 2026-09 target supplies TM4E. Its Generic Editor implementation class is
  `org.eclipse.ui.internal.genericeditor.ExtensionBasedTextEditor`; the preview
  registration uses it directly and tests class resolution. Recheck this internal
  entry point on future platform upgrades.
- Verification: 9 new OSGi tests passed; the full 20-module package build passed
  with tests skipped after the isolated test run. The resulting p2 ZIP contains
  both editors and the packaged grammar. Interactive UI/upgrade checks remain.
- Added the new-file wizard using Eclipse's standard selection/validation page,
  UTF-8 workspace creation, project line delimiters, cancellation, and overwrite
  protection. Moved the old wizard/command/shortcut registrations to the new
  bundle while retaining their IDs. CSV creation is supported; its content-type
  override/highlighting remains a compatibility gap, explicitly described in the UI.
- Wizard verification: full reactor `verify` passed all 110 tests, including 19
  ImpEx grammar, registration, and workspace-creation tests. The p2 ZIP contains
  the wizard and its NLS resources with no legacy wizard registration. Interactive
  browsing, Java selection, shortcut execution, and editor-opening checks remain.
- Next: continuation-aware lexical scanning and LSP
  lifecycle, formatting, and project-scoped type-system services. These features
  are not implemented by the preview, and the legacy editor must not be retired.

## Goal and scope

Replace the custom `com.hybris.hyeclipse.impexed` editor with Eclipse's Generic
Editor, TM4E syntax highlighting, and LSP4E-backed language services. Formatting,
type-system completion, and the new-file wizard are mandatory replacement
features. Highlighting-only builds are opt-in previews, not the completed
replacement; retire the legacy editor only after these features pass acceptance
tests and the remaining migration decisions are resolved.

Inputs:

- [ImpEx behavior notes](impex-docs.md): reference for lexical edge cases, not a
  complete formal language specification. Runtime-dependent claims require
  verification against the supported Commerce version.
- [TextMate grammar](../../../com.hybris.hyeclipse.impex/syntaxes/impex.tmLanguage.json):
  canonical packaged grammar, still under migration.
- Existing `com.hybris.hyeclipse.impexed/plugin.xml`, editor, wizard, formatter,
  completion processors, and hyperlink classes.

## Architecture

```text
.impex file -> ImpEx content type -> Eclipse Generic Editor
                                    |-> TM4E: TextMate scopes and theme colors
                                    |-> LSP4E: document synchronization/services
                                                |-> ImpEx language server
```

TextMate grammars are not LSP servers. TM4E provides immediate lexical
highlighting; LSP4E is the client for a separately implemented server. LSP
semantic tokens can enrich highlighting later, but are unnecessary for the
first milestone. Do not create an empty server just to enable coloring.

Proposed modules:

- `com.hybris.hyeclipse.impex`: Eclipse content-type/editor registration,
  packaged grammar, language configuration, wizard, and eventual LSP4E binding.
- `com.hybris.hyeclipse.impex.ls`: non-UI Java language server using LSP4J,
  introduced when useful language services are implemented.
- `com.hybris.hyeclipse.impex.tests`: grammar, protocol, and Eclipse integration
  tests; not a fragment of the importer bundle.

Start with an in-process server connection, provided the target LSP4E APIs
support it cleanly. Keep Eclipse UI/resource adapters outside the server's
language logic. Defer a standalone server distribution and external process
management unless required by other clients.

Use dependencies from p2 and manifest declarations. Verify Generic Editor,
TM4E, LSP4E, and LSP4J versions available in Eclipse 2026-09 before choosing
exact APIs. If additional repositories are needed, use compatible versioned
release sites rather than floating snapshots/latest sites. Ensure the delivered
p2 feature resolves these dependencies, not just the development target.

## Grammar corrections

The original supplied grammar started a string on any quote, treated `\\.` as
an escape, recognized only unquoted `#` comments, and could not distinguish
scripts from comments. The preview fixes initial cases; complete and validate
the following behavior before editor cutover:

- Model cell boundaries and quoted multiline cells; quoting starts at the
  beginning of a cell, allowing leading whitespace as verified by fixtures.
- Use doubled `""` for escaped quotes. A backslash does not escape a quote.
- Separate trailing-backslash continuation from quoted multiline values.
- Recognize first-cell comments, including quoted first cells starting with
  `#`; keep `#` in later cells as ordinary data.
- Match quoted/unquoted `#%` script directives before general comments;
  distinguish engine tags and hooks such as `if:`, `endif:`, and `beforeEach:`.
  Do not execute scripts or promise full embedded-language validation.
- Highlight operation headers, type names, column attributes, nested reference
  paths, header/column modifiers, macro definitions/references, document IDs,
  special columns, `<ignore>`, `<null>`, and collection prefixes `(+)/(-)/(+?)`.
- Treat `<null>` as a token, not a universal null assignment. Do not label
  unknown macros or configurable header shorthands as errors by default.
- Use standard theme scopes, preserving `text.tabular.impex` unless compatibility
  testing identifies a reason to change it. Color booleans/numbers contextually,
  avoiding false matches inside identifiers or arbitrary data.

TextMate is a lexical approximation, not the import parser. Document unsupported
contexts instead of using increasingly fragile regexes to simulate execution.
Choose one canonical grammar location when packaging; do not maintain two
independently edited copies in documentation and the bundle.

## Delivery phases

### 1. Dependency and migration spike

- Resolve target bundles and inspect their extension schemas.
- Register an ImpEx content type derived from text and map `.impex` files to it.
- Prove Generic Editor + TM4E highlighting in a minimal installable feature.
- Verify light/dark themes and opening files outside a Commerce project.
- Audit all references to the old bundle, editor ID, command IDs, and preferences.
  The existing new-file wizard explicitly targets the old editor ID via a marker.
- Capture representative fixtures from the supplied notes and real project files.

Exit: a working editor integration and a documented dependency/migration matrix.

### 2. Highlighting release (opt-in)

- Implement and test the grammar corrections above with TM4E token-scope tests.
- Package grammar/configuration resources via `build.properties`.
- Add the new bundle to root `pom.xml` and the installable feature.
- Keep the old editor available through **Open With**; avoid competing default
  registrations and global formatter shortcuts.
- Implement the new-file wizard described below. New files must open in the
  Generic Editor, not target the old editor ID.

Exit: reliable highlighting with no server, no required Commerce installation,
and an explicit user-visible list of remaining legacy-only features.

### 3. Required language services and formatting

- Implement server initialization/shutdown and open/change/close synchronization.
  Start with full document synchronization; optimize only after measurement.
- Add macro/header document symbols and macro completion/hover as initial services.
- Add conservative lexical diagnostics (for example, unmatched cell quotes) using
  source ranges from a dedicated cell-aware scanner, not TextMate scopes alone.
- Ignore stale results after edits; bound background work and clean up documents
  and connections on close/shutdown. Advertise only implemented capabilities.
- Implement type-system completion and navigation using the shared
  `TypeSystemSnapshot`, as specified below.
- Implement document/range formatting using the same cell-aware scanner and
  the safety rules below; wire the Eclipse formatting action through LSP4E.

Exit: formatting, type-system completion, and navigation pass acceptance tests.
Highlighting still works if language services fail or the Commerce type index
is unavailable; the wizard remains usable without a server.

### 4. Cutover and legacy retirement

Require the three features below and resolve the remaining migration decisions
before changing the default editor:

| Legacy behavior | Replacement policy |
| --- | --- |
| Syntax coloring and color preferences | TM4E grammar and theme preferences |
| New ImpEx wizard/shortcut | Required: validated creation and Generic Editor routing |
| Type/attribute completion | Required: context-sensitive LSP completion using the shared type index |
| Type hyperlinks | LSP definition locations |
| Formatting (`M1+Shift+F`) | Required: safe document/range formatting and undo/redo |
| Custom auto-edit/comment behavior | TM4E language configuration where safe; verify against data cells |
| Old editor IDs/preferences | Document reassociation and preference changes; migrate only with explicit policy |

Remove the old editor from the main feature/reactor only after parity decisions
and upgrade tests pass. Test upgrades from an existing installation: dropping a
bundle from a feature does not by itself prove old associations or installed
bundles disappear. Keep a temporary opt-in legacy feature if gaps require it.
Do not delete the old source or published update-site history during the spike.

## Required feature specifications

### Formatting

- Provide LSP `textDocument/formatting` and `textDocument/rangeFormatting`.
  Preserve the existing **Format ImpEx** workflow, context-menu entry, and
  `M1+Shift+F`, scoped to the ImpEx editor without conflicting with other editors.
- Align semicolon-separated header/data columns within each header block using
  whitespace only at verified safe cell boundaries. Honor supported LSP
  formatting options and document any fixed alignment policy.
- Use a scanner that preserves raw cell text, quoting, physical line boundaries,
  and continuation state. The old `Formatter`/`LineData` behavior is a comparison
  baseline, not a parser to copy unchanged.
- Never reorder rows/columns, add/remove cells, expand macros, normalize values,
  rewrite scripts, or change `<ignore>`, `<null>`, collection prefixes, or modifiers.
  Preserve empty/trailing cells, quote doubling, line endings, final-newline state,
  comments, script lines, and macro definitions.
- Leave multiline quoted records and backslash-continuation records unchanged
  unless a tested transformation proves their values are preserved. For malformed
  or ambiguous input, return no edits for affected blocks rather than guessing.
- Range formatting changes only complete, safely parseable blocks contained in
  the selection; otherwise return no edits. Width calculations must not cause
  changes outside the selected range.
- Return non-overlapping `TextEdit`s against the analyzed document version.
  Applying formatting must be one undoable editor operation and be idempotent.

Acceptance: golden-output tests for aligned blocks; parse-before/after checks for
equivalent cell values and structure; explicit unchanged-output tests for scripts,
multiline/continued records, and invalid input; range isolation, Unicode, CRLF/LF,
idempotence, and editor undo/redo. Use the documented ImpEx lexical rules, not an
RFC 4180 CSV parser, for equivalence checks.

### Type-system completion and navigation

- Provide context-sensitive LSP `textDocument/completion` for type names after
  operation keywords, attributes after header separators, and reference attributes
  inside nested paths such as `catalogVersion(catalog(id),version)`.
- Reuse the shared type-system index exposed through the commons bundle and
  `TypeSystemSnapshot`; avoid duplicate indexing and dependencies on the legacy
  editor's `Activator`. Include inherited attributes when exposed by the index.
- Add modifier-name/value proposals in the correct header/column context, alongside
  macro and operation proposals. Filter by the typed prefix and replace only the
  current token using precise edit ranges, including partially typed headers.
- Supply useful type/attribute details from indexed metadata, without promising
  runtime database validation. Add type definition navigation and attribute
  navigation where source locations are available.
- Bridge workspace/project-scoped immutable metadata into server services through
  an adapter. Verify whether the existing index supports project isolation; if it
  does not, address that before enabling completion across multiple Commerce roots.
- Refresh proposals after index rebuilds without blocking the UI. With a missing
  index or unknown type, retain syntax/macro/modifier proposals and return no
  speculative attributes; do not mark valid project-dependent names as errors.

Acceptance: type, inherited attribute, nested-reference, modifier, and macro
proposals; prefix filtering and replacement ranges; partial documents; no attribute
proposals in ordinary data/comments; index refresh; missing index; two projects with
different types; definition locations and no legacy-editor dependencies.

### New-file wizard

- Register **File > New > Other > SAP > New ImpEx File** in the new Eclipse bundle,
  retaining the existing `M1+Shift+Y` workflow. Reuse standard Eclipse new-file
  wizard/page APIs rather than rebuilding generic container/file validation.
- Preselect the project/folder from the current selection, including adaptable
  Java package selections; allow browsing and use `test.impex` as the initial
  filename for compatibility. Create the file in a writable workspace container.
- Validate names, accessible containers, and existing files. Never silently
  overwrite an existing file (the old wizard currently does so).
- Create a minimal starter comment explaining content assist, with explicit
  encoding and the project's line-ending convention; reveal the file and open it
  in the Generic Editor so highlighting and language services attach normally.
- Work without a Commerce installation, type index, or running language server.
  Perform file operations with workspace scheduling/progress support and open the
  editor on the display thread. External-file creation is outside wizard scope.
- Localize labels/errors and preserve or alias the old wizard command ID only
  when its registration is removed from the old bundle: avoid duplicate commands
  and shortcuts during coexistence.
- The old page also accepts `.csv`. Preserve explicit `.csv` creation, but do not
  globally associate all CSV files with ImpEx. Open wizard-created ImpEx CSV files
  explicitly in the Generic Editor with the ImpEx content-type override, if the
  target APIs support it; otherwise document that compatibility gap for approval.

Acceptance: project/folder/Java selection, browsing, filename and duplicate-file
validation, read-only/closed projects, cancellation, encoding, starter content,
editor opening, shortcut/command routing, and operation without language services.

## Validation and release gates

- Token fixtures: doubled and mid-cell quotes, quoted multiline cells, malformed
  quotes, backslash continuation, CRLF/LF, quoted comments/scripts, later-cell
  `#`, macros, nested references, modifiers, special values, and collection prefixes.
- Editor tests: content-type registration, new/open/save/save-as, wizard routing,
  Unicode, external files, themes, and old workspace/file associations.
- LSP tests: lifecycle, edits/positions (UTF-16), stale diagnostics, completion,
  cancellation where supported, and multiple projects with different type systems.
- Required feature gates: all formatting, completion/navigation, and wizard
  acceptance cases above pass before legacy-editor retirement.
- Performance: benchmark large generated `.impex` files and long unterminated
  quoted cells; set latency/memory budgets from baseline measurements.
- Run `make test` and `make package`; install the produced p2 ZIP in a clean
  Eclipse 2026-09 instance and an upgraded existing installation.
- Never execute ImpEx scripts, import data, resolve server files, or access a
  database as part of highlighting or language analysis.

## Decisions needed

1. Should the opt-in highlighting preview be published before feature completion?
2. Which additional legacy behaviors must be preserved beyond the required features?
3. Is a reusable standalone language server required, or only Eclipse integration?

Recommended defaults: opt-in highlighting preview first; wizard, formatting,
type-system completion, and navigation before retirement; defer standalone
distribution. Implementation effort should be estimated after the dependency
spike and any additional parity requirements are agreed.

References: [TM4E](https://github.com/eclipse-tm4e/tm4e),
[LSP4E](https://github.com/eclipse-lsp4e/lsp4e).
