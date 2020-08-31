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
package com.hybris.yps.hyeclipse;

import java.util.Collection;
import java.util.LinkedList;

public class ExtensionHolder {
	
	private String path;
	private String name;
	// jar files all the files in myext/lib
	private Collection<String> jarFiles = new LinkedList<String>();
	
	private boolean webModule;
	private boolean coreModule;
	private boolean hmcModule;
	private boolean addOnModule;
	private boolean backofficeModule;
	
	private Collection<String> dependentExtensions = new LinkedList<String>();
	
	public Collection<String> getJarFiles() {
		return jarFiles;
	}

	public void setJarFiles(Collection<String> jarFiles) {
		this.jarFiles = jarFiles;
	}

	public ExtensionHolder()
	{
		// nothing
	}
	
	public ExtensionHolder(String path, String name)
	{
		this.path = path;
		this.name = name;
	}
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isWebModule() {
		return webModule;
	}

	public void setWebModule(boolean webModule) {
		this.webModule = webModule;
	}

	public boolean isCoreModule() {
		return coreModule;
	}

	public void setCoreModule(boolean coreModule) {
		this.coreModule = coreModule;
	}

	public boolean isHmcModule() {
		return hmcModule;
	}

	public void setHmcModule(boolean hmcModule) {
		this.hmcModule = hmcModule;
	}

	public boolean isAddOnModule() {
		return addOnModule;
	}

	public void setAddOnModule(boolean addOnModule) {
		this.addOnModule = addOnModule;
	}

	public Collection<String> getDependentExtensions() {
		return dependentExtensions;
	}

	public void setDependentExtensions(Collection<String> dependentExtensions) {
		this.dependentExtensions = dependentExtensions;
	}
	
	public boolean isBackofficeModule() {
		return backofficeModule;
	}

	public void setBackofficeModule(boolean backofficeModule) {
		this.backofficeModule = backofficeModule;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ExtensionHolder))
			return false;
		ExtensionHolder other = (ExtensionHolder) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
