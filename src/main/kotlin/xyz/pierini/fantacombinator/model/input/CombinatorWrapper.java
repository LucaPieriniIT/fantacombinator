package xyz.pierini.fantacombinator.model.input;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CombinatorWrapper {
	private Setting settings;
	private List<Club> clubs;
	private List<Day> days;
}