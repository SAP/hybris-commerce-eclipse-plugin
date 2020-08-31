/*******************************************************************************
 * Copyright 2020 SAP
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.impexformatter.rules;

import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

public class RuleFactory {

	public enum ImpexRules {KEYWORD, VARIABLE, REFERENCE, SEMICOLON, COMMA};
	
	public static WordRule buildRule(ImpexRules ruleType, IToken token) {
		WordRule result = null;
		switch (ruleType) {
		case KEYWORD:
			result = buildKeywordRule(token);
			break;
		case VARIABLE:
			result = buildVariableRule('$', token);
			break;
		case REFERENCE:
			result = buildVariableRule('&', token);
			break;
		case SEMICOLON:
			result = buildCharRule(';', token);
			break;
		case COMMA:
			result = buildCharRule(',', token);
			break;
		default:
			result = null;
			break;
		}
		return result;
	}
	
	private static WordRule buildCharRule(final char startChar, IToken token) {
		if (token == null) {token = Token.UNDEFINED;}
		WordRule rule = new WordRule(new IWordDetector() {
			
			@Override
			public boolean isWordStart(char c) {
				return c == startChar;
			}
			
			@Override
			public boolean isWordPart(char c) {
				return false;
			}
		}, token);
		return rule;
	}

	private static WordRule buildKeywordRule(IToken token) {
		if (token == null) {token = Token.UNDEFINED;}
		WordRule rule = new WordRule(new IWordDetector() {
			public boolean isWordStart(char c) {
				return Character.isJavaIdentifierStart(c);
			}

			public boolean isWordPart(char c) {
				return Character.isJavaIdentifierPart(c);
			}
		}, token);
		return rule;
	}
	
	private static WordRule buildVariableRule(final char startChar, IToken token) {
		if (token == null) {token = Token.UNDEFINED;}
		WordRule rule = new WordRule(new IWordDetector() {
			public boolean isWordStart(char c) {
				return c == startChar;
			}

			public boolean isWordPart(char c) {
				return Character.isJavaIdentifierPart(c);
			}
		}, token);
		return rule;
	}
}
