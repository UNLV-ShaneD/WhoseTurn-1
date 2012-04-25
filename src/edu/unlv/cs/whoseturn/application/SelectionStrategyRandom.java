package edu.unlv.cs.whoseturn.application;

import java.util.LinkedList;
import java.util.List;

import edu.unlv.cs.whoseturn.domain.Category;
import edu.unlv.cs.whoseturn.domain.User;

public class SelectionStrategyRandom extends SelectionStrategy {

	@Override
	public List<User> findActiveParticipants(List<User> users,
			int quantity, Category category) {
		List<User> activeParticipants = new LinkedList<User>();
		activeParticipants.add(chooseRandomUser(users));
		return activeParticipants;
	}

}
