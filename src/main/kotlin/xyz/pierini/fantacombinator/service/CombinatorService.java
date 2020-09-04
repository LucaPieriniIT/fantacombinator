package xyz.pierini.fantacombinator.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import xyz.pierini.fantacombinator.enums.CombinationTypeEnum;
import xyz.pierini.fantacombinator.model.input.Club;
import xyz.pierini.fantacombinator.model.input.CombinatorWrapper;
import xyz.pierini.fantacombinator.model.input.Day;
import xyz.pierini.fantacombinator.model.input.Match;
import xyz.pierini.fantacombinator.model.output.AssociatedClub;
import xyz.pierini.fantacombinator.model.output.ClubOutput;
import xyz.pierini.fantacombinator.model.output.OutputWrapper;

@Service
public class CombinatorService {
	
	@Autowired
	private InputService inputService;
	
	@Autowired
	private TripletService tripletService;

	public void init() throws JsonMappingException, JsonProcessingException {
		CombinatorWrapper input = inputService.getCombinatorWrapper();
		List<String> bigClubs = getBigClubs(input);
		
		OutputWrapper weightCombination = combineWithWeight(input, bigClubs);
		System.out.println("\n\nCombinazioni con pesi\n" + weightCombination.getFormattedOutput());
		OutputWrapper bigMatchesCombination = combineWithBigMatches(input, bigClubs);
		System.out.println("\n\nCombinazioni con incontri vs. big\n" + bigMatchesCombination.getFormattedOutput());
		OutputWrapper mergedOutput = mergeOutput(bigMatchesCombination, weightCombination);
		System.out.println("\n\nMerge\n" + mergedOutput.getFormattedOutput());
		List<List<String>> triplets = tripletService.calculateTriplets(mergedOutput, input, bigClubs);
		System.out.println("\n\nTriplette\n" + triplets);
	}

	private OutputWrapper mergeOutput(OutputWrapper bigMatchesCombination, OutputWrapper weightCombination) {
		/*
		 * in caso di parità nei bigMatchesCombination, ordino per weightCombination
		 */
		List<ClubOutput> clubsOutput = new ArrayList<>();
		for (ClubOutput co : bigMatchesCombination.getClubs()) {
			co.getBestClubs().sort((o1, o2) -> {
				if (o1.getAssociatedValue() != o2.getAssociatedValue()) {
					return Integer.valueOf(o1.getAssociatedValue()).compareTo(o2.getAssociatedValue());
				} else {
					// parità! controllo in weightCombination
					String o1Club = o1.getName();
					String o2Club = o2.getName();
					ClubOutput cow = weightCombination.getClubs().stream().filter((c) -> c.getName().equals(co.getName())).findFirst().get();
					Integer o1Weight = cow.getBestClubs().stream().filter((bc) -> bc.getName().equals(o1Club)).findFirst().get().getAssociatedValue();
					Integer o2Weight = cow.getBestClubs().stream().filter((bc) -> bc.getName().equals(o2Club)).findFirst().get().getAssociatedValue();
					if (o1Weight == null || o2Weight == null) {
						// non dovrebbe mai succedere...
						return 0;
					}
					return o1Weight.compareTo(o2Weight);
				}
			});
			clubsOutput.add(new ClubOutput(co.getName(), co.getBestClubs()));
		}
		return new OutputWrapper(clubsOutput);
	}

	private OutputWrapper combineWithBigMatches(CombinatorWrapper input, List<String> bigClubs) {
		return outerCombine(input, bigClubs, CombinationTypeEnum.BIG_MATCHES);
	}
	
	private OutputWrapper combineWithWeight(CombinatorWrapper input, List<String> bigClubs) {
		return outerCombine(input, bigClubs, CombinationTypeEnum.WEIGHT);
	}
	
