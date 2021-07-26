package com.sap.cc.lsp.impex;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

public class CompletionOptions extends org.eclipse.lsp4j.CompletionOptions {

	
	public final static String HEADER_MODE = "(UPDATE|INSERT_UPDATE|INSERT|REMOVE|$START_USERRIGHTS).*";
	public final static List<String> HEADER_MODE_PROPOSALS = Lists.newArrayList("INSERT_UPDATE","UPDATE", "INSERT", "REMOVE");
	
	public final static List<String> INSTRUCTION_CLASS_PROPOSALS = Lists.newArrayList("impex.", "if:", "endif:", "afterEach:", "afterEach:end", "beforeEach:", "beforeEach:end");
	public final static List<String> INSTRUCTION_CLASS_PROPOSALS_SPACE = Lists.newArrayList("if:", "afterEach:", "beforeEach:");
	
	public final static List<String> IMPEX_KEYWORDS_ATTRIBUTES = Lists.newArrayList("batchmode", "cacheUnique", "processor", "parallel",
			"translator", "default", "lang", "unique", "allowNull", "ignoreNull", "dateformat", "numberformat",
			"collection-delimiter", "path-delimiter", "key2value-delimiter", "map-delimiter", "mode", "cellDecorator", "virtual", "ignoreKeyCase", "alias", "pos", "forceWrite");
	public final static List<String> IMPEX_KEYWORDS_ATTRIBUTES_BOOLEAN = Lists.newArrayList("allowNull", "batchmode", "cacheUnique", "forceWrite", "ignoreKeyCase", "ignoreNull", 
			"unique", "virtual");
	
	public final static List<String> KEYWORDS_VALUES =  Lists.newArrayList("true", "false", "append", "remove");
	
	public final static String LINE_SEPARATOR = "line.separator";
	private final static String DOUBLE_QUOTATION_MARK = "\"\"";

	private final static String SEMICOLON_MARK = ";";
	private final static String PERCENT_MARK = "%";
	private final static String FIRST_LFT_FORMATTER = "%-";
	private final static String LFT_FORMATTER = " %-";
	private final static String COL_STR_FORMATTER = "s;";
	private final static String STR_FORMATTER = "s";
	private final static String NEW_LINE_MARK = "\n";
	
	@Override
	public void setTriggerCharacters(List<String> triggerCharacters) {
		super.setTriggerCharacters(Arrays.asList(":", ".", ">", "@", "INSERT_UPDATE","UPDATE", "INSERT", "REMOVE"));
	}

}
