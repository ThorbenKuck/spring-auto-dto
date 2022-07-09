package com.github.thorbenkuck.conversion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AutoConfiguration {

	@Bean
	public MappingContext mappingContext(List<DtoMapper<?, ?>> mapperList) {
		return new MappingContext(mapperList);
	}

	@Bean
	public ResponseConverter responseConverter(MappingContext mappingContext) {
		return new ResponseConverter(mappingContext);
	}

	@Bean
	public RequestConverter requestConverter(MappingContext mappingContext) {
		return new RequestConverter(mappingContext);
	}

}
