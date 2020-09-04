package xyz.pierini.fantacombinator.model.input;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Day {
	private int number;
	private List<Match> matches;
}