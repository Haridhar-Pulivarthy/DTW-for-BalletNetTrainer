import java.io.FileWriter;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

public class ExternalFile {
    public static void createJSON(String fileName) throws JSONException {
        JSONObject body = new JSONObject();
        int[][] bodyArray = new int[2][Body.finalFrameArray.length];
        for (int i = 0; i < Body.finalFrameArray.length; i++) {
            bodyArray[0][i] = i;
            bodyArray[1][i] = Body.finalFrameArray[i];
        }
        body.put("Index X of the first array, which represents the amateur video, correlates to Index X of the second array, which represents the professional video.", bodyArray);
        try (FileWriter file = new FileWriter(fileName)) {
            file.write(body.toString());
            file.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }
}