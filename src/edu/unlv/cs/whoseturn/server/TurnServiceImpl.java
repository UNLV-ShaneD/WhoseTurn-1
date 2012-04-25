package edu.unlv.cs.whoseturn.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.unlv.cs.whoseturn.client.TurnService;
import edu.unlv.cs.whoseturn.domain.Category;
import edu.unlv.cs.whoseturn.domain.PMF;
import edu.unlv.cs.whoseturn.domain.Strategy;
import edu.unlv.cs.whoseturn.domain.Turn;
import edu.unlv.cs.whoseturn.domain.TurnItem;
import edu.unlv.cs.whoseturn.domain.User;

/**
 * Category Service which allows the client to get information from the server
 * regarding categories.
 */
@SuppressWarnings("serial")
public class TurnServiceImpl extends RemoteServiceServlet implements
		TurnService {

	/**
	 * Alternative (better) form of findDriver
	 * Adapts to the inferior form of findDriver which uses print-friendly strings
	 * @param users
	 * @param category
	 * @return
	 */
	public List<User> findDriver(List<User> users, Category category) {
		List<String> usernames = new LinkedList<String>();
		List<User> usersSelected; // = new LinkedList<User>();
		
		/*
		for (User user : users) {
			usernames.add(user.getUsername());
		}
		
		String categoryName = category.getName();
		
		List<String> results = findDriver(usernames, categoryName);
		
		usersSelected = getUserObjects(results);
		*/
		

		// TODO: METHOD STUB. Does not actually calculate a valid selected user.
		usersSelected = new LinkedList<User>();
		if (users.size() > 0) {
			usersSelected.add(users.get(0));
		}
		
		return usersSelected;
	}
	
	/**
	 * findDriver is called from another function located in a service call on
	 * the client layer.
	 * 
	 * @param usernames The list of usernames in the turn.
	 * @param category	The category name of the turn.
	 * @return a String which will represent the selected driver of Whose Turn
	 */
	public List<String> findDriver(List<String> usernames, String categoryName) {
		User driver;

		/**
		 * Persistence manager
		 */
		PersistenceManager pm = PMF.get().getPersistenceManager();

		/**
		 * Add the logged in user's username to the list.
		 */
		usernames = addLoggedUser(usernames);
		
		/**
		 * Get the user objects based off the usernames provided.
		 */
		List<User> userObjects = getUserObjects(usernames);
		
		/**
		 * Get the category based off the category name provided.
		 */
		Category category = getCategoryObject(categoryName);

		/**
		 * Get the strategy from the category.
		 */
		String strategyKeyString = category.getStrategyKeyString();			// The keystring of the strategy for the category
		Key strategyKey = KeyFactory.stringToKey(strategyKeyString);		// The key of the strategy
		Strategy strategy = (Strategy) pm.getObjectById(Strategy.class,		// The strategy object
				strategyKey);

		/**
		 * Close the persistence manager
		 */
		pm.close();

		/**
		 * Depending on the strategy from the category, execute the respective
		 * strategy.
		 * 
		 * Case 1: Least recently gone
		 * Case 2: Lowest ratio
		 * Case 3: Random user
		 */
		switch (strategy.getStrategyId()) {
		case 1:
			driver = leastRecentlyGone(userObjects, category);
			break;
		case 2:
			driver = lowestRatio(userObjects, category);
			break;
		case 3:
			driver = chooseRandomUser(userObjects);
			break;
		default:
			// TODO Error message if an invalid strategy ID was found
			driver = new User();
			driver.setUsername("UnknownDriver");
		}

		/**
		 * Persistence manager
		 */
		pm = PMF.get().getPersistenceManager();
		
		/**
		 * Generate a turn for this request to be persisted.
		 */
		Turn turn = new Turn();									// The turn object for this turn
		TurnItem tempTurnItem;									// Temporary turnitem to be used for persistence for each user
		List<User> userList = new ArrayList<User>();	// The list of the user objects from the database for modification
		User tempUser;				// Temporary user to calculate a turn item for
		String tempUserKeyString;								// Keystring for the user to be used for turnitem addition
		Key tempUserKey;										// Key of the user to be usd for turnitem addition
		turn.setCategoryKeyString(category.getKeyString());		// Sets the turn's category
		turn.setTurnDateTime(new Date());						// Sets the turn's datetime to now
		turn.setTurnItems(new HashSet<String>());				// Prepares the turn for a set of turn items
		
		/**
		 * The user chose no one for the turn and therefore the turn must be deleted.
		 */
		if (usernames.size() == 1) {
			turn.setDeleted(true);
		} else {
			turn.setDeleted(false);
		}
		
		/**
		 * Persist the turn.
		 */
		turn = pm.makePersistent(turn);
		String turnKeyString = turn.getKeyString();
		pm.close();
		
		
		pm = PMF.get().getPersistenceManager();
		turn = new Turn();
		turn = pm.getObjectById(Turn.class, KeyFactory.stringToKey(turnKeyString));

		/**
		 * Generate turn items for reach user and persist those for each user,
		 * adding it to their TurnItem lists.
		 */
		for (int i = 0; i < userObjects.size(); i++) {
			
			/**
			 * Generate a turnitem for the current user in the list.
			 */
			tempTurnItem = new TurnItem();											// Generate a new temporary turnitem for the current user in the list
			tempTurnItem.setCategoryKeyString(category.getKeyString());				// Set the category of the turnitem to the turn's category
			tempTurnItem.setDeleted(false);											// Set the deleted flag to false
			tempTurnItem.setOwnerKeyString(userObjects.get(i).getKeyString());		// Set the owner of the turnitem to the current user in the list
			
			/**
			 * If the chosen driver equals this current user in the list,
			 * set them as being selected.
			 */
			if (driver.getUsername().equals(userObjects.get(i).getUsername())) {
				tempTurnItem.setSelected(true);
			} else {
				tempTurnItem.setSelected(false);
			}
			
			tempTurnItem.setTurnKeyString(turn.getKeyString());		// Set the turnkeystring to the persisted turn's
			tempTurnItem.setVote(0);								// Default the vote flag to 0 (not verified)
			
			/**
			 * The user chose no one for the turn and therefore the turnitem must also be deleted.
			 */
			if (usernames.size() == 1) {
				tempTurnItem.setDeleted(true);
			} else {
				tempTurnItem.setDeleted(false);
			}
			
			tempTurnItem = pm.makePersistent(tempTurnItem);		// Persist the turnitem for the current user
			turn.addTurnItem(tempTurnItem);						// Add the turnitem to the turn
			
			/**
			 * Add the turnitem to the user's turnitem list.
			 * Users need be retrieved again to allow the persistance 
			 * manager to keep track of the modification to the new objects.
			 */
			tempUser = new User();									// Creates a temporary user
			tempUserKeyString = userObjects.get(i).getKeyString();								// Gets the user's keystring out of the userObject list
			tempUserKey = KeyFactory.stringToKey(tempUserKeyString);							// Generate and store the key from the keystring
			tempUser = pm.getObjectById(User.class, tempUserKey);	// Get the user from the database
			tempUser.addTurnItem(tempTurnItem);													// Add the generated turnitem to the user's list
			userList.add(tempUser);																// Store the user into a list for update
		}
		
		
		/**
		 * Close the persistance manager, persist changes, and return the found username and keystring
		 */
		
		pm.close();
		
		List<String> returnList = new ArrayList<String>();
		returnList.add(driver.getUsername());
		returnList.add(turn.getKeyString());
		return returnList;
	}

	/**
	 * Algorithm which chooses a user based explicitly on the amount of times
	 * they were selected/turnItems; the user with the least ratio will be
	 * chosen to handle all driving responsibilities
	 * 
	 * @param users
	 *            list of users, as well as a category are passed into the
	 *            lowestRatio method to handle and retrieve the amount of times
	 *            the user participated in such a category.
	 * @param category
	 *            will represent a drive, chips & salsa, or ice cream
	 * @return User, which then will be used to access the a string
	 *         representing the user name
	 */
	public User lowestRatio(
			List<User> users, Category category) {
	}

	/**
	 * Algorithm which chooses a user based explicitly on turnDateTime from
	 * Turn.java, the user which has the oldest date will be selected to drive.
	 * Elementary comparisons between a default currentTurnDate and a tempTurnDate,
	 * will be used to determine which of the users has the earliest of Dates once
	 * the for loop terminates
	 * 
	 * @param users
	 *            list of users, as well as a category are passed into the
	 *            leastRecentlyGone method to handle and retrieve the amount of
	 *            times the user participated in such a category.
	 * @param category
	 *            will represent a drive, chips & salsa, or ice cream
	 * @return a User, which then will be used to access the a string
	 *         representing the user name
	 */
	public User leastRecentlyGone(
			List<User> users, Category category) {
	}

	@SuppressWarnings("unchecked")
	public List<User> getUserObjects(
			List<String> usernames) {
		
		/**
		 * Persistence manager
		 */
		PersistenceManager pm = PMF.get().getPersistenceManager();

		/**
		 * Get the user objects of the users in the username list
		 */
		Query userQuery = pm.newQuery(User.class,
				"username == usernameParam");
		userQuery.declareParameters("String usernameParam");

		List<User> userList = new ArrayList<User>();
		List<User> tempUserList = new ArrayList<User>();

		for (int i = 0; i < usernames.size(); i++) {
			tempUserList = (List<User>) userQuery
					.execute(usernames.get(i));
			userList.add(tempUserList.get(0));
		}
		userList.size();
		userQuery.closeAll();
		pm.close();
		return userList;
	}

	/**
	 * Gets the object of the category based off the categoryName.
	 * @param categoryName	Name of the category.
	 * @return	Returns the category object.
	 */
	@SuppressWarnings("unchecked")
	public Category getCategoryObject(String categoryName) {
		
		/**
		 * Persistence manager
		 */
		PersistenceManager pm = PMF.get().getPersistenceManager();

		/**
		 * Get the category object
		 */
		Query categoryQuery = pm.newQuery(Category.class,					// Generates a query for the category based off the categoryNameParam
				"name == categoryNameParam");
		categoryQuery.declareParameters("String categoryNameParam");		// Adds the parameter to the query

		List<Category> categoryList = (List<Category>) categoryQuery		// Gets the list of categories that match the query (Should only be one)
				.execute(categoryName);
		Category category = categoryList.get(0);							// Stores the category object

		/**
		 * Close the persistence manager.
		 */
		categoryQuery.closeAll();
		pm.close();
		
		/**
		 * Return the category object.
		 */
		return category;
	}

	/**
	 * Adds the currently logged in user to the list.
	 * 
	 * @param usernames Usernames of the users currently in the turn.
	 * @return	Returns the updated username list.
	 */
	@SuppressWarnings("unchecked")
	public List<String> addLoggedUser(List<String> usernames) {
		/**
		 * User auth service.
		 */
		UserService userService = UserServiceFactory.getUserService();
		
		/**
		 * Logged in user.
		 */
		com.google.appengine.api.users.User user = userService.getCurrentUser();

		/**
		 * Persistence manager
		 */
		PersistenceManager pm = PMF.get().getPersistenceManager();
		
		/**
		 * Finds the user's username who is logged in based off their OpenID email.
		 */
		Query loggedUserQuery = pm.newQuery(										// Generates a query for users whose email is emailParam
				User.class, "email == emailParam");
		loggedUserQuery.declareParameters("String emailParam");						// Adds the paramter to the query
		
		// Get a list of users who meet the query. (Should only be one)
		List<User> loggedUserList = (List<User>) loggedUserQuery
				.execute(user.getEmail());
		usernames.add(loggedUserList.get(0).getUsername());		// Add the user to the list
		usernames.size();										// Object Manager bug fix
		
		/**
		 * Closes the persistence manager.
		 */
		loggedUserQuery.closeAll();
		pm.close();
		
		/**
		 * Returns their updated username list.
		 */
		return usernames;
	}
}
