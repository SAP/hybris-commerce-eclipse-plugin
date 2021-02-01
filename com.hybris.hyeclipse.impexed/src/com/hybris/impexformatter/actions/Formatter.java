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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.Lists;
import com.hybris.impexformatter.Activator;

public class Formatter {

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

	public static String formatData(BufferedReader reader, StringWriter writer, String line) throws IOException {
		ArrayList<Integer> columnMaxWidth = new ArrayList<>();
		List<LineData> dataBlock = new ArrayList<>();
		try {
			LineData header = new LineData(line);
			dataBlock.add(header);
			// assign for each column max width - at the begging it's width of header column
			for (int col = 0; col < header.numberOfCol; col++) {
				columnMaxWidth.add(col, header.colData.get(col).length());
			}
			// if column value is written in more than row remembered is index of column where value is
			// helpful when value ends in later rows and after it are more data, so these data should be formatted in
			// rights columns
			int lastColIndex = 0;
			while ((line = reader.readLine()) != null) {
				// check if line contains column data
				if (line.startsWith(SEMICOLON_MARK) || line.trim().startsWith(SEMICOLON_MARK)) {
					LineData dataRow = new LineData(line);
					dataBlock.add(dataRow);
					// iterate through all columns in new row and check if width is higher than max width
					for (int columnIndex = 0; columnIndex < dataRow.numberOfCol; columnIndex++) {
						// check if data is a string - starts with " and not ends in the same line
						if (dataRow.colData.get(columnIndex).startsWith(LineData.QUOTATION_MARK)
						                && !((dataRow.colData.get(columnIndex).endsWith(LineData.QUOTATION_MARK) && !(dataRow.colData
						                                .get(columnIndex).endsWith(DOUBLE_QUOTATION_MARK))) || (dataRow.colData
						                                .get(columnIndex).endsWith(
						                                                LineData.QUOTATION_MARK + SEMICOLON_MARK) && !(dataRow.colData
						                                .get(columnIndex).endsWith(DOUBLE_QUOTATION_MARK
						                                + SEMICOLON_MARK))))) {
							// assign index of last column in the row which can be not last for particular value line
							lastColIndex = columnIndex;
						}
						// check if column index is lower than size of array
						// if not number of columns in new row is higher than size of this array
						if (columnIndex <= columnMaxWidth.size() - 1) {
							// check if current column is longer than column assigned to max width array
							if (columnMaxWidth.get(columnIndex) < (dataRow.colData.get(columnIndex).length())) {
								columnMaxWidth.set(columnIndex, (dataRow.colData.get(columnIndex).length()));
							}
						} else {
							// if there are more columns add new max width
							columnMaxWidth.add(columnIndex, (dataRow.colData.get(columnIndex).length()));
						}
					}
				} else if ("".equals(line)) {
					// add line if empty
					dataBlock.add(null);
				} else if (line.startsWith(LineData.HASH_MARK)) {
					// add line if comment
					dataBlock.add(new LineData(line));
				} else if (line.startsWith(LineData.QUOTATION_HASH_PERCENT_MARK)) { // check if this is block of code
					// check if quotation mark not at last place in the line
					dataBlock.add(new LineData(line));
					if (line.lastIndexOf(LineData.QUOTATION_MARK) <= 0) {
						boolean ifContinue = true;
						// while line value is in next row
						do {
							line = reader.readLine();
							if (line != null) {
								LineData lineData = new LineData(line);
								dataBlock.add(lineData);
								if (lineData.singleQuotationIndex > -1) {
									ifContinue = false;
								}
							}
							else {
								ifContinue = false;
							}
						} while (ifContinue);
					}
				} else if (line.length() > 0) {
					LineData lineData = new LineData(line);
					if (line.toUpperCase(Locale.ENGLISH).matches(HEADER_MODE)) {
						break;
					}
					// if line is continue of line value added somewhere before
					if (lastColIndex > 0) {
						// if there is single quotation in line check if there are more data - semicolon after quotation
						// mark
						if (lineData.singleQuotationIndex > -1) {
							if (line.indexOf(SEMICOLON_MARK, lineData.singleQuotationIndex) > 0) {
								// assign index of column where long data is assigned
								lineData.longValueColIndex = lastColIndex;
							}
						}
					}
					dataBlock.add(lineData);
				} else {
					break;
				}
			}

		} 
		catch (IOException e) {
			Activator.logError("IOException", e);
		}
		formatLines(columnMaxWidth, writer, dataBlock);
		return line;
	}

	public static void formatLines(ArrayList<Integer> columnMaxWidth, StringWriter writer, List<LineData> dataBlock)
	                throws IOException {
		int firstItemLength = dataBlock.get(0).colData.get(0).length();
		for (LineData row : dataBlock) {
			if (row != null) {
				String format = FIRST_LFT_FORMATTER + firstItemLength + STR_FORMATTER;
				// if row starts with ;
				if (row.colData.get(0).toString().startsWith(SEMICOLON_MARK)) {
					// remove semicolon from first value
					row.colData.set(0, row.colData.get(0).substring(0, row.colData.get(0).length() - 1));
					format = PERCENT_MARK + firstItemLength + COL_STR_FORMATTER;
				} else if (row.numberOfCol > 1) { // if row at the beginning has end of long value
					if (row.colData.get(0).toString().endsWith(SEMICOLON_MARK)) {
						row.colData.set(0, row.colData.get(0).substring(0, row.colData.get(0).length() - 1));
					}
					if (row.longValueColIndex <= 0) {
						format = FIRST_LFT_FORMATTER + firstItemLength + COL_STR_FORMATTER;
					}
				}
				if (row.longValueColIndex > 0) {
					// size of first item plus number of columns (spaces)
					int widthToNextCol = firstItemLength + row.longValueColIndex;
					// widthToNextCol + max width of next columns
					for (int i = 1; i <= row.longValueColIndex; i++) {
						int colSize = columnMaxWidth.get(i) + 1;
						widthToNextCol += colSize;
					}
					format = FIRST_LFT_FORMATTER + widthToNextCol + COL_STR_FORMATTER;
				}

				for (int singleData = 1; singleData < row.numberOfCol; singleData++) {
					int formatWidth;
					if (columnMaxWidth.size() >= singleData) {
						formatWidth = columnMaxWidth.get(singleData);
					} else {
						formatWidth = row.colData.get(singleData).length();
						columnMaxWidth.add(singleData, formatWidth);
					}

					if (row.longValueColIndex > 0) {
						int colRealIndex = row.longValueColIndex + singleData;
						// if column index is higher than max width array size at it to this array
						if (columnMaxWidth.size() <= colRealIndex) {
							columnMaxWidth.add(colRealIndex, (row.colData.get(singleData).length()));
						}
						formatWidth = columnMaxWidth.get(colRealIndex);
					}
					if (formatWidth == 0) {
						formatWidth = 1;
					}
					String colValue = row.colData.get(singleData);
					if (colValue.endsWith(SEMICOLON_MARK)) {
						row.colData.set(singleData, colValue.substring(0, colValue.length() - 1));
						format += LFT_FORMATTER + formatWidth + COL_STR_FORMATTER;
					} else {
						format += LFT_FORMATTER + formatWidth + STR_FORMATTER;
					}
				}
				format += NEW_LINE_MARK;
				writer.append(String.format(format, row.colData.toArray()));
			} else {
				writer.write(System.getProperty(LINE_SEPARATOR));
			}
		}
	}

}
