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
package com.hybris.hyeclipse.ytypesystem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.hybris.bootstrap.typesystem.YAttributeDescriptor;
import de.hybris.bootstrap.typesystem.YComposedType;
import de.hybris.bootstrap.typesystem.YDeployment;
import de.hybris.bootstrap.typesystem.YExtension;
import de.hybris.bootstrap.typesystem.YIndex;
import de.hybris.bootstrap.typesystem.YIndexDeployment;
import de.hybris.bootstrap.typesystem.YNamespace;
import de.hybris.bootstrap.typesystem.YTypeSystem;

public class ExtendedYTypeSystem extends YTypeSystem {

	private static Field mergedNameSpaceField = null;
	private static Method mergeNameSpaceMethod = null;
	private static Field resolvedClassMapField = null;
	
	// horrible hack to make a private member public
	static {
		try {
		mergedNameSpaceField  = YTypeSystem.class.
		            getDeclaredField("mergedNamespace");
		mergedNameSpaceField.setAccessible(true);
		
		mergeNameSpaceMethod = YNamespace.class.getDeclaredMethod("mergeNamespace", new Class[]{YNamespace.class});
		mergeNameSpaceMethod.setAccessible(true);
		
		resolvedClassMapField = YTypeSystem.class.getDeclaredField("resolvedClassMap");
		resolvedClassMapField.setAccessible(true);
		
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to do reflection hack so we need to abort", e);
		}
	}

	public ExtendedYTypeSystem(boolean buildMode) {
		super(buildMode);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void createInheritedAttributes(final YComposedType composedType, final YAttributeDescriptor inheritFrom) {
		// skip all subtypes with existing attribute
		if (getAttribute(composedType.getCode(), inheritFrom.getQualifier()) == null) {
			YAttributeDescriptor inherited = null;
			
			// fix here for bad attributes
			try {
				inherited = new YAttributeDescriptor(composedType.getCode(), inheritFrom);
			}
			catch (Exception e) {
				return;
			}
			
			inheritFrom.getNamespace().registerTypeSystemElement(inherited);
			for (final YComposedType subtype : (Set<YComposedType>) getSubtypes(composedType.getCode())) {
				createInheritedAttributes(subtype, inherited);
			}
		}
	}
	
	@Override
	protected void deployIndex(final YIndex idx) {
		try {
			final YDeployment depl = idx.getEnclosingType().getDeployment();
			final YIndexDeployment iDepl = depl.getIndexDeployment(idx.getName().toLowerCase(Locale.ENGLISH));
			if (iDepl == null) {
				final YIndexDeployment newOne = new YIndexDeployment(idx);
				idx.getNamespace().registerTypeSystemElement(newOne);
				depl.resetCaches();
			}
		}
		catch (Exception e) {
			return;
		}
		
	}
	
	protected void mergeNamespaces() {
		YNamespace namespace = new YExtension(this, "<merged>", null);
		for (final YExtension ext : getExtensions()) {
			try {
				mergeNameSpaceMethod.invoke(namespace, ext);
			} 
			catch (Exception e) {
				throw new IllegalStateException("Failed to use reflection to set namespace, aborting", e);
			}
		}
		try {
			// set the field
			mergedNameSpaceField.set(this, namespace);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to use reflection to set namespace, aborting", e);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Class<?> resolveClass(final Object resolveFor, final String className)  {
		Class<?> ret = null;
		try {
			Map<String, Class> resolvedClassMapMerged = (Map<String, Class>)resolvedClassMapField.get(this);
			ret = resolvedClassMapMerged.get(className);
			
			if (ret == null && !resolvedClassMapMerged.containsKey(className)) {
				try {
					ret = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
				}
				catch (final ClassNotFoundException e) {
					if (!isBuildMode()) {
						throw new IllegalStateException("invalid typesystem element " + resolveFor + " due to missing class '" + className
								+ "'");
					}
				}
				
				resolvedClassMapMerged.put(className, ret);
			}
		}
		catch (IllegalStateException ise) {
			Activator.logError("IllegalStateException", ise);
		}
		catch (IllegalAccessException iae) {
			Activator.logError("IllegalAccessException", iae);
		}
		return ret;
	}
			
}
