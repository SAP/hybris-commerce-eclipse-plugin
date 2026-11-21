## The ImpEx CSV parser
ImpEx reads a file with its own CSVReader. In 2016 I referred to RFC 4180 as the specification. That was a mistake: this parser has its own rules.

Quotes don't protect spaces
A quoted value is trimmed at the edges exactly like an unquoted one:


Example.

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-a1;"   padded   "
```

```
[ih-a1] bar=[padded]
```
There is no way to import a value with leading or trailing spaces through ImpEx, except with a cell decorator or a translator that adds them back. Spaces inside a multiline value are preserved: only the edges of the whole value are trimmed.

### A quote starts quoting only at the beginning of a cell
In the middle of a value, a quote is an ordinary character:
```impex
;ih-a2;abc"def
```

```
[ih-a2] bar=[abc"def]
```

But a non-whitespace character after the closing quote is an error:
```impex
;ih-a3;"abc"def
```
result
```
unexpected char d after cell end at 12 in line 1 : ';ih-a3;"abc"def'
```

People often trip over this with inches. A backslash in ImpEx does not escape a quote, so `"5\" monitor"` is the cell 5\, closed by a quote and followed by monitor:
```
unexpected char m after cell end at 12 in line 2 : ';ih-b2;"5\" monitor'
```

A quote inside quotes is doubled: `"5"" monitor"`.

### An unmatched quote and an error on the wrong line
If a line ends "inside quotes," the parser assumes the value continues and appends the next line. Then another one, until the quotes balance or the file ends.

```groovy
// illustration
boolean isWrappedLine(String line, boolean alreadyQuoted) {
    boolean quoted = alreadyQuoted, cellStarted = alreadyQuoted, cellEnded = false;
    for (char c : line) {
        if (quoted) {
            if (c == '"') { if (next == '"') skip(); else { quoted = false; cellEnded = true; } }
        } else if (c == '"') {
            if (cellEnded)    throw new IllegalStateException("unexpected text separator ...");
            if (!cellStarted) { quoted = true; cellStarted = true; }   // only at the start of a cell
        } else if (c == ';') {
            cellStarted = cellEnded = false;
        } else if (!isWhitespace(c)) {
            if (cellEnded)    throw new IllegalStateException("unexpected char " + c + " after cell end ...");
            cellStarted = true;
        }
    }
    return quoted;   // true → glue the next line
}
```


Illustrative reconstruction, not platform source code. See the note above.

Example. Here the second data line is to blame, with an opening quote and no closing one:

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-e1;ok
;ih-e2;"27 inch monitor
;ih-e3;fine
;ih-e4;"bold";x
;ih-e5;last
```

Result:
```
[ih-e1] bar=[ok]
[ih-e5] bar=[last]
```

Three lines are gone, and the log says:
```
unexpected char b after cell end at 8 in line 2 : ';ih-e4;"bold";x'
```

The line in the message, ;ih-e4;"bold";x, is correct. The error happened when the parser, gluing lines after ih-e2, reached the quote in ih-e4 and took it for a closing one. The number line 2 points to the real cause (ih-e2 is line 2 if you count from zero and include the header), while the text points to ih-e4. And ih-e3, which doesn't appear in the message at all, wasn't imported either.

If there are no more quotes until the end of the file, the message is more accurate:

```
reached EOF and got odd number of '"'! File is corrupt. Error starts in line:
";ih-c2;"27 inch monitor" and occurs somewhere in the following lines.
```

If more than csv.maxmultilines lines get glued together (10,000 by default), the parser stops with a separate message.

None of these errors are visible in HAC. The result block says "Import has encountered problems." and there is nothing below it. The error text is only in the server console log.

### Line breaks and backslashes are two different mechanisms
Quotes preserve a line break inside a value:

```
;ih-a4;"line1
line2"
```
```
[ih-a4] bar=[line1\nline2]
```
A backslash at the end of a line joins it with the next one directly, with no space and no line break:

```
;ih-a5;part1\
part2
```

```
[ih-a5] bar=[part1part2]
```


This is a correction to the 2016 article. Back then I wrote that \ cannot be used as a multiline separator in a data block. On 2211 that is not true: in the example above, \ is in a data line, and the joining works.

### A comment is determined by the first cell
A line is a comment if its first cell starts with #. So # in the middle of a line is ordinary data:

```
;ih-a7;value # not a comment
```

```
[ih-a7] bar=[value # not a comment]

```

But this is a comment, even though it looks like data:
```
"# quoted comment?";ih-a7b;x
```

The check runs after the cell has been extracted from its quotes. So a quoted first cell that starts with # is also a comment. This is exactly why quoted code lines like "#% ...", described below, work.

## An empty cell, <ignore>, and <null>

How the three options behave on 2211:

| In the cell | String attribute | Reference
|-----|--------|--------|
| empty |	`null` is written | 	`null` is written |
| `<ignore>`| 	the attribute is left untouched | 	the attribute is left untouched |
| `<null>` | 	the string `<null>` is written | it looks for an item whose key is `null` |


Example.

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-m1;start
;ih-m2;start
;ih-m3;start
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-m1;
;ih-m2;<ignore>
;ih-m3;<null>
```

```
[ih-m1] bar=[null]
[ih-m2] bar=[start]
[ih-m3] bar=[<null>]
```

`<ignore>` does what you expect. But `<null>` is used for lookups in a reference. Try clearing a reference:

```impex
INSERT_UPDATE Employee;uid[unique=true];sessionLanguage(isocode)
;ih-emp2;<null>
```
```
column 2: cannot resolve value '<null>' for attribute 'sessionLanguage'
```
ImpEx looked for a language with isocode IS NULL and didn't find one. To clear a reference, you need an empty cell.

`<null>` is meant for composite keys where one of the parts is empty. For example, an address with no town:

```impex
INSERT Address;owner(Employee.uid);streetname;town
;ih-emp1;ih-street;
;ih-emp1;ih-street;Berlin

INSERT_UPDATE Employee;uid[unique=true];defaultShipmentAddress(streetname,town)
;ih-emp1;ih-street:<null>
;ih-emp2;ih-street:Berlin
```

```
ih-emp1 -> ih-street/null
ih-emp2 -> ih-street/Berlin
```


Under the hood, the engine simply rewrites the query text: every =?parameter whose value turns out to be null is replaced with IS NULL.

```groovy
// illustration
for (Map.Entry<String, Object> e : values.entrySet()) {
    if (e.getValue() == null) {
        query = query.replaceAll("=\\?" + e.getKey(), " IS NULL ");
    }
}
```


Illustrative reconstruction, not platform source code. See the note above.

Case doesn't matter: `<NULL>` works the same way.

### Resolving references with one UNION ALL for the whole line

If a line has more than one reference column (catalogVersion(...), unit(code), supercategories(code)), ImpEx resolves them all with one query. Here is what the JDBC log shows for such a line:

```impex
INSERT_UPDATE Employee;uid[unique=true];sessionLanguage(isocode);sessionCurrency(isocode);defaultShipmentAddress(streetname,town)
;ih-emp6;fr;USD;ih-street-q3:Paris
```

```sql
SELECT x.a, x.b FROM (
    SELECT item_t0.PK AS a, 0 AS b FROM languages  item_t0
     WHERE ( item_t0.p_isocode =?) AND (item_t0.TypePkString=? )
  UNION ALL
    SELECT item_t1.PK AS a, 1 AS b FROM currencies item_t1
     WHERE ( item_t1.p_isocode =?) AND (item_t1.TypePkString=? )
  UNION ALL
    SELECT item_t2.PK AS a, 2 AS b FROM addresses  item_t2
     WHERE ( item_t2.p_streetname =? AND item_t2.p_town =?) AND (item_t2.TypePkString=? )
) x
```

Column b is the translator number. It is used to distribute the results back to the columns. The parameters of each subquery are renamed (?v0 → ?t0v0, ?t1v0, …) so they don't collide.

```groovy
// illustration
sb.append("SELECT x.a, x.b FROM (");
for (int i = 0; i < translators.size(); i++) {
    if (i > 0) sb.append(" UNION ALL ");
    sb.append("{{").append(t.select()).append(" AS a, ").append(i).append(" AS b ")
      .append(t.body().replaceAll("\\?v", "?t" + i + "v")).append("}}");
}
sb.append(") x");
```

Illustrative reconstruction, not platform source code. See the note above.

The same mechanism works for collection elements in a single cell. Three groups in groups(uid) are also one query:

```sql
SELECT x.a, x.b FROM (
    SELECT item_t0.PK AS a, 0 AS b FROM usergroups item_t0 WHERE ( item_t0.p_uid =?) AND (...)
  UNION ALL
    SELECT item_t1.PK AS a, 1 AS b FROM usergroups item_t1 WHERE ( item_t1.p_uid =?) AND (...)
  UNION ALL
    SELECT item_t2.PK AS a, 2 AS b FROM usergroups item_t2 WHERE ( item_t2.p_uid =?) AND (...)
) x
```

### The query goes through the FlexibleSearch cache
This is a regular FlexibleSearch query with a regular cache key. When I repeated the import with the same combination of language, currency, and address but a different uid, the query was not in the JDBC log: the result came from the cache. If thousands of lines reference the same catalog version and the same unit, this saves queries. If the combination of values is unique in every line, the cache fills up with single-use entries. And this is one more reason why the NOW() problem from the FlexibleSearch article applies to ImpEx too.

### Chunks
The combined query is split into chunks, but not on every database. The default chunk size depends on the database:
```groovy
// illustration
int defaultChunkSize() {
    boolean bigQueriesAreAProblem = isHana() || isOracle() || isSqlServer() || isMySQL8();
    return config.getInt("impex.query.chunk.size", bigQueriesAreAProblem ? 400 : 0);
}
```

Illustrative reconstruction, not platform source code. See the note above.

0 means "don't split." On HANA, Oracle, SQL Server, and MySQL 8, the query is split into chunks of 400 subqueries; on the rest, which in practice means local HSQLDB, it is built in one piece. So locally on HSQLDB you are testing a different query from the one that will run in production in SAP Commerce Cloud on SQL Server. The query-chunk-size column modifier overrides this value, but only for collection elements in that column. The combined query across the columns of a line always uses the global value. If an import fails with "too many tables in query" on a line with a large collection, the modifier helps. If it fails on a line with many reference columns, only impex.query.chunk.size helps.

### Turning it off
impex.query.combined=false (default true) brings back one query per cell. Sometimes this is the only way to get an import working on a database that chokes on long UNIONs.

### Multi-pass import and the stop condition
Lines whose references didn't resolve are not treated as errors right away. They are written to a dump, and the dump is imported again. The classic case is a forward reference:

```impex
INSERT_UPDATE Category;code[unique=true];catalogVersion(catalog(id),version)[unique=true]
;ih-cat-root;ih-catalog:Staged
INSERT_UPDATE CatalogVersion;catalog(id)[unique=true];version[unique=true]
;ih-catalog;Staged
INSERT_UPDATE Catalog;id[unique=true]
;ih-catalog
```

The category references a catalog version, which references a catalog, which is declared last. In the log:

```
Finished 1 pass ... - processed: 3, dumped: 2 (last pass: 0)
Starting pass 2
Finished 2 pass ... - processed: 2, dumped: 1 (last pass: 2)
Starting pass 3
Finished 3 pass ... - processed: 1, no lines dumped (last pass 1)
```

The import took three passes, one per link in the chain.

Now a reference that will never resolve:

```impex
INSERT_UPDATE Employee;uid[unique=true];sessionLanguage(isocode)
;ih-emp9;no_such_lang
```

```
Finished 1 pass ... - processed: 1, dumped: 1 (last pass: 0)
Starting pass 2
Finished 2 pass ... - processed: 1, dumped: 1 (last pass: 1)
Can not resolve any more lines ... Aborting further passes (at pass 2).
Finally could not import 1 lines!
```


The condition for continuing looks like this:
```groovy
// illustration
boolean anotherPass =
       (maxPass < 0 || currentPass <= maxPass)                 // maxPass defaults to -1: no limit
    && (currentPass == 1 || dumpedNow < dumpedLastPass);      // strictly fewer than last time
```


Illustrative reconstruction, not platform source code. See the note above.

The second pass always runs if anything is left after the first. The third and later passes run only if the number of deferred lines has strictly decreased. What follows from this in practice:

A circular dependency in which neither item can be created without the other will never resolve. The import will stop at the second pass.
The Aborting further passes message means the number of deferred lines stopped decreasing. There is no limit on attempts by default at all.
A long dependency chain in a file costs a pass per link, and each pass rereads the whole dump. So the order of blocks affects import speed too.


### A line that wasn't imported can leave an item in the database
Let's look at what is in the dump for an unresolvable line (HAC shows it in the result block):

```impex
INSERT_UPDATE Employee;uid[unique=true];sessionLanguage(isocode)
Employee,8809954312196,,,column 2: cannot resolve value 'no_such_lang' for attribute 'sessionLanguage';<ignore>ih-emp9;no_such_lang
```


The first cell contains the type and the PK. The key column is marked <ignore>. This means the employee ih-emp9 was already created in the first pass with all the attributes that could be resolved. Only the unresolved part is deferred, to be written later by PK. And if the import ultimately "failed," the item stays in the database, without a language, but it exists:

```
userService.getUserForUID("ih-emp9")  →  EmployeeModel (8809954312196@1)
```

The import is not rolled back: after "Finally could not import 1 lines!" an incomplete item may remain in the database.

### Validation errors also go to the second pass
If a save failed with a ModelSavingException (a validator, an interceptor, a uniqueness violation), the line is not considered finally failed. It goes to the dump with the message Failed to process item creation due to ModelSavingException. Retrying in the next turn. and runs again on the next pass. That's why validation error messages in ImpEx often look like this, with two line numbers:

```
Exception : line 4: cannot create Title ... found 1 item(s) using the same keys,
Exception : line 3: cannot create Title ... found 1 item(s) using the same keys
```

It is the same line: the number from the main file and the number from the second-pass dump. Interceptors with side effects will run twice on such a line.

There is an undocumented key impex.retry.on.interceptorexceptions.only (default false, not present in any shipped .properties file) that narrows the retry to cases where the cause was specifically an InterceptorException.

## Modifiers
The documentation describes about fifteen modifiers. The platform has 29 in a single list of constants, the header reads three more separately, and there is testMode, which is read directly in the code and declared nowhere.

| Modifier | 	Where	What it does | 
|----|----|
| unique | column | part of the key used to find an existing item |
| allownull | column | allow null for a mandatory attribute|
| ignorenull | column | skip null collection elements| 
| default | column | value for an empty cell|
| virtual | column | the column is absent from the data, only default|
| lang | column | language of a localized attribute|
| translator | column | custom value translator|
| cellDecorator | column | custom cell decorator|
| dateformat, numberformat | column | parsing formats|
| collection-delimiter | column | , by default|
| path-delimiter | column | : by default|
| key2value-delimiter | column | -> by default|
| map-delimiter | column | ; by default|
| mode | column | append / remove / merge for collections|
| query-chunk-size | column | chunk of the combined query, only for collection elements|
| forceWrite | column | write to a non-writable attribute|
| pos | column | explicit column position|
| alias | column | column name on export|
| sourceSystemId, pkFromExternalImportKey | column | external import keys|
| batchmode | header | UPDATE/REMOVE by a non-unique key, many items|
| cacheUnique | header | key → PK cache for the whole type|
| processor | header | custom ImportProcessor|
| parallel | header | false runs this block in one thread|
| ignoreKeyCase | header | case-insensitive key lookup|
| unlessExisting | header | skip existing items in INSERT|
| impex.legacy.mode | header | Jalo instead of ServiceLayer for this block|
| sld.enabled | header | read only by distributed ImpEx|
| disable.interceptor.types | header | disable interceptors by type (validate, prepare, …)|
| disable.interceptor.beans | header | disable interceptors by bean name|
| disable.UniqueAttributesValidator.for.types | header | disable the uniqueness check|
| testMode | header | with cacheUnique: check the cache against the database on every line|

The last one, testMode, is a debug mode for cacheUnique. It rechecks every cache answer with a regular query and fails on a mismatch. It is not in the list of constants.

Below are the ones that behave in non-obvious ways.

### Without forceWrite the write is skipped with no error

Every item has the sealed attribute, and it is not writable. Let's try writing it with and without the modifier:

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-s1;x
;ih-s2;x
UPDATE TestItemType2;foo[unique=true];sealed
;ih-s1;true
UPDATE TestItemType2;foo[unique=true];sealed[forceWrite=true]
;ih-s2;true
```

```
[ih-s1, null, ...]
[ih-s2, true, ...]
```


HAC says "Import finished successfully." The ih-s1 line didn't change, and the only trace of this is a WARN in the console log:
```
column sealed of type TestItemType2 is not writable, but you want to update it.
If you want to write this non writable attribute explicit, you have to use the
forceWrite=true modifier. This warning is printed only once for header.
Please check subsequent lines for similar problem.
```

"Printed only once for header" explains why in a large import you see one warning and think the problem is a one-off.

An attribute that can only be set on creation (initial) can't be updated even with forceWrite:

```impex
UPDATE TestItemType2;foo[unique=true];creationtime[dateformat=yyyy-MM-dd,forceWrite=true]
;ih-a4;2001-01-01
```

```
attribute de.hybris.platform.jalo.test.TestItem.creationtime cannot be changed except during item creation
```


### unlessExisting works only in INSERT and only with the value true

```impex
INSERT TestItemType2[unlessExisting=true];foo[unique=true];bar
;ih-a1;overwritten?
;ih-u1;new
INSERT TestItemType2[unlessExisting=yes];foo[unique=true];bar
;ih-a2;overwritten?
INSERT_UPDATE TestItemType2[unlessExisting=true];foo[unique=true];bar
;ih-a4;overwritten-by-insert-update
```


Result:

ih-a1 is untouched, ih-u1 is created, so the modifier works.
unlessExisting=yes is false: Cannot insert. Item exists: [8796093153305]. The value is parsed with Boolean.valueOf, so anything other than true in any case counts as false, and nothing is written to the log.
In INSERT_UPDATE the modifier is ignored with no warning: ih-a4 is overwritten.

### ignoreKeyCase works only on the header

The modifier looks like a column modifier, since it is about comparing the key value. But the lookup of an existing item reads it from the header:


```impex
INSERT_UPDATE TestItemType2;foo[unique=true,ignoreKeyCase=true];bar
;IH-A2;matched-case-insensitively?
INSERT_UPDATE TestItemType2[ignoreKeyCase=true];foo[unique=true];bar
;IH-A4;header-level
```

```
ih-a2 -> abc"def                          ← not found
IH-A2 -> matched-case-insensitively?      ← new item created
ih-a4 -> header-level                     ← found and updated
```

On a column the modifier is ignored, with no warning.

Since we're talking about case: attribute names in the header are case-insensitive. INSERT_UPDATE TestItemType2;foo[unique=true];BAR writes to bar.

### cacheUnique loads the keys of the whole type

When the header starts, the engine fetches the keys of all items of the type:

```impex
INSERT_UPDATE Employee[cacheUnique=true];uid[unique=true];name
;ih-emp6;Cached
```

```
SELECT item_t0.PK, item_t0.p_uid FROM users item_t0 WHERE (item_t0.TypePkString=? )
```


No WHERE on the values. For a large update that touches most of the table, this pays off: one query instead of thousands. For a ten-line file against a ten-million-row table, it means reading ten million rows for the sake of ten.

In an INSERT without a unique column, the modifier is not allowed:

```
You can not use cacheUnique modifier in INSERT mode when no column is unique,
use UPDATE / INSERT_UPDATE mode or declare at least one column as unique
```

And if the database already has two items with the same key value, the whole header fails: found more than one item for unique key values .... So cacheUnique also finds key duplicates that already exist in the database.

### Collection prefixes (+), (-), and the undocumented (+?)

The 2016 article had (+) and (-). There is a third prefix:

| Prefix | Action |
|----|-----|
| (+) |	add the element, always
| (-) |	remove the element
| (+?) |	add it if it isn't there yet


The difference between (+) and (+?) shows on a list attribute. fallbackLanguages on Language is a list:

```impex
UPDATE Language;isocode[unique=true];fallbackLanguages(isocode)
;xx_test;en
;xx_test;(+) de
;xx_test;(+) de
;xx_test;(+?) en
;xx_test;(+?) fr
```

```
[en, de, de, fr]
```


(+) added `de` twice. (+?) didn't add the existing `en`. Sets (Set, for example groups on a user) won't have duplicates, so the difference only shows on lists.

Prefixes can't be mixed with "bare" values in the same cell:

```
;ih-emp7;ih-g2,(+) ih-g3
```

```
Invalid syntax! The assigned collectionvalues must either have one of the prefixes
'(+)' or '(-)' or modifier mode must be set
```


The mode=append|remove|merge modifier on a column sets the action for all values in the column at once. Without it, every value in the cell needs a prefix, not just the first one.

### A header that turns off business logic

Three header modifiers are not in the common list, but are read separately and put into the parameters of the session in which the save runs:

```
disable.interceptor.types=validate,prepare
disable.interceptor.beans='beanName1,beanName2'
disable.UniqueAttributesValidator.for.types='Title,Product'
```


These modifiers turn off validation and business logic for one block of the file. An example is the interceptor that disables login for users.

### ImpEx disables login for users

The impex extension registers the disableLoginForImportedUserInterceptor interceptor. It fires on any User that ImpEx creates or modifies and sets loginDisabled=true if the user has no password.

```impex
INSERT_UPDATE Employee;uid[unique=true];name
;ih-emp10;plain
INSERT_UPDATE Employee[disable.interceptor.beans='disableLoginForImportedUserInterceptor'];uid[unique=true];name
;ih-emp11;interceptor off
UPDATE Employee;uid[unique=true];name
;ih-emp-sl;just renamed
```


The user ih-emp-sl had been created beforehand through ServiceLayer, with no password and loginDisabled=false. Result:

```
ih-emp10  loginDisabled=true   name=plain
ih-emp11  loginDisabled=false  name=interceptor off
ih-emp-sl loginDisabled=true   name=just renamed
```


### A plain UPDATE of the name through ImpEx blocked the user's login. If you have users without a local password (SSO, technical accounts), any ImpEx that touches them will disable their login without any error.

The log shows:

```
Disabling login for 'null' because of empty password.
```

null, because the message prints the PK, and on creation there isn't one yet.

How does the interceptor know ImpEx is running? From the session attributes that ImpEx sets for the duration of the save: impex.creation and impex.modification together with impex.modification.pk. Your own interceptor can check these attributes too, to behave differently during import.

The interceptor is marked @Deprecated(since = "2211", forRemoval = true) but is enabled by default. It is turned off globally by the property disable.login.for.imported.user.interceptor.enabled=false, which appears in the shipped project.properties only in commented-out form.

disable.UniqueAttributesValidator.for.types
The modifier does turn off the validator, which you can see from how the error changes:

```impex
INSERT Title;code
;ih-title
INSERT Title[disable.UniqueAttributesValidator.for.types='Title'];code
;ih-title
```

```
... due to [UniqueAttributesInterceptor]:ambiguous unique keys {code=ih-title} ... found 1 item(s) using the same keys
... due to Duplicate code 'ih-title' for type Title
```


The first line was stopped by the uniqueness validator, the second by a check further down the stack. Title has one. When you disable the validator, you are relying on your type having one too. Check this on the specific type before leaving the modifier in the file.

A practical rule: run grep -rn "disable\.\(interceptor\|UniqueAttributesValidator\)" --include=*.impex . across the whole project. These modifiers usually show up "temporarily."

### Rewriting headers with impex.header.replacement
The documentation doesn't describe this mechanism, although the shorthands E1p, E1t, E1d, and C@ are used in every project with prices and classification. They are not ImpEx syntax but rules from advanced.properties:

```impex
impex.header.replacement.1=C@(\\w+) ... @$1[system='\\$systemName',version='\\$systemVersion',translator='...ClassificationAttributeTranslator']
impex.header.replacement.2=E1p ... europe1Prices[translator='de.hybris.platform.europe1.jalo.impex.Europe1PricesTranslator']
impex.header.replacement.3=E1t ... europe1Taxes[translator='...Europe1TaxesTranslator']
impex.header.replacement.4=E1d ... europe1Discounts[translator='...Europe1DiscountsTranslator']
```


Format: impex.header.replacement.<priority> = <regex> ... <replacement>, with three dots as the separator. The rules are applied like this:

```groovy
// illustration
for (int i = 1; i < columns.size(); i++) {          // column 0 (mode and type) is not touched
    for (Rule r : rulesByPriorityDesc) {
        String replaced = columns.get(i).replaceAll(r.pattern, r.replacement);
        if (!replaced.equals(columns.get(i))) {
            columns.set(i, expandMacros(replaced));  // macros are expanded after the replacement
            break;                                   // the first rule that matches is the last one
        }
    }
}
```


Illustrative reconstruction, not platform source code. See the note above.

The properties are read every time a reader is created, so a rule can be added without a restart. I added one from the Groovy console:

```impex
de.hybris.platform.util.Config.setParameter(
    "impex.header.replacement.10", "barD ... bar[default='from-rule']")

```

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];barD
;ih-h3;
;ih-h4;explicit
```

```
[ih-h3] bar=[from-rule]
[ih-h4] bar=[explicit]
```


This is why the rule for C@ has \\$systemName: the replacement inserts a macro reference into the column, and the macro is expanded in the next step.

The regex is not anchored: replaceAll looks for a match anywhere in the column name:

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];oldbarD
```

