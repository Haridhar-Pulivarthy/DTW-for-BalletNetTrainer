import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Integer;
import java.util.Collections;
import java.util.Arrays;
import java.lang.Math;

public class Body {

  // DATA
  public static int[] simplifiedFrameArray;
  public static int[] finalFrameArray;
  public static int numOfStudentFrames;
  public static int numOfTeacherFrames;
  private static int modeThreshold; // arbitrary
  private static final int arbitraryRejectNum = Integer.MIN_VALUE; // arbitrary
  private static int quadrantSize;
  private static final int rangeDifferenceLimit = 3; // arbitrary

  // CONSTRUCTOR
  public Body(ArrayList<AngleMap> bodyList, ArrayList<String> fileNameList) {
    HashMap<Integer, ArrayList<Integer>> frameMap = createFrameMap(bodyList);
    finalFrameArray = simplifyFrameMap(frameMap);
  }

  
  // This method is only for testing. When there is only one angle map to evaluate, this bypasses
  // the simplification phase because it is already matching one frame to one frame.
  /*
  private static void turnFrameMapIntoFrameArray(HashMap<Integer, ArrayList<Integer>> frameMap) {
      for (int i = 0; i < numOfStudentFrames; i++) simplifiedFrameArray[i] = frameMap.get(i).get(0);
  }
  */

  // This method creates a HashMap where the key is the frame # (and tsI frame) and the value is an
  // ArrayList of the teacher frames for that frame #.
  // Theoretically, there should be 28 values in the arrayList.
  // There should be a key for every frame (30 - 35 probably)
  // @param : a Map containing all of the AngleMaps 
  private static HashMap<Integer, ArrayList<Integer>> createFrameMap(ArrayList<AngleMap> bodyList) {
    HashMap<Integer, ArrayList<Integer>> frameMap = new HashMap<>();
    for (int i = 0; i < numOfStudentFrames; i++) {
      ArrayList<Integer> associatedFrames = new ArrayList<>();
      for (AngleMap a:bodyList) {
          if (i < a.tsJframes.size()) associatedFrames.add(a.tsJframes.get(i));
      }
      frameMap.put(i, associatedFrames);
    }
    return frameMap;
  }


  // This is a util method for simplifyFrameMap().
  // This method takes an arrayList of teacher frames for one
  // index and builds a count array. Then it gets the highest
  // count frame, and if its count is higher than the modeThreshold,
  // that frame is returned.
  // Otherwise, an arbitrary reject number is returned.
  // @param: list of teacher frames
  private static int simplifyByMode(ArrayList<Integer> teacherFrames) {
    int max = 0;
    for (int f:teacherFrames) {
        if (max < f) max = f;
    }
    int[] count = new int[max + 1];
    for (int f:teacherFrames) count[f]++;
    ArrayList<Integer> originalCount = new ArrayList<>();
    for (Integer i:count) originalCount.add(i);
    Arrays.sort(count);
    int maxCount = count[count.length - 1];
    modeThreshold = teacherFrames.size() / 4;
    for (Integer i:originalCount) {
        if (i == maxCount && i > modeThreshold) return originalCount.indexOf(i);
    }
    return arbitraryRejectNum;
  }


  // This is a util method for areQuadrantsOfTeacherFramesEvenlySplit().
  // It gets the size of the teacherFrames arrayList and divides it by 4
  // if it is odd, then it ignores the last number, which isn't great
  // but that's just how life is sometimes
  // @param: list of teacherFrames
  private static int getQuadrantSize(ArrayList<Integer> teacherFrames) {
    int quadrantSize = 0;
    if (teacherFrames.size() % 2 == 0) quadrantSize = teacherFrames.size() / 4;
    else quadrantSize = (teacherFrames.size() - 1) / 4;
    // if (quadrantSize == 0) System.out.println("ERROR: Quadrant Size = 0.");
    return quadrantSize;
  }


  // This is a util method for areQuadrantsOfTeacherFramesEvenlySplit().
  // It fills up a quadrant arrayList given the overall list of teacher frames, the quadrant we need,
  // and the the quadrant size.
  // @param: List of teacher frames
  // @param: The quadrant we are building
  private static ArrayList<Integer> buildQuadrantOfTeacherFrames(ArrayList<Integer> teacherFrames, int quadrantNum) {
    ArrayList<Integer> quadrant = new ArrayList<>();
    int lowerBound = quadrantSize * (quadrantNum - 1);
    int upperBound = quadrantSize * quadrantNum;
    for (int i = lowerBound; i < upperBound; i++)
      quadrant.add(teacherFrames.get(i));
    if (quadrant.size() == 0) System.out.println("ERROR: Quadrant is empty.");
    return quadrant;
  }


