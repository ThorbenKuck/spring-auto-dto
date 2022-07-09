package test.dto;

import com.github.thorbenkuck.conversion.annotations.OfDomain;
import com.github.thorbenkuck.conversion.annotations.ToDomain;
import test.domain.Entity;


public class EntityDto {

	private String foo;

	public void setFoo(String foo) {
		this.foo = foo;
	}

	public String getFoo() {
		return foo;
	}

	@OfDomain
	public static EntityDto ofEntity(Entity entity) {
		EntityDto dto = new EntityDto();
		dto.setFoo(entity.getBar());
		return dto;
	}

	@ToDomain
	public Entity toDto() {
		return new Entity(foo);
	}
}
