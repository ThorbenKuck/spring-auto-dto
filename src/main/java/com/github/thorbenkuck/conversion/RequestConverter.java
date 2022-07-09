package com.github.thorbenkuck.conversion;

import com.github.thorbenkuck.conversion.annotations.AsDto;
import com.github.thorbenkuck.conversion.annotations.OfDto;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

@RestControllerAdvice
public class RequestConverter extends RequestBodyAdviceAdapter {

	private final MappingContext mappingContext;
	@Autowired
	private ApplicationContext applicationContext;

	private static final Logger logger = LoggerFactory.getLogger(RequestConverter.class);

	public RequestConverter(MappingContext mappingContext) {
		this.mappingContext = mappingContext;
		logger.info("Initialized RequestConverter");
	}

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
		return extractMappingInformation(methodParameter)
				.map(it -> mappingContext.canMap(it.getDomainType(), it.getDtoType()))
				.orElse(false);
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
		MappingInformation mappingInformation = extractMappingInformation(parameter).orElseThrow(() -> new IllegalStateException("Could not find MappingInformation"));
		HttpMessageConverter<Object> converter = (HttpMessageConverter<Object>) applicationContext.getAutowireCapableBeanFactory().getBean(converterType);

		Object dto = converter.read(mappingInformation.getDtoType(), inputMessage);
		Object domain = mappingContext.map(mappingInformation.getDomainType(), mappingInformation.getDtoType(), dto);
		List<MediaType> supportedMediaTypes = converter.getSupportedMediaTypes(domain.getClass());

		BufferingHttpOutputMessage bufferingHttpOutputMessage = new BufferingHttpOutputMessage(inputMessage);

		converter.write(domain, supportedMediaTypes.get(0), bufferingHttpOutputMessage);
		return new BufferedInputMessage(bufferingHttpOutputMessage);
	}

	private class BufferedInputMessage implements HttpInputMessage {

		private final HttpHeaders headers;
		private final InputStream inputStream;

		public BufferedInputMessage(BufferingHttpOutputMessage outputMessage) {
			headers = outputMessage.headers;
			inputStream = new ByteArrayInputStream(outputMessage.outputStream.toByteArray());
		}

		@Override
		public InputStream getBody() throws IOException {
			return inputStream;
		}

		@Override
		public HttpHeaders getHeaders() {
			return headers;
		}
	}

	private class BufferingHttpOutputMessage implements HttpOutputMessage {

		private final HttpHeaders headers;
		private final ByteArrayOutputStream outputStream;

		public BufferingHttpOutputMessage(HttpInputMessage message) {
			headers = message.getHeaders();
			outputStream = new ByteArrayOutputStream();
		}

		@Override
		public OutputStream getBody() throws IOException {
			return outputStream;
		}

		@Override
		public HttpHeaders getHeaders() {
			return headers;
		}
	}

	private Optional<MappingInformation> extractMappingInformation(MethodParameter returnType) {
		Method method = returnType.getMethod();
		if(method == null) {
			return Optional.empty();
		}

		for (Parameter parameter : method.getParameters()) {
			if(parameter.isAnnotationPresent(OfDto.class)) {
				Class<?> domainType = parameter.getType();
				Class<?> dtoType = parameter.getAnnotation(OfDto.class).value();
				return Optional.of(new MappingInformation(domainType, dtoType));
			}
		}

		return Optional.empty();
	}
}
