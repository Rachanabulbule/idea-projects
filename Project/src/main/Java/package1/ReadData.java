package package1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Scanner;
import java.sql.*;

public class ReadData {
    public static void read()
    {

        Scanner input = new Scanner(System.in);

        System.out.print("Enter an your id: ");
        int ID = input.nextInt();
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Employee","root","new-password");
            Statement stmt = connection.createStatement();
            // SELECT query
            String q1 = "select * from user_info WHERE id = '" + ID + "'";
            ResultSet rs = stmt.executeQuery(q1);
            if (rs.next())
            {
                System.out.println("ID : " + rs.getString(1));
                System.out.println("Name :" + rs.getString(2));
                System.out.println("Age :" + rs.getString(3));
            }
            else
            {
                System.out.println("No such user id is already registered");
            }
            connection.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }
}

