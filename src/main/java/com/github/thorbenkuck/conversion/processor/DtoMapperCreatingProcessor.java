package com.github.thorbenkuck.conversion.processor;

import com.github.thorbenkuck.conversion.DtoMapper;
import com.github.thorbenkuck.conversion.annotations.OfDomain;
import com.github.thorbenkuck.conversion.annotations.ToDomain;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import org.springframework.stereotype.Component;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

@AutoService(Processor.class)
public class DtoMapperCreatingProcessor extends AbstractProcessor {

	private void error(String message, Element element) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
	}

	private void info(String message, Element element) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
		System.out.println("[INFO] " + message);
	}

	private void info(String message) {
		info(message, null);
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(
				OfDomain.class.getName(),
				ToDomain.class.getName()
		);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
	}

	@Override
	public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
		roundEnvironment.getElementsAnnotatedWith(OfDomain.class).forEach(this::buildMapperForOfDomainAnnotation);
		roundEnvironment.getElementsAnnotatedWith(ToDomain.class).forEach(this::buildMapperForToDomainAnnotation);

		return true;
	}

	private void buildMapperForOfDomainAnnotation(Element annotatedElement) {
		if(!(annotatedElement instanceof ExecutableElement)) {
			error("Only Methods may be annotated with @OfEntity", annotatedElement);
			return;
		}

		ExecutableElement methodElement = (ExecutableElement) annotatedElement;
		if(!methodElement.getModifiers().contains(Modifier.STATIC)) {
			error("Methods annotated with @OfEntity must be static", annotatedElement);
			return;
		}

		Element enclosingElement = methodElement.getEnclosingElement();
		if(!(enclosingElement instanceof TypeElement)) {
			throw new IllegalStateException();
		}
		TypeElement dtoTypeElement = (TypeElement) enclosingElement;
		VariableElement variableElement = methodElement.getParameters().get(0);
		TypeMirror entityType = variableElement.asType();

		TypeName entityTypeName = TypeName.get(entityType);
		TypeName dtoTypeName = TypeName.get(dtoTypeElement.asType());

		TypeSpec mappingClass = TypeSpec.classBuilder(dtoTypeElement.getSimpleName() + "OfEntityBuilder")
				.addAnnotation(Component.class)
				.addAnnotation(AnnotationSpec.builder(Generated.class)
						.addMember("value", "$S", DtoMapperCreatingProcessor.class.getName())
						.build())
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addSuperinterface(ParameterizedTypeName.get(ClassName.get(DtoMapper.class), entityTypeName, dtoTypeName))
				.addMethod(
						MethodSpec.methodBuilder("inputType")
								.addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
								.addAnnotation(Override.class)
								.returns(ParameterizedTypeName.get(ClassName.get(Class.class), entityTypeName))
								.addStatement("return $T.class", entityTypeName)
								.build()
				).addMethod(
						MethodSpec.methodBuilder("outputType")
								.addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
								.addAnnotation(Override.class)
								.returns(ParameterizedTypeName.get(ClassName.get(Class.class), dtoTypeName))
								.addStatement("return $T.class", dtoTypeName)
								.build()
				).addMethod(
						MethodSpec.methodBuilder("map")
								.addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
								.addAnnotation(Override.class)
								.returns(dtoTypeName)
								.addParameter(entityTypeName, "parameter", Modifier.FINAL)
								.addStatement("return $T.$L(parameter)", dtoTypeName, methodElement.getSimpleName())
								.build()
				).build();

		try {
			Name packageName = processingEnv.getElementUtils().getPackageOf(dtoTypeElement).getQualifiedName();
			JavaFile.builder(packageName.toString(), mappingClass)
					.build()
					.writeTo(processingEnv.getFiler());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void buildMapperForToDomainAnnotation(Element annotatedElement) {
		if(!(annotatedElement instanceof ExecutableElement)) {
			error("Only Methods may be annotated with @ToEntity", annotatedElement);
			return;
		}

		ExecutableElement methodElement = (ExecutableElement) annotatedElement;
		Element enclosingElement = methodElement.getEnclosingElement();
		if(!(enclosingElement instanceof TypeElement)) {
			throw new IllegalStateException();
		}
		TypeElement dtoTypeElement = (TypeElement) enclosingElement;
		TypeMirror entityType = methodElement.getReturnType();
		TypeName entityTypeName = TypeName.get(entityType);
		TypeName dtoTypeName = TypeName.get(dtoTypeElement.asType());

		TypeSpec mappingClass = TypeSpec.classBuilder(dtoTypeElement.getSimpleName() + "ToEntityBuilder")
				.addAnnotation(Component.class)
				.addAnnotation(AnnotationSpec.builder(Generated.class)
						.addMember("value", "$S", DtoMapperCreatingProcessor.class.getName())
						.build())
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addSuperinterface(ParameterizedTypeName.get(ClassName.get(DtoMapper.class), dtoTypeName, entityTypeName))
				.addMethod(
						MethodSpec.methodBuilder("outputType")
								.addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
								.addAnnotation(Override.class)
								.returns(ParameterizedTypeName.get(ClassName.get(Class.class), entityTypeName))
								.addStatement("return $T.class", entityTypeName)
								.build()
				).addMethod(
						MethodSpec.methodBuilder("inputType")
								.addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
								.addAnnotation(Override.class)
								.returns(ParameterizedTypeName.get(ClassName.get(Class.class), dtoTypeName))
								.addStatement("return $T.class", dtoTypeName)
								.build()
				).addMethod(
						MethodSpec.methodBuilder("map")
								.addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
								.addAnnotation(Override.class)
								.returns(entityTypeName)
								.addParameter(dtoTypeName, "parameter", Modifier.FINAL)
								.addStatement("return parameter.$L()", methodElement.getSimpleName())
								.build()
				).build();

		try {
			Name packageName = processingEnv.getElementUtils().getPackageOf(dtoTypeElement).getQualifiedName();
			JavaFile.builder(packageName.toString(), mappingClass)
					.build()
					.writeTo(processingEnv.getFiler());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
