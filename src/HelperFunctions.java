import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;

import com.cedarsoftware.util.io.JsonWriter;

public class HelperFunctions {

	public HelperFunctions() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Helper function to retrieve a random integer between min and max inclusive.
	 * @param min
	 * @param max
	 * @return
	 */
	static public int randomIntBetween(int min, int max) {
		Random rand = new Random();
		return rand.nextInt(max - min + 1) + min;
	}

	/**
	 * Helper function to wait some period of time in milliseconds
	 * @param time
	 */
	static public void waitSec(int time) {
		System.out.println("SLEEPING...");

		int timeInterval = 60;
		int sleepTime = timeInterval;

		do {
			System.out.println("Time remaining: " + time);

			sleepTime = timeInterval;
			time = time - timeInterval;
			if (time < 0) {
				sleepTime+=time;
			}

			try {
				TimeUnit.SECONDS.sleep(sleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		} while (time > 0); 

	}
	
	public static void writeOutJsonObject(JSONObject obj, String filename, String destination) {
		File file = new File(destination);
		if (!file.exists()) {
			file.mkdir();
			System.out.println("Created new "+ destination +" folder");
		}
		
		try (Writer writer = new BufferedWriter(new FileWriter(new File(destination+"/"+filename+".json")))) {	
			writer.write(JsonWriter.formatJson(obj.toJSONString()));
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//TODO: Function to read in .json file as JSON Object
	
}
