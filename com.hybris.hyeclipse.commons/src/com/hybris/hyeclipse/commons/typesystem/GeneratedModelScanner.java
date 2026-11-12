/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.hyeclipse.commons.typesystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.hybris.hyeclipse.commons.Activator;

/** Parses generated Java source without loading Commerce classes or requiring a successful compilation. */
public final class GeneratedModelScanner {
	private static final Pattern JAVA_NAME = Pattern.compile("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*");
	private static final Set<String> SIMPLE_TYPES = new HashSet<>();
	static {
		Collections.addAll(SIMPLE_TYPES, "boolean", "byte", "char", "double", "float", "int", "long", "short",
				"void", "extends", "super");
	}

	/**
	 * @param platform imported, open platform project
	 * @param monitor cancellation and progress monitor
	 * @return complete model snapshot
	 * @throws CoreException if workspace sources cannot be read
	 * @throws IOException if generated source or model constants are invalid
	 */
	public TypeSystemSnapshot scan(final IProject platform, final IProgressMonitor monitor)
			throws CoreException, IOException {
		final IFolder folder = platform.getFolder(TypeSystemConstants.GENERATED_SOURCE_DIRECTORY);
		if (!platform.isOpen()) {
			throw new IOException("The imported platform project is closed");
		}
		// Ant can add generated files outside Eclipse; refresh before enumerating workspace resources.
		folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		if (!folder.exists()) {
			throw new IOException("The imported platform has no accessible bootstrap/gensrc directory");
		}
		final Map<String, SourceModel> models = new TreeMap<>();
		folder.accept(resource -> {
			checkCanceled(monitor);
			if (resource instanceof IFile && "java".equals(resource.getFileExtension())) {
				final IFile file = (IFile) resource;
				try (final InputStream input = file.getContents(true)) {
					addSource(file.getFullPath().toPortableString(),
							new String(input.readAllBytes(), Charset.forName(file.getCharset())), models, monitor);
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Could not read generated model source", e));
				}
			}
			return true;
		});
		return buildSnapshot(models, monitor);
	}

	/**
	 * Builds an index from workspace-path/source pairs; useful for tooling and deterministic source fixtures.
	 * @param sources generated compilation units
	 * @param monitor cancellation monitor
	 * @return immutable snapshot
	 * @throws IOException if source, constants or inheritance are invalid
	 */
	public TypeSystemSnapshot scanSources(final Map<String, String> sources, final IProgressMonitor monitor)
			throws IOException {
		final Map<String, SourceModel> models = new TreeMap<>();
		for (final Map.Entry<String, String> entry : new TreeMap<>(sources).entrySet()) {
			addSource(entry.getKey(), entry.getValue(), models, monitor);
		}
		return buildSnapshot(models, monitor);
	}

	private static void addSource(final String path, final String source, final Map<String, SourceModel> models,
			final IProgressMonitor monitor) throws IOException {
		checkCanceled(monitor);
		final ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		final Map<String, String> options = new HashMap<>();
		JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
		parser.setCompilerOptions(options);
		final CompilationUnit unit = (CompilationUnit) parser.createAST(monitor);
		for (final IProblem problem : unit.getProblems()) {
			if (problem.isError()) {
				throw new IOException("Invalid generated source " + path + ": " + problem.getMessage());
			}
		}
		for (final Object declaration : unit.types()) {
			if (declaration instanceof TypeDeclaration) {
				addDeclaration((TypeDeclaration) declaration, unit, path, models);
			}
		}
	}

	private static void addDeclaration(final TypeDeclaration declaration, final CompilationUnit unit,
			final String path, final Map<String, SourceModel> models) throws IOException {
		final SourceModel model = new SourceModel(declaration, unit, path);
		if (models.put(model.className, model) != null) {
			throw new IOException("Duplicate generated class: " + model.className);
		}
		for (final TypeDeclaration nested : declaration.getTypes()) {
			addDeclaration(nested, unit, path, models);
		}
	}