```
INSERT_UPDATE TestItemType2;foo[unique=true];oldbar[default='from-rule']
# unknown attribute 'oldbar' in header 'INSERT_UPDATE TestItemType2'
```


The oldbarD column turned into oldbar[...]. With the standard rules, this means an attribute whose name contains E1p, E1t, or E1d will be mangled before the header is even parsed. Write your own rules with anchors: ^MyAttr$ ... myRealAttribute[...].

Programmatically, reader.addHeaderReplacementRule(src, tgt, prio) does the same.

### Code inside ImpEx

Lines starting with #% are code. By default (impex.legacy.scripting=true) it is executed by BeanShell. Half of the 2016 article was devoted to this. Almost all of it works, but there are a few differences I didn't write about back then.

Without Enable code execution an if: block is always true
In the HAC import form, the Enable code execution checkbox is unchecked by default. In that case code is skipped with a WARN in the log. With an if: block it gets more interesting:


```impex
INSERT_UPDATE TestItemType2;foo[unique=true];bar
"#% if: ""a"".equals(""b"")"
;ih-nocode-inside-false-if;imported anyway?
#% endif:
```


Without Enable code execution:

```
[ih-nocode-inside-false-if] bar=[imported anyway?]
```


HAC: "Import finished successfully." The line was imported even though the condition is false: when code execution is off, the condition is not evaluated, and the engine treats it as true:


