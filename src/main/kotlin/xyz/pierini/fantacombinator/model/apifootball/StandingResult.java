package xyz.pierini.fantacombinator.model.apifootball;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StandingResult extends ApiResultWrapper {

	// non so perchè questa sia List<List<>> ma è così...
	private List<List<Standing>> standings;
	
}
