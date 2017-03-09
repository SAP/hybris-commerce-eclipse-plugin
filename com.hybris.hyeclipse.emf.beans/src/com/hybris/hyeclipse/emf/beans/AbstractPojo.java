/**
 * Copyright by SAP Hybris
 */
package com.hybris.hyeclipse.emf.beans;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Abstract Pojo</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.AbstractPojo#getClass_ <em>Class</em>}</li>
 *   <li>{@link com.hybris.hyeclipse.emf.beans.AbstractPojo#getTemplate <em>Template</em>}</li>
 * </ul>
 *
 * @see com.hybris.hyeclipse.emf.beans.BeansPackage#getAbstractPojo()
 * @model abstract="true"
 *        extendedMetaData="name='abstractPojo' kind='empty'"
 * @generated
 */
public interface AbstractPojo extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "Copyright by SAP Hybris";

	/**
	 * Returns the value of the '<em><b>Class</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Class</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Class</em>' attribute.
	 * @see #setClass(String)
	 * @see com.hybris.hyeclipse.emf.beans.BeansPackage#getAbstractPojo_Class()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='class' namespace='##targetNamespace'"
	 * @generated
	 */
	String getClass_();

	/**
	 * Sets the value of the '{@link com.hybris.hyeclipse.emf.beans.AbstractPojo#getClass_ <em>Class</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Class</em>' attribute.
	 * @see #getClass_()
	 * @generated
	 */
	void setClass(String value);

	/**
	 * Returns the value of the '<em><b>Template</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Template</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Template</em>' attribute.
	 * @see #setTemplate(String)
	 * @see com.hybris.hyeclipse.emf.beans.BeansPackage#getAbstractPojo_Template()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='template' namespace='##targetNamespace'"
	 * @generated
	 */
	String getTemplate();

	/**
	 * Sets the value of the '{@link com.hybris.hyeclipse.emf.beans.AbstractPojo#getTemplate <em>Template</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Template</em>' attribute.
	 * @see #getTemplate()
	 * @generated
	 */
	void setTemplate(String value);

} // AbstractPojo
