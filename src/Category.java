import org.json.simple.JSONObject;

class Category {
		String name;
		int val;
		
		Category(String name, int val) {
			this.name = name;
			this.val = val;
		}
		
		public JSONObject convertToJson() {
			JSONObject obj =  new JSONObject();
			obj.put("name", name);
			obj.put("occurence", Integer.toString(val));
			return obj;
		}
	}


