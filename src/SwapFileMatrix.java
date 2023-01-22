import java.io.RandomAccessFile;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Random;

class SwapFileMatrix implements CostMatrix {
   private static final double OUT_OF_WINDOW_VALUE = Double.POSITIVE_INFINITY;
   private static final Random RAND_GEN = new Random();
   private final SearchWindow window;

   // Private data needed to store the last 2 columns of the matrix.
   private double[] lastCol;
   private double[] currCol;
   private int currColIndex;
   private int minLastRow;
   private int minCurrRow;

   // Private data needed to read values from the swap file.
   private final File swapFile;
   private final RandomAccessFile cellValuesFile;
   private boolean isSwapFileFreed;
   private final long[] colOffsets;

   SwapFileMatrix(SearchWindow searchWindow) {
      window = searchWindow;
      if (window.maxI() > 0) {
         currCol = new double[window.maxJforI(1)-window.minJforI(1)+1];
         currColIndex = 1;
         minLastRow = window.minJforI(currColIndex-1);
      }
      else currColIndex = 0; // special case for a <=1 point time series, less than 2 columns to fill in

      minCurrRow = window.minJforI(currColIndex);
      lastCol = new double[window.maxJforI(0)-window.minJforI(0)+1];

      swapFile = new File("swap" + RAND_GEN.nextLong());
      isSwapFileFreed = false;

      colOffsets = new long[window.maxI()+1];

      try { cellValuesFile = new RandomAccessFile(swapFile, "rw"); }
      catch (FileNotFoundException e) { throw new InternalError("ERROR:  Unable to create swap file: " + swapFile); }
   }

   public void put(int col, int row, double value) {
      if ( (row<window.minJforI(col)) || (row>window.maxJforI(col)) ) throw new InternalError("CostMatrix is filled in a cell (col=" + col + ", row=" + row + ") that is not in the " + "search window");
      else { if (col == currColIndex) currCol[row-minCurrRow] = value;
         else if (col == currColIndex-1) { lastCol[row-minLastRow] = value; }
         else if (col == currColIndex+1) {
            // Write the last column to the swap file.
            try {
               if (isSwapFileFreed) throw new InternalError("The SwapFileMatrix has been freed by the freeMemory() method");
               else {
                  cellValuesFile.seek(cellValuesFile.length());  // move file pointer to end of file
                  colOffsets[currColIndex-1] = cellValuesFile.getFilePointer();
                  // Write an entire column to the swap file.
                  cellValuesFile.write(doubleArrayToByteArray(lastCol));
               }
            }
            catch (IOException e) { throw new InternalError("Unable to fill the CostMatrix in the Swap file (IOException)"); }

            lastCol = currCol;
            minLastRow = minCurrRow;
            minCurrRow = window.minJforI(col);
            currColIndex++;
            currCol = new double[window.maxJforI(col)-window.minJforI(col)+1];
            currCol[row-minCurrRow] = value;
         }
         else throw new InternalError("A SwapFileMatrix can only fill in 2 adjacent columns at a time");
      }
   }

   public double get(int col, int row) {
      if ((row < window.minJforI(col)) || (row > window.maxJforI(col))) return OUT_OF_WINDOW_VALUE;
      else if (col == currColIndex) return currCol[row-minCurrRow];
      else if (col == currColIndex-1) return lastCol[row-minLastRow];
      else {
         try {
            if (isSwapFileFreed) throw new InternalError("The SwapFileMatrix has been freed by the freeMemory() method");
            else {
               cellValuesFile.seek( colOffsets[col] + 8*(row-window.minJforI(col)) );
               return cellValuesFile.readDouble();
            }
         }
         catch (IOException e) {
            if (col > currColIndex) throw new InternalError("The requested value is in the search window but has not been entered into " + "the matrix: (col=" + col + "row=" + row + ").");
            else throw new InternalError("Unable to read CostMatrix in the Swap file (IOException)");
         }
      }
   }

   // This method closes and deletes the swap file when the object's finalize() mehtod is called.  This method will ONLY be called by the JVM if the object is garbage collected while the application is still running.
   // This method must be called explicitly to guarantee that the swap file is deleted.
   protected void finalize() throws Throwable {
      // Close and Delete the (possibly VERY large) swap file.
      try { if (!isSwapFileFreed) cellValuesFile.close(); }
      catch (Exception e) { System.err.println("unable to close swap file '" + this.swapFile.getPath() + "' during finialization"); }
      finally {
         swapFile.delete();   // delete the swap file
      }
   }

   public int size() { return window.size(); }
   
   public void freeMemory() {
      try { cellValuesFile.close(); }
      catch (IOException e) { System.err.println("unable to close swap file '" + this.swapFile.getPath() + "'"); }
      finally {
         if (!swapFile.delete()) System.err.println("unable to delete swap file '" + this.swapFile.getPath() + "'");
      }
   }

   private static byte[] doubleArrayToByteArray(double[] nums) {
      final int doubleSize = 8;  // 8 byes in a double
      final byte[] byteArray = new byte[nums.length*doubleSize];  
        for (int x=0; x<nums.length; x++) System.arraycopy(doubleToByteArray(nums[x]), 0, byteArray, x*doubleSize, doubleSize);
        return byteArray;
    }

   private static byte[] doubleToByteArray(double nums) {
      // double to long representation
      long longNum = Double.doubleToLongBits(nums);
      // long to 8 bytes
      return new byte[] {(byte)((longNum >>> 56) & 0xFF),
      (byte)((longNum >>> 48) & 0xFF),
      (byte)((longNum >>> 40) & 0xFF),
      (byte)((longNum >>> 32) & 0xFF),
      (byte)((longNum >>> 24) & 0xFF),
      (byte)((longNum >>> 16) & 0xFF),
      (byte)((longNum >>>  8) & 0xFF),
      (byte)((longNum >>>  0) & 0xFF)};
   }
}