```groovy
// illustration
if (!(ifLine.getResult() instanceof Boolean)) {
    result = codeExecutionEnabled
        ? Boolean.FALSE    // invalid expression: "false", with a WARN
        : Boolean.TRUE;    // code disabled: everything inside if: is imported
}
```

Illustrative reconstruction, not platform source code. See the note above.

If you use if: for "this block is dev only" or "only if the extension is installed," a file imported without the checkbox imports everything.

### A semicolon inside code disappears

A code line is also a CSV line. A ; in it is a cell separator. In the default BeanShell mode, the engine assembles the code by concatenating all cells with no separator:

```impex
#% impex.info("IH1 a;b;c");
"#% impex.info(""IH2 a;b;c"");"
```

```
IH1 abc
IH2 a;b;c
```

If there are two statements, you get a syntax error whose cause is hard to figure out:

```impex
#% counter = counter + 1; impex.info("counter=" + counter);
```

```
inline evaluation of: ``counter = counter + 1impex.info("IH3 counter=" + counter);''
Encountered "impex" at line 1, column 22.
```

Hence the rule I followed in 2016 without explaining it: code in ImpEx is always written in quotes, and quotes inside are doubled.

### One interpreter per file

One BeanShell interpreter is created per reader and lives until the end of the file. Variables survive headers:

