package com.github.thorbenkuck.conversion;

import com.github.thorbenkuck.conversion.annotations.AsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.util.Optional;

@RestControllerAdvice
public class ResponseConverter implements ResponseBodyAdvice<Object> {

	private final MappingContext mappingContext;
	private static final Logger logger = LoggerFactory.getLogger(RequestConverter.class);

	public ResponseConverter(MappingContext mappingContext) {
		this.mappingContext = mappingContext;
		logger.info("Initialized ResponseConverter");
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return extractMappingInformation(returnType)
				.map(information ->
				mappingContext.canMap(information.getDomainType(), information.getDtoType())
		).orElse(false);
	}

	@Override
	public Object beforeBodyWrite(
			Object body,
			MethodParameter returnType,
			MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request,
			ServerHttpResponse response
	) {
		MappingInformation mappingInformation = extractMappingInformation(returnType)
				.orElseThrow(() -> new IllegalStateException("Could not find MappingInformation"));

		return mappingContext.map(mappingInformation.getDomainType(), mappingInformation.getDtoType(), body);
	}

	private Optional<MappingInformation> extractMappingInformation(MethodParameter returnType) {
		AsDto annotation = returnType.getMethodAnnotation(AsDto.class);
		if (annotation == null) {
			return Optional.empty();
		}

		Method method = returnType.getMethod();
		if (method == null) {
			return Optional.empty();
		}

		Class<?> dtoType = annotation.value();
		Class<?> domainType = method.getReturnType();
		return Optional.of(new MappingInformation(dtoType, domainType));
	}
}
