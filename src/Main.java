import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.*;
import org.json.JSONException;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws Exception {
    System.out.println("Hi Emily!");
    Scanner in = new Scanner(System.in);
    System.out.println("Please enter the folder location of your OpenPose files for the first video. Remember to use always two backslashes, not one.");
    String f1 = in.nextLine();
    System.out.println("Please enter the folder location of your OpenPose files for the second video.");
    String f2 = in.nextLine();
    System.out.println("Please enter the name that you want your output file to have. Feel free to add a file type if you have a preference. If you don't, .txt is a generally good place to start because it works well with Notepad.");
    String outputFileName = in.nextLine();
    in.close();
    // Test 1:
    // .\\free throw\\video2
    // .\\free throw\\video4
    // Test 2: .\\free throw\\video4 + .\\free throw\\video2
    // Sped-up Test: .\\free throw\\video2spedup + .\\free throw\\video2test
    // Test 3: .\\free throw\\empty + .\\free throw\\video2
    // Test 4: laksdjflakfjds;l + .\\free throw\\video2
    // Test 5: .\\free throw\\nonJSON + .\\free throw\\video2
    // Test 6: .\\free throw\\emptyFile + .\\free throw\\video2
    // Test 7: .\\free throw\\video2 + .\\free throw\\video6

    try{
      ArrayList<String> vidNames = createVidNamesArrayList(f1);
      ArrayList<String> vidNames2 = createVidNamesArrayList(f2);
      // autoswitch the files for emily if she inputs them in the wrong order
      if (vidNames.size() > vidNames2.size()) {
        bodyDTW(vidNames2, vidNames, outputFileName);
      } else { bodyDTW(vidNames, vidNames2, outputFileName); }

      System.out.println("Congratulations, your output file has been completed. Just open the FrameMatcher folder and open the file named " + outputFileName + " to see your frame matches.");
      System.out.println("Remember, matches are organized by index. That means index x of the first frame sequence correlates to index x of the second frame sequence, where each frame sequence represents one of your input videos.");
    } catch(FileNotFoundException e) { System.out.println("One of your folders was not found. Remember, your file location must have two backslashes. For example, C:\\Example\\example vids\\video files");
    } catch(NullPointerException e) { System.out.println("This folder location does not denote a directory. Note that this program does not work for individual files — if you want align single files, please place them in emtpy folders and try again.");
    } catch(Exception e) { System.out.println("Your input folder(s) either contain non-JSON or your JSON files are empty. Please check them and try again."); }
  }
  
  public static AngleMap angleDTW(String tsIAngleFileName, String tsJAngleFileName, int i, HashMap<Integer, ArrayList<Integer>> tsIZeroHashMap, HashMap<Integer, ArrayList<Integer>> tsJZeroHashMap) throws Exception {
    double[] tsIframes = jsonParser(tsIAngleFileName);
    double[] tsJframes = jsonParser(tsJAngleFileName);
    tsIframes = buildZeroHashMap(tsIframes, i, tsIZeroHashMap);
    tsJframes = buildZeroHashMap(tsJframes, i, tsJZeroHashMap);
    Body.numOfStudentFrames = tsIframes.length;
    Body.numOfTeacherFrames = tsJframes.length;
    TimeWarpInfo s = preWarping(tsIframes, tsJframes);
    AngleMap aMap = s.getPath().createAngleMap("aMap.ser", tsIZeroHashMap, tsJZeroHashMap, i);
    return aMap;
  }
  
  public static double[] buildZeroHashMap(double[] frames, int i, HashMap<Integer, ArrayList<Integer>> zeroHashMap) {
    ArrayList<Integer> zeroIndexes = new ArrayList<>();   
    ArrayList<Double> newFrames = new ArrayList<>();
    for (int a = 0; a < frames.length; a++) {
        if (frames[a] == 0)
            zeroIndexes.add(a);
        else newFrames.add(frames[a]);
    }
    double[] frameArray = new double[newFrames.size()];
    for (int a = 0; a < newFrames.size(); a++)
        frameArray[a] = newFrames.get(a);
    frames = frameArray;
    zeroHashMap.put(i, zeroIndexes);
    return frames;
  }
  
  public static double[] jsonParser(String angleFileName) throws FileNotFoundException, IOException, ParseException {
    JSONParser parser = new JSONParser();
    Object obj = parser.parse(new FileReader(angleFileName));
    JSONObject jsonObject = (JSONObject)obj;
    JSONArray angles = (JSONArray)jsonObject.get("final_vid_angles");
    double[] frames = new double[angles.size()];
    for (int j = 0; j < frames.length; j++) frames[j] = Double.valueOf(angles.get(j).toString());    
    return frames;
  }
  
  public static void bodyDTW(ArrayList<String> video1, ArrayList<String> video2, String fileName) throws JSONException, Exception {
    int studentVidSize = video1.size();
    ArrayList<AngleMap> bodyList = new ArrayList<>();
    ArrayList<String> fileNameList = new ArrayList<>();  
    HashMap<Integer, ArrayList<Integer>> tsIZeroHashMap = new HashMap<>();
    HashMap<Integer, ArrayList<Integer>> tsJZeroHashMap = new HashMap<>();
    for (int i = 0; i < studentVidSize; i++) {
        AngleMap aMap = angleDTW(video1.get(i), video2.get(i), i, tsIZeroHashMap, tsJZeroHashMap);
        bodyList.add(aMap);
        fileNameList.add("aMap.ser");
    }
    Body body = new Body(bodyList, fileNameList);
    ExternalFile.createJSON(fileName);
  }
  
  public static TimeWarpInfo preWarping(double[] tsIframes, double[] tsJframes) throws Exception {
    TimeSeries tsI = new TimeSeries(tsIframes);
    TimeSeries tsJ = new TimeSeries(tsJframes);
    final DistanceFunction distFn = new EuclideanDistance();
    final TimeWarpInfo infoTSI = DTW.getWarpInfoBetween(tsI, tsJ, 500, distFn);
    return infoTSI;
  }

  public static ArrayList<String> createVidNamesArrayList(String folderName) {
    File f = new File(folderName);
    ArrayList<String> fileNames = new ArrayList<String>(Arrays.asList(f.list()));
    if (fileNames.isEmpty()) {
      System.out.println("ERROR: One of the folders you provided is empty. Check if you have a typo in your folder location.");
      System.exit(0);
    }
    ArrayList<String> vidNames = new ArrayList<String>();
    for (int i = 0; i < fileNames.size(); i++) {
      vidNames.add(folderName + "\\" + fileNames.get(i));
    }
    return vidNames;
  }
}