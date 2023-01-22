public class EuclideanDistance implements DistanceFunction {
   public EuclideanDistance() {}
   public double calcDistance(double[] vector1, double[] vector2) {
      if (vector1.length != vector2.length) throw new InternalError("ERROR:  cannot calculate the distance between vectors of different sizes.");
      double sqSum = 0.0;
      for (int x=0; x < vector1.length; x++)
          sqSum += Math.pow(vector1[x]-vector2[x], 2.0);
      return Math.sqrt(sqSum);
   }
}