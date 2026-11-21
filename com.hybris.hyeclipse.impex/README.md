# ImpEx editor preview

Generic Editor/TM4E foundation for the [migration plan](../docs/dev/impex/editor-replacement-plan.md).
The legacy `com.hybris.hyeclipse.impexed` bundle remains in the main feature.

After installing the rebuilt feature, choose **Open With > ImpEx Editor (Preview)**
on an `.impex` file. The preview is not registered as the default editor and does
not claim `.csv` files. Colors follow the installed TM4E theme.

The canonical grammar is `syntaxes/impex.tmLanguage.json`. Headers, modifiers,
macros, data-cell strings, comments, and script directives have initial scopes;
this is not a complete ImpEx parser. Backslash-continuation context and configurable
header replacements still require further work.

Use **File > New > Other > SAP > New ImpEx File**, or **M1+Shift+Y**, to create
files with the replacement wizard. It uses the selected workspace project/folder,
refuses overwrites (including local files not yet refreshed into the workspace),
and opens the file in the preview. New files use UTF-8 and the project's configured
line delimiter. Explicit `.csv` names are accepted but currently open without
ImpEx highlighting. The old wizard/command registrations have been transferred
to this bundle without changing their IDs or duplicating the shortcut.

The in-process language server now provides safe document/range formatting through
**Format ImpEx** or **M1+Shift+F**. Select a complete header block to format only that
block, or use the caret with no selection for document formatting. Multiline,
continued, malformed, or unsupported blocks are left unchanged. See the
[formatting policy](../com.hybris.hyeclipse.impex.ls/README.md) for limits and options.
The legacy formatter shortcut is scoped to its own editor to avoid interference.

Type-system completion/navigation, macro assistance, symbols, and diagnostics are
not implemented yet. Use the legacy editor for type-system completion workflows.

Run the isolated OSGi tests:

```bash
./mvnw -pl com.hybris.hyeclipse.commons,com.hybris.hyeclipse.impexed,com.hybris.hyeclipse.impex.ls,com.hybris.hyeclipse.impex,com.hybris.hyeclipse.impex.tests -am verify
```

Rebuild both editor bundles so registration tests do not use cached legacy metadata.
The tests exercise the packaged grammar, file creation, registration, formatting,
real JSON-RPC lifecycle, and LSP4E's stale-edit protection and document undo/redo.
They do not replace interactive shortcut, wizard, light/dark-theme, and
existing-workspace upgrade testing.
