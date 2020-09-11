package xyz.pierini.fantacombinator.model.spiratings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import xyz.pierini.fantacombinator.utility.Utility;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpiWrapper {

	// @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	@JsonProperty("last_updated")
	private String lastUpdated;
	
	private List<SpiForecast> forecasts;
	
	public SpiForecast getLastUpdatedForecast() {
		if (Utility.isEmpty(forecasts) || Utility.isEmpty(this.getLastUpdated())) {
			return null;
		}
		for(SpiForecast forecast : this.forecasts) {
			if (forecast.getLastUpdated().equals(this.getLastUpdated())) {
				return forecast;
			}
		}
		return null;
	}

}
