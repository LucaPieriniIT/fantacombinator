package xyz.pierini.fantacombinator.model.apifootball;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeagueResult extends ApiResultWrapper {

	private List<League> leagues;

}
