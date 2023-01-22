import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeSeries {
    private static final int[] ZERO_ARRAY = new int[0];
    private static final boolean DEFAULT_IS_TIME_1ST_COL = true;
    private static final char DEFAULT_DELIMITER = ',';
    private static final boolean DEFAULT_IS_LABELED = true;
    private final ArrayList<String> labels;
    private final ArrayList<Double> timeReadings;
    private final ArrayList<TSPoint> tsArray;

    public TimeSeries(double[] x) {
        AtomicInteger counter = new AtomicInteger();
        TSPoint tsValues;
        timeReadings = new ArrayList<>();
        ArrayList<Double> values;
        labels = new ArrayList<>();
        labels.add("Time");
        labels.add("c1");
        tsArray = new ArrayList<>();
        for (double value : x) {
            timeReadings.add((double) counter.getAndIncrement());
            values = new ArrayList<>();
            values.add(value);
            tsValues = new TSPoint(values);
            tsArray.add(tsValues);
        }
    }
    
    TimeSeries() {
        labels = new ArrayList<>();
        timeReadings = new ArrayList<>();
        tsArray = new ArrayList<>();
    }

    public TimeSeries(int numOfDimensions) {
        this();
        labels.add("Time");
        for (int x = 0; x < numOfDimensions; x++) labels.add("" + x);
    }

    public TimeSeries(TimeSeries origTS) {
        labels = new ArrayList<>(origTS.labels);
        timeReadings = new ArrayList<>(origTS.timeReadings);
        tsArray = new ArrayList<>(origTS.tsArray);
    }

    public TimeSeries(String inputFile, boolean isFirstColTime) { this(inputFile, ZERO_ARRAY, isFirstColTime); }
    public TimeSeries(String inputFile, char delimiter) { this(inputFile, ZERO_ARRAY, DEFAULT_IS_TIME_1ST_COL, DEFAULT_IS_LABELED, delimiter); }
    public TimeSeries(String inputFile, boolean isFirstColTime, char delimiter) { this(inputFile, ZERO_ARRAY, isFirstColTime, DEFAULT_IS_LABELED, delimiter); }
    public TimeSeries(String inputFile, boolean isFirstColTime, boolean isLabeled, char delimiter) { this(inputFile, ZERO_ARRAY, isFirstColTime, isLabeled, delimiter); }
    public TimeSeries(String inputFile, int[] colToInclude, boolean isFirstColTime) { this(inputFile, colToInclude, isFirstColTime, DEFAULT_IS_LABELED, DEFAULT_DELIMITER); }

    public TimeSeries(String inputFile, int[] colToInclude, boolean isFirstColTime, boolean isLabeled, char delimiter) {
        this();
        try {
            // Record the Label names (fropm the top row.of the input file).
            BufferedReader br = new BufferedReader(new FileReader(inputFile));  // open the input file
            String line = br.readLine();  // the top row that contains attribiute names.
            StringTokenizer st = new StringTokenizer(line, String.valueOf(delimiter));

            if (isLabeled) {
                int currentCol = 0;
                while (st.hasMoreTokens()) {
                    final String currentToken = st.nextToken();
                    if ((colToInclude.length == 0) || (contains(colToInclude, currentCol))) labels.add(currentToken);
                    currentCol++;
                }

                // Make sure that the first column is labeled is for Time.
                if (labels.size() == 0) {
                    br.close();
                    throw new InternalError("ERROR:  The first row must contain label " + "information, it is empty!");
                }
                else if (!isFirstColTime) labels.add(0, "Time");
                else if (isFirstColTime && !((String) labels.get(0)).equalsIgnoreCase("Time")) {
                    br.close();
                    throw new InternalError("ERROR:  The time column (1st col) in a time series must be labeled as 'Time', '" + labels.get(0) + "' was found instead");
                }
            } else {
                if ((colToInclude == null) || (colToInclude.length == 0)) {
                    labels.add("Time");
                    if (isFirstColTime)
                        st.nextToken();
                    int currentCol = 1;
                    while (st.hasMoreTokens()) {
                        st.nextToken();
                        labels.add(new String("c" + currentCol++));
                    }
                } else {
                    java.util.Arrays.sort(colToInclude);
                    labels.add("Time");
                    for (int c = 0; c < colToInclude.length; c++)
                        if (colToInclude[c] > 0) labels.add(new String("c" + c));
                }
                br.close();
                br = new BufferedReader(new FileReader(inputFile));
            }
            // Read in all of the values in the data file.
            while ((line = br.readLine()) != null) {   // read lines until end of file
                if (line.length() > 0) { // ignore empty lines
                    st = new StringTokenizer(line, String.valueOf(delimiter));
                    // Read all currentLineValues in the current line
                    final ArrayList<Double> currentLineValues = new ArrayList<>();
                    int currentCol = 0;
                    while (st.hasMoreTokens()) {
                        final String currentToken = st.nextToken();
                        if ((colToInclude.length == 0) || (contains(colToInclude, currentCol))) {
                            final Double nextValue;
                            try {
                                nextValue = Double.valueOf(currentToken);
                            } catch (NumberFormatException e) {
                                throw new InternalError("ERROR:  '" + currentToken + "' is not a valid number");
                            }
                            currentLineValues.add(nextValue);
                        }
                        currentCol++;
                    }
                    if (isFirstColTime) timeReadings.add(currentLineValues.get(0));
                    else timeReadings.add(Double.valueOf(timeReadings.size()));
                    final int firstMeasurement;
                    if (isFirstColTime) firstMeasurement = 1;
                    else firstMeasurement = 0;
                    final TSPoint readings = new TSPoint(currentLineValues.subList(firstMeasurement, currentLineValues.size()));
                    tsArray.add(readings);
                }
            }
            br.close();
        } catch (FileNotFoundException e) { throw new InternalError("ERROR:  The file '" + inputFile + "' was not found."); }
        catch (IOException e) { throw new InternalError("ERROR:  Problem reading the file '" + inputFile + "'."); }
    }

    public void save(File outFile) throws IOException {
        final PrintWriter out = new PrintWriter(new FileOutputStream(outFile));
        out.write(this.toString());
        out.flush();
        out.close();
    }
    public void clear() {
        labels.clear();
        timeReadings.clear();
        tsArray.clear();
    }
    public int size() {return timeReadings.size(); }
    public int numOfPts() { return this.size(); }
    public int numOfDimensions() { return labels.size() - 1; }
    public double getTimeAtNthPoint(int n) { return ((Double) timeReadings.get(n)).doubleValue(); }
    public String getLabel(int index) { return (String) labels.get(index); }
    public String[] getLabelsArr() {
        final String[] labelArr = new String[labels.size()];
        for (int x = 0; x < labels.size(); x++)
            labelArr[x] = (String) labels.get(x);
        return labelArr;
    }
    public ArrayList<String> getLabels() { return labels; }
    public void setLabels(ArrayList<String> newLabels) {
        labels.clear();
        for (int x = 0; x < newLabels.size(); x++)
            labels.add(newLabels.get(x));
    }
    public void setLabels(String[] newLabels) {
        labels.clear();
        for (int x = 0; x < newLabels.length; x++)
            labels.add(newLabels[x]);
    }
    public double getMeasurement(int pointIndex, int valueIndex) { return ((TSPoint) tsArray.get(pointIndex)).get(valueIndex); }
    public double getMeasurement(int pointIndex, String valueLabel) {
        final int valueIndex = labels.indexOf(valueLabel);
        if (valueIndex < 0)
            throw new InternalError("ERROR:  the label '" + valueLabel + "' was not one of:  " + labels);
        return ((TSPoint) tsArray.get(pointIndex)).get(valueIndex - 1);
    }
    public double[] getMeasurementVector(int pointIndex) { return ((TSPoint) tsArray.get(pointIndex)).toArray(); }
    public void setMeasurement(int pointIndex, int valueIndex, double newValue) { ((TSPoint) tsArray.get(pointIndex)).set(valueIndex, newValue); }
    public void addFirst(double time, TSPoint values) {
        if (labels.size() != values.size() + 1)
            throw new InternalError("ERROR:  The TSPoint: " + values +
                    " contains the wrong number of values. " +
                    "expected:  " + labels.size() + ", " +
                    "found: " + values.size());

        if (time >= ((Double) timeReadings.get(0)).doubleValue())
            throw new InternalError("ERROR:  The point being inserted into the " +
                    "beginning of the time series does not have " +
                    "the correct time sequence. ");

        timeReadings.add(0, Double.valueOf(time));
        tsArray.add(0, values);
    }

    public void addLast(double time, TSPoint values) {
        if (labels.size() != values.size() + 1)
            throw new InternalError("ERROR:  The TSPoint: " + values +
                    " contains the wrong number of values. " +
                    "expected:  " + labels.size() + ", " +
                    "found: " + values.size());

        if ((this.size() > 0) && (time <= ((Double) timeReadings.get(timeReadings.size() - 1)).doubleValue()))
            throw new InternalError("ERROR:  The point being inserted at the " +
                    "end of the time series does not have " +
                    "the correct time sequence. ");

        timeReadings.add(Double.valueOf(time));
        tsArray.add(values);
    }

    public void removeFirst() {
        if (this.size() == 0)
            System.err.println("WARNING:  TSPoint:removeFirst() called on an empty time series!");
        else {
            timeReadings.remove(0);
            tsArray.remove(0);
        }
    }

    public void removeLast() {
        if (this.size() == 0)
            System.err.println("WARNING:  TSPoint:removeLast() called on an empty time series!");
        else {
            tsArray.remove(timeReadings.size() - 1);
            timeReadings.remove(timeReadings.size() - 1);
        }
    }

    public void normalize() {
        // Calculate the mean of each FD.
        final double[] mean = new double[this.numOfDimensions()];
        for (int col = 0; col < numOfDimensions(); col++) {
            double currentSum = 0.0;
            for (int row = 0; row < this.size(); row++)
                currentSum += this.getMeasurement(row, col);

            mean[col] = currentSum / this.size();
        }

        final double[] stdDev = new double[numOfDimensions()];
        for (int col = 0; col < numOfDimensions(); col++) {
            double variance = 0.0;
            for (int row = 0; row < this.size(); row++)
                variance += Math.abs(getMeasurement(row, col) - mean[col]);

            stdDev[col] = variance / this.size();
        }


        // Normalize the values in the data using the mean and standard deviation
        //    for each FD.  =>  Xrc = (Xrc-Mc)/SDc
        for (int row = 0; row < this.size(); row++) {
            for (int col = 0; col < numOfDimensions(); col++) {
                // Normalize data point.
                if (stdDev[col] == 0.0)   // prevent divide by zero errors
                    setMeasurement(row, col, 0.0);  // stdDev is zero means all pts identical
                else   // typical case
                    setMeasurement(row, col, (getMeasurement(row, col) - mean[col]) / stdDev[col]);
            }
        }
    }

    public String toString() {
        final StringBuffer outStr = new StringBuffer();

        // Write the data for each row.
        for (int r = 0; r < timeReadings.size(); r++) {

            // The rest of the value on the row.
            final TSPoint values = (TSPoint) tsArray.get(r);
            for (int c = 0; c < values.size(); c++)
                outStr.append(values.get(c));

            if (r < timeReadings.size() - 1)
                outStr.append("\n");
        }

        return outStr.toString();
    }

    protected void setMaxCapacity(int capacity) {
        this.timeReadings.ensureCapacity(capacity);
        this.tsArray.ensureCapacity(capacity);
    }

    private static boolean contains(int arr[], int val) {
        for (int x=0; x<arr.length; x++)
           if (arr[x] == val) return true;
        return false;
    }
}