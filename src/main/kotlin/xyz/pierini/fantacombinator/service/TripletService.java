package xyz.pierini.fantacombinator.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import xyz.pierini.fantacombinator.model.input.CombinatorWrapper;
import xyz.pierini.fantacombinator.model.input.Day;
import xyz.pierini.fantacombinator.model.input.Match;
import xyz.pierini.fantacombinator.model.output.AssociatedClub;
import xyz.pierini.fantacombinator.model.output.ClubOutput;
import xyz.pierini.fantacombinator.model.output.OutputWrapper;

@Service
public class TripletService {
	
	private Map<String, List<Integer>> bigMatchesByClub = null;
	
	//private Map<Integer, List<String>> noBigMatchesByDay = null;

	public List<List<String>> calculateTriplets(OutputWrapper mergedOutput, CombinatorWrapper input, List<String> bigClubs) {
		/*
		 * calcolo le possibile triplette solo per chi non ha modo di non affrontare le big in due club
		 * escludo le big, altrimenti ha poco senso...
		 * uso i set per evitare duplicati
		 */
		List<String> needTriplets = whoNeedTriplets(mergedOutput, bigClubs);
		if (needTriplets.size() == 0) {
			return null;
		}
		getTripletsMaps(needTriplets, input, bigClubs);
		return getTriplets(needTriplets, bigClubs);
	}

	private List<List<String>> getTriplets(List<String> needTriplets, List<String> bigClubs) {
		Set<List<String>> rs = new HashSet<>();
		for (String club : needTriplets) {
			List<Integer> bigDaysForThisClub = this.bigMatchesByClub.get(club);
			for (Map.Entry<String, List<Integer>> entry : this.bigMatchesByClub.entrySet()) {
				if (!entry.getKey().equals(club)) {
					String secondClub = entry.getKey();
					List<Integer> bigDaysForSecondClub = entry.getValue();
					List<Integer> bigDaysDiff = new ArrayList<>(bigDaysForThisClub);
					bigDaysDiff.retainAll(bigDaysForSecondClub);
					
					if (bigDaysDiff.size() == 0) {
						// bah, mi pare strano perchè dovrei averle escluse prima, però si sa mai...
						/*Set<String> singleTriplet = new HashSet<>();
						singleTriplet.add(club);
						singleTriplet.add(secondClub);
						rs.add(singleTriplet);*/
					} else {
						// cerco il terzo
						for (Map.Entry<String, List<Integer>> entry3 : this.bigMatchesByClub.entrySet()) {
							if (!entry3.getKey().equals(club) && !entry3.getKey().equals(secondClub)) {
								String thirdClub = entry3.getKey();
								List<Integer> bigDaysForThirdClub = entry3.getValue();
								
								List<Integer> bigDaysIntermediateDiff1 = new ArrayList<>(bigDaysForThisClub);
								bigDaysIntermediateDiff1.retainAll(bigDaysForThirdClub);
								List<Integer> bigDaysIntermediateDiff2 = new ArrayList<>(bigDaysForSecondClub);
								bigDaysIntermediateDiff2.retainAll(bigDaysForThirdClub);
								
								if (bigDaysIntermediateDiff1.size() != 0 && bigDaysIntermediateDiff2.size() != 0) {
									List<Integer> bigDaysFinalDiff = new ArrayList<>(bigDaysForThisClub);
									bigDaysFinalDiff.retainAll(bigDaysForSecondClub);
									bigDaysFinalDiff.retainAll(bigDaysForThirdClub);
									if (	
											bigDaysFinalDiff.size() == 0 &&
											!bigClubs.contains(secondClub) &&
											!bigClubs.contains(thirdClub)
											) {
										List<String> singleTriplet = new ArrayList<>();
										singleTriplet.add(club);
										singleTriplet.add(secondClub);
										singleTriplet.add(thirdClub);
										Collections.sort(singleTriplet);
										rs.add(singleTriplet);
									}
								}
							}
						}
					}
				}
			}
		}
		return new ArrayList<>(rs).stream().sorted((o1, o2) -> {
			if (o1.size() != 3 || o2.size() != 3) {
				// there's something wrong here...
				return 0;
			}
			if (o1.get(0).equals(o2.get(0))) {
				if (o1.get(1).equals(o2.get(1))) {
					return o1.get(2).compareTo(o2.get(2));
				} else {
					return o1.get(1).compareTo(o2.get(1));
				}
			} else {
				return o1.get(0).compareTo(o2.get(0));
			}
		}).collect(Collectors.toList());
	}

