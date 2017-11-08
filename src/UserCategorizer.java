import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import javax.print.DocFlavor.URL;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import twitter4j.*;


public class UserCategorizer {

	private TwitterHandler twitterHandler;
	public ArrayList<String> stopWords = new ArrayList<String>();


	/**
	 * Default initializer. Creates twitter connections based on information in a txt document.
	 */
	public UserCategorizer(TwitterHandler handler) {
		
		this.twitterHandler = handler;
	
		try(BufferedReader br = new BufferedReader(new FileReader("stopWords.txt"))) {
			//StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {

				stopWords.add(line);
				line = br.readLine();
			}
		}		
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	public List<Category> categorizeUser(String username) {
		ArrayList<Category> sortedCategories = new ArrayList<Category>();
		ArrayList<Category> pickedCategories = new ArrayList<Category>();
		
		ArrayList<User> friends;
		Hashtable<String, Integer> categories = new Hashtable<String, Integer>();
		
		try {
			//Step 1: Get the friends of the user
			System.out.println("Retrieving friends for " + username);
			friends = twitterHandler.getFriendsForUser(username, true);
			
			//Step 2: For every friend, get the all the lists with them included
			//for (User u : friends) {
			System.out.println("Retrieving lists for users...");
			int friendCount = Math.min(friends.size(), 100);
			for (int i = 0; i < friendCount; i++) {
				User u = friends.get(i);
						
				System.out.println(i+1 + "/" + friendCount + "\tUSER: " + u.getScreenName());
				
				ArrayList<UserList> lists = twitterHandler.getListsForUser(u.getScreenName(), new ArrayList<UserList>(), -1, 50);
				
				//Step 3: For each list with the user contained in them, parse the name and include it in the categories hashtable
				for (UserList list : lists) {
					String name = list.getFullName().toLowerCase();
					System.out.println("Parsing name: " + name);
//					String words[] = name.split("[^\\w']+");
//					
//					
//					for (String word : words) {
//						int newVal = categories.get(word) != null ? (Integer) categories.get(word) + 1 : 1;
//						categories.put(word, newVal);
//					}
					
					Analyzer analyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
					
					String theSentence = name;
					StringReader reader = new StringReader(theSentence);
					TokenStream tokenStream = analyzer.tokenStream("content", reader);
					ShingleFilter theFilter = new ShingleFilter(tokenStream);
					theFilter.setMinShingleSize(2);
					theFilter.setMaxShingleSize(3);
					theFilter.setOutputUnigrams(true);

					CharTermAttribute charTermAttribute = theFilter.addAttribute(CharTermAttribute.class);
					theFilter.reset();

					while (theFilter.incrementToken()) {
					    
					    String word = charTermAttribute.toString();
					    System.out.println("Word: " + word);
					    int newVal = categories.get(word) != null ? (Integer) categories.get(word) + 1 : 1;
					    categories.put(word, newVal);
					}

					theFilter.end();
					theFilter.close();
				}
			}
			
			//Step 3.5: Remove stopwords
			for (String stopWord : stopWords) {
				categories.put(stopWord, -1);
			}
			System.out.println("Removed stopwords from hashtable.");
			
			
			//Step 4: Convert the hashtable into an array of categories
			for (Entry<String, Integer> entry : categories.entrySet()) {
				sortedCategories.add(new Category(entry.getKey(), entry.getValue()));
			}
			System.out.println("Converted hashtable into array.");
			
			//Step 5: Sort the categories
			sortedCategories.sort(new Comparator<Category>() {
				
				public int compare(Category c1, Category c2) {
					return c2.val - c1.val;
				}
				
			});
			System.out.println("Sorted categories.");
			
			System.out.println("\nORIGINAL LIST");
			for (int i = 0; i < Math.min(sortedCategories.size(), 20); i++) {
				Category x = sortedCategories.get(i);
				System.out.println(x.name + ": " + x.val);
			}
			System.out.println("Printed top 20 categories.");
			
			
			//Step 6: Print the top 20 categories
			System.out.println("\nNEW LIST");
			int currentIndex = 0; //The current category the program is analyzing
			int printedCount = 0; //The number of items already printed
			int numToPrint = 25; //Change to print more categories
			int lookAhead = 20; //Change to see how far the program should look for find similar n-grams
			float minimumPercentSame = 0.9f;
			
			while (printedCount < numToPrint && currentIndex < sortedCategories.size()) {
				Category cat = sortedCategories.get(currentIndex);
				
				boolean skipPrint = false; //Only skip if there exists another string
				int searchEndIndex = currentIndex + lookAhead;
				for (int searchIndex = currentIndex+1; searchIndex <= searchEndIndex && searchIndex < sortedCategories.size(); searchIndex++) {
					Category compareCat = sortedCategories.get(searchIndex);
					
					//System.out.println("CAT VAL*0.9: " + cat.val * minimumPercentSame + "\t|\tCOMPARE VAL: " + compareCat.val + "\t|\tOUTPUT: " + (compareCat.val >= cat.val * minimumPercentSame));
					
					if (compareCat.val >= cat.val * minimumPercentSame) {
						if (compareCat.name.contains(cat.name)) {
							skipPrint = true;
							break;
						}
					} else { //Since the array is sorted and the .val property is only decreasing, the program should stop checking for other categories at this point
						break;
					}
				}
				if (!skipPrint) {
					printedCount++;
					pickedCategories.add(cat);
					System.out.println(cat.name + ": " + cat.val);
				}
				currentIndex++;
			}
			System.out.println("Printed top "+printedCount+" categories.\n\n");
			

			
			//Step 7: Store categories for user in JSON for later use
			JSONObject userCategories = new JSONObject();
			JSONArray categoriesArray = new JSONArray();
			
			for (Category category : pickedCategories) {
				categoriesArray.add(category.convertToJson());
				if (category.val == -1) {
					break;
				}
			}
			userCategories.put("topCategories", categoriesArray);
			
			HelperFunctions.writeOutJsonObject(userCategories, username+"_top_categories", "user_categories");
			System.out.println("Stored top categories for " + username);
			return pickedCategories;
			
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  catch (Exception e) {
			e.printStackTrace();
		}
		
		return sortedCategories;
		 
	}
	
	List<String> getPossibleURLSForCategory(String category) {
		ArrayList<String> possibleURLS = new ArrayList<String>();
		
		List<Status> tweets = twitterHandler.searchTweetsForString(category, 100);
		
		//Filter tweets to contain URL links
		tweets.removeIf(tweet -> tweet.getURLEntities().length == 0);
		
		//Get the URLS inside the tweets
		List<URLEntity> urls = twitterHandler.getURLSInTweets(tweets);

		for (URLEntity url : urls) {
			possibleURLS.add(url.getURL());
		}
		
		//Return the list of URLs
		return possibleURLS;
	}
}


