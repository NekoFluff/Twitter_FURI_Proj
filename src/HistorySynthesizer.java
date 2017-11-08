import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.URLEntity;
import twitter4j.User;

public class HistorySynthesizer {

	private TwitterHandler twitterHandler;

	public HistorySynthesizer(TwitterHandler handler) {
		this.twitterHandler = handler;
	}
	
	ArrayList<String> users = new ArrayList<String>();
	
	public void createSyntheticHistoryForRandomUsers() {
		TwitterStream twitterStream = TwitterStreamFactory.getSingleton();

		twitterStream.addListener(new StatusListener () {
			public void onStatus(Status status) {
				System.out.println(status.getUser().getScreenName()); // print tweet text to console
				System.out.println(status.getText()); // print tweet text to console
				users.add(status.getUser().getScreenName());

				if (users.size() == 1100) 
					twitterStream.shutdown();
			}

			@Override
			public void onException(Exception arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onDeletionNotice(StatusDeletionNotice arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onScrubGeo(long arg0, long arg1) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTrackLimitationNotice(int arg0) {
				// TODO Auto-generated method stub
				HelperFunctions.waitSec(10);
			}
		});

		FilterQuery tweetFilterQuery = new FilterQuery(); // See 

		tweetFilterQuery.language(new String[]{"en"}); // Note that language does not work properly on Norwegian tweets 


		twitterStream.sample();
		int currentUserIndex = 0;

		while(currentUserIndex < 1000) {
			HelperFunctions.waitSec(10);

			if (currentUserIndex < users.size()) {
				this.createSyntheticHistoryForUser(users.get(currentUserIndex));
				currentUserIndex++;
			}
		}
	}
	
	
	public void createSyntheticHistoryForUser(String username) {

		//Initialize variables
		twitterHandler.reset();

		ArrayList<URLEntity> randomFriendLinks = new ArrayList<URLEntity>();
		ArrayList<String> selectedFriends = new ArrayList<String>();
		ArrayList<URLEntity> randomFriendOfFriendLinks = new ArrayList<URLEntity>();
		ArrayList<String> selectedFriendsOfFriends = new ArrayList<String>();
		
		int randomLinksCount = 100;
		int randomFriendOfFriendLinksCount = 20;
		try {
			//-------------------PART 1: Friend Links-------------------//
			ArrayList<User> listFriends = twitterHandler.getFriendsForUser(username, false);
			randomFriendLinks = twitterHandler.getRandomFriendLinks(listFriends, selectedFriends, randomLinksCount);


			//-------------------PART 2: Friend-of-Friend Links-------------------//
			randomFriendOfFriendLinks = twitterHandler.getRandomFriendOfFriendLinks(listFriends, selectedFriendsOfFriends, randomFriendOfFriendLinksCount);
			
			
			//-------------------PART 3: Combine Links to create Synthetic History-------------------//
			
			//Mix the friend and friend-of-friend URLs to create a collection of synthetic histories for each user with various sizes and link compositions.
			
			//Make several synthetic histories for one user
			for (int i = 0; i < 3; i++) {
				int size = 50;
				
				switch(i) {
				case 0:
					size = 30;
					break;
				case 1:
					size = 50;
					break;
				case 2:
					size = 100;
					break;
				default: 
					size = 30;
					break;
				}
				
				/*
				ArrayList<URLEntity> history = new ArrayList<URLEntity>();
				
				ArrayList<URLEntity> randomFriendLinksCopy = (ArrayList<URLEntity>) randomFriendLinks.clone();
				ArrayList<URLEntity> randomFriendOfFriendLinksCopy = (ArrayList<URLEntity>) randomFriendOfFriendLinks.clone();
				
				//Add links
				for (int j = 0; j < size; j++) {
					
					
					int chance = randomIntBetween(1,10);
					if (chance <= 8) { //80% chance to take a link from the random friend links array
						if (randomFriendLinksCopy.size() == 0) continue; //TODO: Correct duplicate handling
						int randomInt = randomIntBetween(0, randomFriendLinksCopy.size()-1);
						
						history.add(randomFriendLinksCopy.get(randomInt));
						randomFriendLinksCopy.remove(randomInt);
						
					} else { //20% chance to take a link from the random friend-of-friend links array
						if (randomFriendOfFriendLinksCopy.size() == 0) continue; //TODO: Correct duplicate handling
						int randomInt = randomIntBetween(0,randomFriendOfFriendLinksCopy.size()-1);
						
						history.add(randomFriendOfFriendLinksCopy.get(randomInt));
						randomFriendOfFriendLinksCopy.remove(randomInt);
					} 
				}
				
				
				System.out.println("--------------------------------------------SYNTHETIC HISTORY:--------------------------------------------");
				for (URLEntity url : history) {
					System.out.println(url.getURL());
				}
				
				//Write out the history to a file.
				writeHistoryToFile(history, username+"_"+history.size()+".txt");
				*/
				
				
				JSONObject syntheticHistory = new JSONObject();
				JSONArray friends = new JSONArray();
				
				for (User friend : listFriends) {
					friends.add(friend.getScreenName());
				}
				syntheticHistory.put("friends", friends);
				syntheticHistory.put("numFriends", friends.size());
				
				JSONArray links = new JSONArray();
				
				ArrayList<URLEntity> randomFriendLinksCopy = (ArrayList<URLEntity>) randomFriendLinks.clone();
				ArrayList<String> selectedFriendsCopy = (ArrayList<String>) selectedFriends.clone();
				ArrayList<URLEntity> randomFriendOfFriendLinksCopy = (ArrayList<URLEntity>) randomFriendOfFriendLinks.clone();
				ArrayList<String> selectedFriendsOfFriendsCopy = (ArrayList<String>) selectedFriendsOfFriends.clone();
				
				//Add links
				for (int j = 0; j < size; j++) {
					
					
					int chance = HelperFunctions.randomIntBetween(1,10);
					if (chance <= 8) { //80% chance to take a link from the random friend links array
						if (randomFriendLinksCopy.size() == 0) continue; //TODO: Correct duplicate handling
						int randomInt = HelperFunctions.randomIntBetween(0, randomFriendLinksCopy.size()-1);
						
						JSONObject linkPair = new JSONObject();
						linkPair.put("link", randomFriendLinksCopy.get(randomInt).getURL());
						linkPair.put("friend", selectedFriendsCopy.get(randomInt));
						linkPair.put("type", "friend");
						links.add(linkPair);
						
						randomFriendLinksCopy.remove(randomInt);
						selectedFriendsCopy.remove(randomInt);
						
					} else { //20% chance to take a link from the random friend-of-friend links array
						if (randomFriendOfFriendLinksCopy.size() == 0) continue; //TODO: Correct duplicate handling
						int randomInt = HelperFunctions.randomIntBetween(0,randomFriendOfFriendLinksCopy.size()-1);
						
						JSONObject linkPair = new JSONObject();
						linkPair.put("link", randomFriendOfFriendLinksCopy.get(randomInt).getURL());
						linkPair.put("friend", selectedFriendsOfFriendsCopy.get(randomInt));
						linkPair.put("type", "friend-of-friend");
						links.add(linkPair);
						
						randomFriendOfFriendLinksCopy.remove(randomInt);
						selectedFriendsOfFriendsCopy.remove(randomInt);
					} 
				}
				
				syntheticHistory.put("url-links", links);
				
				System.out.println("--------------------------------------------SYNTHETIC HISTORY:--------------------------------------------");
				for (int z = 0; z < links.size(); z++) {
					System.out.println(((JSONObject) links.get(z)).get("link"));
				}
				
				HelperFunctions.writeOutJsonObject(syntheticHistory, username+"_"+links.size()+".txt", "\\synthetic_histories\\");
			}
			
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
			System.out.println("--------------------------------------------ERROR OCCURED WHEN CREATIN SYNTHETIC HISTORYv --------------------------------------------");
			System.out.println("--------------------------------------------Collected Friend Links:--------------------------------------------");
			for (URLEntity url : randomFriendLinks) {
				System.out.println(url.getURL());
			}
			
			System.out.println("--------------------------------------------Collected Friend-of-Friend Links:--------------------------------------------");
			for (URLEntity url : randomFriendOfFriendLinks) {
				System.out.println(url.getURL());
			}
			
			//System.out.println("Will retry in 10 seconds...");
			//waitSec(10);
			//createSyntheticHistoryForUser(username);
		}
	}
	
	/**
	 * DEPRECIATED. 
	 * Helper function used to write out a synthetic history to a file.
	 * @param history ArrayList of URLEntity. (URLs)
	 * @param filename Name of txt file
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void writeHistoryToFile(ArrayList<URLEntity> history, String filename) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		File file = new File("\\synthetic_histories\\");
		if (!file.exists()) {
			file.mkdir();
			System.out.println("Created new synthetic_histories folder");
		}
		
		//try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
		try (Writer writer = new BufferedWriter(new FileWriter(new File("\\synthetic_histories\\"+filename)))) {
			//-------------------- Print synthetic history		
			writer.write("--------------------------------------------SYNTHETIC HISTORY:--------------------------------------------\n");
			writer.write("Size: " + history.size()+"\n\n");
			for (URLEntity url : history) {
				writer.write(url.getURL() + "\n");
			}
		}
		
	}
	

}