```impex
#% counter = 1;
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-bsh1;x
INSERT_UPDATE TestItemType2;foo[unique=true];bar
"#% impex.info(""counter still visible="" + counter);"
```

```
counter still visible=1
```

The context always contains:

| Variable |	What it is |
|-----|------|
| impex |	the reader itself (ImpExImportJob$MyImpExImportReader when importing from HAC) |
| jaloSession | 	the current Jalo session |
| line |	Map<Integer,String> of the current data line |
| currentLineNumber, currentLocation | 	position in the file |

Plus the pre-imported packages de.hybris.platform.core, ...core.model.user, ...util, ...impex.jalo, ...jalo, ...jalo.c2l, ...jalo.user, ...jalo.flexiblesearch. That's why you can write Config.getBoolean(...) in code without an import.

### beforeEach: can rewrite or drop a line

The mechanism from the 2016 example with languages still works:

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];bar
"#% beforeEach: if (line.get(Integer.valueOf(2)).startsWith(""skip"")) line.clear(); else line.put(Integer.valueOf(2), line.get(Integer.valueOf(2)).toUpperCase());"
;ih-be1;make me loud
;ih-be2;skip me
#% beforeEach:end
;ih-be3;untouched
```

```
[ih-be1] bar=[MAKE ME LOUD]
[ih-be3] bar=[untouched]
```

line.clear() is an undocumented way to filter out a line by a condition: an empty line is simply not imported. afterEach: works the same way, but after the item is saved, and it is called on every line (forEach: is its deprecated name and logs a WARN when used).

### getLastImportedItem() returns a Jalo item

Even in ServiceLayer Direct mode:

```impex
"#% impex.info(""IH-LAST "" + impex.getLastImportedItem().getClass().getName());"
```

```
IH-LAST de.hybris.platform.jalo.test.TestItem
```


This is exactly why impex.getLastImportedItem().calculate() worked in the 2016 order example: it is a Jalo method of a Jalo object. To work with the model, you need modelService.get(impex.getLastImportedItem()).

### Two 2016 recipes that no longer work
addDefinition from code. In 2016 you could define a macro from BeanShell. Now the method is protected, and BeanShell can't see it:

```impex
#% impex.addDefinition("$dyn=from-code");
```

```
Error in method invocation: Method addDefinition ...
```


Meanwhile, the unknown macro $dyn further down the file doesn't cause an error but stays in the data literally: bar=[$dyn]. A working workaround is to insert a definition line into the stream:


```impex
"#% m = new HashMap(); m.put(Integer.valueOf(0), ""$dyn=from-code-"" + (6 * 7)); impex.insertLine(m);"
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-def3;$dyn
```

```
[ih-def3] bar=[from-code-42]
```


includeSQLData with three arguments. The includeSQLData(url, driver, sql) signature from the 2016 article doesn't exist. There is (url, user, password, driverClass, sql), described below.

$config- still works, though, if commerceservices is installed:

```impex
UPDATE GenericItem[processor=de.hybris.platform.commerceservices.impex.impl.ConfigPropertyImportProcessor];pk[unique=true]
$workers=$config-impex.import.workers
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-cfg1;$workers
;ih-cfg2;$config-impex.legacy.scripting
```

```
[ih-cfg1] bar=[4]
[ih-cfg2] bar=[true]
```

### #%groovy% with impex.legacy.scripting=true

There is also a "modern" syntax that names the engine: #%groovy% ..., #%javascript% .... But it works only with impex.legacy.scripting=false. With the default value, the whole line, groovy% included, goes to BeanShell:

```impex
#%groovy% impex.info("IH-GROOVY engine=" + this.getClass().getName())
```

```
IH-GROOVY engine=bsh.XThis
... inline evaluation of: ``groovy% impex.info(...)'' : illegal use of undefined variable, class, or 'void' literal
```


The log message appeared before the error. BeanShell read groovy% impex.info(...) as the expression "groovy modulo impex.info(...)," evaluated the right operand, which means it called the method, and failed on the left one. If such a line contains something with a side effect, the side effect will happen, and the import will report an error.

With impex.legacy.scripting=false, the same line is executed by Groovy (engine=Script73). But this mode has its own differences: the code is taken only from the first cell, that is, it is cut off at the first unquoted ; rather than concatenated. Old files with BeanShell code that has semicolons inside will break in a new way after the switch.

### A code line stops parallel processing
Import is multithreaded by default: the reader distributes data lines into a queue, and workers save them. If the file has a code line, it has to see the result of all lines above it. So before each standalone #% line and before each new header, the reader waits for the queue to empty:

```groovy
// illustration
@Override
protected void execute(CodeLine line, Map csvLine, boolean perLineHook) {
    if (!perLineHook) {
        waitForEmptyValueLineQueue();   // barrier: all workers must finish their work
    }
    super.execute(line, csvLine, perLineHook);
}
```

Illustrative reconstruction, not platform source code. See the note above.

A #% impex.info("progress") line after every hundred data lines turns a multithreaded import into a sequence of short parallel stretches. The interpreter itself is guarded by synchronized, so beforeEach: / afterEach: also run strictly one at a time.

The parallel=false header modifier switches a specific block to one thread without touching the others.

## Files, the classpath, and other databases

### Reading server files with file:, zip:, and jar:
A @media column with MediaDataTranslator accepts a path with a prefix:

```impex
INSERT_UPDATE Media;code[unique=true];catalogVersion(catalog(id),version)[unique=true];mime;@media[translator=de.hybris.platform.impex.jalo.media.MediaDataTranslator]
;ih-m-file;ih-catalog:Staged;text/plain;file:/path/on/server/srvfile.txt
;ih-m-zip;ih-catalog:Staged;text/plain;zip:/path/on/server/arch.zip&inner/note.txt
;ih-m-jar;ih-catalog:Staged;application/octet-stream;jar:de.hybris.platform.impex.jalo.ImpExManager&ImpExManager.class
```

```
ih-m-file size=37    data=hello from the app server filesystem
ih-m-zip  size=24    data=hello from inside a zip
ih-m-jar  size=29123 data=����...de/hybris/platform/impex/jalo/GeneratedImpExMan...
```


file: reads any file accessible to the application server process.
zip: reads an entry from an archive on the server. The separator between the archive and the entry is &.
jar:Class&resource loads the class whose name is given in the cell and reads the resource through its class loader. In the example above, the bytecode of ImpExManager itself ended up in the media.

### http: is broken and https: is not supported

The http: prefix doesn't download the file; it writes the URL to the media. Or rather, it is supposed to:

```
;ih-m-http;ih-catalog:Staged;image/png;http://example.com/x.png
;ih-m-https;ih-catalog:Staged;image/png;https://example.com/x.png
```

```
Exception : Error while setting URL to media
Path 'https://example.com/x.png' couldn't be resolved!
```

There is no https: prefix. And http: breaks because the engine cuts off the prefix before writing the URL:

```groovy
// illustration
if (path.toLowerCase().startsWith("http:")) {
    media.setURL(path.substring("http:".length()));   // → "//example.com/x.png"
}
```


Illustrative reconstruction, not platform source code. See the note above.

What remains is //example.com/x.png, and it is rejected by MediaExternalUrlSourcesValidator, which doesn't appear in the ImpEx messages. If you repeat the write from Groovy, you see the real cause:

Access protocol is not allowed for media URL //example.com/y.png.
Only: [https, http, fromjar] are allowed.
The same happens in legacy (Jalo) mode. The workaround is to duplicate the prefix:

```impex
;ih-m-http3;ih-catalog:Staged;image/png;http:http://example.com/z.png
;ih-m-https3;ih-catalog:Staged;image/png;http:https://example.com/z.png
```

```
ih-m-http3  url=http://example.com/z.png
ih-m-https3 url=https://example.com/z.png
```

In the failed case, the Media item is created anyway, with no URL and no data. This is the same "created but not imported" effect as in the section on passes.

### Lines from another database with includeSQLData

The impex variable is the reader with all its public methods. One of them opens a JDBC connection and substitutes the query result as data lines of the current header:

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];bar
"#% impex.includeSQLData(""jdbc:hsqldb:mem:ihdemo"", ""SA"", """", ""org.hsqldb.jdbcDriver"", ""SELECT 'ih-sql-' || TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'INFORMATION_SCHEMA' ORDER BY TABLE_NAME LIMIT 3"");"
```

