/**
 * Copyright by SAP Hybris
 */
package com.hybris.hyeclipse.emf.beans;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see com.hybris.hyeclipse.emf.beans.BeansFactory
 * @model kind="package"
 *        extendedMetaData="qualified='false'"
 * @generated
 */
public interface BeansPackage extends EPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "Copyright by SAP Hybris";

	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "beans";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "file:/Users/i316750/development/hybris/tata/TSERes1_12Dec2016/hybris/hybris/bin/custom/ttweb/ttwebfacades/resources/beans.xsd";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "beans";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	BeansPackage eINSTANCE = com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl.init();

	/**
	 * The meta object id for the '{@link com.hybris.hyeclipse.emf.beans.impl.AbstractPojoImpl <em>Abstract Pojo</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.hybris.hyeclipse.emf.beans.impl.AbstractPojoImpl
	 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getAbstractPojo()
	 * @generated
	 */
	int ABSTRACT_POJO = 0;

	/**
	 * The feature id for the '<em><b>Class</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJO__CLASS = 0;

	/**
	 * The feature id for the '<em><b>Template</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJO__TEMPLATE = 1;

	/**
	 * The number of structural features of the '<em>Abstract Pojo</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJO_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Abstract Pojo</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJO_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link com.hybris.hyeclipse.emf.beans.impl.AbstractPojosImpl <em>Abstract Pojos</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.hybris.hyeclipse.emf.beans.impl.AbstractPojosImpl
	 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getAbstractPojos()
	 * @generated
	 */
	int ABSTRACT_POJOS = 1;

	/**
	 * The feature id for the '<em><b>Group</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJOS__GROUP = 0;

	/**
	 * The feature id for the '<em><b>Bean</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJOS__BEAN = 1;

	/**
	 * The feature id for the '<em><b>Enum</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJOS__ENUM = 2;

	/**
	 * The number of structural features of the '<em>Abstract Pojos</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJOS_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Abstract Pojos</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ABSTRACT_POJOS_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link com.hybris.hyeclipse.emf.beans.impl.BeanImpl <em>Bean</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.hybris.hyeclipse.emf.beans.impl.BeanImpl
	 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getBean()
	 * @generated
	 */
	int BEAN = 2;

	/**
	 * The feature id for the '<em><b>Class</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN__CLASS = ABSTRACT_POJO__CLASS;

	/**
	 * The feature id for the '<em><b>Template</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN__TEMPLATE = ABSTRACT_POJO__TEMPLATE;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN__DESCRIPTION = ABSTRACT_POJO_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN__PROPERTY = ABSTRACT_POJO_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Deprecated</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN__DEPRECATED = ABSTRACT_POJO_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Extends</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN__EXTENDS = ABSTRACT_POJO_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN__TYPE = ABSTRACT_POJO_FEATURE_COUNT + 4;

	/**
	 * The number of structural features of the '<em>Bean</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN_FEATURE_COUNT = ABSTRACT_POJO_FEATURE_COUNT + 5;

	/**
	 * The number of operations of the '<em>Bean</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int BEAN_OPERATION_COUNT = ABSTRACT_POJO_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link com.hybris.hyeclipse.emf.beans.impl.DocumentRootImpl <em>Document Root</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.hybris.hyeclipse.emf.beans.impl.DocumentRootImpl
	 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getDocumentRoot()
	 * @generated
	 */
	int DOCUMENT_ROOT = 3;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__MIXED = 0;

	/**
	 * The feature id for the '<em><b>XMLNS Prefix Map</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__XMLNS_PREFIX_MAP = 1;

	/**
	 * The feature id for the '<em><b>XSI Schema Location</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__XSI_SCHEMA_LOCATION = 2;

	/**
	 * The feature id for the '<em><b>Beans</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__BEANS = 3;

	/**
	 * The number of structural features of the '<em>Document Root</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Document Root</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link com.hybris.hyeclipse.emf.beans.impl.EnumImpl <em>Enum</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.hybris.hyeclipse.emf.beans.impl.EnumImpl
	 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getEnum()
	 * @generated
	 */
	int ENUM = 4;

	/**
	 * The feature id for the '<em><b>Class</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUM__CLASS = ABSTRACT_POJO__CLASS;

	/**
	 * The feature id for the '<em><b>Template</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUM__TEMPLATE = ABSTRACT_POJO__TEMPLATE;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUM__DESCRIPTION = ABSTRACT_POJO_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUM__VALUE = ABSTRACT_POJO_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Deprecated</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUM__DEPRECATED = ABSTRACT_POJO_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Enum</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUM_FEATURE_COUNT = ABSTRACT_POJO_FEATURE_COUNT + 3;

	/**
	 * The number of operations of the '<em>Enum</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENUM_OPERATION_COUNT = ABSTRACT_POJO_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link com.hybris.hyeclipse.emf.beans.impl.PropertyImpl <em>Property</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.hybris.hyeclipse.emf.beans.impl.PropertyImpl
	 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getProperty()
	 * @generated
	 */
	int PROPERTY = 5;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY__DESCRIPTION = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY__NAME = 1;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY__TYPE = 2;

	/**
	 * The number of structural features of the '<em>Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Property</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY_OPERATION_COUNT = 0;


	/**
	 * Returns the meta object for class '{@link com.hybris.hyeclipse.emf.beans.AbstractPojo <em>Abstract Pojo</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Abstract Pojo</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.AbstractPojo
	 * @generated
	 */
	EClass getAbstractPojo();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.AbstractPojo#getClass_ <em>Class</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.AbstractPojo#getClass_()
	 * @see #getAbstractPojo()
	 * @generated
	 */
	EAttribute getAbstractPojo_Class();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.AbstractPojo#getTemplate <em>Template</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Template</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.AbstractPojo#getTemplate()
	 * @see #getAbstractPojo()
	 * @generated
	 */
	EAttribute getAbstractPojo_Template();

	/**
	 * Returns the meta object for class '{@link com.hybris.hyeclipse.emf.beans.AbstractPojos <em>Abstract Pojos</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Abstract Pojos</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.AbstractPojos
	 * @generated
	 */
	EClass getAbstractPojos();

	/**
	 * Returns the meta object for the attribute list '{@link com.hybris.hyeclipse.emf.beans.AbstractPojos#getGroup <em>Group</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Group</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.AbstractPojos#getGroup()
	 * @see #getAbstractPojos()
	 * @generated
	 */
	EAttribute getAbstractPojos_Group();

	/**
	 * Returns the meta object for the containment reference list '{@link com.hybris.hyeclipse.emf.beans.AbstractPojos#getBean <em>Bean</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Bean</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.AbstractPojos#getBean()
	 * @see #getAbstractPojos()
	 * @generated
	 */
	EReference getAbstractPojos_Bean();

	/**
	 * Returns the meta object for the containment reference list '{@link com.hybris.hyeclipse.emf.beans.AbstractPojos#getEnum <em>Enum</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Enum</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.AbstractPojos#getEnum()
	 * @see #getAbstractPojos()
	 * @generated
	 */
	EReference getAbstractPojos_Enum();

	/**
	 * Returns the meta object for class '{@link com.hybris.hyeclipse.emf.beans.Bean <em>Bean</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Bean</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Bean
	 * @generated
	 */
	EClass getBean();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Bean#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Bean#getDescription()
	 * @see #getBean()
	 * @generated
	 */
	EAttribute getBean_Description();

	/**
	 * Returns the meta object for the containment reference list '{@link com.hybris.hyeclipse.emf.beans.Bean#getProperty <em>Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Property</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Bean#getProperty()
	 * @see #getBean()
	 * @generated
	 */
	EReference getBean_Property();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Bean#getDeprecated <em>Deprecated</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Deprecated</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Bean#getDeprecated()
	 * @see #getBean()
	 * @generated
	 */
	EAttribute getBean_Deprecated();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Bean#getExtends <em>Extends</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Extends</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Bean#getExtends()
	 * @see #getBean()
	 * @generated
	 */
	EAttribute getBean_Extends();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Bean#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Bean#getType()
	 * @see #getBean()
	 * @generated
	 */
	EAttribute getBean_Type();

	/**
	 * Returns the meta object for class '{@link com.hybris.hyeclipse.emf.beans.DocumentRoot <em>Document Root</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Document Root</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.DocumentRoot
	 * @generated
	 */
	EClass getDocumentRoot();

	/**
	 * Returns the meta object for the attribute list '{@link com.hybris.hyeclipse.emf.beans.DocumentRoot#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.DocumentRoot#getMixed()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EAttribute getDocumentRoot_Mixed();

	/**
	 * Returns the meta object for the map '{@link com.hybris.hyeclipse.emf.beans.DocumentRoot#getXMLNSPrefixMap <em>XMLNS Prefix Map</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>XMLNS Prefix Map</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.DocumentRoot#getXMLNSPrefixMap()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_XMLNSPrefixMap();

	/**
	 * Returns the meta object for the map '{@link com.hybris.hyeclipse.emf.beans.DocumentRoot#getXSISchemaLocation <em>XSI Schema Location</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>XSI Schema Location</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.DocumentRoot#getXSISchemaLocation()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_XSISchemaLocation();

	/**
	 * Returns the meta object for the containment reference '{@link com.hybris.hyeclipse.emf.beans.DocumentRoot#getBeans <em>Beans</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Beans</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.DocumentRoot#getBeans()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_Beans();

	/**
	 * Returns the meta object for class '{@link com.hybris.hyeclipse.emf.beans.Enum <em>Enum</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Enum</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Enum
	 * @generated
	 */
	EClass getEnum();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Enum#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Enum#getDescription()
	 * @see #getEnum()
	 * @generated
	 */
	EAttribute getEnum_Description();

	/**
	 * Returns the meta object for the attribute list '{@link com.hybris.hyeclipse.emf.beans.Enum#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Value</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Enum#getValue()
	 * @see #getEnum()
	 * @generated
	 */
	EAttribute getEnum_Value();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Enum#getDeprecated <em>Deprecated</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Deprecated</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Enum#getDeprecated()
	 * @see #getEnum()
	 * @generated
	 */
	EAttribute getEnum_Deprecated();

	/**
	 * Returns the meta object for class '{@link com.hybris.hyeclipse.emf.beans.Property <em>Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Property</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Property
	 * @generated
	 */
	EClass getProperty();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Property#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Property#getDescription()
	 * @see #getProperty()
	 * @generated
	 */
	EAttribute getProperty_Description();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Property#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Property#getName()
	 * @see #getProperty()
	 * @generated
	 */
	EAttribute getProperty_Name();

	/**
	 * Returns the meta object for the attribute '{@link com.hybris.hyeclipse.emf.beans.Property#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see com.hybris.hyeclipse.emf.beans.Property#getType()
	 * @see #getProperty()
	 * @generated
	 */
	EAttribute getProperty_Type();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	BeansFactory getBeansFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link com.hybris.hyeclipse.emf.beans.impl.AbstractPojoImpl <em>Abstract Pojo</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.hybris.hyeclipse.emf.beans.impl.AbstractPojoImpl
		 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getAbstractPojo()
		 * @generated
		 */
		EClass ABSTRACT_POJO = eINSTANCE.getAbstractPojo();

		/**
		 * The meta object literal for the '<em><b>Class</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ABSTRACT_POJO__CLASS = eINSTANCE.getAbstractPojo_Class();

		/**
		 * The meta object literal for the '<em><b>Template</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ABSTRACT_POJO__TEMPLATE = eINSTANCE.getAbstractPojo_Template();

		/**
		 * The meta object literal for the '{@link com.hybris.hyeclipse.emf.beans.impl.AbstractPojosImpl <em>Abstract Pojos</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.hybris.hyeclipse.emf.beans.impl.AbstractPojosImpl
		 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getAbstractPojos()
		 * @generated
		 */
		EClass ABSTRACT_POJOS = eINSTANCE.getAbstractPojos();

		/**
		 * The meta object literal for the '<em><b>Group</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ABSTRACT_POJOS__GROUP = eINSTANCE.getAbstractPojos_Group();

		/**
		 * The meta object literal for the '<em><b>Bean</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ABSTRACT_POJOS__BEAN = eINSTANCE.getAbstractPojos_Bean();

		/**
		 * The meta object literal for the '<em><b>Enum</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ABSTRACT_POJOS__ENUM = eINSTANCE.getAbstractPojos_Enum();

		/**
		 * The meta object literal for the '{@link com.hybris.hyeclipse.emf.beans.impl.BeanImpl <em>Bean</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.hybris.hyeclipse.emf.beans.impl.BeanImpl
		 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getBean()
		 * @generated
		 */
		EClass BEAN = eINSTANCE.getBean();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute BEAN__DESCRIPTION = eINSTANCE.getBean_Description();

		/**
		 * The meta object literal for the '<em><b>Property</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference BEAN__PROPERTY = eINSTANCE.getBean_Property();

		/**
		 * The meta object literal for the '<em><b>Deprecated</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute BEAN__DEPRECATED = eINSTANCE.getBean_Deprecated();

		/**
		 * The meta object literal for the '<em><b>Extends</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute BEAN__EXTENDS = eINSTANCE.getBean_Extends();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute BEAN__TYPE = eINSTANCE.getBean_Type();

		/**
		 * The meta object literal for the '{@link com.hybris.hyeclipse.emf.beans.impl.DocumentRootImpl <em>Document Root</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.hybris.hyeclipse.emf.beans.impl.DocumentRootImpl
		 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getDocumentRoot()
		 * @generated
		 */
		EClass DOCUMENT_ROOT = eINSTANCE.getDocumentRoot();

		/**
		 * The meta object literal for the '<em><b>Mixed</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DOCUMENT_ROOT__MIXED = eINSTANCE.getDocumentRoot_Mixed();

		/**
		 * The meta object literal for the '<em><b>XMLNS Prefix Map</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DOCUMENT_ROOT__XMLNS_PREFIX_MAP = eINSTANCE.getDocumentRoot_XMLNSPrefixMap();

		/**
		 * The meta object literal for the '<em><b>XSI Schema Location</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DOCUMENT_ROOT__XSI_SCHEMA_LOCATION = eINSTANCE.getDocumentRoot_XSISchemaLocation();

		/**
		 * The meta object literal for the '<em><b>Beans</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DOCUMENT_ROOT__BEANS = eINSTANCE.getDocumentRoot_Beans();

		/**
		 * The meta object literal for the '{@link com.hybris.hyeclipse.emf.beans.impl.EnumImpl <em>Enum</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.hybris.hyeclipse.emf.beans.impl.EnumImpl
		 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getEnum()
		 * @generated
		 */
		EClass ENUM = eINSTANCE.getEnum();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ENUM__DESCRIPTION = eINSTANCE.getEnum_Description();

		/**
		 * The meta object literal for the '<em><b>Value</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ENUM__VALUE = eINSTANCE.getEnum_Value();

		/**
		 * The meta object literal for the '<em><b>Deprecated</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ENUM__DEPRECATED = eINSTANCE.getEnum_Deprecated();

		/**
		 * The meta object literal for the '{@link com.hybris.hyeclipse.emf.beans.impl.PropertyImpl <em>Property</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.hybris.hyeclipse.emf.beans.impl.PropertyImpl
		 * @see com.hybris.hyeclipse.emf.beans.impl.BeansPackageImpl#getProperty()
		 * @generated
		 */
		EClass PROPERTY = eINSTANCE.getProperty();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PROPERTY__DESCRIPTION = eINSTANCE.getProperty_Description();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PROPERTY__NAME = eINSTANCE.getProperty_Name();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PROPERTY__TYPE = eINSTANCE.getProperty_Type();

	}

} //BeansPackage
