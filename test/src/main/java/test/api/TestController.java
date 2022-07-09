package test.api;

import com.github.thorbenkuck.conversion.annotations.AsDto;
import com.github.thorbenkuck.conversion.annotations.OfDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import test.domain.Entity;
import test.dto.EntityDto;

@RestController
public class TestController {

	@GetMapping("test")
	@AsDto(EntityDto.class)
	public Entity get() {
		return new Entity("Example");
	}

	@PostMapping("test")
	@AsDto(EntityDto.class)
	public Entity post(@RequestBody @OfDto(EntityDto.class) Entity entity) {
		return entity;
	}
}
