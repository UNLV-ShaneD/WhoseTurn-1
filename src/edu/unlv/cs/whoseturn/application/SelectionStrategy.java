package edu.unlv.cs.whoseturn.application;

import java.util.List;
import java.util.Random;

import edu.unlv.cs.whoseturn.domain.Category;
import edu.unlv.cs.whoseturn.domain.User;

public abstract class SelectionStrategy {
	public abstract List<User> findActiveParticipants(List<User> participants,
			int quantity, Category category);

	/**
	 * Algorithm which chooses a user based explicitly on the predefined random
	 * generator The Random object will use the nextInt() method to generate an
	 * integer value, which given a parameter of the the number of users will
	 * choose a number in such 0 to n-1 range
	 * 
	 * @param users
	 *            The list of usernames.
	 * @return a User at the arbitrarily generated index, which then will be
	 *         used to access a string representing the user name
	 */
	public User chooseRandomUser(List<User> users) {

		Random generator = new Random(); // Creates a new random generator
		int randomIndex = generator.nextInt(users.size()); // Gets a random
															// index value with
															// a max of the size
															// of the users

		/**
		 * Return the random user.
		 */
		return users.get(randomIndex);
	}
}
