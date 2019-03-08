package external;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction, 也可以specify到 "music", "sports"
	private static final String API_KEY = "dHlBUaFsGmHYXw3gLmoS3aH6tQfRmgxS";

//	public JSONArray search(double lat, double lon, String keyword) {
	public List<Item> search(double lat, double lon, String keyword) {
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		
		try {
			keyword = URLEncoder.encode(keyword, "UTF-8"); //"Rick Sun" => "Rick%20Sun"
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String geoHash = GeoHash.encodeGeohash(lat, lon, 8); // 8是精度

		// "apikey=qqPuP6n3ivMUoT9fPgLepkRMreBcbrjV&latlong=37,-120&keyword=event&radius=50"
//		String query = String.format("apikey=%s&latlong=%s,%s&keyword=%s&radius=%s", API_KEY, lat, lon, keyword, 50); 
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, 50);

		// &size=1 可以控制output的数量
		String url = URL + "?" + query;
		
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			
			int responseCode = connection.getResponseCode(); //  It will return 200 and 401 respectively
															 // HTTP/1.0 200 OK; HTTP/1.0 401 Unauthorized
															 //  Returns -1 if no code can be discerned
			System.out.println("Sending request to url: " + url);
			System.out.println("Response code: " + responseCode);
			
			if (responseCode != 200) { // if it do not return 200, return an empty array
				System.out.println("error status code is:" + responseCode);
				return new ArrayList();
			}
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())); 
			// .getInputStream()返回 TicketMaster API 返回的结果; Returns an input stream that reads from this open connection.
			String line;
			StringBuilder response = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			reader.close(); //! 记得关闭reader，工程师的素质！
			JSONObject obj = new JSONObject(response.toString()); //把response一行行读取出来，建成一个JSONObject返回
			
			if (!obj.isNull("_embedded")) { // 所需要的event的details都放在"_embedded"下面
				JSONObject embedded = obj.getJSONObject("_embedded"); //_embedded 也是一个object
//				return embedded.getJSONArray("events"); // _embedded下面还放有event，是array
														//这些都是根据TicketMaster response structure
				return getItemList(embedded.getJSONArray("events"));
			}	
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList();
	}
	
	// Convert JSONArray to a list of item objects.
		private List<Item> getItemList(JSONArray events) throws JSONException {
			List<Item> itemList = new ArrayList<>();
			for (int i = 0; i < events.length(); ++i) {
				JSONObject event = events.getJSONObject(i);
				// here!先建立一个ItemBuilder
				ItemBuilder builder = new ItemBuilder();
				//然后call ItemBuilder的setters，去填充各个data fields
				if (!event.isNull("id")) {
					builder.setItemId(event.getString("id"));
				}
				if (!event.isNull("name")) {
					builder.setName(event.getString("name"));
				}
				if (!event.isNull("url")) {
					builder.setUrl(event.getString("url"));
				}
				if (!event.isNull("distance")) {
					builder.setDistance(event.getDouble("distance"));
				}
				if (!event.isNull("rating")) {
					builder.setRating(event.getDouble("rating"));
				}
				
				builder.setAddress(getAddress(event));
				builder.setCategories(getCategories(event));
				builder.setImageUrl(getImageUrl(event));
				// 用创建好的ItemBuilder，去build一个Item
				// build()是ItemBudiler class里的一个method
				// this constructor method 用来创建一个Item
				itemList.add(builder.build());
			}
			return itemList;
		}
		
		/**
		 * Helper methods
		 */
		private String getAddress(JSONObject event) throws JSONException {
			if (!event.isNull("_embedded")) {
				JSONObject embedded = event.getJSONObject("_embedded");
				if (!embedded.isNull("venues")) {
					JSONArray venues = embedded.getJSONArray("venues");
					for (int i = 0; i < venues.length(); ++i) {
						JSONObject venue = venues.getJSONObject(i);
						StringBuilder addressBuilder = new StringBuilder();
						if (!venue.isNull("address")) {
							JSONObject address = venue.getJSONObject("address");
							if (!address.isNull("line1")) {
								addressBuilder.append(address.getString("line1"));
							}
							if (!address.isNull("line2")) {
								addressBuilder.append(",");
								addressBuilder.append(address.getString("line2"));
							}
							if (!address.isNull("line3")) {
								addressBuilder.append(",");
								addressBuilder.append(address.getString("line3"));
							}
						}
						
						if (!venue.isNull("city")) {
							JSONObject city = venue.getJSONObject("city");
							if (!city.isNull("name")) {
								addressBuilder.append(",");
								addressBuilder.append(city.getString("name"));
							}
						}
						
						String addressStr = addressBuilder.toString();
						if (!addressStr.equals("")) {
							return addressStr;
						}
					}
				}
			}
			return ""; // 如果啥都没读出来，return空String
		}

		private String getImageUrl(JSONObject event) throws JSONException {
			if (!event.isNull("images")) {
				JSONArray array = event.getJSONArray("images");
				for (int i = 0; i < array.length(); i++) {
					JSONObject image = array.getJSONObject(i);
					if (!image.isNull("url")) {
						return image.getString("url");
					}
				}
			}
			return "";
		}
		
		private Set<String> getCategories(JSONObject event) throws JSONException {
			Set<String> categories = new HashSet<>();
			if (!event.isNull("classifications")) {
				JSONArray classifications = event.getJSONArray("classifications");
				for (int i = 0; i < classifications.length(); ++i) {
					JSONObject classification = classifications.getJSONObject(i);
					if (!classification.isNull("segment")) {
						JSONObject segment = classification.getJSONObject("segment");
						if (!segment.isNull("name")) {
							categories.add(segment.getString("name"));
						}
					}
				}
			}
			return categories;
		}

	private void queryAPI(double lat, double lon) {
//		JSONArray events = search(lat, lon, null);
		List<Item> events = search(lat, lon, null);

//		try {
//			for (int i = 0; i < events.length(); ++i) {
//				JSONObject event = events.getJSONObject(i);
//				System.out.println(event.toString(2)); // format indent 缩进
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		for(Item event: events){
			System.out.println(event.toJSONObject());
			
		}
	}


	/**
	 * Main entry for sample TicketMaster API requests.
	 */
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(29.682684, -95.295410);
	}

}
