//package com.sap.cc.lsp.impex;
//
//import java.util.Arrays;
//import java.util.List;
//
//import org.eclipse.lsp4j.CompletionOptions;
//
//import com.google.common.collect.Lists;
//
//public class ImpexCompletionOptions extends CompletionOptions {
//
//	public static final String HEADER_MODE = "(UPDATE|INSERT_UPDATE|INSERT|REMOVE|$START_USERRIGHTS).*";
//	public static final List<String> HEADER_MODE_PROPOSALS = Lists.newArrayList("INSERT_UPDATE", "UPDATE", "INSERT",
//			"REMOVE");
//
//	public static final List<String> INSTRUCTION_CLASS_PROPOSALS = Lists.newArrayList("impex.", "if:", "endif:",
//			"afterEach:", "afterEach:end", "beforeEach:", "beforeEach:end");
//	public static final List<String> INSTRUCTION_CLASS_PROPOSALS_SPACE = Lists.newArrayList("if:", "afterEach:",
//			"beforeEach:");
//
//	public static final List<String> IMPEX_KEYWORDS_ATTRIBUTES = Lists.newArrayList("batchmode", "cacheUnique",
//			"processor", "parallel", "translator", "default", "lang", "unique", "allowNull", "ignoreNull", "dateformat",
//			"numberformat", "collection-delimiter", "path-delimiter", "key2value-delimiter", "map-delimiter", "mode",
//			"cellDecorator", "virtual", "ignoreKeyCase", "alias", "pos", "forceWrite");
//	public static final List<String> IMPEX_KEYWORDS_ATTRIBUTES_BOOLEAN = Lists.newArrayList("allowNull", "batchmode",
//			"cacheUnique", "forceWrite", "ignoreKeyCase", "ignoreNull", "unique", "virtual");
//
//	public static final List<String> KEYWORDS_VALUES = Lists.newArrayList("true", "false", "append", "remove");
//
//	public static final String LINE_SEPARATOR = "line.separator";
//	private static final String DOUBLE_QUOTATION_MARK = "\"\"";
//
//	private static final String SEMICOLON_MARK = ";";
//	private static final String PERCENT_MARK = "%";
//	private static final String FIRST_LFT_FORMATTER = "%-";
//	private static final String LFT_FORMATTER = " %-";
//	private static final String COL_STR_FORMATTER = "s;";
//	private static final String STR_FORMATTER = "s";
//	private static final String NEW_LINE_MARK = "\n";
//	
//	public ImpexCompletionOptions() {
//		super();
//		this.setResolveProvider(Boolean.TRUE);
//	}
//
//	@Override
//	public void setTriggerCharacters(List<String> triggerCharacters) {
//		super.setTriggerCharacters(Arrays.asList(":", ".", ">", "@", "INSERT_UPDATE", "UPDATE", "INSERT", "REMOVE"));
//	}
//
//}
