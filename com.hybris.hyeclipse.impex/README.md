# ImpEx editor

Generic Editor/TM4E/LSP4E replacement described in the
[migration plan](../docs/dev/impex/editor-replacement-plan.md).
The legacy bundle has been removed from the source, reactor, and installable feature.

After installing the rebuilt feature, **ImpEx Editor** is the default for `.impex`
files. It does not claim `.csv` files. Colors follow the installed TM4E theme.

The canonical grammar is `syntaxes/impex.tmLanguage.json`. Headers, modifiers,
macros, data-cell strings, comments, and script directives have initial scopes;
this is not a complete ImpEx parser. Backslash-continuation context and configurable
header replacements still require further work.

Use **File > New > Other > SAP > New ImpEx File**, or **M1+Shift+Y**, to create
files with the replacement wizard. It uses the selected workspace project/folder,
refuses overwrites (including local files not yet refreshed into the workspace),
and opens the file in the editor. New files use UTF-8 and the project's configured
line delimiter. Explicit `.csv` names are accepted but currently open without
ImpEx highlighting. The old wizard/command registrations have been transferred
to this bundle without changing their IDs or duplicating the shortcut.

The in-process language server now provides safe document/range formatting through
**Format ImpEx** or **M1+Shift+F**. Select a complete header block to format only that
block, or use the caret with no selection for document formatting. Multiline,
continued, malformed, or unsupported blocks are left unchanged. See the
[formatting policy](../com.hybris.hyeclipse.impex.ls/README.md) for limits and options.
Formatting uses LSP4E's action; the legacy formatter and its shortcut contribution are removed.

Content assist (**Ctrl+Space**) now offers operation/type names, inherited
attributes, nested reference attributes, header/column modifiers, supported modifier
values, and macros defined earlier in the document. Definition hyperlinks resolve
indexed type/attribute declarations to their generated Java sources.

Indexed proposals are limited to workspace files in the Commerce tree belonging to
the open project named `platform`. Other/unindexed roots, external extensions, and
ambiguous workspace aliases get syntax/macro assistance only—not another platform's
types. The index is reused and refreshed by the existing background indexing jobs.
Quoted/continued headers and macro-expanded header structures are not yet resolved
for type assistance. See the [language-service policy](../com.hybris.hyeclipse.impex.ls/README.md).

Hover now shows generated model/attribute details and the source of macro definitions
without evaluating them. The Outline lists header and macro declarations. Confirmed
CSV quote errors appear as diagnostics and clear after correction or document close.
Unknown types/macros and runtime-dependent import behavior are not reported as errors.

Interactive UI/upgrade validation, CSV compatibility refinements, and multi-platform
indexing remain outstanding; removal approval does not imply those checks were performed.

## Existing installations

- User-specific file associations can override the new default. Reselect
  **Open With > ImpEx Editor** or update the `.impex` association in Eclipse preferences.
- An independently installed `com.hybris.hyeclipse.impexed` bundle may remain after
  a feature upgrade. Review **Help > About Eclipse > Installation Details** and
  uninstall the old editor if still present. No workspace preferences are reset automatically.
- Historical published update-site snapshots are unchanged; new builds exclude the old bundle.

Run the isolated OSGi tests:

```bash
./mvnw -pl com.hybris.hyeclipse.commons,com.hybris.hyeclipse.impex.ls,com.hybris.hyeclipse.impex,com.hybris.hyeclipse.impex.tests -am verify
```

The tests assert the new default registration and absence of legacy runtime contributions.
The tests exercise the packaged grammar, file creation, registration, formatting,
real JSON-RPC lifecycle, scoped completion/navigation, hover, symbols, diagnostic
publication/clearing, index refresh, cancellation, and LSP4E's stale-edit protection
and document undo/redo.
They do not replace interactive shortcut, wizard, light/dark-theme, and
existing-workspace upgrade testing.
