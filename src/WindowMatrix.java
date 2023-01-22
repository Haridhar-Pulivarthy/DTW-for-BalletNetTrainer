class WindowMatrix implements CostMatrix {
  private CostMatrix windowCells;

  WindowMatrix(SearchWindow searchWindow) {
    try { windowCells = new MemMatrix(searchWindow); }
    catch (OutOfMemoryError e) { System.err.println("Ran out of memory. The program will use a swap file instead.");
      System.gc();
      windowCells = new SwapFileMatrix(searchWindow);
    }
  }

  public void put(int col, int row, double value) { windowCells.put(col, row, value); }
  public double get(int col, int row) { return windowCells.get(col, row); }
  public int size() { return windowCells.size(); }

  public void freeMemory() {
    // Resources freed for SwapFileMatrix.
    if (windowCells instanceof SwapFileMatrix) {
      try { ((SwapFileMatrix)windowCells).freeMemory(); }
      catch (Throwable t) {}
    }
  }
}