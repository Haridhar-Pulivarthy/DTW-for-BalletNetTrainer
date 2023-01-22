public class PAA extends TimeSeries {

   private int[] aggregatePointSize;
   private final int origLen;

   public PAA(TimeSeries ts, int shrunkSize) {
      if (shrunkSize > ts.size())
         throw new InternalError("ERROR in PAA");
      if (shrunkSize <= 0)
         throw new InternalError("ERROR in PAA");
      this.origLen = ts.size();
      this.aggregatePointSize = new int[shrunkSize];
      this.setLabels(ts.getLabels());
      final double reducedPointSize = (double)ts.size()/(double)shrunkSize;
      int pointToReadFrom = 0;
      int pointToReadTo;
      while (pointToReadFrom < ts.size()) {
         pointToReadTo = (int) Math.round(reducedPointSize * (this.size() + 1)) - 1;
         final int pointsToRead = pointToReadTo - pointToReadFrom + 1;
         double timeSum = 0.0;
         final double[] measurementSums = new double[ts.numOfDimensions()];
         // Sum all of the values over the range pointToReadFrom...pointToReadFrom.
         for (int point=pointToReadFrom; point <= pointToReadTo; point++) {
            final double[] currentPoint = ts.getMeasurementVector(point);
            timeSum += ts.getTimeAtNthPoint(point);
            for (int dim=0; dim<ts.numOfDimensions(); dim++)
               measurementSums[dim] += currentPoint[dim];
         }
         // Determine the average value
         timeSum = timeSum / pointsToRead;
         for (int dim=0; dim<ts.numOfDimensions(); dim++)
               measurementSums[dim] = measurementSums[dim] / pointsToRead;   // find the average of each measurement
         // Add the computed average value to the aggregate approximation.
         this.aggregatePointSize[super.size()] = pointsToRead;
         this.addLast(timeSum, new TSPoint(measurementSums));

         pointToReadFrom = pointToReadTo + 1; // next window of points to average startw where the last window ended
      }
   }

   public int originalSize() { return origLen; }
   public int aggregatePointSize(int pointIndex) { return aggregatePointSize[pointIndex]; }
   public String toString() { return "(" + this.origLen + " point time series represented as " + this.size() + " points)\n" + super.toString(); }
}