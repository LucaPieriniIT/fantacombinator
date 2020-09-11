package xyz.pierini.fantacombinator.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.pierini.fantacombinator.model.apifootball.Fixture;
import xyz.pierini.fantacombinator.model.apifootball.League;
import xyz.pierini.fantacombinator.model.apifootball.Standing;
import xyz.pierini.fantacombinator.model.input.Club;
import xyz.pierini.fantacombinator.model.input.CombinatorWrapper;
import xyz.pierini.fantacombinator.model.input.Day;
import xyz.pierini.fantacombinator.model.input.Match;
import xyz.pierini.fantacombinator.model.input.Setting;
import xyz.pierini.fantacombinator.model.spiratings.SpiForecast;
import xyz.pierini.fantacombinator.model.spiratings.SpiTeam;
import xyz.pierini.fantacombinator.utility.Utility;

@Service
public class InputService {
	
	private final String JSON_BASE_PATH = "fantacombinator/";
	private final String JSON_SETTINGS_FILENAME = "settings.json";
	private final String JSON_CALENDAR_FILENAME = "calendar.json";
	private final String JSON_CLUBS_FILENAME = "clubs.json";
	
	private static final BigDecimal DEFAULT_PROMOTED_WEIGHT = new BigDecimal(30);

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private ApiFootballService apiFootballService;
	
	@Autowired
	private SpiForecastService spiForecastService;
	
	public CombinatorWrapper getCombinatorWrapper() throws Exception {
		try {
			Setting setting = getSetting();
			List<Day> days = getCalendar(setting);
			List<Club> clubs = getClubs(setting, days);
			return new CombinatorWrapper(setting, clubs, days);
		} catch (Exception e) {
			throw e;
		}
	}
	
	private Setting getSetting() throws Exception {
		Setting setting = getFile(JSON_BASE_PATH + JSON_SETTINGS_FILENAME, Setting.class);
		if (!checkSetting(setting)) {
			throw new Exception("Error checking settings file");
		}
		return setting;
	}
	
	private boolean checkSetting(Setting setting) {
		return setting != null
				&& setting.getBigClubs() > 0
				&& Utility.isNotEmpty(setting.getApiKey())
				&& Utility.isNotEmpty(setting.getThisYearSeason())
				&& Utility.isNotEmpty(setting.getPreviousYearSeason())
				&& Utility.isNotEmpty(setting.getCountryName())
				&& Utility.isNotEmpty(setting.getMainLeagueName())
				//&& Utility.isNotEmpty(setting.getPromotedFromLeagueName())
				;
	}
	
	private List<Club> getClubs(Setting setting, List<Day> days) throws Exception {
		List<Club> clubs;
		try {
			clubs = getFile(JSON_BASE_PATH + JSON_CLUBS_FILENAME, new TypeReference<List<Club>>() {});
		} catch (Exception e) {
			clubs = null;
		}
		if (!checkClub(clubs, days, setting)) {
			// get from Days + SPI ratings
			clubs = getFromDaysAndSpiRatings(setting, days);
			if (checkClub(clubs, days, setting)) {
				return clubs;
			}
			// get from API
			clubs = getClubsFromApiFootball(setting, days);
			if (checkClub(clubs, days, setting)) {
				return clubs;
			}
			throw new Exception("Error getting clubs");
		}
		return clubs;
	}

	private boolean checkClub(List<Club> clubs, List<Day> days, Setting setting) {
		if (Utility.isEmpty(clubs) || clubs.size() < setting.getBigClubs()) {
			return false;
		}
		List<String> clubNames = clubs.stream().map(Club::getName).collect(Collectors.toList());
		for (Day d : days) {
			for (Match m : d.getMatches()) {
				if (!clubNames.contains(m.getAway()) || !clubNames.contains(m.getHome())) {
					return false;
				}
			}
		}
		return true;
	}
	
	private List<Day> getCalendar(Setting setting) throws Exception {
		List<Day> days;
		try {
			days = getFile(JSON_BASE_PATH + JSON_CALENDAR_FILENAME, new TypeReference<List<Day>>() {});
		} catch (Exception e) {
			days = null;
		}
		if (!checkCalendar(days)) {
			// get from API
			days = getCalendarFromApiFootball(setting);
			if (checkCalendar(days)) {
				return days;
			}
			throw new Exception("Error getting clubs");
		}
		return days;
	}

