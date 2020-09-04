package xyz.pierini.fantacombinator.model.apifootball;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureTeam {

	@JsonProperty("team_name")
	private String teamName;

}