	private static TypeSystemSnapshot buildSnapshot(final Map<String, SourceModel> models,
			final IProgressMonitor monitor) throws IOException {
		for (final SourceModel model : models.values()) {
			model.superClass = resolveName(model.superClass, model, models);
		}
		final List<YType> types = new ArrayList<>();
		final Map<String, Map<String, YAttributeDescriptor>> inherited = new HashMap<>();
		for (final SourceModel model : models.values()) {
			checkCanceled(monitor);
			if (!isItemModel(model, models, new HashSet<>())
					|| !model.constants.containsKey(TypeSystemConstants.TYPE_CODE_FIELD)) {
				continue;
			}
			final String code = constant(model.constants.get(TypeSystemConstants.TYPE_CODE_FIELD), model, models,
					new HashSet<>());
			if (code == null || code.isBlank()) {
				throw new IOException("Cannot resolve _TYPECODE in " + model.className);
			}
			types.add(new YType(code, model.className, model.superClass, model.path, model.sourceLine,
					attributes(model, models, inherited).values()));
		}
		try {
			return new TypeSystemSnapshot(types);
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid generated type index", e);
		}
	}

	private static boolean isItemModel(final SourceModel model, final Map<String, SourceModel> models,
			final Set<String> visiting) throws IOException {
		if (!visiting.add(model.className)) {
			throw new IOException("Cyclic model inheritance: " + model.className);
		}
		if (TypeSystemConstants.ITEM_MODEL_CLASS.equals(model.className)
				|| TypeSystemConstants.ITEM_MODEL_CLASS.equals(model.superClass)) {
			return true;
		}
		final SourceModel parent = models.get(model.superClass);
		return parent != null && isItemModel(parent, models, visiting);
	}

	private static Map<String, YAttributeDescriptor> attributes(final SourceModel model,
			final Map<String, SourceModel> models, final Map<String, Map<String, YAttributeDescriptor>> cache)
			throws IOException {
		if (cache.containsKey(model.className)) {
			final Map<String, YAttributeDescriptor> cached = cache.get(model.className);
			if (cached == null) {
				throw new IOException("Cyclic attribute inheritance: " + model.className);
			}
			return cached;
		}
		cache.put(model.className, null);
		final Map<String, YAttributeDescriptor> result = new TreeMap<>();
		final SourceModel parent = models.get(model.superClass);
		if (parent != null) {
			result.putAll(attributes(parent, models, cache));
		}
		final Set<String> getters = new HashSet<>();
		for (final AccessorSource accessor : model.accessors) {
			if (!TypeSystemConstants.ACCESSOR_CLASS.equals(resolveName(accessor.annotationName, model, models))) {
				continue;
			}
			final String name = constant(accessor.qualifier, model, models, new HashSet<>());
			if (name == null || name.isBlank()) {
				throw new IOException("Cannot resolve Accessor qualifier in " + model.className);
			}
			if (!accessor.getter && getters.contains(name)) {
				continue;
			}
			result.put(name, descriptor(name, accessor.javaType, accessor.sourceLine, model, models));
			if (accessor.getter) {
				getters.add(name);
			}
		}
		cache.put(model.className, result);
		return result;
	}

	private static YAttributeDescriptor descriptor(final String name, final String type, final int sourceLine,
			final SourceModel model, final Map<String, SourceModel> models) {
		final Matcher matcher = JAVA_NAME.matcher(type);
		final StringBuilder resolved = new StringBuilder();
		while (matcher.find()) {
			final String token = matcher.group();
			matcher.appendReplacement(resolved, Matcher.quoteReplacement(resolveName(token, model, models)));
		}
		matcher.appendTail(resolved);
		return new YAttributeDescriptor(name, resolved.toString(), model.className, model.path, sourceLine);
	}

	private static void addAccessor(final List<?> modifiers, final String javaType, final boolean getter,
			final int line, final List<AccessorSource> accessors) {
		for (final Object modifier : modifiers) {
			if (modifier instanceof NormalAnnotation) {
				final NormalAnnotation annotation = (NormalAnnotation) modifier;
				for (final Object value : annotation.values()) {
					final MemberValuePair pair = (MemberValuePair) value;
					if (TypeSystemConstants.QUALIFIER_MEMBER.equals(pair.getName().getIdentifier())) {
						accessors.add(new AccessorSource(annotation.getTypeName().getFullyQualifiedName(),
								ConstantValue.from(pair.getValue()), javaType, getter, line));
					}
				}
			}
		}
	}

