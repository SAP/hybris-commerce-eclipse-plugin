package com.hybris.hyeclipse.emf.beans.presentation.platformdependent;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.hybris.hyeclipse.emf.beans.AbstractPojos;
import com.hybris.hyeclipse.emf.beans.Bean;
import com.hybris.hyeclipse.emf.beans.BeansFactory;
import com.hybris.hyeclipse.emf.beans.DocumentRoot;
import com.hybris.hyeclipse.emf.beans.Enum;
import com.hybris.hyeclipse.emf.beans.Property;

import de.hybris.bootstrap.config.ExtensionInfo;
import de.hybris.bootstrap.config.PlatformConfig;
import de.hybris.bootstrap.typesystem.YAtomicType;
import de.hybris.bootstrap.typesystem.YAttributeDescriptor;
import de.hybris.bootstrap.typesystem.YCollectionType;
import de.hybris.bootstrap.typesystem.YComposedType;
import de.hybris.bootstrap.typesystem.YEnumType;
import de.hybris.bootstrap.typesystem.YEnumValue;
import de.hybris.bootstrap.typesystem.YExtension;
import de.hybris.bootstrap.typesystem.YMapType;
import de.hybris.bootstrap.typesystem.YNameSpaceElement;
import de.hybris.bootstrap.typesystem.YNamespace;
import de.hybris.bootstrap.typesystem.YType;

class BeansGenerator implements IRunnableWithProgress {
	
	private final BeansFactory beansFactory = BeansFactory.eINSTANCE;
	
	final DocumentRoot documentRoot = BeansFactory.eINSTANCE.createDocumentRoot();
	final AbstractPojos beans = BeansFactory.eINSTANCE.createAbstractPojos();
	{
		documentRoot.setBeans(beans);
	}
	
	final Collection<? extends YType> typesToProcess;
	
	final PlatformConfig platformConfig;
	
	BeansGenerator(PlatformConfig platformConfig, final Collection<? extends YType> typesToProcess) {
		this.platformConfig = platformConfig;
		this.typesToProcess = typesToProcess;
	}

	private String getPackageRoot(YNameSpaceElement element) {
		final YNamespace namespace = element.getNamespace();
		if (namespace instanceof YExtension) {
			final YExtension yExtension = (YExtension) namespace;
			final ExtensionInfo extensionInfo = platformConfig.getExtensionInfo(yExtension.getExtensionName());
			if (extensionInfo != null && extensionInfo.getCoreModule() != null && extensionInfo.getCoreModule().getPackageRoot() != null) {
				return extensionInfo.getCoreModule().getPackageRoot();
			}
		}
		
		return "unknownpackage";
	}
	
	private String getBeanPackage(YNameSpaceElement element) {
		return getPackageRoot(element) + ".data";
	}
	
	private void addPojo(final YType type) {
		if (type instanceof YEnumType) {
			beans.getEnum().add(createEnum((YEnumType) type));
		}
		else if (type instanceof YComposedType) {
			beans.getBean().add(createBean((YComposedType) type));
		}
	}

	private Enum createEnum(YEnumType type) {
		final Enum e = beansFactory.createEnum();
		e.setClass(getEnumTypeName(type));
		for (final YEnumValue v : type.getValues()) {
			e.getValue().add(v.getCode());
		}
		return e;
	}

	public void run(IProgressMonitor monitor) {
		final SubMonitor sub = SubMonitor.convert(monitor, typesToProcess.size());
		try {
			for (final YType composedType : typesToProcess) {
				addPojo(composedType);
				sub.worked(1);
			}
		}
		finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		
	}
	
	private String getEnumTypeName(final YEnumType enumType) {
		return getBeanPackage(enumType) + "." + enumType.getCode() + "Enum";
	}
	
	private String getBeanTypeName(final YComposedType composedType) {
		return getBeanPackage(composedType) + "." + composedType.getCode() + "Data";
	}
	
	private String getTypeName(YType type) {
		if (type instanceof YComposedType) {
			return getBeanTypeName((YComposedType)type);
		}
		else if (type instanceof YAtomicType) {
			return type.getJavaClassName();
		}
		else if (type instanceof YCollectionType) {
			return getCollectionTypeName((YCollectionType) type);
		}
		else if (type instanceof YMapType) {
			return getMapTypeName((YMapType)type);
		}
		else if (type instanceof YEnumType) {
			return getEnumTypeName((YEnumType)type);
		}
		else {
			return "UNKNOWN TYPE: " + type.getClass();
		}
	}
	
	private String getMapTypeName(YMapType type) {
		if (type.isLocalizationMap()) {
			return String.class.getName();
		}
		else {
			return "UNKNOWN MAP TYPE: " + type;
		}
	}

	private String getCollectionTypeName(String rawTypeName, YType elementType) {
		return rawTypeName + "<" + getTypeName(elementType) + ">";
	}
	
	@SuppressWarnings("deprecation")
	private String getCollectionTypeName(YCollectionType collectionType) {
		switch (collectionType.getTypeOfCollection()) {
		case SET:
		case SORTED_SET:
			return getCollectionTypeName("java.util.Set", collectionType.getElementType());
		case LIST:
			return getCollectionTypeName("java.util.List", collectionType.getElementType());
		case COLLECTION:
		default:
			return getCollectionTypeName("java.util.Collection", collectionType.getElementType());
		}
	}

	private void populateBean(final Bean bean, final YComposedType composedType) {
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
		property.setType(getTypeName(attributeDescriptor.getType()));
	}

	private Bean createBean(YComposedType composedType) {
		final Bean bean = beansFactory.createBean();
		populateBean(bean, composedType);
		return bean;
	}

}
