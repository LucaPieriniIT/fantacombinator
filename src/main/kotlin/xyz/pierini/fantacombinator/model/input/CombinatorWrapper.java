package xyz.pierini.fantacombinator.model.input;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CombinatorWrapper {
	private Setting settings;
	private List<Club> clubs;
	private List<Day> days;
}