	private static String constant(final ConstantValue expression, final SourceModel model,
			final Map<String, SourceModel> models, final Set<String> visiting) {
		if (expression == null) {
			return null;
		}
		if (expression.literal != null) {
			return expression.literal;
		}
		if (expression.parts != null) {
			final StringBuilder value = new StringBuilder();
			for (final ConstantValue part : expression.parts) {
				final String text = constant(part, model, models, new HashSet<>(visiting));
				if (text == null) {
					return null;
				}
				value.append(text);
			}
			return value.toString();
		}
		if (expression.reference != null) {
			final String name = expression.reference;
			final int separator = name.lastIndexOf('.');
			final String field = separator < 0 ? name : name.substring(separator + 1);
			SourceModel owner = separator < 0 ? model
					: models.get(resolveName(name.substring(0, separator), model, models));
			if (separator < 0 && !model.constants.containsKey(field) && model.staticImports.containsKey(field)) {
				final String imported = model.staticImports.get(field);
				owner = models.get(imported.substring(0, imported.lastIndexOf('.')));
			}
			while (owner != null && visiting.add(owner.className + "." + field)) {
				if (owner.constants.containsKey(field)) {
					return constant(owner.constants.get(field), owner, models, visiting);
				}
				owner = models.get(owner.superClass);
			}
		}
		return null;
	}

	private static String resolveName(final String name, final SourceModel model,
			final Map<String, SourceModel> models) {
		if (name.isEmpty() || SIMPLE_TYPES.contains(name)) {
			return name;
		}
		final int qualifier = name.indexOf('.');
		if (qualifier >= 0) {
			final String imported = model.imports.get(name.substring(0, qualifier));
			if (imported != null) {
				return imported + name.substring(qualifier);
			}
			final String qualified = model.packageName + "." + name;
			return models.containsKey(qualified) ? qualified : name;
		}
		if (model.imports.containsKey(name)) {
			return model.imports.get(name);
		}
		String enclosing = model.className;
		while (models.containsKey(enclosing)) {
			if (models.containsKey(enclosing + "." + name)) {
				return enclosing + "." + name;
			}
			final int end = enclosing.lastIndexOf('.');
			if (end < 0) {
				break;
			}
			enclosing = enclosing.substring(0, end);
		}
		final String samePackage = model.packageName.isEmpty() ? name : model.packageName + "." + name;
		if (models.containsKey(samePackage)) {
			return samePackage;
		}
		for (final String packageName : model.wildcardImports) {
			final String candidate = packageName + "." + name;
			if (models.containsKey(candidate) || candidate.equals(TypeSystemConstants.ITEM_MODEL_CLASS)
					|| candidate.equals(TypeSystemConstants.ACCESSOR_CLASS)
					|| ("java.util".equals(packageName) && ("List".equals(name) || "Set".equals(name)
							|| "Map".equals(name) || "Collection".equals(name) || "Date".equals(name)))) {
				return candidate;
			}
		}
		if ("String".equals(name) || "Boolean".equals(name) || "Integer".equals(name) || "Long".equals(name)
				|| "Double".equals(name) || "Float".equals(name) || "Number".equals(name) || "Object".equals(name)
				|| "Byte".equals(name) || "Short".equals(name) || "Character".equals(name)) {
			return "java.lang." + name;
		}
		return name;
	}