```
[ih-sql-ADMINISTRABLE_ROLE_AUTHORIZATIONS] bar=[VIEW]
[ih-sql-APPLICABLE_ROLES] bar=[VIEW]
[ih-sql-ASSERTIONS] bar=[VIEW]
```

The first column of the result goes to the first header column after the empty cell, the second to the second. For ETL this is a perfectly workable tool. Alongside it there is the includeExternalData(...) family for files and streams, and initDatabase(...) for several queries against the same database.

### Security

#% is arbitrary code on the server, file: and zip: are file system reads, jar: loads any class by name, and includeSQLData connects to any database the server can reach. The right to run an ImpEx import with code execution enabled is equivalent to the right to execute code on the server. Without code execution, server files can still be read through file:, zip:, and jar:.

In HAC this is an administrator operation anyway. But ImpEx is often given to content managers through Backoffice, to integrations through ImportService, and to data loading pipelines. That's where this access is worth reviewing.

The thread count is a SpEL expression
The number of workers comes from a property, and its value is a SpEL expression:

```properties
impex.import.workers=#cores * 2          # project.properties
impex.import.workers.hsqldb=1            # advanced.properties: override by database name
```

```groovy
// illustration
int defaultMaxThreads(Tenant t) {
    String expr = config("impex.import.workers." + t.databaseName().toLowerCase());
    if (isBlank(expr)) expr = config("impex.import.workers");
    return numberOfThreadsFromExpression(expr, 2);   // #cores variable, fallback value 2
}
```


