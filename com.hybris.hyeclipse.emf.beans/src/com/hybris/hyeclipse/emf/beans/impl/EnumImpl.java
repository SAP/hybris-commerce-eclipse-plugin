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

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EDataTypeEList;

import com.hybris.hyeclipse.emf.beans.BeansPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Enum</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.impl.EnumImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.impl.EnumImpl#getValue <em>Value</em>}</li>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.impl.EnumImpl#getDeprecated <em>Deprecated</em>}</li>
 * </ul>
 *
 * @generated
 */
public class EnumImpl extends AbstractPojoImpl implements com.hybris.hyeclipse.emf.beans.Enum {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "Copyright by SAP";

	/**
	 * The default value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected static final String DESCRIPTION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected String description = DESCRIPTION_EDEFAULT;

	/**
	 * The cached value of the '{@link #getValue() <em>Value</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getValue()
	 * @generated
	 * @ordered
	 */
	protected EList<String> value;

	/**
	 * The default value of the '{@link #getDeprecated() <em>Deprecated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDeprecated()
	 * @generated
	 * @ordered
	 */
	protected static final String DEPRECATED_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDeprecated() <em>Deprecated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDeprecated()
	 * @generated
	 * @ordered
	 */
	protected String deprecated = DEPRECATED_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EnumImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return BeansPackage.Literals.ENUM;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDescription(String newDescription) {
		String oldDescription = description;
		description = newDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, BeansPackage.ENUM__DESCRIPTION, oldDescription, description));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<String> getValue() {
		if (value == null) {
			value = new EDataTypeEList<String>(String.class, this, BeansPackage.ENUM__VALUE);
		}
		return value;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getDeprecated() {
		return deprecated;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDeprecated(String newDeprecated) {
		String oldDeprecated = deprecated;
		deprecated = newDeprecated;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, BeansPackage.ENUM__DEPRECATED, oldDeprecated, deprecated));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case BeansPackage.ENUM__DESCRIPTION:
				return getDescription();
			case BeansPackage.ENUM__VALUE:
				return getValue();
			case BeansPackage.ENUM__DEPRECATED:
				return getDeprecated();
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
			case BeansPackage.ENUM__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case BeansPackage.ENUM__VALUE:
				getValue().clear();
				getValue().addAll((Collection<? extends String>)newValue);
				return;
			case BeansPackage.ENUM__DEPRECATED:
				setDeprecated((String)newValue);
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
			case BeansPackage.ENUM__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case BeansPackage.ENUM__VALUE:
				getValue().clear();
				return;
			case BeansPackage.ENUM__DEPRECATED:
				setDeprecated(DEPRECATED_EDEFAULT);
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
			case BeansPackage.ENUM__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? description != null : !DESCRIPTION_EDEFAULT.equals(description);
			case BeansPackage.ENUM__VALUE:
				return value != null && !value.isEmpty();
			case BeansPackage.ENUM__DEPRECATED:
				return DEPRECATED_EDEFAULT == null ? deprecated != null : !DEPRECATED_EDEFAULT.equals(deprecated);
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
		result.append(" (description: ");
		result.append(description);
		result.append(", value: ");
		result.append(value);
		result.append(", deprecated: ");
		result.append(deprecated);
		result.append(')');
		return result.toString();
	}

} //EnumImpl
