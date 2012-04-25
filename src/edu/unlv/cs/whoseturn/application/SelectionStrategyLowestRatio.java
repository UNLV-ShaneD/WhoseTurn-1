package edu.unlv.cs.whoseturn.application;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.jdo.PersistenceManager;

import com.google.appengine.api.datastore.KeyFactory;

import edu.unlv.cs.whoseturn.domain.Category;
import edu.unlv.cs.whoseturn.domain.PMF;
import edu.unlv.cs.whoseturn.domain.TurnItem;
import edu.unlv.cs.whoseturn.domain.User;

public class SelectionStrategyLowestRatio extends SelectionStrategy {

	/**
	 * TODO: Account for quantity
	 */
	@Override
	public List<User> findActiveParticipants(List<User> users, int quantity, Category category) {
		List<Double> ratioList = new ArrayList<Double>();	// Ratio list to store the ratio of the users in the turn
		List<String> turnItemsKeyStrings;					// Temporary storage of a user's turnitems keystrings
		List<TurnItem> turnItems;							// Temporary storage of a user's turnitem objects
		Double tempTurnCount;								// Temporary storage of a user's total times gone in the category
		Double tempSelectedCount;							// Temporary storage of a user's selected times in the category
		String turnItemKeyString;							// Temporary storage of a user's turnitem keystring
		List<User> activeParticipants = new LinkedList<User>();
		
		/**
		 * Persistence manager
		 */
		PersistenceManager pm = PMF.get().getPersistenceManager();

		
		/**
		 * Generates the user's ratio in the specified category and stores it.
		 */
		for (int i = 0; i < users.size(); i++) {
			turnItems = new ArrayList<TurnItem>();			// Resets the temporary turnitems list
			tempTurnCount = 0.0;							// Resets the turn count
			tempSelectedCount = 0.0;						// Resets the selected count
			turnItemsKeyStrings = new ArrayList<String>(users.get(i).getTurnItems());	// Sets the keystrings to the list retrieved from the user's object

			/**
			 * Gets all the turnitem objects for the current user.
			 */
			for (int j = 0; j < turnItemsKeyStrings.size(); j++) {
				turnItemKeyString = turnItemsKeyStrings.get(j);				// Gets the keystring for the current turnitem to retrieve
				turnItems.add(pm.getObjectById(TurnItem.class, KeyFactory	// Adds the turnitem's object in the turnItems list
						.stringToKey(turnItemKeyString)));
			}

			/**
			 * Generates a ratio for the user based off how many times 
			 * they've gone compared to the number of times they've driven.
			 */
			for (int k = 0; k < turnItems.size(); k++) {
				
				/**
				 * Checks if the turnitem is in the current category.
				 * If so, add it to the ratio calculation.
				 */
				if (turnItems.get(k).getCategoryKeyString()
						.equals(category.getKeyString())) {
					tempTurnCount++;
					
					/**
					 * Check if the user drove for this turnitem.
					 * If so, increase the selected count.
					 */
					if (turnItems.get(k).getSelected()) {
						tempSelectedCount++;
					}
				}
			}
			
			/**
			 * Add the ratio to the ratio list.
			 */
			ratioList.add(tempSelectedCount / tempTurnCount);
		}

		/**
		 * Calculate who is to drive based off the lowest ratio.
		 */
		Integer index = 0;								// Index of the currently chosen user
		Double tempCurrentRatio = ratioList.get(0);		// Sets the first ratio to the first user in the list
		Double tempRatio;								// Storage of a user's ratio to compare to the currentratio
		Integer sameCounter = 0;						// Counter to check if all the users in the turn have the same ratio.

		/**
		 * Calculate what user in the turn has the lowest ratio.
		 */
		for (int i = 1; i < ratioList.size(); i++) {
			tempRatio = ratioList.get(i);				// Gets the ratio of the user from the ratioList

			/**
			 * Checks if the ratio of this user is lower than the current lowest user.
			 * If so, set the currentRatio to this user's ratio, and set the index to this user.
			 */
			if (tempRatio < tempCurrentRatio) {
				tempCurrentRatio = tempRatio;
				index = i;
			}
			
			/**
			 * Increases the counter if the current user has the same ratio as the first user
			 */
			if (tempRatio.equals(tempCurrentRatio)) {
				sameCounter++;
			}
		}

		/**
		 * If all the users have the same ratio, return a random user.
		 */
		if (sameCounter.equals(users.size())) {
			activeParticipants.add(chooseRandomUser(users));
			return activeParticipants;
		}
		
		/**
		 * Return the user based off the index found
		 */
		activeParticipants.add(users.get(index));
		return activeParticipants;
	}

}
