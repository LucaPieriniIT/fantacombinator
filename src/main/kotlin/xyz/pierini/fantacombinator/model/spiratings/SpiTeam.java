package xyz.pierini.fantacombinator.model.spiratings;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpiTeam {

	 private String code;
	 
	 private String name;
	 
	 @JsonProperty("global_rating")
	 private BigDecimal globalRating;
	 
}
