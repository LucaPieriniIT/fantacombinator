package xyz.pierini.fantacombinator.model.output;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AssociatedClub {

	private String name;

	private BigDecimal associatedValue;
	
	private List<Integer> daysAgainstBigClub;

}
