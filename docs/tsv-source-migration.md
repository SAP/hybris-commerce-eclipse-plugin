# Source-built TSV migration

## Reference project

The implementation was informed by the supplied `consultant-toolbox/tsv-extended`
source project, version **3.0.1**, particularly:

- `rules/TypeSystemRule.java`: inherited indexes and redeclaration handling.
- `utils/TypeSystemEvalUtils.java`: known platform-type index exemptions.
- `src/main/resources/extended-ruleset.xml`: the nine updated schema rules.
- The sibling `tsv` project: XPath selection, test and source-location conventions.

The source fixes the old system-classloader cast and declares JAXB providers,
but still uses SAP `SystemConfig`/`PlatformConfig`/`YTypeSystem`, Spring and Groovy.
It also uses a newer `RulesCheckResults` API incompatible with the plugin's old
TSV jars. Copying it wholesale would not complete the repository migration.

## Implementation

`com.hybris.hyeclipse.commons.validation` now supplies the source-built engine:

- `ValidationXml`: secure JDK XML parsing, source lines and report writing.
- `ItemsSchema`: read-only, cross-extension XML schema metadata, including
  inherited attributes, relation-mapped attributes, custom properties and indexes.
- `ValidationService`: standard XPath checks and typed Java schema checks.
- `ValidationResult`: findings with source, priority and PASS/FAIL/ERROR/SKIPPED.

Both TSV plugins now compile against commons rather than prebuilt toolbox jars.
TSV no longer packages or loads `tsv-2.1.0-jar-with-dependencies.jar`,
`tsv-extended-0.0.1-SNAPSHOT.jar`, `platform-dependent.jar` or `ybootstrap.jar`.
The three unused TSV jar files have also been removed from the plugin `lib/`
directories. Historical releases under `updatesite/neon/` remain unchanged.
No Spring, Groovy, JAXB or customer class loading is used
by the new TSV code. The separate toolbox checkout was not changed.

The standard project builder uses the original **16 XPath checks**. Extended
analysis keeps 15 other XPath checks and replaces the two immediate-parent
deployment checks with their updated hierarchy-aware versions below. This avoids
duplicate findings for the same rule identifiers.

## Adopted schema rules

1. `NoDeploymentTableShouldExistForItemIfNotExtendingGenericItem`
2. `DeploymentTableMustExistForItemExtendingGenericItem`
3. `TypeHasAtLeastOneIndexSpecified`
4. `NoDatabaseIndexesForUniqueAttributesOfType`
5. `CatalogVersionAttributeShouldBeMarkedUniqueForCatalogAwareTypes`
6. `UniqueAttributesDontMatchCatalogUniqueAttributeKey`
7. `One2ManyRelationWhereTheOneSideIsCatalogAwareTypeAndTheManySideIsNot`
8. `NoUniqueAttributesDefinedForType`
9. `UniqueIndexUsingNonUniqueAttributes`

Index matching compares qualifier sets, not only attribute counts. Inherited
indexes are included, excluding GenericItem's indexes as in the reference.
Named index removals are applied. Catalog keys can be covered by unique attribute
modifiers or a covering unique index. Relation-end modifiers map to attributes on
the opposite item, allowing relation-declared catalogVersion uniqueness to work.
Catalog-aware one-to-many checks handle both orientations and partof endpoints.

## Inputs and lifecycle

Extended analysis takes extension paths/order from the workspace-persisted shared
`SystemConfig`. The picker defaults custom extensions to selected; templates and
platform/modules extensions remain selectable but unselected. Template detection
uses the reference convention: a `y`-prefixed name plus `extgen.properties`.

All imported items XML is loaded in extension dependency order for cross-project
metadata, while findings are generated for the user's selected files. File lookup
supports `<extension>-items.xml`, `<extension>.items.xml` and `items.xml` under
`resources`. Missing or malformed imported files produce errors and prevent
schema checks from silently passing on partial data. Unknown types/redeclarations,
missing ancestors and unevaluable custom-property expressions produce SKIPPED.

XML schema snapshots are rebuilt **on each extended analysis**, not persisted as
a second repository. The existing generated-model type/attribute index remains
workspace-persisted and post-build refreshed. The standard TSV builder continues
to validate changed items XML during IDE builds. Extended schema analysis is an
explicit command, not a new automatic builder.

The new **TSV** menu provides standard and imported-type-system analysis. The
existing results editor is registered and reads/writes the same
`validation_results` XML shape through JDK XML APIs, without JAXB. Reports include
failures, errors and skipped checks; passes are omitted. Basic build markers no
longer dereference a nonexistent rule object for XML parse failures.

## Deliberate boundaries

- This implements the nine packaged policies, not arbitrary Groovy rule scripts.
- It is not a replacement for the full SAP items.xsd validation/bootstrap model.
- Java expressions in custom-property values are not executed. Relevant checks
  are skipped unless qualifier values can be interpreted as literals.
- Legacy `*-advanced-deployment.xml` metadata and implicit metadata only created
  by SAP bootstrap code are not imported. Missing deployment evidence is skipped
  rather than treated as a successful deployment check.
- Full real-platform GUI acceptance testing is still needed: open the menu,
  select custom extensions, run analysis, review source navigation and cancellation.

## Verification

`make test` runs headless Equinox tests for the nine policies, inheritance,
redeclarations, removed indexes, relation-mapped uniqueness, both relation
orientations, missing metadata, malformed XML, entity rejection, cancellation,
report round trips, and actual standard-builder marker creation/cleanup.
