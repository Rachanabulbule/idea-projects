package package1;
import java.sql.*;
import java.util.Scanner;

public class InsertData {
    public static void insert()
    {
        Scanner input = new Scanner(System.in);

        System.out.print("Enter an your id: ");
        int ID = input.nextInt();
        System.out.print("Enter an your name: ");
        String Name = input.next();
        System.out.print("Enter an your age: ");
        int Age = input.nextInt();

        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Employee","root","new-password");
            Statement stmt = connection.createStatement();

            // Inserting data in database
            String q1 = "insert into Table1 values('" +ID+ "', '" +Name+ "','"+Age+ "')";
            int x = stmt.executeUpdate(q1);
            if (x > 0)
                System.out.println("Successfully Inserted");
            else
                System.out.println("Insert Failed");

            connection.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }

}
