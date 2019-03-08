package db.mysql;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MySQLConnection2 implements DBConnection {
	
	private Connection conn;
	   
	   // constructor:
	   public MySQLConnection2() {
	  	 try {
	  		 // Class.forNam()这个方法就是启动JDBC Driver，并创建一个DriverManager instance
	  		 // 可以由DriverManager， call getConnection，从而建立起连接
	  		 // singleton pattern only create one instance conn
	  		 Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
	  		 conn = DriverManager.getConnection(MySQLDBUtil.URL);
	  		// 如果conn建立起来是null，建立失败了，exp会被catch
	  	 } catch (Exception e) {
	  		 e.printStackTrace();
	  	 }
	   }

	@Override
	public void close() { //关闭连接
		 if (conn != null) {
	  		 try {
	  			 conn.close();
	  		 } catch (Exception e) {
	  			 e.printStackTrace();
	  		 }
	  	 }

	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			System.err.println("DB connection failed");
		  	return;
		}
		
		try {
			String sql = "INSERT IGNORE INTO history(user_id, item_id) VALUES (?, ?)";
		  	PreparedStatement ps = conn.prepareStatement(sql);
		  	ps.setString(1, userId);
		  	for (String itemId : itemIds) {
		  		ps.setString(2, itemId);
		  		ps.execute();
		  	}
		  	} catch (Exception e) {
		  		 e.printStackTrace();
		  		 }
		  
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		 if (conn == null) {
	  		 System.err.println("DB connection failed");
	  		 return;
	  	       }
	  	
	  	 try {
	  		 String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
	  		 PreparedStatement ps = conn.prepareStatement(sql);
	  		 ps.setString(1, userId);
	  		 for (String itemId : itemIds) {
	  			 ps.setString(2, itemId);
	  			 ps.execute();
	  		 }
	  	    } catch (Exception e) {
	  	  		 e.printStackTrace();
	  	  	}


	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		if (conn == null) {
			return new HashSet<>();
		}
		
		Set<String> favoriteItems = new HashSet<>();
		
		try {
			String sql = "SELECT item_id FROM history WHERE user_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				String itemId = rs.getString("item_id");
				favoriteItems.add(itemId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItems;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		if (conn == null) {
			return new HashSet<>();
		}
		
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);
		
		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for (String itemId : itemIds) {
				stmt.setString(1, itemId);
				
				ResultSet rs = stmt.executeQuery();
				
				ItemBuilder builder = new ItemBuilder();
				
				while (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setAddress(rs.getString("address"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
					builder.setCategories(getCategories(itemId));
					builder.setDistance(rs.getDouble("distance"));
					builder.setRating(rs.getDouble("rating"));
					
					favoriteItems.add(builder.build());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		if (conn == null) {
			return null;
		}
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category from categories WHERE item_id = ? "; // 占位符
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, itemId); //setString API，把itemId衔接上去
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String category = rs.getString("category");
				categories.add(category);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		TicketMasterAPI ticketMasterAPI = new TicketMasterAPI();
        List<Item> items = ticketMasterAPI.search(lat, lon, term); // term is keyword

        for(Item item : items) {
        	saveItem(item); //把之前搜索过的结果，保存到DB里
        }

        return items;//把search 结果返回前端
	}

	@Override
	public void saveItem(Item item) {
		  if (conn == null) {
	  		   System.err.println("DB connection failed");
	  		   return;
	  	         }
	  	
	  	 // sql injection
	  	 // select * from users where username = '' AND password = '';
	  	
	  	 // username: fakeuser ' OR 1 = 1; DROP  --
	  	 // select * from users where username = 'fakeuser ' OR 1 = 1 --' AND password = '';
	  	
	  	
	  	 try {
	  		 String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
	  		 PreparedStatement ps = conn.prepareStatement(sql);
	  		 ps.setString(1, item.getItemId());
	  		 ps.setString(2, item.getName());
	  		 ps.setDouble(3, item.getRating());
	  		 ps.setString(4, item.getAddress());
	  		 ps.setString(5, item.getImageUrl());
	  		 ps.setString(6, item.getUrl());
	  		 ps.setDouble(7, item.getDistance());
	  		 ps.execute(); //执行语句
	  		
	  		 // category table 
	  		 sql = "INSERT IGNORE INTO categories VALUES(?, ?)"; // insert ignore可以去重复
	  		 ps = conn.prepareStatement(sql);
	  		 ps.setString(1, item.getItemId());
	  		 // category is a set, need iterate
	  		 for(String category : item.getCategories()) {
	  			 ps.setString(2, category);
	  			 ps.execute(); //执行语句
	  		 }
	  		
	  	 } catch (Exception e) {
	  		 e.printStackTrace();
	  	 }


	}

	@Override
	public String getFullname(String userId) {
		if (conn == null) {
			return null;
		}
		String name = "";
		try {
			String sql = "SELECT first_name, last_name FROM users WHERE user_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			//目前可以使用if，因为
			while(rs.next()) {//如果skema改变了，可能user_id不再是pk，可能会重复了，那么while依然可用
				// rs.hasnext()没有这个method
				// rs.next() 起点为-1
				name = rs.getString("first_name") + " " + rs.getString("last_name");
			}
			
		}catch (SQLException e){
			e.printStackTrace();
		}
		
		return name;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		if(conn == null) {
			return false;
		}
		
		
		try {
			String sql = "SELECT * FROM users WHERE user_id = ? and password = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId); // parameterIndex the first parameter is 1, the second is 2, ...
										    // x the parameter value
			statement.setString(2, password);
			ResultSet rs = statement.executeQuery();//验证的事情，交给数据库做
			
			while(rs.next()) {
				return true; 
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean registerUser(String userId, String password, String firstname, String lastname) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return false;
		}

		try {
			String sql = "INSERT IGNORE INTO users VALUES (?, ?, ?, ?)";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			ps.setString(2, password);
			ps.setString(3, firstname);
			ps.setString(4, lastname);
			
			return ps.executeUpdate() == 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;	
	}

}
