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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.pierini.fantacombinator.model.apifootball.Fixture;
import xyz.pierini.fantacombinator.model.apifootball.League;
import xyz.pierini.fantacombinator.model.apifootball.Standing;
import xyz.pierini.fantacombinator.model.input.Club;
import xyz.pierini.fantacombinator.model.input.CombinatorWrapper;
import xyz.pierini.fantacombinator.model.input.Day;
import xyz.pierini.fantacombinator.model.input.Match;
import xyz.pierini.fantacombinator.model.input.Setting;

@Service
public class InputService {
	
	private final String JSON_WRAPPER_FILENAME = "fantacombinator.json";
	
	private static final int DEFAULT_PROMOTED_WEIGHT = 30;

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private ApiFootballService apiFootballService;

	public CombinatorWrapper getCombinatorWrapper() throws JsonMappingException, JsonProcessingException {
		InputStream is = getResourceFileAsInputStream(JSON_WRAPPER_FILENAME);
		if (is != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String json = (String) reader.lines().collect(Collectors.joining(System.lineSeparator()));
			CombinatorWrapper rs = objectMapper.readValue(json, CombinatorWrapper.class);
			if (areSettingsOk(rs)) {
				if (true) {//rs.getDays() == null || rs.getDays().isEmpty()) {
					rs.setDays(getCalendarFromApiFootball(rs.getSettings()));
				}
				if (true) {//rs.getClubs() == null || rs.getClubs().isEmpty()) {
					rs.setClubs(getClubsFromApiFootball(rs));
				}
				return rs;
			}
			throw new RuntimeException("Settings not ok");
		} else {
			throw new RuntimeException("resource not found");
		}
	}

	private boolean areSettingsOk(CombinatorWrapper rs) {
		// TODO check json
		return true;
	}
	
	private List<Club> getClubsFromApiFootball(CombinatorWrapper wrapper) {
		List<League> leagues = apiFootballService.getLeaguesByCountryAndSeason(wrapper.getSettings().getApiKey(), wrapper.getSettings().getCountryName(), wrapper.getSettings().getPreviousYearSeason());
		Integer mainLeagueId = null;
		//Integer promotedFromLeagueId = null;
		for (League league : leagues) {
			if (league.getName().equals(wrapper.getSettings().getMainLeagueName())) {
				mainLeagueId = league.getLeague_id();
			}/* else if (league.getName().equals(wrapper.getSettings().getPromotedFromLeagueName())) {
				promotedFromLeagueId = league.getLeague_id();
			}*/
		}
		if (mainLeagueId == null) {
			throw new RuntimeException("Main league not found in previous year " + wrapper.getSettings().getMainLeagueName());
		}
		List<Standing> mainLeagueStandings = apiFootballService.getLeagueTableByLeagueId(wrapper.getSettings().getApiKey(), mainLeagueId);
		
		List<Club> clubs = new ArrayList<>();
		// spero che tutte giochino in tutte le giornate...
		for (Match match : wrapper.getDays().get(0).getMatches()) {
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

	private static InputStream getResourceFileAsInputStream(String fileName) {
		ClassLoader classLoader = CombinatorService.class.getClassLoader();
		return classLoader.getResourceAsStream(fileName);
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

}
