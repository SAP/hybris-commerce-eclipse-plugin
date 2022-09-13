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
package com.hybris.hyeclipse.emf.beans;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Abstract Pojos</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.AbstractPojos#getGroup <em>Group</em>}</li>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.AbstractPojos#getBean <em>Bean</em>}</li>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.AbstractPojos#getEnum <em>Enum</em>}</li>
 * </ul>
 *
 * @see com.hybris.hyeclipse.emf.beans.BeansPackage#getAbstractPojos()
 * @model extendedMetaData="name='abstractPojos' kind='elementOnly'"
 * @generated
 */
public interface AbstractPojos extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "Copyright by SAP";

	/**
	 * Returns the value of the '<em><b>Group</b></em>' attribute list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Group</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Group</em>' attribute list.
	 * @see com.hybris.hyeclipse.emf.beans.BeansPackage#getAbstractPojos_Group()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='group' name='group:0'"
	 * @generated
	 */
	FeatureMap getGroup();

	/**
	 * Returns the value of the '<em><b>Bean</b></em>' containment reference list.
	 * The list contents are of type {@link com.hybris.hyeclipse.emf.beans.Bean}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Bean</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Bean</em>' containment reference list.
	 * @see com.hybris.hyeclipse.emf.beans.BeansPackage#getAbstractPojos_Bean()
	 * @model containment="true" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='bean' namespace='##targetNamespace' group='group:0'"
	 * @generated
	 */
	EList<Bean> getBean();

	/**
	 * Returns the value of the '<em><b>Enum</b></em>' containment reference list.
	 * The list contents are of type {@link com.hybris.hyeclipse.emf.beans.Enum}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Enum</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Enum</em>' containment reference list.
	 * @see com.hybris.hyeclipse.emf.beans.BeansPackage#getAbstractPojos_Enum()
	 * @model containment="true" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='enum' namespace='##targetNamespace' group='group:0'"
	 * @generated
	 */
	EList<com.hybris.hyeclipse.emf.beans.Enum> getEnum();

} // AbstractPojos
