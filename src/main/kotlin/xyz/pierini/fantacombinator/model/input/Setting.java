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
	
	// api-football.com
	private String apiKey;
	private String thisYearSeason;
	private String previousYearSeason;
	private String countryName;
	private String mainLeagueName;
}