package com.hybris.hyeclipse.tsv.builder;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.xml.sax.SAXException;

import com.hybris.hyeclipse.tsv.validator.ItemsXmlValidator;
import com.hybris.ps.tsv.main.CmdLineOptions;
import com.hybris.ps.tsv.main.TSVMain;
import com.hybris.ps.tsv.results.IResultFactory;
import com.hybris.ps.tsv.rules.IRuleSet;

public class StandardTsvBuilder extends TsvBuilder {

	public static final String BUILDER_ID = "com.hybris.hyeclipse.tsv.builder";

	private static final String TSV_SPRING_CONFIG = "tsv-spring-config.xml";
	private static final String TSV_OPTIONS_BEAN = "cmdLineOptions";
    private static final String TSV_MAIN_BEAN = "tsvMain";
	

	@Override
	protected ItemsXmlValidator getValidator() throws CoreException {
		ApplicationContext appContext = new ClassPathXmlApplicationContext(new String[]{ TSV_SPRING_CONFIG });
		TSVMain tsvMain = (TSVMain) appContext.getBean(TSV_MAIN_BEAN);
		CmdLineOptions options = (CmdLineOptions) appContext.getBean(TSV_OPTIONS_BEAN);
		options.setErrorsOnly(true);
		
		final IRuleSet ruleSet = tsvMain.getRuleService().loadDefaultRules();
		final IResultFactory resultFactory = (IResultFactory) appContext.getBean("resultFactory");
		
		try {
			return new ItemsXmlValidator(ruleSet, resultFactory);
		} catch (ParserConfigurationException | SAXException e) {
			throw new CoreException(new Status(IStatus.ERROR, "", "Failed to create ItemsXmlValidator", e));
		}
	}

}
