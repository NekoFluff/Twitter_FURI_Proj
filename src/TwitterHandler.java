import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterHandler {

	//A list of tweets for a user 
	private Hashtable<String, ArrayList<Status>> userTweets = new Hashtable<String, ArrayList<Status>>();

	//A hashtable to store the friends of a friend
	private Hashtable<String, ArrayList<User>> friendsOfFriend = new Hashtable<String, ArrayList<User>>();

	//A set of twitter objects
	private Set<Twitter> twitterConnections;

	//-----------------------------------------------------
	
	public TwitterHandler() {
		twitterConnections = new HashSet<Twitter>();
		twitterConnections.add(TwitterFactory.getSingleton());
		initTwitterConnections("config.txt");
	}

	public void reset() {
		this.userTweets = new Hashtable<String, ArrayList<Status>>();
		this.friendsOfFriend = new Hashtable<String, ArrayList<User>>();
	}

	private void initTwitterConnections(String fileName) {
		BufferedReader br = null;
		try {
			String line = null;
			String[] lines = new String[4];
			int linesIndex = 0;
			br = new BufferedReader(new FileReader(fileName));
			
			while ((line = br.readLine()) != null) {
				if (linesIndex == 4) {
					createAndAddTiwtterConnections(lines);
					linesIndex = 0;
				}
				lines[linesIndex] = line;
				linesIndex++;
			}
			
			if (linesIndex == 4) {
				createAndAddTiwtterConnections(lines);
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) br.close();
				
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private void createAndAddTiwtterConnections(String[] lines) {
		ConfigurationBuilder twitterConfigBuilder = new ConfigurationBuilder();
		twitterConfigBuilder.setDebugEnabled(true);
		
	    for (int i = 0; i < lines.length; ++i) {
	        String[] input = lines[i].split("=");

	        if (input[0].equalsIgnoreCase("consumerkey")) {
	            twitterConfigBuilder.setOAuthConsumerKey(input[1]);
	            System.out.println("Consumer Key: " + input[1]);
	        }
	        if (input[0].equalsIgnoreCase("consumersecret")) {
	            twitterConfigBuilder.setOAuthConsumerSecret(input[1]);
	            System.out.println("Consumer Secret: " + input[1]);
	        }
	        if (input[0].equalsIgnoreCase("accesstoken")) {
	            twitterConfigBuilder.setOAuthAccessToken(input[1]);
	            System.out.println("Access Token: " + input[1]);
	        }
	        if (input[0].equalsIgnoreCase("accesstokensecret")) {
	            twitterConfigBuilder.setOAuthAccessTokenSecret(input[1]);
	            System.out.println("Access Token Secret: " + input[1]);
	        }
	    }
	    
	    Twitter twitter = new TwitterFactory(twitterConfigBuilder.build()).getInstance();
	    twitterConnections.add(twitter);
	}
	
	private Twitter getTwitterConnection(String endPoint) {
		for (Twitter tc : twitterConnections) {
		    try {
		    	Map<String, RateLimitStatus> status = tc.getRateLimitStatus();
                if (status != null) {
                    if (status.containsKey(endPoint)) {
                        if (status.get(endPoint) != null) {
                            
                            if (status.get(endPoint).getRemaining() > 1) {
                            	System.out.println("tc endpoint: " + endPoint + "\t\trate: "+status.get(endPoint).getRemaining());
                            	System.out.println("Working Consumer Key: " + tc.getConfiguration().getOAuthConsumerKey() + " with Consumer Secret: " + tc.getConfiguration().getOAuthConsumerSecret());

                                return tc;
                            }
                        }
                    }
                }
            } catch (TwitterException e) {
            	//System.out.println("Invalid Consumer Key: " + tc.getConfiguration().getOAuthConsumerKey() + " with Consumer Secret: " + tc.getConfiguration().getOAuthConsumerSecret());
                //e.printStackTrace();
            }
        }
		return null;
	}
	
	//-----------------------------------------------------
	
	public int getFriendCallsRemaining(Twitter twitter) {
		RateLimitStatus status;
		try {
			status = twitter.getRateLimitStatus().get("/friends/list");
			return status.getRemaining();
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
		
	}
	
	public int getFriendTimer(Twitter twitter) {
		try {
			RateLimitStatus status = twitter.getRateLimitStatus().get("/friends/list");
			System.out.println("-----getFriends-----\nLimit: " + status.getLimit());
			System.out.println("Remaining: " + status.getRemaining());

			System.out.println("Seconds Until Reset: " + status.getSecondsUntilReset());
			
			return status.getSecondsUntilReset();
			
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public ArrayList<UserList> getListsForUser(String username, ArrayList<UserList> _lists, long _cursor, int _repeatCount) {
		ArrayList<UserList> lists = _lists;
		
		PagableResponseList<UserList> pagableList;
		long cursor = _cursor;
		int repeatCount = _repeatCount; 
		
		do {
			//Get all the tweets for the user
			Twitter tc = getTwitterConnection("/lists/memberships");
			while (tc == null) {
				HelperFunctions.waitSec(60);
				tc = getTwitterConnection("/lists/memberships");
			}
			try {
				pagableList = tc.getUserListMemberships(username, 500, cursor);
				
				for (UserList list : pagableList) {
					lists.add(list);
					//System.out.println("id:" + list.getId() + ", name:" + list.getName() + ", description:" + list.getDescription() + ", slug:" + list.getSlug() + "");
				}
				
			} catch (TwitterException e) {
				
				//On failure, try again
				if (e.getErrorCode() == 34) {
					System.out.println("ERROR: URI Requested is invalid or the resource requested does not exist.");
					return lists;
				} else if (e.getErrorCode() == 130) {
					System.out.println("ERROR: Twitter servers are overloaded. Attempting again...");
					return this.getListsForUser(username, lists, cursor, repeatCount);
				} else {
					System.out.println("UNKNOWN ERROR OCCURED");
					e.printStackTrace();
					return lists;
				}
				
			}
			
		} while ((cursor = pagableList.getNextCursor()) != 0 && repeatCount > 0); 
		
		return lists;
	}

	public ArrayList<URLEntity> getRandomFriendLinks(ArrayList<User> listFriends, ArrayList<String> selectedFriends, int numLinks) throws TwitterException {

		//Get numLinks random friend links
		ArrayList<URLEntity> randomFriendLinks = new ArrayList<URLEntity>();

		while (randomFriendLinks.size() < numLinks && listFriends.size() != 0) {
			//Get a random friend. 
			User randomFriend = listFriends.get(HelperFunctions.randomIntBetween(0, listFriends.size()-1));

			//Get a random link from that friend
			URLEntity link = getRandomURLLinkForUser(randomFriend.getScreenName(), randomFriendLinks);
			if (link != null) {

				//Add it to the list of links
				randomFriendLinks.add(link);
				selectedFriends.add(randomFriend.getScreenName());


			} else {
				/////TODO: Currently allowing duplicates links???

				//TODO: Remove user from the list because there are no links for that user
				listFriends.remove(randomFriend);
			}


		}

		//////DEBUG TEST
		System.out.println("\n-------------------------------------- COMPILED RANDOM FRIEND LINKS--------------------------------------");
		for (int i = 0; i < randomFriendLinks.size(); i++) {
			System.out.println(selectedFriends.get(i) + " :\t\t" + randomFriendLinks.get(i).getDisplayURL());
		}
		System.out.println("\n");
		

		return randomFriendLinks;
	}
	

	public ArrayList<URLEntity> getRandomFriendOfFriendLinks(ArrayList<User> listFriends, ArrayList<String> selectedFriendsOfFriends, int numLinks) {
		
		ArrayList<URLEntity> randomFriendOfFriendLinks = new ArrayList<URLEntity>();
		if (listFriends.size() == 0)
			return randomFriendOfFriendLinks;
		
		while (randomFriendOfFriendLinks.size() < numLinks) {
			//Get a random friend
			
			User randomFriend = listFriends.get(HelperFunctions.randomIntBetween(0, listFriends.size()-1));

			//Get the friends of that random friend 
			ArrayList<User> friendsOfRandomFriend;
			if ((friendsOfRandomFriend = friendsOfFriend.get(randomFriend.getScreenName())) == null) { 
				
//				if (getFriendsCallCount >= 9) { //Fallback to ensure limited use of getFriends() call
//					User[] f = (User[]) friendsOfFriend.values().toArray(a)
//					friendsOfRandomFriend = friendsOfFriend.get(f[randomIntBetween(0,f.length)]);
//				} else {
					System.out.println("Random Friend of Friend Links: " + randomFriendOfFriendLinks.size() + " / " + numLinks);
					try {
						friendsOfRandomFriend = getFriendsForUser(randomFriend.getScreenName(), true);
						friendsOfFriend.put(randomFriend.getScreenName(), friendsOfRandomFriend);
					} catch (TwitterException e) {
						// TODO Auto-generated catch block
						System.out.println("\nERROR OCCURED GETTING FRIENDS FOR USER: " + randomFriend.getScreenName());
						e.printStackTrace();
						continue;
					}
					
//				}
			}
			
			//If the random friend has friends
			if (friendsOfRandomFriend.size() > 0) {
				
				URLEntity link = null;
				
				while (link == null && friendsOfRandomFriend.size() > 0) {
					//Get the a random friend-of-friend
					User randomFriendOfFriend = friendsOfRandomFriend.get(HelperFunctions.randomIntBetween(0,friendsOfRandomFriend.size()-1));
		
					//Get a link from the friend-of-friend
					link = getRandomURLLinkForUser(randomFriendOfFriend.getScreenName(), randomFriendOfFriendLinks);
					
					if (link != null) { //If the link is not null, add it to the list
						if (!randomFriendOfFriendLinks.contains(link)) {
							randomFriendOfFriendLinks.add(link);
							selectedFriendsOfFriends.add(randomFriendOfFriend.getScreenName());
						}
						
						
					} else {
						friendsOfRandomFriend.remove(randomFriendOfFriend);
					}
				} //End of while loop
				
				
			} // End of if statement to check if the random friend has friends
			
		} //End of while loop to check if the number of friend-of-friend links has been reached.

		return randomFriendOfFriendLinks;
	}

	/**
	 * Gets the friends for a user.
	 * @param username Username of the user that you are trying to find the friends of
	 * @return ArrayList<User> The list of friends for the user specified
	 * @throws TwitterException
	 */
	public ArrayList<User> getFriendsForUser(String username, boolean limited) throws TwitterException {

		ArrayList<User> listFriends = new ArrayList<User>();
		//ArrayList<User> listFollowers = new ArrayList<User>();


		int repeatCount = 1; //Due to rate limiting issues, only 200 random friends will be grabbed instead of all the friends if limited == true
		 
		long cursor = -1;
		PagableResponseList<User> pagableFriends;
		do { 
			
			Twitter tc = getTwitterConnection("/friends/list");
			while (tc == null) {
				HelperFunctions.waitSec(60);
				tc = getTwitterConnection("/friends/list");
			}
			
			pagableFriends = tc.getFriendsList(username, cursor, 200);
			
			for (User user : pagableFriends) {
				listFriends.add(user); // ArrayList<User>
				//System.out.println(user.getScreenName());
			}
			
			repeatCount = repeatCount - 1;
			
		} while ((cursor = pagableFriends.getNextCursor()) != 0 && (repeatCount > 0 || !limited));

		if (limited) {
			System.out.println("Successfully found " + listFriends.size() + " (max 200) " + " friends for user: " + username);
		} else {
			System.out.println("Successfully found " + listFriends.size() + " friends for user: " + username);
		}

		return listFriends;
	}

	/**
	 * Retrieves the tweets for a user in past 30 days with a maximum of 3000 Tweets. 
	 * @param username
	 * @return
	 * @throws TwitterException
	 */
	private ArrayList<Status> getTweetsForUserInPast30Days(String username) throws TwitterException {
		ArrayList<Status> tweets = new ArrayList<Status>();
		ResponseList<Status> statuses;
		Paging paging = new Paging(1, 100); //100 tweets per page


		Date today = new Date();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(today);
		calendar.add(Calendar.DAY_OF_MONTH, -30);
		Date thirtyDaysPrior = calendar.getTime();
		boolean exceeded30Days = false;
		int repeatCount = 30; //Retrieves a maximum of 3000 tweets
		
		do {
			//Get all the tweets for the user
			Twitter tc = getTwitterConnection("/statuses/user_timeline");
			while (tc == null) {
				HelperFunctions.waitSec(60);
				tc = getTwitterConnection("/statuses/user_timeline");
			}
			
			statuses = tc.getUserTimeline(username, paging);
			
			for (Status status : statuses) {

				//Assuming the tweets are in chronological order, stop adding the tweets to the array once it exceeds 30 days
				if (!status.getCreatedAt().before(thirtyDaysPrior)) {
					tweets.add(status);
				} else {
					exceeded30Days = true;
					break;
				}

				//Increment page
				paging.setPage(paging.getPage()+1);
				repeatCount--;
			}
		} while (statuses.size() != 0 && repeatCount > 0 && exceeded30Days == false); 

		System.out.println("Successfully found " + tweets.size() + " (max 3000) tweets for user: " + username);
		
		return tweets;
	}
	
	/**
	 * Gets a random URL link for the specified user in the past 30 days.
	 * @param username The username of the user
	 * @param currentLinkList The arraylist of links already added.
	 * @return
	 */
	private URLEntity getRandomURLLinkForUser(String username, ArrayList<URLEntity> currentLinkList) {
		
		ArrayList<Status> tweets;
		
		if ((tweets = userTweets.get(username)) == null) {
			//Compile a list of all tweets for user.
			try {
				tweets = getTweetsForUserInPast30Days(username);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				
				System.out.println("\nERROR OCCURED WHILE GETTING URL LINK FOR USER: " + username);
				e.printStackTrace();
				return null;
			}
			
			//Filter tweets to contain URL links
			tweets.removeIf(tweet -> tweet.getURLEntities().length == 0);
			
			//Store for possible later use
			userTweets.put(username, tweets);
		}


		//Get a random URL Link from the last 30 days //( Status class 'getCreatedAt()' returns date. 'getURLEntities' to check if there is a URL attached to the tweet.)
		if (tweets.size() > 0) {
			
			URLEntity url = null;

			Status randomTweet = tweets.get(HelperFunctions.randomIntBetween(0,tweets.size()-1));
			URLEntity[] urls = randomTweet.getURLEntities();
			url =  urls[HelperFunctions.randomIntBetween(0,urls.length-1)];
			System.out.println("Found URL:\t" + url.getDisplayURL() + "\t(" + url.getURL() + ")" + "\t\tUser: " + username);
			return url;
		}

		System.out.println("ERROR: User [" + username + "] has not posted a link in the past 3000 tweets or 30 days");
		return null;
	}
	
	public void writeOutFriendsListForUser(String user) {
		
		ArrayList<User> listFriends = null ;
		try {
			listFriends = getFriendsForUser(user, false);
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			System.out.println("Will retry write out operation in 10 seconds");
			HelperFunctions.waitSec(10);
			writeOutFriendsListForUser(user);
		}
		
		
		File file = new File("\\synthetic_histories\\");
		if (!file.exists()) {
			file.mkdir();
			System.out.println("Created new synthetic_histories folder");
		}
		
		//try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
		try (Writer writer = new BufferedWriter(new FileWriter(new File("\\synthetic_histories\\"+user+"_friends.txt")))) {
			//-------------------- Print synthetic history		
			writer.write("--------------------------------------------FRIENDS:--------------------------------------------\n");
			writer.write("Size: " + listFriends.size()+"\n\n");
			for (User u : listFriends) {
				writer.write(u.getScreenName() + "\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<Status> searchTweetsForString(String str, int count) {
		
	    long currentID = Long.MAX_VALUE;
		List<Status> tweets = new ArrayList<Status>();
	    
		
		//Create the query
	    Query query = new Query(str);
	    query.setResultType(Query.MIXED);
	    query.setCount(100);
	    
		while (tweets.size() < count) {
			//Get a twitter connection
			Twitter tc = getTwitterConnection("/search/tweets");
			while (tc == null) {
				HelperFunctions.waitSec(60);
				tc = getTwitterConnection("/search/tweets");
			}
		    
		    
		    try {
		    	
		    	//Search and store in tweets
		    	for (Status status : tc.search(query).getTweets()) {
		    		if (status.getId() < currentID) {
		    			currentID = status.getId();
		    		}
		    		tweets.add(status);
		    		
		    		if (tweets.size() == count)
		    			return tweets;
		    	}
		    	
				
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
				System.out.println("Error occured. Will retry in 10 seconds.");
				HelperFunctions.waitSec(10);
				continue;
			}
		    
		    //Makes sure you don't get duplicate tweets
		    query.setMaxId(currentID);
		}
		
		return tweets;
	}
	
	
	public List<URLEntity> getURLSInTweets(List<Status> tweets) {
		ArrayList<URLEntity> urls = new ArrayList<URLEntity>();

	    for (Status status : tweets) {
	        //System.out.println("@" + status.getUser().getScreenName() + ": " + status.getText());
	        
	    	for (URLEntity url : status.getURLEntities()) {
	    		urls.add(url);
	    	}
	    } 

		return urls;
	}

}
