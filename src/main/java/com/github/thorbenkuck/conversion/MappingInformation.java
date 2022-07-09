package com.github.thorbenkuck.conversion;

class MappingInformation<DTO, DOMAIN> {
	private final Class<DTO> dtoType;
	private final Class<DOMAIN> domainType;

	public MappingInformation(Class<DTO> dtoType, Class<DOMAIN> domainType) {
		this.dtoType = dtoType;
		this.domainType = domainType;
	}

	public Class<DTO> getDtoType() {
		return dtoType;
	}

	public Class<DOMAIN> getDomainType() {
		return domainType;
	}
}