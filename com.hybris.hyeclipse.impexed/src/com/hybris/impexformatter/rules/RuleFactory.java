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
