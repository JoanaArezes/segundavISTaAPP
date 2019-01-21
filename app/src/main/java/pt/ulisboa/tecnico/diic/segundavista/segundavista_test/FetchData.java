package pt.ulisboa.tecnico.diic.segundavista.segundavista_test;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FetchData extends AsyncTask<Void, Void, Void> {
    String data = "";
    String parsedData = "";
    //String singleParsed = "";

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            // Connection
            URL url = new URL("https://segundavista.azurewebsites.net/api/BeaconApi");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Accept", "application/json");

            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line = "";

            while (line != null) {
                line = bufferedReader.readLine();
                data = data + line;
            }

            JSONArray JA = new JSONArray(data);
            for (int i = 0; i < JA.length(); i++) {
                JSONObject JO = (JSONObject) JA.get(i);
                Beacon beacon = new Beacon();

                beacon.setBeaconID(JO.get("BeaconID").toString());
                beacon.setName(JO.get("Name").toString() + ".");

                beacon.setExtraInfo(JO.get("ExtraInfo").toString());
                beacon.setFunFacts(JO.get("FunFacts").toString());
                beacon.setSoundURL(JO.get("Sound").toString());

                beacon.setQuestion1(JO.get("Question_1").toString());
                beacon.setQuestion2(JO.get("Question_2").toString());
                beacon.setQuestion3(JO.get("Question_3").toString());

                beacon.setAnswer1(JO.get("Answer_1").toString());
                beacon.setAnswer2(JO.get("Answer_2").toString());
                beacon.setAnswer3(JO.get("Answer_3").toString());

                beacon.setExtraAnswer1(JO.get("ExtraAnswer_1").toString());
                beacon.setExtraAnswer2(JO.get("ExtraAnswer_2").toString());
                beacon.setExtraAnswer3(JO.get("ExtraAnswer_3").toString());

                if (!MainActivity.beaconMap.containsValue(beacon)) {
                    MainActivity.beaconMap.put(beacon.getBeaconID(), beacon);
                    //Log.d("fetchData", beacon.getName() + " " + beacon.getBeaconID());
                    Log.d("fetchData", MainActivity.beaconMap.get(beacon.getBeaconID()).getName() +  " id: " + beacon.getBeaconID());
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        //MainActivity.currentBeacon = MainActivity.beaconMap.get("beacon 4QeR");
        //parsedData = "Name:" + MainActivity.beaconMap.get("beacon 4QeR").getName()+ "\n" + MainActivity.beaconMap.get("beacon 4QeR").getExtraInfo();
        //MainActivity.data.setText(this.parsedData);
    }
}