	private OutputWrapper outerCombine(CombinatorWrapper input, List<String> bigClubs, CombinationTypeEnum combinationType) {
		List<ClubOutput> outputClubs = new ArrayList<>();

		for (Club c : input.getClubs()) {
			List<AssociatedClub> bestClubs = new ArrayList<>();
			for (Day d : input.getDays()) {
				if (!shouldDayBeSkipped(d, input)) {
					for (Match m : d.getMatches()) {
						String against = null;
						if (m.getAway().equals(c.getName())) {
							against = m.getHome();
						}
						if (m.getHome().equals(c.getName())) {
							against = m.getAway();
						}
	
						if (against != null && bigClubs.contains(against)) {
							// sono contro un big club, valuto tutti gli altri club
							
							for (Match mm : d.getMatches()) {
								if (!mm.getAway().equals(c.getName())) {
									// guardo il peso di quello in casa e lo aggiungo alla lista
									bestClubs = checkAgainstBigClub(input, combinationType, bigClubs, bestClubs, d, mm.getAway(), mm.getHome());
								}
								if (!mm.getHome().equals(c.getName())) {
									// guardo il peso di quello in trasferta e lo aggiungo alla lista						
									bestClubs = checkAgainstBigClub(input, combinationType, bigClubs, bestClubs, d, mm.getHome(), mm.getAway());
								}
							}
						}
					}
				}
			}

			if (combinationType == CombinationTypeEnum.BIG_MATCHES) {
				bestClubs = addMissingClubs(bestClubs, c.getName(), input.getClubs());
			}
			Collections.sort(bestClubs, Comparator.comparing(AssociatedClub::getAssociatedValue));
			outputClubs.add(new ClubOutput(c.getName(), bestClubs));
		}

		return new OutputWrapper(outputClubs);
	}

	private List<AssociatedClub> checkAgainstBigClub(CombinatorWrapper input, CombinationTypeEnum combinationType,
			List<String> bigClubs, List<AssociatedClub> bestClubs, Day d, String myName, String against) {
		
		if (combinationType == CombinationTypeEnum.BIG_MATCHES) {
			if (bigClubs.contains(against)) {
				bestClubs = updateBestClubs(bestClubs, myName, 1, d.getNumber());
			}
		} else if (combinationType == CombinationTypeEnum.WEIGHT) {
			int weight = getWeightByName(against, input.getClubs());
			bestClubs = updateBestClubs(bestClubs, myName, weight, null);
		}
		return bestClubs;
	}

	private boolean shouldDayBeSkipped(Day d, CombinatorWrapper input) {
		/*if (input.getSettings() == null || input.getSettings().getSkipDays() == null || input.getSettings().getSkipDays().size() == 0) {
			return false;
		}
		if (d.getNumber() <= input.getDays().get(input.getDays().size() -1).getNumber()) {
			// sono entro il girone d'andata
			return input.getSettings().getSkipDays().contains(d.getNumber());
		}
		// girone di ritorno
		*/
		return false;
	}

	private List<AssociatedClub> addMissingClubs(List<AssociatedClub> bestClubs, String myName, List<Club> clubs) {
		/*
		 * usato solo nel caso del calcolo BIG_MATCHES
		 */
		for (Club c : clubs) {
			if (!c.getName().equals(myName)) {
				boolean found = false;
				for (AssociatedClub ac : bestClubs) {
					if (ac.getName().equals(c.getName())) {
						found = true;
					}
				}
				if (!found) {
					bestClubs.add(new AssociatedClub(c.getName(), 0, null));
				}
			}
		}
		return bestClubs;
	}

	private List<AssociatedClub> updateBestClubs(List<AssociatedClub> bestClubs, String name, int weight, Integer dayNumber) {
		for (AssociatedClub ac : bestClubs) {
			if (ac.getName().equals(name)) {
				ac.setAssociatedValue(ac.getAssociatedValue() + weight);
				ac = addDayNumber(dayNumber, ac);
				return bestClubs;
			}
		}
		AssociatedClub newAc = new AssociatedClub(name, weight, null);
		newAc = addDayNumber(dayNumber, newAc);
		bestClubs.add(newAc);
		return bestClubs;
	}

	private AssociatedClub addDayNumber(Integer dayNumber, AssociatedClub ac) {
		if (dayNumber != null) {
			if (ac.getDaysAgainstBigClub() == null) {
				// https://stackoverflow.com/a/5755510
				ac.setDaysAgainstBigClub(new ArrayList<>(Arrays.asList(dayNumber)));
			} else {
				List<Integer> days = ac.getDaysAgainstBigClub();
				days.add(dayNumber);
				ac.setDaysAgainstBigClub(days);
			}
		}
		return ac;
	}

	private int getWeightByName(String name, List<Club> clubs) {
		return clubs.stream().filter(c -> c.getName().equals(name)).findFirst().get().getWeight();
	}

	private List<String> getBigClubs(CombinatorWrapper input) {
		List<Club> clubs = input.getClubs();
		int bigClubsNumber = input.getSettings().getBigClubs();

		Collections.sort(clubs, Comparator.comparing(Club::getWeight).reversed()); // DESC

		return clubs.stream().limit(bigClubsNumber).map(Club::getName).collect(Collectors.toList());
	}

}
