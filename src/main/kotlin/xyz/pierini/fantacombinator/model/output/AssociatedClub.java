package xyz.pierini.fantacombinator.model.output;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AssociatedClub {

	private String name;

	private int associatedValue;
	
	private List<Integer> daysAgainstBigClub;

}
