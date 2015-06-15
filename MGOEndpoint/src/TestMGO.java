import static org.hamcrest.CoreMatchers.equalTo;

import java.util.ArrayList;

import org.json.JSONObject;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;

public class TestMGO {
    
    @Test
    public void testStatus() {
    	expect().contentType("application/json").body("status", equalTo("database running")).when().get("MGOEndpoint/1.0/status");
    }
    
    @Test
    public void testCreateUser() {
    	given().contentType("application/json")
    	.param("fname", "fname")
    	.param("lname", "lname")
    	.param("city", "city")
    	.param("title", "title")
    	.param("login", "login")
    	.param("password", "password")
    	.expect().contentType("application/json").body("success", equalTo("successfully created user")).when().post("MGOEndpoint/1.0/create_user");
    }
    
    @Test
    public void testAuthUser() {
    	given().contentType("application/json")
    	.param("login", "login")
    	.param("password", "password")
    	.expect().body("success",equalTo("")).when().post("MGOEndpoint/1.0/authenticate_user");
    }
    
    @Test
    public void listDirectory() {
		ArrayList<JSONObject> files = new ArrayList<JSONObject>();
		JSONObject temp = new JSONObject();
		temp.put("path", "c:\test\test.txt");
		files.add(temp);
		JSONObject ret = new JSONObject();
		ret.put("files", files);
	    
		given().param("directory", "c:/test")
    	.expect().
        contentType("application/json").body(equalTo(ret)).when().get("MGOEndpoint/1.0/list_directory");
    }

}