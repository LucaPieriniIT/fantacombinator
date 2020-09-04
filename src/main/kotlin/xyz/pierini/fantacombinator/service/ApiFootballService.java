package xyz.pierini.fantacombinator.service;

import java.net.URI;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import xyz.pierini.fantacombinator.model.apifootball.ApiOuterWrapper;
import xyz.pierini.fantacombinator.model.apifootball.Fixture;
import xyz.pierini.fantacombinator.model.apifootball.FixtureResult;
import xyz.pierini.fantacombinator.model.apifootball.League;
import xyz.pierini.fantacombinator.model.apifootball.LeagueResult;
import xyz.pierini.fantacombinator.model.apifootball.RoundsResult;
import xyz.pierini.fantacombinator.model.apifootball.Standing;
import xyz.pierini.fantacombinator.model.apifootball.StandingResult;

@Service
public class ApiFootballService {

	private static final String BASE_URL = "https://v2.api-football.com/";
	private static final String API_KEY_HEADER = "X-RapidAPI-Key";

	private static final String URL_LEAGUES = "leagues/";
	private static final String URL_COUNTRY = "country/";

	private static final String URL_FIXTURES = "fixtures/";
	private static final String URL_LEAGUE = "league/";
	
	private static final String URL_ROUNDS = "rounds/";
	
	private static final String URL_LEAGUE_TABLE = "leagueTable/";

	public List<League> getLeaguesByCountryAndSeason(String apiKey, String country, String season) {
		try {
			String url = BASE_URL + URL_LEAGUES + URL_COUNTRY + country + "/" + season;
			ApiOuterWrapper<LeagueResult> rs = getWebClientUri(url, apiKey).retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiOuterWrapper<LeagueResult>>() {})
					.block();
			return rs.getApi().getLeagues();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<Fixture> getFixturesByLeagueId(String apiKey, int leagueId) {
		try {
			String url = BASE_URL + URL_FIXTURES + URL_LEAGUE + leagueId;
			ApiOuterWrapper<FixtureResult> rs = getWebClientUri(url, apiKey).retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiOuterWrapper<FixtureResult>>() {})
					.block();
			return rs.getApi().getFixtures();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<String> getRoundsByLeagueId(String apiKey, int leagueId) {
		try {
			String url = BASE_URL + URL_FIXTURES + URL_ROUNDS + leagueId;
			ApiOuterWrapper<RoundsResult> rs = getWebClientUri(url, apiKey).retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiOuterWrapper<RoundsResult>>() {})
					.block();
			return rs.getApi().getFixtures();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<Standing> getLeagueTableByLeagueId(String apiKey, int leagueId) {
		try {
			String url = BASE_URL + URL_LEAGUE_TABLE + leagueId;
			ApiOuterWrapper<StandingResult> rs = getWebClientUri(url, apiKey).retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiOuterWrapper<StandingResult>>() {})
					.block();
			return rs.getApi().getStandings().get(0); // mi sembra assurdo ma vabbè...
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private WebClient.RequestBodySpec getWebClientUri(String url, String apiKey) {
		WebClient client = WebClient.builder()
				// .baseUrl(ApiFootballService.BASE_URL)
				.defaultHeader(ApiFootballService.API_KEY_HEADER, apiKey)
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)).build())
				.build();

		return client.method(HttpMethod.GET).uri(URI.create(url));
	}
}
