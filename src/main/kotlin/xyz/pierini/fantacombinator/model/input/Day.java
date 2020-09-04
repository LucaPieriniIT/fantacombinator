package xyz.pierini.fantacombinator.model.input;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Day {
	private int number;
	private List<Match> matches;
}