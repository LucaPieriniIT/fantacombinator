package xyz.pierini.fantacombinator.model.output;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutputWrapper {

	private List<ClubOutput> clubs;

	public String getFormattedOutput() {
		if (clubs == null) {
			return null;
		}
		String rs = "";
		for (ClubOutput c : clubs) {
			rs += c.getName() + ": ";
			String lastClubName = c.getBestClubs().get(c.getBestClubs().size() -1).getName();
			for (AssociatedClub ac : c.getBestClubs()) {
				rs += ac.getName() + "(" + ac.getAssociatedValue() + ")";
				if (!ac.getName().equals(lastClubName)) {
					rs += ", ";
				} else {
					rs += ";\n";
				}
			}
		}
		return rs;
	}

}
