import java.util.*;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class DTW {

   public static WarpPath getWarpPathBetween(TimeSeries tsI, TimeSeries tsJ, int searchRadius, DistanceFunction distFn) {
       return recursiveDTW(tsI, tsJ, searchRadius, distFn).getPath();
   }
   public static TimeWarpInfo getWarpInfoBetween(TimeSeries tsI, TimeSeries tsJ, int searchRadius, DistanceFunction distFn) {
       return recursiveDTW(tsI, tsJ, searchRadius, distFn);
   }

   private static TimeWarpInfo recursiveDTW(TimeSeries tsI, TimeSeries tsJ, int searchRadius, DistanceFunction distFn) {
      if (searchRadius < 0) searchRadius = 0;
      final int minTSsize = searchRadius + 2;
      if ( (tsI.size() <= minTSsize) || (tsJ.size() <= minTSsize) ) { return timeWarp(tsI, tsJ, distFn); }
      else {
         final double resolutionFactor = 2.0;

         final PAA shrunkI = new PAA(tsI, (int)(tsI.size()/resolutionFactor));
         final PAA shrunkJ = new PAA(tsJ, (int)(tsJ.size()/resolutionFactor));

          // Determine the search window that constrains the area of the cost matrix that will be evaluated based on the warp path found at the previous resolution (smaller time series).
          final SearchWindow window = new ExpandedResWindow(tsI, tsJ, shrunkI, shrunkJ, getWarpPathBetween(shrunkI, shrunkJ, searchRadius, distFn), searchRadius);
         // Find the optimal warp path through this search window constraint.
         return getWarpInfoBetween(tsI, tsJ, window, distFn);
      }
   }

  public static double calcWarpCost(WarpPath path, TimeSeries tsI, TimeSeries tsJ, DistanceFunction distFn) {
      double totalCost = 0.0;
      for (int p=0; p<path.size(); p++) {
         final ColMajorCell currWarp = path.get(p);
         totalCost += distFn.calcDistance(tsI.getMeasurementVector(currWarp.getCol()), tsJ.getMeasurementVector(currWarp.getRow()));
      }
      return totalCost;
  }

  private static TimeWarpInfo timeWarp(TimeSeries tsI, TimeSeries tsJ, DistanceFunction distFn) {
    //     COST MATRIX:
    //   5|_|_|_|_|_|_|E| E = min Global Cost
    //   4|_|_|_|_|_|_|_| S = Start point
    //   3|_|_|_|_|_|_|_| each cell = min global cost to get to that point
    // j 2|_|_|_|_|_|_|_|
    //   1|_|_|_|_|_|_|_|
    //   0|S|_|_|_|_|_|_|
    //     0 1 2 3 4 5 6
    //            i
    //   access is M(i,j)... column-row

    final double[][] costMatrix = new double[tsI.size()][tsJ.size()];
    final int maxI = tsI.size()-1;
    final int maxJ = tsJ.size()-1;

    costMatrix[0][0] = distFn.calcDistance(tsI.getMeasurementVector(0), tsJ.getMeasurementVector(0));
    for (int j=1; j<=maxJ; j++) costMatrix[0][j] = costMatrix[0][j-1] + distFn.calcDistance(tsI.getMeasurementVector(0), tsJ.getMeasurementVector(j));
    for (int i=1; i<=maxI; i++) {  // i = columns
        costMatrix[i][0] = costMatrix[i-1][0] + distFn.calcDistance(tsI.getMeasurementVector(i), tsJ.getMeasurementVector(0));
        for (int j=1; j<=maxJ; j++) { // j = rows
          final double minGlobalCost = Math.min(costMatrix[i-1][j], Math.min(costMatrix[i-1][j-1], costMatrix[i][j-1]));
          costMatrix[i][j] = minGlobalCost + distFn.calcDistance(tsI.getMeasurementVector(i), tsJ.getMeasurementVector(j));
        }
    }

    final double minimumCost = costMatrix[maxI][maxJ];

    // Find the Warp Path by searching the matrix from the solution at
    //    (maxI, maxJ) to the beginning at (0,0).  At each step move through
    //    the matrix 1 step left, down, or diagonal, whichever has the
    //    smallest cost.  Favor diagonal moves and moves towards the i==j
    //    axis to break ties.
    final WarpPath minCostPath = new WarpPath(maxI+maxJ-1);
    int i = maxI;
    int j = maxJ;
    minCostPath.addFirst(i, j);
    while ((i>0) || (j>0)) {
      // Find the costs of moving in all three possible directions (left,
      //    down, and diagonal (down and left at the same time).
        final double diagCost;
        final double leftCost;
        final double downCost;

        if ((i>0) && (j>0)) diagCost = costMatrix[i-1][j-1];
        else diagCost = Double.POSITIVE_INFINITY;
        if (i > 0) leftCost = costMatrix[i-1][j];
        else leftCost = Double.POSITIVE_INFINITY;
        if (j > 0) downCost = costMatrix[i][j-1];
        else downCost = Double.POSITIVE_INFINITY;
        // Determine which direction to move in.  Prefer moving diagonally and
        //    moving towards the i==j axis of the matrix if there are ties.
        if ((diagCost<=leftCost) && (diagCost<=downCost))
        {
          i--;
          j--;
        }
        else if ((leftCost<diagCost) && (leftCost<downCost)) i--;
        else if ((downCost<diagCost) && (downCost<leftCost)) j--;
        else if (i <= j) j--;  // leftCost==rightCost > diagCost
        else i--;  // leftCost==rightCost > diagCost
         
        // Add the current step to the warp path.
        minCostPath.addFirst(i, j);
    }

    // <<I index, J index>, cost>
    HashMap<ArrayList<Integer>, Double> costMatrixMap = new HashMap<>();
    for (int a = 0; a < tsI.size(); a++) {
      for (int b = 0; b < tsJ.size(); b++) {
        ArrayList<Integer> indexes = new ArrayList<>();
        indexes.add(a);
        indexes.add(b);
        costMatrixMap.put(indexes, costMatrix[a][b]);
      }
    } try {
      FileOutputStream fos = new FileOutputStream("costMatrixHashmap.ser");
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(costMatrixMap);
      oos.close();
      fos.close();
    } catch (IOException ioe) { ioe.printStackTrace(); }
    return new TimeWarpInfo(minimumCost, minCostPath);
  }

  public static double getWarpDistBetween(TimeSeries tsI, TimeSeries tsJ, SearchWindow window, DistanceFunction distFn) {
    //     COST MATRIX:
    //   5|_|_|_|_|_|_|E| E = min Global Cost
    //   4|_|_|_|_|_|_|_| S = Start point
    //   3|_|_|_|_|_|_|_| each cell = min global cost to get to that point
    // j 2|_|_|_|_|_|_|_|
    //   1|_|_|_|_|_|_|_|
    //   0|S|_|_|_|_|_|_|
    //     0 1 2 3 4 5 6
    //            i
    //   access is M(i,j)... column-row
    final CostMatrix costMatrix = new PartialWindowMatrix(window);
    final int maxI = tsI.size()-1;
    final int maxJ = tsJ.size()-1;

    // Get an iterator that traverses the window cells in the order that the cost matrix is filled. (first to last row (1..maxI), bottom to top (1..MaxJ)
    final Iterator<Object> matrixIterator = window.iterator();
    while (matrixIterator.hasNext()) {
      final ColMajorCell currentCell = (ColMajorCell)matrixIterator.next();  // current cell being filled
      final int i = currentCell.getCol();
      final int j = currentCell.getRow();
      if ( (i==0) && (j==0) )      // bottom left cell (first row AND first column)
            costMatrix.put(i, j, distFn.calcDistance(tsI.getMeasurementVector(0), tsJ.getMeasurementVector(0)));
        else if (i == 0) // first column
        {
          costMatrix.put(i, j, distFn.calcDistance(tsI.getMeasurementVector(0), tsJ.getMeasurementVector(j)) + costMatrix.get(i, j-1));
        }
        else if (j == 0) // first row
        {
          costMatrix.put(i, j, distFn.calcDistance(tsI.getMeasurementVector(i), tsJ.getMeasurementVector(0)) + costMatrix.get(i-1, j));
        }
        else // not first column or first row
        {
          final double minGlobalCost = Math.min(costMatrix.get(i-1, j), Math.min(costMatrix.get(i-1, j-1), costMatrix.get(i, j-1)));
          costMatrix.put(i, j, minGlobalCost + distFn.calcDistance(tsI.getMeasurementVector(i), tsJ.getMeasurementVector(j)));
        }
      }
      // min cost is at (maxI, maxJ)
      return costMatrix.get(maxI, maxJ);
   }
  public static TimeWarpInfo getWarpInfoBetween(TimeSeries tsI, TimeSeries tsJ, SearchWindow window, DistanceFunction distFn) { return constrainedWarp(tsI, tsJ, window, distFn); }
  private static TimeWarpInfo constrainedWarp(TimeSeries tsI, TimeSeries tsJ, SearchWindow window, DistanceFunction distFn) {
    //     COST MATRIX:
    //   5|_|_|_|_|_|_|E| E = min Global Cost
    //   4|_|_|_|_|_|_|_| S = Start point
    //   3|_|_|_|_|_|_|_| each cell = min global cost to get to that point
    // j 2|_|_|_|_|_|_|_|
    //   1|_|_|_|_|_|_|_|
    //   0|S|_|_|_|_|_|_|
    //     0 1 2 3 4 5 6
    //            i
    //   access is M(i,j)... column-row
    final WindowMatrix costMatrix = new WindowMatrix(window);
    final int maxI = tsI.size()-1;
    final int maxJ = tsJ.size()-1;

    // Get an iterator that traverses the window cells in the order that the cost matrix is filled.
    //    (first to last row (1..maxI), bottom to top (1..MaxJ)
    final Iterator<Object> matrixIterator = window.iterator();
    while (matrixIterator.hasNext()) {
      final ColMajorCell currentCell = (ColMajorCell)matrixIterator.next();  // current cell being filled
      final int i = currentCell.getCol();
      final int j = currentCell.getRow();

      if ( (i==0) && (j==0) ) // bottom left cell (first row AND first column)
        costMatrix.put(i, j, distFn.calcDistance(tsI.getMeasurementVector(0), tsJ.getMeasurementVector(0)));
      else if (i == 0)        // first column
      {
        costMatrix.put(i, j, distFn.calcDistance(tsI.getMeasurementVector(0), tsJ.getMeasurementVector(j)) + costMatrix.get(i, j-1));
      }
      else if (j == 0)        // first row
      {
        costMatrix.put(i, j, distFn.calcDistance(tsI.getMeasurementVector(i), tsJ.getMeasurementVector(0)) + costMatrix.get(i-1, j));
      }
      else                    // not first column or first row
      {
        final double minGlobalCost = Math.min(costMatrix.get(i-1, j), Math.min(costMatrix.get(i-1, j-1), costMatrix.get(i, j-1)));
        costMatrix.put(i, j, minGlobalCost + distFn.calcDistance(tsI.getMeasurementVector(i), tsJ.getMeasurementVector(j)));
      }
    }
    // Minimum Cost is at (maxI, maxJ)
    final double minimumCost = costMatrix.get(maxI, maxJ);

    // Find the Warp Path by searching the matrix from the solution at
    //    (maxI, maxJ) to the beginning at (0,0).  At each step move through
    //    the matrix 1 step left, down, or diagonal, whichever has the
    //    smallest cost.  Favoer diagonal moves and moves towards the i==j
    //    axis to break ties.
    final WarpPath minCostPath = new WarpPath(maxI+maxJ-1);
    int i = maxI;
    int j = maxJ;
    minCostPath.addFirst(i, j);
    while ((i>0) || (j>0))
    {
    // Find the costs of moving in all three possible directions (left,
    //    down, and diagonal (down and left at the same time).
    final double diagCost;
    final double leftCost;
    final double downCost;

    if ((i>0) && (j>0)) diagCost = costMatrix.get(i-1, j-1);
    else diagCost = Double.POSITIVE_INFINITY;
    if (i > 0) leftCost = costMatrix.get(i-1, j);
    else leftCost = Double.POSITIVE_INFINITY;
    if (j > 0) downCost = costMatrix.get(i, j-1);
    else downCost = Double.POSITIVE_INFINITY;
    // Determine which direction to move in.  Prefer moving diagonally and
    //    moving towards the i==j axis of the matrix if there are ties.
    if ((diagCost<=leftCost) && (diagCost<=downCost)) {
            i--;
            j--;
    }
    else if ((leftCost<diagCost) && (leftCost<downCost)) i--;
    else if ((downCost<diagCost) && (downCost<leftCost)) j--;
    else if (i <= j)  // leftCost==rightCost > diagCost
      j--;
    else   // leftCost==rightCost > diagCost
      i--;
    // Add the current step to the warp path.
    minCostPath.addFirst(i, j);
    }  // end while loop

    // Free any rescources associated with the costMatrix (a swap file may have been created if the swa file did not fit into main memory).
    costMatrix.freeMemory();  
    return new TimeWarpInfo(minimumCost, minCostPath);
   }
}