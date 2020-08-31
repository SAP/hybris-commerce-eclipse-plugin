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
package com.hybris.hyeclipse.tsv.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="result")
public class TSVResult {
	
	private String filename;
	private int lineNumber;
	private String ruleId;
	private String rulePriority;
	private String state;
	private String element;
	private String description;
	
	public TSVResult() {}
	
	public String getFilename() {
		return filename;
	}
	@XmlElement
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getLineNumber() {
		return lineNumber;
	}
	@XmlElement(name="line")
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public String getRuleId() {
		return ruleId;
	}
	@XmlElement(name="rule_id")
	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	public String getRulePriority() {
		return rulePriority;
	}
	@XmlElement(name="rule_priority")
	public void setRulePriority(String rulePriority) {
		this.rulePriority = rulePriority;
	}

	public String getState() {
		return state;
	}
	@XmlElement
	public void setState(String state) {
		this.state = state;
	}

	public String getElement() {
		return element;
	}
	@XmlElement
	public void setElement(String element) {
		this.element = element;
	}

	public String getDescription() {
		return description;
	}
	@XmlElement
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder("Line ");
		sb.append(getLineNumber()).append(": ");
		if (getLineNumber() < 100) {
			sb.append(" ");
		}
		sb.append("[").append(getElement()).append("] ");
		sb.append(getRuleId());
		
		return sb.toString();
	}
	
}
