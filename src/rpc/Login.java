package rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;

/**
 * Servlet implementation class Login
 */
@WebServlet("/login")
public class Login extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Login() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		 DBConnection connection = DBConnectionFactory.getConnection(); //建立起数据库连接
//		 try {
//			 	// ！！input为空和放True是一样的效果，有就返回，没有就创建一个返回
//			 	// 放false， 有就返回，没有就返回null
//				HttpSession session = request.getSession(false);
//
//				JSONObject obj = new JSONObject();
//				if (session != null) {
//					String userId = session.getAttribute("user_id").toString();
//					obj.put("status", "OK").put("user_id", userId).put("name", connection.getFullname(userId));	
//				} else {
//					response.setStatus(403); //确认没有log in
//					obj.put("status", "Invalid session");
//				}
//
//				RpcHelper.writeJsonObject(response, obj);
//				
//			} catch (JSONException e) {
//				e.printStackTrace();
//			} finally {
//				connection.close();
//			}
		DBConnection connection = DBConnectionFactory.getConnection();
		try {
			HttpSession session = request.getSession(false);
			//System.out.println("session: " + session.toString());
			
			JSONObject obj = new JSONObject();
			if (session != null) {
				String userId = session.getAttribute("user_id").toString();
				obj.put("status", "OK").put("user_id", userId).put("name", connection.getFullname(userId));	
			} else {
				response.setStatus(403);
				obj.put("status", "Session Invalid");
			}

			RpcHelper.writeJsonObject(response, obj);
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		 DBConnection connection = DBConnectionFactory.getConnection(); //建立起数据库连接
//	  	 try {
//	  		 JSONObject input = RpcHelper.readJSONObject(request);
//	  		 String userId = input.getString("user_id");
//	  		 String password = input.getString("password");
//	  		 JSONObject obj = new JSONObject();
//	  		 if(connection.verifyLogin(userId, password)) {
//	  			// 创建session
//	  			// Returns the current session associated with this request, 
//	  			// or if the request does not have a session, creates one
//	  			 HttpSession session = request.getSession();// sessoin id已经自动保存在response里了,保存在server的内存里，也可以存到DB里
//	  			 session.setAttribute("user_id", userId);
//	  			 session.setMaxInactiveInterval(600); //Specifies the time, in seconds, between client requests before the servlet container will invalidate this session.
//	  			 obj.put("status", "OK").put("user_id", userId).put("name", connection.getFullname(userId));
//
//	  		 }else {
//	  			 response.setStatus(401); // 用户名和密码错了，返回401
//	  			 obj.put("status", "User doesn't exist");
//	  		 }
//	  		 
//	  		RpcHelper.writeJsonObject(response, obj);
//
//	  	 } catch (Exception e) {
//	  		 e.printStackTrace();
//	  	 } finally {
//	  		 connection.close();
//	  	 }
		DBConnection connection = DBConnectionFactory.getConnection();
		try {
			JSONObject input = RpcHelper.readJSONObject(request);
			String userId = input.getString("user_id");
			String password = input.getString("password");
			
			JSONObject obj = new JSONObject();
			if (connection.verifyLogin(userId, password)) {
				HttpSession session = request.getSession();
				System.out.println("session:" + session.toString());
				session.setAttribute("user_id", userId);
				session.setMaxInactiveInterval(600);
				obj.put("status", "OK").put("user_id", userId).put("name", connection.getFullname(userId));
			} else {
				response.setStatus(401);
				obj.put("status", "User Doesn't Exists");
			}
			RpcHelper.writeJsonObject(response, obj);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}

	}

}