	private boolean checkCalendar(List<Day> days) {
		if (Utility.isEmpty(days)) {
			return false;
		}
		for (Day d : days) {
			if (Utility.isEmpty(d.getMatches())) {
				return false;
			}
			for (Match m : d.getMatches()) {
				if (Utility.isEmpty(m.getAway()) || Utility.isEmpty(m.getHome())) {
					return false;
				}
			}
		}
		return true;
	}

	private <T> T getFile(String fileName, Class<T> clazz) throws Exception {
		return objectMapper.readValue(baseReadFile(fileName), clazz);
	}
	
	private <T> T getFile(String fileName, TypeReference<T> valueTypeRef) throws Exception {
		return objectMapper.readValue(baseReadFile(fileName), valueTypeRef);
	}
	
	private String baseReadFile(String fileName) throws Exception {
		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream is = classLoader.getResourceAsStream(fileName);
		if (is == null) {
			throw new Exception("File " + fileName + " not found");
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String json = (String) reader.lines().collect(Collectors.joining(System.lineSeparator()));
		if (json == null) {
			throw new RuntimeException("File " + fileName + " not found!");
		}
		return json;
	}
	
	private List<Club> getClubsFromApiFootball(Setting setting, List<Day> days) {
		List<League> leagues = apiFootballService.getLeaguesByCountryAndSeason(
				setting.getApiKey(),
				setting.getCountryName(),
				setting.getPreviousYearSeason()
				);
		Integer mainLeagueId = null;
		//Integer promotedFromLeagueId = null;
		for (League league : leagues) {
			if (league.getName().equals(setting.getMainLeagueName())) {
				mainLeagueId = league.getLeague_id();
			}/* else if (league.getName().equals(wrapper.getSettings().getPromotedFromLeagueName())) {
				promotedFromLeagueId = league.getLeague_id();
			}*/
		}
		if (mainLeagueId == null) {
			throw new RuntimeException("Main league not found in previous year " + setting.getMainLeagueName());
		}
		List<Standing> mainLeagueStandings = apiFootballService.getLeagueTableByLeagueId(setting.getApiKey(), mainLeagueId);
		
		List<Club> clubs = new ArrayList<>();
		// spero che tutte giochino in tutte le giornate...
		for (Match match : days.get(0).getMatches()) {
			Club clubHome = new Club(match.getHome(), DEFAULT_PROMOTED_WEIGHT);
			Club clubAway = new Club(match.getAway(), DEFAULT_PROMOTED_WEIGHT);
			for (Standing standing : mainLeagueStandings) {
				if (standing.getTeamName().equals(clubHome.getName())) {
					clubHome.setWeight(new BigDecimal(standing.getAll().getGoalsFor()));
				} else if (standing.getTeamName().equals(clubAway.getName())) {
					clubAway.setWeight(new BigDecimal(standing.getAll().getGoalsFor()));
				}
			}
			clubs.add(clubHome);
			clubs.add(clubAway);
		}
		clubs.sort((o1, o2) -> {
			return o1.getWeight().compareTo(o2.getWeight()) * -1;
		});
		return clubs;
	}
	
	private List<Day> getCalendarFromApiFootball(Setting settings) {
		int leagueId = getLeagueIdForCalendarFromApiFootball(settings);
		List<Fixture> fixtures = apiFootballService.getFixturesByLeagueId(settings.getApiKey(), leagueId);
		List<String> rounds = apiFootballService.getRoundsByLeagueId(settings.getApiKey(), leagueId);
		return convertFixturesToListDay(fixtures, rounds);
	}

	private int getLeagueIdForCalendarFromApiFootball(Setting settings) {
		List<League> leagues = apiFootballService.getLeaguesByCountryAndSeason(settings.getApiKey(), settings.getCountryName(), settings.getThisYearSeason());
		for (League league : leagues) {
			if (league.getName().equals(settings.getMainLeagueName())) {
				return league.getLeague_id();
			}
		}
		throw new RuntimeException("League " + settings.getMainLeagueName() + " not found!");
	}
	
	private List<Day> convertFixturesToListDay(List<Fixture> fixtures, List<String> rounds) {
		Map<String, Day> mapDays = new HashMap<>();
		int counter = 1;
		for (String round : rounds) {
			// mi auguro che i rounds siano sempre ordinati...
			
			// in rounds ho "Regular_Season_-_1" ecc...
			// nelle fixtures sono scritti "Regular Season - 1"...
			// replace dell'underscore e amen
			
			mapDays.put(round.replace("_", " "), Day.builder().number(counter).matches(new ArrayList<Match>()).build());
			counter++;
		}
		for (Fixture fixture : fixtures) {
			Match match = new Match(fixture.getHomeTeam().getTeamName(), fixture.getAwayTeam().getTeamName());
			Day d = mapDays.get(fixture.getRound());
			List<Match> matches = d.getMatches();
			matches.add(match);
			d.setMatches(matches);
			mapDays.put(fixture.getRound(), d);
		}
		List<Day> rs = new ArrayList<>(mapDays.values());
		rs.sort((o1, o2) -> {
			return o1.getNumber() > o2.getNumber() ? 1 : -1;
		});
		return rs;
	}
	
	private List<Club> getFromDaysAndSpiRatings(Setting setting, List<Day> days) {
		SpiForecast forecast = spiForecastService.getLeaguesByCountryAndSeason(setting.getThisYearSeason(), setting.getMainLeagueName());
		if (forecast.getTeams().size() != days.get(0).getMatches().size() * 2) {
			return null;
		}
		// problema: api-football e fivethirtyeight scrivono i nomi delle squadre in modo diverso (ad es. Inter -> Internazionale)
		// assegno i pesi a quelle con il nome più simile, per le altre verifico la somiglianza della stringa.......
		Map<String, BigDecimal> mapForecast = forecast.getTeams().stream().collect(Collectors.toMap(SpiTeam::getName, SpiTeam::getGlobalRating));
		// ottengo una lista di club dalla prima giornata
		List<Club> clubs = new ArrayList<>();
		for (Match match : days.get(0).getMatches()) {
			// TODO refactor...
			BigDecimal weightHome = null;
			if (mapForecast.containsKey(match.getHome())) {
				weightHome = mapForecast.get(match.getHome());
				mapForecast.remove(match.getHome());
			}
			BigDecimal weightAway = null;
			if (mapForecast.containsKey(match.getAway())) {
				weightAway = mapForecast.get(match.getAway());
				mapForecast.remove(match.getAway());
			}
			Club clubHome = new Club(match.getHome(), weightHome);
			Club clubAway = new Club(match.getAway(), weightAway);
			clubs.add(clubHome);
			clubs.add(clubAway);
		}
		// sono stati mappati tutti
		if (mapForecast.size() == 0) {
			return clubs;
		}
		// qualcuno non è stato mappato...
		// se è uno solo, lo assegno alla squadra che manca, altrimenti verifico la somiglianza delle stringhe
		if (mapForecast.size() == 1) {
			for (Club c : clubs) {
				if (c.getWeight() == null) {
					c.setWeight(mapForecast.entrySet().iterator().next().getValue());
					return clubs;
				}
			}
		}
		// ne manca più di uno, verifico la somiglianza delle stringhe
		for (Club c : clubs) {
			if (c.getWeight() == null) {
				String mostSimilarClubName = null;
				Double similarityValue = 0.0;
				for (Map.Entry<String, BigDecimal> mapEntry : mapForecast.entrySet()) {
					double d = similarity(mapEntry.getKey(), c.getName());
					if (d > similarityValue) {
						similarityValue = d;
						mostSimilarClubName = mapEntry.getKey();
					}
				}
				if (mostSimilarClubName == null) {
					// qualcosa è andato storto...
					return null;
				}
				c.setWeight(mapForecast.get(mostSimilarClubName));
				mapForecast.remove(mostSimilarClubName);
				if (mapForecast.size() == 1) {
					for (Club c1 : clubs) {
						if (c1.getWeight() == null) {
							c1.setWeight(mapForecast.entrySet().iterator().next().getValue());
							return clubs;
						}
					}
				}
			}
		}
		if (mapForecast.size() > 0) {
			return null;
		}
		return clubs;
	}

	// https://stackoverflow.com/a/16018452
	/**
	 * Calculates the similarity (a number within 0 and 1) between two strings.
	 */
	public static double similarity(String s1, String s2) {
		String longer = s1, shorter = s2;
		if (s1.length() < s2.length()) { // longer should always have greater length
			longer = s2;
			shorter = s1;
		}
		int longerLength = longer.length();
		if (longerLength == 0) {
			return 1.0;
			/* both strings are zero length */ }
		/*
		 * // If you have Apache Commons Text, you can use it to calculate the edit
		 * distance: LevenshteinDistance levenshteinDistance = new
		 * LevenshteinDistance(); return (longerLength -
		 * levenshteinDistance.apply(longer, shorter)) / (double) longerLength;
		 */
		return (longerLength - editDistance(longer, shorter)) / (double) longerLength;

	}

	// Example implementation of the Levenshtein Edit Distance
	// See http://rosettacode.org/wiki/Levenshtein_distance#Java
	public static int editDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}

}
