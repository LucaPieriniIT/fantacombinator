package xyz.pierini.fantacombinator.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import xyz.pierini.fantacombinator.utility.Utility;

@Service
public class InputService {
	
	private final String JSON_BASE_PATH = "fantacombinator/";
	private final String JSON_SETTINGS_FILENAME = "settings.json";
	private final String JSON_CALENDAR_FILENAME = "calendar.json";
	private final String JSON_CLUBS_FILENAME = "clubs.json";
	
	private static final int DEFAULT_PROMOTED_WEIGHT = 30;

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private ApiFootballService apiFootballService;
	
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
		List<Club> clubs = getFile(JSON_BASE_PATH + JSON_CLUBS_FILENAME, new TypeReference<List<Club>>() {});
		if (!checkClub(clubs, days, setting)) {
			// get from API
			clubs = getClubsFromApiFootball(setting, days);
			if (checkClub(clubs, days, setting)) {
				persistClubs(clubs);
				return clubs;
			}
			throw new Exception("Error getting clubs");
		}
		return clubs;
	}

	private boolean checkClub(List<Club> clubs, List<Day> days, Setting setting) {
		if (Utility.isEmpty(clubs) || clubs.size() > setting.getBigClubs()) {
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
		List<Day> days = getFile(JSON_BASE_PATH + JSON_CALENDAR_FILENAME, new TypeReference<List<Day>>() {});
		if (!checkCalendar(days)) {
			// get from API
			days = getCalendarFromApiFootball(setting);
			if (checkCalendar(days)) {
				persistCalendar(days);
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
					clubHome.setWeight(standing.getAll().getGoalsFor());
				} else if (standing.getTeamName().equals(clubAway.getName())) {
					clubAway.setWeight(standing.getAll().getGoalsFor());
				}
			}
			clubs.add(clubHome);
			clubs.add(clubAway);
		}
		clubs.sort((o1, o2) -> {
			return o1.getWeight() > o2.getWeight() ? -1 : 1;
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
	
	private void persistClubs(List<Club> clubs) {
		// TODO Auto-generated method stub
		
	}
	
	private void persistCalendar(List<Day> days) {
		// TODO Auto-generated method stub
		
	}

}