  // This is a util method for areQuadrantsOfTeacherFramesEvenlySplit().
  // It gets the range of two quadrants, and if they are below th limit for the
  // range difference between two quadrants (Which is currently an arbitrary number under
  // DATA), it returns true.
  // @param: the first quadrant arraylist
  // @param: the second quadrant arraylist
  private static boolean rangeOfQuadrants(ArrayList<Integer> q1, ArrayList<Integer> q2) {
    int lowerBoundQ1 = q1.get(0);
    int upperBoundQ1 = q1.get(q1.size() - 1);
    int q1Range = upperBoundQ1 - lowerBoundQ1;
    int lowerBoundQ2 = q2.get(0);
    int upperBoundQ2 = q2.get(q1.size() - 1);
    int q2Range = upperBoundQ2 - lowerBoundQ2;
    int rangeDifference = Math.abs(q1Range - q2Range);
    if (rangeDifference < rangeDifferenceLimit) return true;
    else return false;
  }


  // This is a util method for simplifyByMedian().
  // It gets the size of the quadrants, builds the quadrants, and checks if their range is
  // good enough to qualify using median on the teacherFrames arraylist.
  // If it is, it returns true.
  // @param: list of teacher frames
  private static boolean areQuadrantsOfTeacherFramesEvenlySplit(ArrayList<Integer> teacherFrames) {
    // split into quadrants
    quadrantSize = getQuadrantSize(teacherFrames);
    if (quadrantSize > 1) {
        ArrayList<Integer> quadrantOne = buildQuadrantOfTeacherFrames(teacherFrames, 1);
        ArrayList<Integer> quadrantTwo = buildQuadrantOfTeacherFrames(teacherFrames, 2);
        ArrayList<Integer> quadrantThree = buildQuadrantOfTeacherFrames(teacherFrames, 3);
        ArrayList<Integer> quadrantFour = buildQuadrantOfTeacherFrames(teacherFrames, 4);
        // check the range of 1st quadrant vs 4th quadrant and 2nd quadrant vs 3rd quadrant
        boolean rangeOfQuadrantsOneAndFour = rangeOfQuadrants(quadrantOne, quadrantFour);
        boolean rangeOfQuadrantsTwoAndThree = rangeOfQuadrants(quadrantTwo, quadrantThree);
        // if both ranges are true (i.e. they are evenly split on a Guassian curve), return true
        if (rangeOfQuadrantsOneAndFour && rangeOfQuadrantsTwoAndThree) return true;
        else return false;
    } else return true;
  }


  // This method is a util method for simplifyByMedian().
  // It returns the median for the list of teacher frames.
  // @param: list of teacher frames
  private static int getMedian(ArrayList<Integer> teacherFrames) {
    Collections.sort(teacherFrames);
    int median = 0;
    if (teacherFrames.size() == 1) median = teacherFrames.get(0);
    median = teacherFrames.size() / 2;
    if (teacherFrames.size() % 2 == 1 && teacherFrames.size() != 1)
      median = (teacherFrames.get(median) + teacherFrames.get(median + 1)) / 2;
    else if (median == 0)
      median = teacherFrames.get(median);
    return median;
  }


  // This is a util method for simplifyFrameMap() and medianFilter().
  // This method takes an arrayList of teacher frames for one
  // index if the values are evenly split on a Guassian curve,
  // it returns the median of the teacher frames
  // if the values are not evenly split, it returns an
  // arbitrary reject number
  // @param: list of teacher frames
  private static int simplifyByMedian(ArrayList<Integer> teacherFrames) {
    int returnValue;
    if (areQuadrantsOfTeacherFramesEvenlySplit(teacherFrames)) returnValue = getMedian(teacherFrames);
    else returnValue = arbitraryRejectNum;
    return returnValue;
  }


  // This is a util method for simplifyFrameMap().
  // This method finds the mean in the list of teacher frames for one
  // index and returns it.
  // @param: list of teacher frames
  private static int simplifyByMean(ArrayList<Integer> teacherFrames) {
    int sum = 0;
    if (teacherFrames.size() == 0) System.out.println("teacherFrames is empty.");
    for (Integer frame:teacherFrames)
      sum += frame;
    int average = sum / teacherFrames.size();
    return average;
  }


  // @param: HashMap where the key = frame # and value = tsJ frames
  private static int[] simplifyFrameMap(HashMap<Integer, ArrayList<Integer>> frameMap) {
    simplifiedFrameArray = new int[frameMap.size()];
    Arrays.fill(simplifiedFrameArray, arbitraryRejectNum);
    for (int i = 0; i < frameMap.size(); i++) {
      ArrayList<Integer> teacherFrames = frameMap.get(i);
      int modeNum = simplifyByMode(teacherFrames);
      if (simplifiedFrameArray[i] == arbitraryRejectNum) simplifiedFrameArray[i] = modeNum;
      int medianNum = simplifyByMedian(teacherFrames);
      if (simplifiedFrameArray[i] == arbitraryRejectNum) simplifiedFrameArray[i] = medianNum;
      int meanNum = simplifyByMean(teacherFrames);
      if (simplifiedFrameArray[i] == arbitraryRejectNum) simplifiedFrameArray[i] = meanNum;
    }
    // if (simplifiedFrameArray == null || simplifiedFrameArray.length == 0) { return new int[1]; }
    return simplifiedFrameArray;
  }
}