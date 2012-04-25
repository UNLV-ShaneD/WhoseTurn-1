package edu.unlv.cs.whoseturn.application;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.jdo.PersistenceManager;

import com.google.appengine.api.datastore.KeyFactory;

import edu.unlv.cs.whoseturn.domain.Category;
import edu.unlv.cs.whoseturn.domain.PMF;
import edu.unlv.cs.whoseturn.domain.Turn;
import edu.unlv.cs.whoseturn.domain.TurnItem;
import edu.unlv.cs.whoseturn.domain.User;

public class SelectionStrategyLeastRecentlyGone extends SelectionStrategy {

	@Override
	public List<User> findActiveParticipants(List<User> users,
			int quantity, Category category) {

		List<String> turnItemsKeyStrings;							// Temporary storage of a user's turnitems keystrings
		List<TurnItem> turnItems;									// Temporary storage of a user's turnitem objects
		List<Long> millisecondsList = new ArrayList<Long>();		// Storage of the milliseconds of the difference between now and the user's last turn
		List<User> activeParticipants = new LinkedList<User>();
		
		/**
		 * Persistence manager
		 */
		PersistenceManager pm = PMF.get().getPersistenceManager();
		
		Turn tempTurn;				// Temporary storage of the turn retrieved
		Date today = new Date();	// Stores today's date
		Long tempMilliSeconds;		// Temporary storage of difference between now and the user's last turn

		/**
		 * Generates the user's difference between now and the last turn
		 * and stores it into the millisecondsList
		 */
		for (int i = 0; i < users.size(); i++) {
			turnItemsKeyStrings = new ArrayList<String>(users.get(i).getTurnItems());	// Gets the user's list of turnitems keystrings
			turnItems = new ArrayList<TurnItem>();										// Startsa  new list for turnitems
			
			/**
			 * Gets the user's turnItems
			 */
			for (int j = 0; j < turnItemsKeyStrings.size(); j++) {
				turnItems.add(pm.getObjectById(TurnItem.class, KeyFactory
						.stringToKey(turnItemsKeyStrings.get(j))));
			}
			
			tempMilliSeconds = 9223372036854775807L;	// Stores the maxiumum long value as the default milliseconds of last turn
			
			/**
			 * Finds the last date that the user attended in this category
			 * and stores the milliseconds between today and then.
			 */
			for (int k = 0; k < turnItems.size(); k++) {
				
				/**
				 * Check if the turn is part of the specified category.
				 */
				if (turnItems.get(k).getCategoryKeyString().equals(category.getKeyString())) {
					tempTurn = pm.getObjectById(Turn.class, KeyFactory									// Stores the turn from this turnitem
							.stringToKey(turnItems.get(k).getTurnKeyString()));
					
					/**
					 * Check if this turn's date is closer than the current turn.
					 * If so, store this new milliseconds difference.
					 */
					if((today.getTime() - tempTurn.getTurnDateTime().getTime()) < tempMilliSeconds) {
						tempMilliSeconds = today.getTime() - tempTurn.getTurnDateTime().getTime();
					}
				}
			}
			
			/**
			 * Adds the calculated milliseconds to the list.
			 */
			millisecondsList.add(tempMilliSeconds);
		}

		/**
		 * Closes the persistence manager.
		 */
		pm.close();

		/**
		 * Finds the highest value in the milliseconds list.
		 */
		Integer index = 0;											// Set the index inlitally to the first user in the list
		Long tempCurrentMilliSeconds = millisecondsList.get(0);		// Set the currentMilliseconds to the first user in the list
		Integer sameCounter = 0;									// Set the samecounter to 0
		
		/**
		 * Loops through the milliseconds list to find the highest value.
		 */
		for (int i = 1; i < millisecondsList.size(); i++) {
			tempMilliSeconds = millisecondsList.get(i);			// Stores the user's milliseconds for comparison

			/**
			 * Check if this user's milliseconds is more than the current user's
			 * If so, store this as the new highest found.
			 */
			if (tempMilliSeconds > tempCurrentMilliSeconds) {
				tempCurrentMilliSeconds = tempMilliSeconds;
				index = i;
			}
			
			/**
			 * Checks to see if everyone has the same milliseconds.
			 * If so, increase the sameCounter.
			 */
			if (tempMilliSeconds.equals(tempCurrentMilliSeconds)) {
				sameCounter++;
			}
		}
		
		/**
		 * If all users have the same milliseconds, choose a random user instead.
		 */
		if (sameCounter.equals(millisecondsList.size())) {
			activeParticipants.add(chooseRandomUser(users));
			return activeParticipants;
		}

		/**
		 * Return the user who is chosen.
		 */
		activeParticipants.add(users.get(index));
		return activeParticipants;
	}

}
