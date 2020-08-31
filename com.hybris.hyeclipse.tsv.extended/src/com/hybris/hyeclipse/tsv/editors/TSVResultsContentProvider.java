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
package com.hybris.hyeclipse.tsv.editors;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.hybris.hyeclipse.tsv.model.TSVResult;

public class TSVResultsContentProvider implements ITreeContentProvider {
	
	public TSVResultsContentProvider(HashMap<String, List<TSVResult>> resultMap) {
		this.resultMap = resultMap;
	}

	private HashMap<String, List<TSVResult>> resultMap;
	
	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {}

	@Override
	public Object[] getChildren(Object parent) {
		return getElements(parent);
	}

	@Override
	public Object[] getElements(Object object) {
		
		if (object instanceof HashMap<?, ?>) {
			return ((HashMap<?,?>) object).keySet().toArray();
		}
		else if (object instanceof String) {
			return resultMap.get(object).toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object child) {
		return null;
	}

	@Override
	public boolean hasChildren(Object parent) {
		
		if (resultMap.containsKey(parent)) {
			return true;
		}
		return false;
	}

}
