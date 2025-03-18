package gov.nih.nci.hpc.test.common;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileHelper {

//Function to print filename and extension for the given filepath 
/*public static String printFileNameAndExtension(String filepath) { 
        // Convert filepath to Path 
        Path path = Paths.get(filepath); 
        // Get the filename from the Path 
        String fileName = path.getFileName().toString(); 
        // Print filename 
        System.out.println("Filename and Extension: " + fileName); 
        // Get and print the extension for the filename 
        String extension = getFileExtension(fileName); 
        System.out.println("Extension: " + extension);  
        return filename;
    } 

//Function to get the extension from a filename 
public static String getFileExtension(String fileName) { 
    // Find the last occurrence of '.' in the filename 
    int dotIndex = fileName.lastIndexOf('.'); 
    // If '.' is not found, return "No extension", otherwise return the substring after '.' 
    return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1); 
} */
}