package xyz.pierini.fantacombinator.model.input;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Club {
	private String name;
	private BigDecimal weight;
}