	private void getTripletsMaps(List<String> needTriplets, CombinatorWrapper input,
			List<String> bigClubs) {
		Map<String, List<Integer>> bigMatchesByClub = new HashMap<>();
		//Map<Integer, List<String>> noBigMatchesByDay = new HashMap<>();
		for (Day d : input.getDays()) {
			for (Match m : d.getMatches()) {
				if (bigClubs.contains(m.getHome())) {
//					if (needTriplets.contains(m.getAway())) {
//					if (!bigClubs.contains(m.getAway())) {
						bigMatchesByClub.put(m.getAway(), updateBigDaysByName(bigMatchesByClub, d.getNumber(), m.getAway()));
//					}
				}/* else {
					noBigMatchesByDay.put(d.getNumber(), updateNoBigMatchesClubs(noBigMatchesByDay, d.getNumber(), m.getAway()));
				}*/
				
				if (bigClubs.contains(m.getAway())) {
//					if (needTriplets.contains(m.getHome())) {
//					if (!bigClubs.contains(m.getHome())) {
						bigMatchesByClub.put(m.getHome(), updateBigDaysByName(bigMatchesByClub, d.getNumber(), m.getHome()));
//					}
				}/* else {
					noBigMatchesByDay.put(d.getNumber(), updateNoBigMatchesClubs(noBigMatchesByDay, d.getNumber(), m.getHome()));
				}*/
			}
		}
		this.bigMatchesByClub = bigMatchesByClub;
		//this.noBigMatchesByDay = noBigMatchesByDay;
	}

	/*private List<String> updateNoBigMatchesClubs(Map<Integer, List<String>> noBigMatchesByDay, Integer dayNumber, String name) {
		List<String> noBigMatchesClubs = null;
		if (noBigMatchesByDay.containsKey(dayNumber)) {
			noBigMatchesClubs = noBigMatchesByDay.get(dayNumber);
			noBigMatchesClubs.add(name);
		} else {
			noBigMatchesClubs = new ArrayList<>(Arrays.asList(name));
		}
		return noBigMatchesClubs;
	}*/

	private List<Integer> updateBigDaysByName(Map<String, List<Integer>> rs, Integer dayNumber, String name) {
		List<Integer> days = null;
		if (rs.containsKey(name)) {
			days = rs.get(name);
			days.add(dayNumber);
		} else {
			days = new ArrayList<>(Arrays.asList(dayNumber));
		}
		return days;
	}

	private List<String> whoNeedTriplets(OutputWrapper mergedOutput, List<String> bigClubs) {
		List<String> rs = new ArrayList<>();
		for (ClubOutput co : mergedOutput.getClubs()) {
			if (!bigClubs.contains(co.getName())) {
				// servirebbero dei check, ma tant'è...
				AssociatedClub ac = co.getBestClubs().get(0);
				if (ac.getAssociatedValue() > 0) {
					rs.add(co.getName());
				} else {
					// verifico tra gli 0 che non si tratti di big...
					boolean nonBigWith0Matches = false;
					for (AssociatedClub c : co.getBestClubs()) {
						if (c.getAssociatedValue() == 0 && !bigClubs.contains(c.getName())) {
							nonBigWith0Matches = true;
						}
					}
					if (!nonBigWith0Matches) {
						rs.add(co.getName());
					}
				}
			}
		}
		return rs;
	}
	
}
