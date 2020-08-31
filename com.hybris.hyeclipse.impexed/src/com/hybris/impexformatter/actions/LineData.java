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
package com.hybris.impexformatter.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LineData DTO - item used for line details
 * 
 */
public class LineData {

	public static final String QUOTATION_MARK = "\"";
	public static final char HASH_CHAR = '#';
	public static final char QUOTATION_CHAR = '\"';
	public static final String HASH_MARK = "#";
	private final String SPLIT_VAL = "(?<=;)";
	public static final String QUOTATION_HASH_PERCENT_MARK = "\"#%";
	private final static String SEMICOLON_MARK = ";";

	String line;
	List<String> colData = new ArrayList<>();
	int singleQuotationIndex;
	int numberOfCol;
	int longValueColIndex = -1;

	public LineData(String line) {
		this.line = line;
		if (line.startsWith(HASH_MARK) || line.startsWith(QUOTATION_HASH_PERCENT_MARK)) {
			this.colData = Arrays.asList(line);
		}
		else {
			List<String> datas = Arrays.asList(line.split(SPLIT_VAL, -1));
			if (datas.size() > 1) {
				for (int i = 0; i < datas.size(); i++) {
					boolean ifends = false;
					if (datas.get(i).endsWith(SEMICOLON_MARK)) {
						datas.set(i, datas.get(i).substring(0, datas.get(i).length() - 1));
						ifends = true;
					}
					String dat = datas.get(i).trim();
					if (ifends) {
						dat = dat + SEMICOLON_MARK;
					}
					this.colData.add(dat);
				}
			}
			else {
				this.colData = datas;
			}
		}
		this.singleQuotationIndex = getSingleQuotationIndex(line);
		this.numberOfCol = this.colData.size();

	}

	/**
	 * returns index of single quotation in line Function is checking if there is quotation in line, If yes than it
	 * checks if next char isn't quotation or hash
	 * 
	 */
	private int getSingleQuotationIndex(String line) {
		int singleQuotationIndex = -1;
		int quotationIndex = line.indexOf(QUOTATION_MARK);
		// check if there is quotation in line
		while (quotationIndex >= 0) {
			int nextIndex = quotationIndex + 1;
			if (line.length() > nextIndex) {
				char afterQuotationChar = line.charAt(nextIndex);
				// if it's "" or "# than is not single quotation
				if ((afterQuotationChar != QUOTATION_CHAR) && (afterQuotationChar != HASH_CHAR)) {
					singleQuotationIndex = quotationIndex;
				}
			}
			else {
				singleQuotationIndex = quotationIndex;
			}
			// check if there is quotation more than 2 places after last quotation
			quotationIndex = line.indexOf(QUOTATION_MARK, nextIndex + 1);
		}
		return singleQuotationIndex;
	}
	
}
