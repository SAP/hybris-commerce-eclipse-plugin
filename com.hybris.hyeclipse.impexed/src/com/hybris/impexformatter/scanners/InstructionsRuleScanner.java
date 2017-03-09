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
public class InstructionsRuleScanner extends RuleBasedScanner {
	
	IPreferenceStore store = Activator.getDefault().getPreferenceStore();
	
	public InstructionsRuleScanner(ColorProvider provider) {
		IToken stringToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_STRING_COLOR))));
		IToken commentToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_COMMENT_COLOR))));
		IToken instructToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_INSTRUCT_COLOR)), null, Font.ITALIC));
		IToken definitionsToken = new Token(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, PreferenceConstants.P_DEF_COLOR)), null, Font.ITALIC));

		List<IRule> rules = Lists.newArrayList();
		// rule for Strings - may spanning multiple lines
		rules.add(new MultiLineRule("\"", "\"", stringToken));
		rules.add(new MultiLineRule("\'", "\'", stringToken));
		rules.add(new EndOfLineRule("#%", instructToken));
		// rule for comments - ended by a line delimiter
		rules.add(new EndOfLineRule("//", commentToken));

		WordRule wordRule = RuleFactory.buildRule(ImpexRules.KEYWORD, null);		
		// rule for instructions
		for (String word : Formatter.INSTRUCTION_CLASS_PROPOSALS) {
			wordRule.addWord(word, instructToken);
		}

		rules.add(wordRule);

		// rule for definitions
		wordRule = RuleFactory.buildRule(ImpexRules.VARIABLE, definitionsToken);
		rules.add(wordRule);
		IRule[] ruleArray = new IRule[rules.size()];
		setRules(rules.toArray(ruleArray));
	}
}
