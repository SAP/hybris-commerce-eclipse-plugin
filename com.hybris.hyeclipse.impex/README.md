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

Formatting, type-system completion/navigation, the new-file wizard, and LSP server
integration are not implemented yet. Use the legacy editor for those workflows.

Run the isolated OSGi tests:

```bash
./mvnw -pl com.hybris.hyeclipse.impex,com.hybris.hyeclipse.impex.tests -am verify
```

The tests exercise the packaged grammar in TM4E and check extension registration
and class resolution. They do not replace interactive light/dark-theme and
existing-workspace upgrade testing.
