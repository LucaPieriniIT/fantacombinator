package xyz.pierini.fantacombinator.model.input;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Setting {
	private List<Integer> skipDays;
	private int bigClubs;
	private boolean skipAssociatedBig;
}