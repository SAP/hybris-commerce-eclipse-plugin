package com.hybris.impexformatter.scanners;

import java.awt.Font;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

import com.google.common.collect.Lists;
import com.hybris.impexformatter.Activator;
import com.hybris.impexformatter.actions.Formatter;
import com.hybris.impexformatter.editors.ColorProvider;
import com.hybris.impexformatter.preferences.PreferenceConstants;
import com.hybris.impexformatter.rules.RuleFactory;
import com.hybris.impexformatter.rules.RuleFactory.ImpexRules;

/**
 * Scanner which contains sequence of rules
 */
public class ImpexRuleScanner extends RuleBasedScanner {
	
	IPreferenceStore store = Activator.getDefault().getPreferenceStore();
	
	public ImpexRuleScanner(ColorProvider provider) {
		IToken stringToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_STRING_COLOR))));
		IToken commentToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_COMMENT_COLOR))));
		IToken instructToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_INSTRUCT_COLOR)), null, Font.ITALIC));
		IToken modifiersToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_MODIF_COLOR))));
		IToken impexTagsToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_TAG_COLOR)), null, Font.BOLD));
		IToken keywordsValuesToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_KVAL_COLOR))));
		IToken referenceToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_REF_COLOR)), null, Font.BOLD));
		IToken definitionsToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_DEF_COLOR)), null, Font.ITALIC));

		List<IRule> rules = Lists.newArrayList();
		// trap instruction within quotes first to prevent being picked up by next rules
		rules.add(new EndOfLineRule("\"#%", instructToken));
		
		// rule for Strings - may span multiple lines
		rules.add(new MultiLineRule("\"", "\"", stringToken));
		rules.add(new MultiLineRule("\'", "\'", stringToken));
		
		rules.add(new EndOfLineRule("#%", instructToken));
		// rule for comments - ended by a line delimiter
		rules.add(new EndOfLineRule("#", commentToken));

		WordRule wordRule = RuleFactory.buildRule(ImpexRules.KEYWORD, null);		
		// rule for modifiers
		for (String word : Formatter.IMPEX_KEYWORDS_ATTRIBUTES) {
			wordRule.addWord(word, modifiersToken);
		}

		// rule for impex tags
		for (String header : Formatter.HEADER_MODE_PROPOSALS) {
			wordRule.addWord(header, impexTagsToken);
		}
		
		// rule for keyword values
		for (String value : Formatter.KEYWORDS_VALUES) {
			wordRule.addWord(value, keywordsValuesToken);
		}

		rules.add(wordRule);

		// rule for definitions
		wordRule = RuleFactory.buildRule(ImpexRules.VARIABLE, definitionsToken);
		rules.add(wordRule);
		wordRule = RuleFactory.buildRule(ImpexRules.REFERENCE, referenceToken);
		rules.add(wordRule);
		
		wordRule = RuleFactory.buildRule(ImpexRules.SEMICOLON, modifiersToken);
		rules.add(wordRule);
		wordRule = RuleFactory.buildRule(ImpexRules.COMMA, modifiersToken);
		rules.add(wordRule);
		
		IRule[] ruleArray = new IRule[rules.size()];
		setRules(rules.toArray(ruleArray));
	}
}
