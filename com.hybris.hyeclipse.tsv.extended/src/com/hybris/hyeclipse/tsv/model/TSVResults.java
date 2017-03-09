package com.hybris.hyeclipse.tsv.model;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="validation_results")
public class TSVResults {

	public TSVResults(){}
	
	private Set<TSVResult> results;
	
	public Set<TSVResult> getResults() {
		return results;
	}
	@XmlElement(name="result")
	public void setResults(Set<TSVResult> results) {
		this.results = results;
	}
	
}
