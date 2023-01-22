import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.lang.Integer;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;

public class AngleMap {
  // DATA
  public ArrayList<Integer> tsIframes = new ArrayList<>();
  public ArrayList<Integer> tsJframes = new ArrayList<>();
  private static String fileName;

  // CONSTRUCTOR
  public AngleMap(HashMap<Integer, Integer> solutionMap, String fName) {
    for (int i:solutionMap.keySet()) tsIframes.add(i);
    for (int j:solutionMap.values()) tsIframes.add(j);
    fileName = fName;
  }

  public void put(int tsIframe, int tsJframe) {
    tsIframes.add(tsIframe);
    tsJframes.add(tsJframe);
  }

  // This method serializes the 1 frame - 1 frame angle map
  public void serialize() {
    HashMap<Integer, Integer> serializableSolution = new HashMap<>();
    for (int i = 0; i < tsIframes.size(); i++) 
      serializableSolution.put((int) tsIframes.get(i),(int) tsJframes.get(i));
    try {
      FileOutputStream fos = new FileOutputStream(fileName);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(serializableSolution);
      oos.close();
      fos.close();
    } catch (IOException ioe) { ioe.printStackTrace(); }
  }

  // This method deserializes the 1 frame - 1 frame angle map to a hashmap
  @SuppressWarnings("unchecked")
  public static HashMap<Integer, Integer> deserializeToHashMap() {
    HashMap<Integer, Integer> angleHashMap = new HashMap<>();
    try {
      FileInputStream fis = new FileInputStream(fileName);
      ObjectInputStream ois = new ObjectInputStream(fis);
      angleHashMap = (HashMap<Integer, Integer>) ois.readObject();
      ois.close();
      fis.close();
    } catch (IOException ioe) {
        ioe.printStackTrace();
        System.out.println("Incomplete angle map: IOEexception");
    } catch (ClassNotFoundException c) {
        System.out.println("Class not found");
        System.out.println("Incomplete angle map: ClassNotFoundException");
        c.printStackTrace();
    }
    return angleHashMap;
  }

  // This method deserializes the 1-1 angle map to an AngleMap
  public static AngleMap deserializeToAngleMap(String fileName) {
    HashMap<Integer, Integer> h = deserializeToHashMap();
    return new AngleMap(h, fileName);
  }

    // This method shifts the indexes for the zeroes that were removed earlier to not mess up the indexes for the client.
    public void addZeroes(HashMap<Integer, ArrayList<Integer>> tsIZeroHashMap, HashMap<Integer, ArrayList<Integer>> tsJZeroHashMap, int a) {
        ArrayList<Integer> tsIZeroIndexes = tsIZeroHashMap.get(a);
        ArrayList<Integer> tsJZeroIndexes = tsJZeroHashMap.get(a);
        if (!tsIZeroIndexes.isEmpty()) {
            for (int i = 0; i < tsIframes.size(); i++) {
                if (tsIZeroIndexes.contains(i)) {
                    for (int j = i; j < tsIframes.size(); j++) {
                        int k = tsIframes.get(j);
                        tsIframes.remove(j);
                        tsIframes.add(j, k++);
                    }
                }
            }
        }
        if (!tsJZeroIndexes.isEmpty()) {
            for (int i = 0; i < tsJframes.size(); i++) {
                if (tsJZeroIndexes.contains(i)) {
                    for (int j = i; j < tsJframes.size(); j++) {
                        int k = tsJframes.get(j);
                        tsJframes.remove(j);
                        tsJframes.add(j, k++);
                    }
                }
            }
        }
  }
  
  // This method turns the AngleMap into a printable String
  public String toString() {
    StringBuffer outStr = new StringBuffer("[");
    for (int i = 0; i < tsIframes.size(); i++) {
         outStr.append("(" + tsIframes.get(i) + "," + tsJframes.get(i) + ")");
         if (i < tsIframes.size()-1) outStr.append(",");
      }
      return new String(outStr.append("]"));
   }
  
  // This method sorts the angle map according to tsIframe
  public void sort() {
    TreeMap<Integer, Integer> sorter = new TreeMap<>();
    for (int i = 0; i < tsIframes.size(); i++) sorter.put(tsIframes.get(i), tsJframes.get(i));
    tsIframes.clear();
    tsJframes.clear();
    for (int i:sorter.keySet()) {
      tsIframes.add(i);
      tsJframes.add(sorter.get(i));
    }
  }

  // This method gives the size of the angle map
  public int size() { return tsIframes.size(); }
}