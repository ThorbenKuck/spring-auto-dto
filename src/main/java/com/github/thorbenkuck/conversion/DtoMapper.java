package com.github.thorbenkuck.conversion;

public interface DtoMapper<Input, Output> {

	Output map(Input input);

	Class<Input> inputType();

	Class<Output> outputType();

}
