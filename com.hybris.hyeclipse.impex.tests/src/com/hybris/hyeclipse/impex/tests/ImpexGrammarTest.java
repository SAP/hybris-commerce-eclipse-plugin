/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.tests;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.Platform;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.IToken;
import org.eclipse.tm4e.core.grammar.ITokenizeLineResult;
import org.eclipse.tm4e.core.registry.IGrammarSource;
import org.eclipse.tm4e.core.registry.Registry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ImpexGrammarTest {

	private IGrammar grammar;

	@Before
	public void loadGrammar() throws Exception {
		try (final InputStream stream = Platform.getBundle("com.hybris.hyeclipse.impex")
				.getEntry("syntaxes/impex.tmLanguage.json").openStream()) {
			grammar = new Registry().addGrammar(IGrammarSource.fromString(IGrammarSource.ContentType.JSON,
					new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
		}
	}

	@Test
	public void highlightsHeadersAndModifiers() {
		final String line = "INSERT_UPDATE Product;code[unique=true];catalogVersion(catalog(id),version)";
		assertScope(grammar.tokenizeLine(line), 0, "keyword.control.header.impex");
		assertScope(grammar.tokenizeLine(line), line.indexOf("Product"), "entity.name.type.impex");
		assertScope(grammar.tokenizeLine(line), line.indexOf("unique"), "entity.other.attribute-name.modifier.impex");
	}

	@Test
	public void distinguishesScriptsAndComments() {
		assertScope(grammar.tokenizeLine("#% beforeEach: line.clear();"), 0, "keyword.control.script.impex");
		assertScope(grammar.tokenizeLine("\"#% if: \"\"a\"\".equals(\"\"b\"\")\""), 1,
				"keyword.control.script.impex");
		assertScope(grammar.tokenizeLine("\"# quoted comment\";data"), 1, "comment.line.number-sign.impex");
		assertScope(grammar.tokenizeLine("#"), 0, "comment.line.number-sign.impex");
		assertNoScope(grammar.tokenizeLine(";code;value # not a comment"), 12, "comment.line.number-sign.impex");
	}

	@Test
	public void quoteInsideUnquotedCellDoesNotStartString() {
		assertNoScope(grammar.tokenizeLine(";code;abc\"def"), 9, "string.quoted.double.impex");
	}

	@Test
	public void doublesQuotesAndDoesNotEscapeWithBackslash() {
		assertScope(grammar.tokenizeLine(";code;\"5\"\" monitor\""), 8, "constant.character.escape.impex");
		final String line = ";code;\"5\\\" monitor";
		assertNoScope(grammar.tokenizeLine(line), line.indexOf("monitor"), "string.quoted.double.impex");
	}

	@Test
	public void preservesMultilineStringState() {
		final ITokenizeLineResult<IToken[]> first = grammar.tokenizeLine(";code;\"line1");
		final String secondLine = "line2\";plain";
		final ITokenizeLineResult<IToken[]> second = grammar.tokenizeLine(secondLine, first.getRuleStack(), null);
		assertScope(second, 0, "string.quoted.double.impex");
		assertNoScope(second, secondLine.indexOf("plain"), "string.quoted.double.impex");
	}

	@Test
	public void recognizesHyphenatedMacrosAndCollectionPrefixes() {
		assertScope(grammar.tokenizeLine("$config-value=example"), 0, "variable.other.definition.impex");
		final String line = ";$config-value;(+?) item;<ignore>";
		assertScope(grammar.tokenizeLine(line), 1, "variable.other.impex");
		assertScope(grammar.tokenizeLine(line), line.indexOf("(+?)"), "keyword.operator.collection.impex");
		assertScope(grammar.tokenizeLine(line), line.indexOf("<ignore>"), "constant.language.impex");
	}

	private void assertScope(final ITokenizeLineResult<IToken[]> result, final int offset, final String scope) {
		Assert.assertTrue("Expected " + scope + " at " + offset, hasScope(result, offset, scope));
	}

	private void assertNoScope(final ITokenizeLineResult<IToken[]> result, final int offset, final String scope) {
		Assert.assertFalse("Unexpected " + scope + " at " + offset, hasScope(result, offset, scope));
	}

	private boolean hasScope(final ITokenizeLineResult<IToken[]> result, final int offset, final String scope) {
		for (final IToken token : result.getTokens()) {
			if (token.getStartIndex() <= offset && offset < token.getEndIndex()) {
				return token.getScopes().contains(scope);
			}
		}
		return false;
	}
}
