import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.hsqldb.jdbc.JDBCConnection;


final class MGODBConnection {
	private static JDBCConnection connection;
	
	static JDBCConnection getDB() {
		String url = "jdbc:hsqldb:file:database/mgo_project";  
		String user = "sa";  
		String password = "";
		try {
			Class.forName("org.hsqldb.jdbcDriver");
			if (connection != null) {
				return connection;
			}
			connection = (JDBCConnection) DriverManager.getConnection(url, user, password);
			Statement create = connection.createStatement();
			create.executeUpdate("create table if not exists user (id int IDENTITY not null primary key,fname VARCHAR(255), lname VARCHAR(255), city VARCHAR(255), title VARCHAR(255), login VARCHAR(255) not null unique, password VARCHAR(255) )");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		return connection;
	}
}
