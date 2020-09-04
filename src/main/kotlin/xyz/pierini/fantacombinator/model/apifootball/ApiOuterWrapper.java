package xyz.pierini.fantacombinator.model.apifootball;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiOuterWrapper<T extends ApiResultWrapper> {

	private T api;

}
