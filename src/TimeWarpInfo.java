public class TimeWarpInfo {
   private final double distance;
   private final WarpPath path;
   TimeWarpInfo(double dist, WarpPath wp) {
      distance = dist;
      path = wp;
   }
   public double getDistance() { return distance; }
   public WarpPath getPath() { return path; }
   @Override
   public String toString() { return "(Warp Distance=" + distance + ", Warp Path=" + path + ")"; }
}