Illustrative reconstruction, not platform source code. See the note above.

I ran several expressions through the same function on a machine with 14 cores:

| Expression |	Result
|-----|-----|
| #cores * 2 |	28
| #cores + 2 |	16
|4 |	4
| #core * 2 (typo)|	2, with a WARN |
| cores * 2 (no #) |	2, with a WARN |
| #cores / 0 |	2, with a WARN |
| T(java.lang.Runtime).getRuntime().availableProcessors() |	2, with a WARN|
| #cores * 2,5 |	exception SpelParseException |

The evaluation context is restricted: arithmetic on #cores is available, but class access is not, so T(...) doesn't work. A typo drops you back to two threads with no error, and all that's left is the WARN Exception has been thrown during evaluation of '...'. Null will be returned. A syntax error, on the other hand, is not caught and is thrown out.

On HSQLDB this setting has no effect: impex.import.workers.hsqldb=1 overrides the general value, and getDefaultMaxThreads returns 1. If you measure import speed locally on HSQLDB, you are measuring a single-threaded import.

### Distributed ImpEx runs on a different engine

The HAC import form has a Distributed mode checkbox. It is a separate implementation on top of the task engine: the file is cut into batches (impex.distributed.batch.size=100), the batches are stored in the database as ImportBatch / ImportBatchContent, and they are processed by tasks that can run on any node of the cluster.

I ran a file with two ordinary lines, a code line, and an unresolvable reference through it:

```impex
INSERT_UPDATE TestItemType2;foo[unique=true];bar
;ih-dist1;d1
;ih-dist2;d2
#% impex.info("IH-DIST code line");
INSERT_UPDATE Employee;uid[unique=true];sessionLanguage(isocode)
;ih-dist-emp;no_such_lang
```


The log shows that the code line ran once, when the file was being cut: IH-DIST code line appeared in the HTTP thread before the first batch processing task started. So anything that depends on order (getLastImportedItem(), counters, afterEach:) works differently in distributed mode than in regular mode.

Models are saved in bulk: Number of models saved in bulk: 1.

Retries happen in "turns" on a timer. The unresolvable line was processed four times: at 20:33:33, 20:34:03, 20:34:34, and 20:35:04, about 30 seconds apart on my installation. The stop condition is impex.distributed.noProgressionLimit=3 turns without progress. An import that would have failed in a fraction of a second in regular mode took more than three minutes in distributed mode.

After a failure, the batches remain in the database. Before the run, ImportBatch and ImportBatchContent each had zero rows; after, 12 and 3. They are deleted only on success (impex.distributed.importbatchcontent.removeonsuccess=true). If your ImportBatchContent table keeps growing, those are traces of failed distributed imports.

Distributed mode has its own combined query for resolving references, with its own switch: impex.distributed.query.for.each.line=false.

## Reference of properties documented only in comments
Most of the keys documented only in comments are in ext/impex/project.properties. Together with the keys from advanced.properties and those read only by code:

| Property |	Default |	What it does |
|------|-----|-----|
 | impex.legacy.mode | false | Jalo instead of ServiceLayer for all operations
 |  | impex.legacy.scripting | true | BeanShell with cell concatenation; false enables engines via #%groovy%, etc.
 | impex.query.combined | true | one UNION ALL per line
 | impex.query.chunk.size | 400 / 0 | 400 for HANA, Oracle, SQL Server, MySQL 8; 0 for the rest (in practice, HSQLDB)
 | impex.import.workers | #cores * 2 | SpEL; .hsqldb=1 and the like override per database
 | impex.header.replacement.N | E1p, E1t, E1d, C@ | header rewriting rules
 | impex.log.unresolved.lines | true | log deferred lines
 | impex.retry.on.interceptorexceptions.only | false | retry on the next pass only for InterceptorException
 | impex.clearing.model.context.after.processing.each.item.enabled | true | clear the model context after each item
 | disable.login.for.imported.user.interceptor.enabled | true (in code) | loginDisabled for imported users without a password
 | documentidregistry.disallow.ignored.values | true | disallow <ignore> values in & references
 | csv.maxmultilines | 10000 | how many lines can be glued together on an unmatched quote
 | impex.distributed.batch.size | 100 | batch size
 | impex.distributed.noProgressionLimit | 3 | turns without progress before stopping
 | impex.distributed.importbatchcontent.removeonsuccess | true | delete batches only on success
 | impex.distributed.query.for.each.line | false | separate switch for the combined query
 | impex.import.service.distributed.enabled | false | distributed mode for all of ImportService


 For impex.clearing.model.context.after.processing.each.item.enabled, the shipped comment itself admits that the enabled value can cause "significant performance degradation." If an import slowed down noticeably after an upgrade, this is the first candidate to check. But disabling it is a deliberate trade-off: validators will no longer be guaranteed to see fresh data.

## Checklist
Without the Enable code execution checkbox, if: blocks are imported in full. Check how your files are imported in each of the paths: HAC, Backoffice, ImportService, system setup.
CSV errors are visible only in the console log, not in HAC. With an unmatched quote, the line number in the message is correct, but the text comes from a different line, and the lines in between disappear.
<null> does not clear a reference. You can clear it only with an empty cell.
"Could not import N lines" doesn't mean the items don't exist. The line is created in the first pass, and the deferred part may never be written.
Aborting further passes (at pass 2): the number of deferred lines stopped decreasing; look for cycles and typos in keys. There is no pass limit.
forceWrite, unlessExisting=yes, ignoreKeyCase on a column, (+) on a list, and an unknown $macro don't produce an error, but the result differs from what is written.
cacheUnique on a small file against a large table fetches the keys of the whole type.
An ImpEx UPDATE of a user without a password disables their login (disableLoginForImportedUserInterceptor). SSO and technical accounts are hit first.
Find disable.interceptor.* and disable.UniqueAttributesValidator.for.types by grepping all the .impex files in the project.
Code in ImpEx is written in quotes, otherwise ; disappears. #%groovy% with default settings runs BeanShell.
#% lines in the middle of data put barriers into a multithreaded import.
http: for media works only with a double prefix; https: is not supported.
Access to import with code equals the right to execute code on the server; without code, server files can still be read through file:, zip:, and jar:.
A growing ImportBatchContent holds traces of failed distributed imports.
Import speed on HSQLDB is single-threaded; comparing it with production is meaningless.

## Changes since 2016

Two of the 2016 recipes don't work: addDefinition from code (the method became protected) and the three-argument includeSQLData (that signature doesn't exist). One statement was wrong from the start: the backslash works in data lines too. Everything else (deletion via batchmode, if:, beforeEach: with line, $config-, (+) and (-)) works as before. Still, under every recipe there turned out to be a detail I should have mentioned back then.

What surprised me most is how many failures in ImpEx HAC doesn't report: "Import finished successfully," and the database doesn't contain what the file does. And the errors ImpEx does produce often point to the wrong place: to a different line, to the second pass instead of the first, to null instead of the user ID.

As with FlexibleSearch, for a component designed in the early 2000s and compatible with every file ever imported, this is understandable. There is one practical rule to take from this: verify the ImpEx result against the database, for example with a FlexibleSearch query on the imported keys and a comparison of item counts.