	private static void checkCanceled(final IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	private static final class SourceModel {
		private final int sourceLine;
		private final String path;
		private final String packageName;
		private final String className;
		private String superClass;
		private final Map<String, String> imports = new HashMap<>();
		private final Map<String, String> staticImports = new HashMap<>();
		private final List<String> wildcardImports = new ArrayList<>();
		private final Map<String, ConstantValue> constants = new HashMap<>();
		private final List<AccessorSource> accessors = new ArrayList<>();

		private SourceModel(final TypeDeclaration declaration, final CompilationUnit unit, final String path) {
			sourceLine = unit.getLineNumber(declaration.getStartPosition());
			this.path = path;
			packageName = unit.getPackage() != null ? unit.getPackage().getName().getFullyQualifiedName() : "";
			String typeName = declaration.getName().getIdentifier();
			ASTNode enclosing = declaration.getParent();
			while (enclosing instanceof TypeDeclaration) {
				typeName = ((TypeDeclaration) enclosing).getName().getIdentifier() + "." + typeName;
				enclosing = enclosing.getParent();
			}
			className = packageName.isEmpty() ? typeName : packageName + "." + typeName;
			superClass = declaration.getSuperclassType() != null
					? declaration.getSuperclassType().toString().replaceAll("<.*>", "") : "";
			for (final Object value : unit.imports()) {
				final ImportDeclaration imported = (ImportDeclaration) value;
				final String name = imported.getName().getFullyQualifiedName();
				if (imported.isOnDemand() && !imported.isStatic()) {
					wildcardImports.add(name);
				} else if (!imported.isOnDemand()) {
					(imported.isStatic() ? staticImports : imports).put(name.substring(name.lastIndexOf('.') + 1), name);
				}
			}
			for (final FieldDeclaration field : declaration.getFields()) {
				for (final Object value : field.fragments()) {
					final VariableDeclarationFragment fragment = (VariableDeclarationFragment) value;
					constants.put(fragment.getName().getIdentifier(), ConstantValue.from(fragment.getInitializer()));
				}
				addAccessor(field.modifiers(), field.getType().toString(), false,
						unit.getLineNumber(field.getStartPosition()), accessors);
			}
			for (final MethodDeclaration method : declaration.getMethods()) {
				if (method.isConstructor()) {
					continue;
				}
				final boolean getter = method.getReturnType2() != null
						&& !"void".equals(method.getReturnType2().toString());
				if (getter || method.parameters().size() == 1) {
					final String javaType = getter ? method.getReturnType2().toString()
							: ((SingleVariableDeclaration) method.parameters().get(0)).getType().toString();
					addAccessor(method.modifiers(), javaType, getter,
							unit.getLineNumber(method.getStartPosition()), accessors);
				}
			}
		}
	}

	// Keep only compact source facts; retaining AST nodes would retain every generated method body in the IDE heap.
	private static final class AccessorSource {
		private final String annotationName;
		private final ConstantValue qualifier;
		private final String javaType;
		private final boolean getter;
		private final int sourceLine;

		private AccessorSource(final String annotationName, final ConstantValue qualifier, final String javaType,
				final boolean getter, final int sourceLine) {
			this.annotationName = annotationName;
			this.qualifier = qualifier;
			this.javaType = javaType;
			this.getter = getter;
			this.sourceLine = sourceLine;
		}
	}

	private static final class ConstantValue {
		private final String literal;
		private final String reference;
		private final List<ConstantValue> parts;

		private ConstantValue(final String literal, final String reference, final List<ConstantValue> parts) {
			this.literal = literal;
			this.reference = reference;
			this.parts = parts;
		}

		private static ConstantValue from(final Expression expression) {
			if (expression instanceof StringLiteral) {
				return new ConstantValue(((StringLiteral) expression).getLiteralValue(), null, null);
			}
			if (expression instanceof Name) {
				return new ConstantValue(null, ((Name) expression).getFullyQualifiedName(), null);
			}
			if (expression instanceof ParenthesizedExpression) {
				return from(((ParenthesizedExpression) expression).getExpression());
			}
			if (expression instanceof InfixExpression) {
				final InfixExpression infix = (InfixExpression) expression;
				if (infix.getOperator() == InfixExpression.Operator.PLUS) {
					final List<ConstantValue> parts = new ArrayList<>();
					parts.add(from(infix.getLeftOperand()));
					parts.add(from(infix.getRightOperand()));
					for (final Object part : infix.extendedOperands()) {
						parts.add(from((Expression) part));
					}
					return new ConstantValue(null, null, parts);
				}
			}
			return null;
		}
	}
}
