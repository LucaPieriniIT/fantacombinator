package xyz.pierini.fantacombinator.service;

import java.net.URI;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import xyz.pierini.fantacombinator.model.spiratings.SpiForecast;
import xyz.pierini.fantacombinator.model.spiratings.SpiWrapper;

@Service
public class SpiForecastService {
	
	private static final String BASE_URL = "https://projects.fivethirtyeight.com/soccer-predictions/forecasts/";
	
	private static final String URL_SUFFIX = "_forecast.json";

	public SpiForecast getLeaguesByCountryAndSeason(String thisYearSeason, String mainLeague) {
		try {
			String url = buildSpiUrl(thisYearSeason, mainLeague);
			SpiWrapper rs = getWebClientUri(url).retrieve()
					.bodyToMono(new ParameterizedTypeReference<SpiWrapper>() {})
					.block();
			return rs.getLastUpdatedForecast();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String buildSpiUrl(String thisYearSeason, String mainLeague) {
		// https://projects.fivethirtyeight.com/soccer-predictions/forecasts/2019_serie-a_forecast.json
		// boh, spero funzioni... molto a caso
		return BASE_URL + thisYearSeason + "_" + mainLeague.toLowerCase().replace(" ", "-") + URL_SUFFIX;
	}

	private WebClient.RequestBodySpec getWebClientUri(String url) {
		WebClient client = WebClient.builder()
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024)).build())
				.build();

		return client.method(HttpMethod.GET).uri(URI.create(url));
	}
	
}
