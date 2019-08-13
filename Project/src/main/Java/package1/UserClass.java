package package1;
import java.util.Scanner;
import java.sql.*;

public class UserClass {
    public static void main(String args[]) throws SQLException
    {
        Scanner input = new Scanner(System.in);
        InsertData insertobj = new InsertData();
        ReadData readobj = new ReadData();
        UpdateData updateobj = new UpdateData();
        DeleteData deleteobj = new DeleteData();

        while(true)
        {
            System.out.println("User management operation");
            System.out.println("1. For Insert");
            System.out.println("2. For Read");
            System.out.println("3. For Update");
            System.out.println("4. For Delete");
            System.out.println("5. For Exit");
            System.out.println("Enter your choice:");
            int choice = input.nextInt();
            switch (choice) {
                case 1:
                    insertobj.insert();
                    break;
                case 2:
                    readobj.read();
                    break;
                case 3:
                    updateobj.update();
                    break;
                case 4:
                    deleteobj.delete();
                    break;
                case 5:
                    System.exit(0);
                    break;
                default:
                    System.out.println("your choice is invalid");
                    break;
            }


        }
    }
}