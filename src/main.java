import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import twitter4j.URLEntity;

public class main {

	public main() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {

		TwitterHandler twitterHandler = new TwitterHandler();
		//HistorySynthesizer synthesizer = new HistorySynthesizer(twitterHandler);
		//synthesizer.createSyntheticHistoryForRandomUsers();
		
		UserCategorizer categorizer = new UserCategorizer(twitterHandler);
		List<Category> categories = categorizer.categorizeUser("TheBigWong");
		
		for (int i = 0; i < 4; i++) {
			if (i >= categories.size()) { //Prevents from going out of bounds
				break;
			}
			
			System.out.println("CATEGORY " + categories.get(i).name);
			List<String> urls = categorizer.getPossibleURLSForCategory(categories.get(i).name);
			
			for (String url : urls) {
				System.out.println(url);
			}
			System.out.println();
		}
		
//			String users[] = {"FireflyAMV", "thesoulofwind1", "alexhammersmith", "Whataburger", "AlexMSmithAutho", "JoelHeyman", "VernonDavis85", "KirkCousins8", "FredoSauce", "Prudential", 
//							"TheGiftedonFOX", "AishaMotan", "EDMShiro", "john_keim", "thecooleyzone", "Walterejones", "Areeba_Javed", "stephenpaddock", "WWERollins", "WWEDanielBryan"};
//			
//			
//			for (String user : users) {
//				//x.writeOutFriendsListForUser(user);
//				x.createSyntheticHistoryForUser(user);
//			}
	}

}
