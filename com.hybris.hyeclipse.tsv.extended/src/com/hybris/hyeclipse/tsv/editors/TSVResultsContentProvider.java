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
