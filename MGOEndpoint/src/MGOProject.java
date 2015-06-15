import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hsqldb.jdbc.JDBCConnection;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

//have class that extends and overrides for new versions
@Path("1.0")
public class MGOProject {	
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
	@Path("create_user")
	public String createUser(String login_info) {
		//create auth JSON object
		JSONObject auth_obj;
		try{
			auth_obj = new JSONObject(login_info);
		}
		catch (JSONException e) {
			JSONObject err = new JSONObject();
			err.put("error", e.toString());
			return err.toString();
		}
		//need to add more error handling
		String fname = auth_obj.getString("fname");
		String lname = auth_obj.getString("lname");
		String city = auth_obj.getString("city");
		String title = auth_obj.getString("title");
		String login = auth_obj.getString("login");
		String password = auth_obj.getString("password");
		
		//create password hash
		String pwhash = getPasswordHash(password);
		
		JSONObject ret = new JSONObject();
		//JDBCConnection con = getDB();
		JDBCConnection con = MGODBConnection.getDB();
		try {
			//TODO check if user exists first
			//add username and pass, should check if user exists first and update password instead, etc.
			PreparedStatement stmt = con.prepareStatement("insert into user(fname,lname,city,title,login,password) values(?,?,?,?,?,?)");
			stmt.setString(1, fname);
			stmt.setString(2, lname);
			stmt.setString(3, city);
			stmt.setString(4, title);
			stmt.setString(5, login);
			stmt.setString(6, pwhash);
			stmt.executeUpdate();
			con.commit();
		} catch (SQLException e) {
			ret.put("failure", "could not create new user: " + e.getMessage());
			return ret.toString();
		}
		ret.put("success", "successfully created user");
		return ret.toString();
	}
	
	
	//login password, talks to hsqldb for lookup
	@POST
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
	@Path("authenticate_user")
	public String userAuth(String login_info) {
		JSONObject auth_obj;
		try{
			auth_obj = new JSONObject(login_info);
		}
		catch (JSONException e) {
			JSONObject err = new JSONObject();
			err.put("error", e.toString());
			return err.toString();
		}
		//check password hash in database for username
		String login = auth_obj.getString("login");
		String password = auth_obj.getString("password");
		
		//get hash for password
		String pwhash = getPasswordHash(password);
		
		//auth user
		JSONObject ret = new JSONObject();
		//JDBCConnection con = getDB();
		JDBCConnection con = MGODBConnection.getDB();
		try {
			PreparedStatement stmt = con.prepareStatement("SELECT password FROM user WHERE login = ?");
			stmt.setString(1, login);
			ResultSet result = stmt.executeQuery();
			if(result.next() && ( result.getString(1).equals(pwhash) ) ){
				 ret.put("success", "user validated");
			}
			else {
				ret.put("failed", "username and/or password invalid");
			}
		} catch (SQLException e) {
			ret.put("failure", "user auth query failed: " + e.getLocalizedMessage());
			return ret.toString();
		}
		return ret.toString();
	}
	
	
	//get info, orderby, hsqldb URI based
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("select_users")
	public String selectUsers(@QueryParam("field") String field,@QueryParam("value") String value, @QueryParam("groupby") String groupby) {
		JSONObject ret = new JSONObject();
		JDBCConnection con = MGODBConnection.getDB();
		try {
			//needed to do a weird group by due to hsqldb handling them differently
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM user WHERE "+ field +" = ? GROUP BY id,"+groupby);
			stmt.setString(1, value);
			ResultSet result = stmt.executeQuery();
			ArrayList<JSONObject> users = new ArrayList<JSONObject>();
			while(result.next()){
				JSONObject element = new JSONObject();
				element.put("fname", result.getString("fname").toString());
				element.put("lname", result.getString("lname").toString());
				element.put("city", result.getString("city").toString());
				element.put("title", result.getString("title").toString());
				users.add(element);
			}
			ret.put("users", users);
		} catch (SQLException e) {
			ret.put("failure", "select query failed:  " + e.getLocalizedMessage());
			e.printStackTrace();
			return ret.toString();
		}
		return ret.toString();
	}
	
	
	//list directory
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("list_directory")
	public String listDir(@QueryParam("directory") String d) {
		File dir = new File(d);
		ArrayList<JSONObject> files = new ArrayList<JSONObject>();
		if(dir.isDirectory()){
			for (File f : dir.listFiles()) {
				JSONObject temp = new JSONObject();
				temp.put("path", f.getAbsolutePath());
				files.add(temp);
			}
		}
		JSONObject ret = new JSONObject();
		ret.put("files", files);
		return ret.toString();
	}
	
	
	//gets status (checks if database is up)
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("status")
	public String status() {
		JSONObject ret = new JSONObject();
		//check status of database
		try {
			Class.forName("org.hsqldb.jdbcDriver");
			String url = "jdbc:hsqldb:file:database/mgo_project;ifexists=true";
			String user = "sa";  
			String password = "";
			JDBCConnection connection = (JDBCConnection) DriverManager.getConnection(url, user, password);
			connection.close();
		} catch (ClassNotFoundException e) {
			ret.put("status", "unable to create JSON object");
			return ret.toString();
		} catch (SQLException e) {
			ret.put("status", "database missing");
			return ret.toString();
		}
		ret.put("status", "database running");
		return ret.toString();
	}

	//could add a salt to this to make this stronger
	private static String getPasswordHash(String password) {
		String generatedPassword = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] bytes = md.digest(password.getBytes());
			StringBuilder sb = new StringBuilder();
			for(int i=0; i< bytes.length ;i++)
			{
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			generatedPassword = sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return generatedPassword;
	}
}