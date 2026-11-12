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
package com.hybris.hyeclipse.emf.beans.presentation.platformdependent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.hybris.hyeclipse.commons.typesystem.TypeSystemSnapshot;
import com.hybris.hyeclipse.commons.typesystem.YAttributeDescriptor;
import com.hybris.hyeclipse.commons.typesystem.YType;
import com.hybris.hyeclipse.emf.beans.AbstractPojos;
import com.hybris.hyeclipse.emf.beans.Bean;
import com.hybris.hyeclipse.emf.beans.BeansFactory;
import com.hybris.hyeclipse.emf.beans.DocumentRoot;
import com.hybris.hyeclipse.emf.beans.Property;


class BeansGenerator implements IRunnableWithProgress {
	private static final Pattern JAVA_TYPE_NAME = Pattern.compile("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*");
	
	private final BeansFactory beansFactory = BeansFactory.eINSTANCE;
	
	final DocumentRoot documentRoot = BeansFactory.eINSTANCE.createDocumentRoot();
	final AbstractPojos beans = BeansFactory.eINSTANCE.createAbstractPojos();
	{
		documentRoot.setBeans(beans);
	}
	
	final Collection<? extends YType> typesToProcess;
	
	private final Map<String, String> beanTypesByModelClass = new HashMap<>();
	
	BeansGenerator(TypeSystemSnapshot snapshot, final Collection<? extends YType> typesToProcess) {
		this.typesToProcess = typesToProcess;
		for (final YType type : snapshot.getTypes()) {
			beanTypesByModelClass.put(type.getJavaClassName(), getBeanTypeName(type));
		}
	}

	@Override
	public void run(IProgressMonitor monitor) {
		final SubMonitor sub = SubMonitor.convert(monitor, typesToProcess.size());
		try {
			for (final YType composedType : typesToProcess) {
				beans.getBean().add(createBean(composedType));
				sub.worked(1);
			}
		}
		finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		
	}
	
	private String getBeanTypeName(final YType type) {
		final String modelClass = type.getJavaClassName();
		final int modelPackage = modelClass.indexOf(".model.");
		final int packageEnd = modelPackage >= 0 ? modelPackage : modelClass.lastIndexOf('.');
		final String packageRoot = packageEnd >= 0 ? modelClass.substring(0, packageEnd) : "";
		return (packageRoot.isEmpty() ? "" : packageRoot + ".") + "data." + type.getCode() + "Data";
	}

	private String getTypeName(final String javaType) {
		final StringBuilder result = new StringBuilder();
		// Preserve generic/array structure while replacing known model references with generated bean classes.
		final Matcher matcher = JAVA_TYPE_NAME.matcher(javaType);
		while (matcher.find()) {
			matcher.appendReplacement(result,
					Matcher.quoteReplacement(beanTypesByModelClass.getOrDefault(matcher.group(), matcher.group())));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private void populateBean(final Bean bean, final YType composedType) {
		bean.setClass(getBeanTypeName(composedType));
		for (final YAttributeDescriptor attributeDescriptor : composedType.getAttributes()) {
			bean.getProperty().add(createProperty(attributeDescriptor));
		}
	}

	private Property createProperty(YAttributeDescriptor attributeDescriptor) {
		final Property property = beansFactory.createProperty();
		populate(property, attributeDescriptor);
		return property;
	}

	private void populate(Property property, YAttributeDescriptor attributeDescriptor) {
		property.setName(attributeDescriptor.getQualifier());
		property.setType(getTypeName(attributeDescriptor.getJavaType()));
	}

	private Bean createBean(YType composedType) {
		final Bean bean = beansFactory.createBean();
		populateBean(bean, composedType);
		return bean;
	}

}
