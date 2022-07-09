package com.github.thorbenkuck.conversion;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MappingContext {

	private final Map<Class<?>, Map<Class<?>, DtoMapper<Object, Object>>> dtoConverters = new HashMap<>();

	public MappingContext(List<DtoMapper<?, ?>> dtoMappers) {
		dtoMappers.forEach(mapper -> {
			dtoConverters.computeIfAbsent(mapper.outputType(), (type) -> new HashMap<>());
			Map<Class<?>, DtoMapper<Object, Object>> classDtoMapperMap = dtoConverters.get(mapper.outputType());
			classDtoMapperMap.put(mapper.inputType(), (DtoMapper<Object, Object>) mapper);
		});
	}

	public boolean canMap(Class<?> domainType, Class<?> dtoType) {
		if (dtoConverters.containsKey(dtoType)) {
			return dtoConverters.get(dtoType).containsKey(domainType);
		}

		return false;
	}

	public Object map(Class<?> domainType, Class<?> dtoType, Object object) {
		Map<Class<?>, DtoMapper<Object, Object>> dtoMap = dtoConverters.get(dtoType);
		if(dtoMap == null) {
			throw new IllegalArgumentException("No mappers registered for the dto type " + dtoType);
		}

		DtoMapper<Object, Object>mapper = dtoMap.get(domainType);
		if(mapper == null) {
			throw new IllegalArgumentException("There is no mapper registered from " + domainType + " to " + dtoType);
		}

		return mapper.map(object);
	}
}
