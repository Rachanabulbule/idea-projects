package package1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Scanner;

public class UpdateData {
    public static void update()
    {
        Scanner input = new Scanner(System.in);

        System.out.print("Enter an your id: ");
        int ID = input.nextInt();
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Employee","root","new-password");
            Statement stmt = connection.createStatement();

            // Deleting from database
            System.out.println("Enter 1 to update Name and 2 to update Age");
            int choice = input.nextInt();
            if (choice==1) {
                System.out.println("Enter Name");
                String name = input.next();
                String q1 = "UPDATE user_info set Name ='" + name + "' WHERE ID = '" + ID + "'";

                int x = stmt.executeUpdate(q1);

                if (x > 0)
                    System.out.println("Your Name is successfully updated");
                else
                    System.out.println("ERROR OCCURED :(");
            }
            else if(choice==2) {
                System.out.println("Enter Age");
                int age = input.nextInt();
                String q1 = "UPDATE user_info set Age ='" + age + "' WHERE ID = '" + ID + "'";

                int x = stmt.executeUpdate(q1);

                if (x > 0)
                    System.out.println("Your Age is successfully updated");
                else
                    System.out.println("ERROR OCCURED :(");
            }
            connection.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }
}


