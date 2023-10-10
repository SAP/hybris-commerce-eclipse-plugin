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
/**
 * Copyright by SAP
 */
package com.hybris.hyeclipse.emf.beans.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import com.hybris.hyeclipse.emf.beans.AbstractPojos;
import com.hybris.hyeclipse.emf.beans.Bean;
import com.hybris.hyeclipse.emf.beans.BeansPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Abstract Pojos</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.impl.AbstractPojosImpl#getGroup <em>Group</em>}</li>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.impl.AbstractPojosImpl#getBean <em>Bean</em>}</li>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.impl.AbstractPojosImpl#getEnum <em>Enum</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AbstractPojosImpl extends MinimalEObjectImpl.Container implements AbstractPojos {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "Copyright by SAP";

	/**
	 * The cached value of the '{@link #getGroup() <em>Group</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGroup()
	 * @generated
	 * @ordered
	 */
	protected FeatureMap group;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AbstractPojosImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return BeansPackage.Literals.ABSTRACT_POJOS;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getGroup() {
		if (group == null) {
			group = new BasicFeatureMap(this, BeansPackage.ABSTRACT_POJOS__GROUP);
		}
		return group;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Bean> getBean() {
		return getGroup().list(BeansPackage.Literals.ABSTRACT_POJOS__BEAN);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<com.hybris.hyeclipse.emf.beans.Enum> getEnum() {
		return getGroup().list(BeansPackage.Literals.ABSTRACT_POJOS__ENUM);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case BeansPackage.ABSTRACT_POJOS__GROUP:
				return ((InternalEList<?>)getGroup()).basicRemove(otherEnd, msgs);
			case BeansPackage.ABSTRACT_POJOS__BEAN:
				return ((InternalEList<?>)getBean()).basicRemove(otherEnd, msgs);
			case BeansPackage.ABSTRACT_POJOS__ENUM:
				return ((InternalEList<?>)getEnum()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case BeansPackage.ABSTRACT_POJOS__GROUP:
				if (coreType) return getGroup();
				return ((FeatureMap.Internal)getGroup()).getWrapper();
			case BeansPackage.ABSTRACT_POJOS__BEAN:
				return getBean();
			case BeansPackage.ABSTRACT_POJOS__ENUM:
				return getEnum();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case BeansPackage.ABSTRACT_POJOS__GROUP:
				((FeatureMap.Internal)getGroup()).set(newValue);
				return;
			case BeansPackage.ABSTRACT_POJOS__BEAN:
				getBean().clear();
				getBean().addAll((Collection<? extends Bean>)newValue);
				return;
			case BeansPackage.ABSTRACT_POJOS__ENUM:
				getEnum().clear();
				getEnum().addAll((Collection<? extends com.hybris.hyeclipse.emf.beans.Enum>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case BeansPackage.ABSTRACT_POJOS__GROUP:
				getGroup().clear();
				return;
			case BeansPackage.ABSTRACT_POJOS__BEAN:
				getBean().clear();
				return;
			case BeansPackage.ABSTRACT_POJOS__ENUM:
				getEnum().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case BeansPackage.ABSTRACT_POJOS__GROUP:
				return group != null && !group.isEmpty();
			case BeansPackage.ABSTRACT_POJOS__BEAN:
				return !getBean().isEmpty();
			case BeansPackage.ABSTRACT_POJOS__ENUM:
				return !getEnum().isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (group: ");
		result.append(group);
		result.append(')');
		return result.toString();
	}

} //AbstractPojosImpl
