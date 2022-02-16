package org.jesperancinha.google.api;


import org.javatuples.Pair;
import org.joda.time.DateTimeZone;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.Date;

public class GoogleAPI {

    private static final String OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";
    private static final String ZERO_RESULTS = "ZERO_RESULTS";
    public static final int TIMEOUT_MILLIS = 5000;
    public static final String POST = "POST";
    public static final String CONTENT_TYPE = "Content-type";
    public static final String TEXT_PLAIN = "text/plain";
    public static final int TRYES = 2;
    public static final String DATA_NOT_SENT_HTTP_CODE = "Data NOT sent HTTP code=";
    public static final String RESPONSE_MSG = " response msg=";

    /**
     * Gets the longitude / latitude from the google api
     *
     * @return
     * @throws Exception
     */
    private static Pair<String, String> getCoordinates(
            String geographiclocation, String countrycode) throws Exception {

        if (null != countrycode) {
            geographiclocation = geographiclocation + " " + countrycode;
        }
        int tries = TRYES;
        String status = null;
        String xmlGeoCoordinates = null;
        do {

            geographiclocation = Normalizer.normalize(geographiclocation,
                    Normalizer.Form.NFD);
            geographiclocation = geographiclocation.replaceAll("[^\\p{ASCII}]",
                    "");

            geographiclocation = geographiclocation.replace(" ", "+");

            String urlCoordinates1 = "http://maps.googleapis.com/maps/api/geocode/xml?";
            String urlCoordinates2CountryCode = "address=%s&region=%s&sensor=false";
            String urlCoordinates2NoCountryCode = "address=%s&sensor=false";
            String address2 = null;

            if (null == countrycode) {
                address2 = String.format(urlCoordinates2NoCountryCode,
                        geographiclocation);
            } else {
                address2 = String.format(urlCoordinates2CountryCode,
                        geographiclocation, countrycode.toUpperCase());
            }
            URL urlToConnecto = new URL(urlCoordinates1.concat(address2));

            HttpURLConnection con = (HttpURLConnection) urlToConnecto
                    .openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty(CONTENT_TYPE, TEXT_PLAIN);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod(POST);
            con.setConnectTimeout(TIMEOUT_MILLIS);

            OutputStream outputSteam = con.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(outputSteam);
            oos.flush();
            oos.close();

            InputStream in = con.getInputStream();
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String testSring =  reader.readLine();
            sb.append(testSring);
            while (null!=testSring) {
                testSring = reader.readLine();
                if(null != testSring)
                {
                    sb.append(testSring);
                }
            }
             xmlGeoCoordinates = sb.toString();
            in.close();
            reader.close();

            int response = con.getResponseCode();
            String resMsg = con.getResponseMessage();
            if (response != HttpURLConnection.HTTP_OK) {
                throw new Exception(DATA_NOT_SENT_HTTP_CODE + response
                        + RESPONSE_MSG + resMsg);
            }

            con.disconnect();
            status = getInnerResult(xmlGeoCoordinates.split("status")[1]);
            if (status.equals(ZERO_RESULTS)
                    && geographiclocation.contains("+")) {
                geographiclocation = geographiclocation
                        .substring(geographiclocation.indexOf('+'));
            }

        } while ((status.equals(OVER_QUERY_LIMIT) || status.equals(ZERO_RESULTS)) && --tries > 0);

        if (tries <= 0) {
            throw new Exception(OVER_QUERY_LIMIT);
        }
        String location = xmlGeoCoordinates.split("geometry")[1].split("location")[1];

        String lat = getInnerResult(location.split("lat")[1]);
        String Lng = getInnerResult(location.split("lng")[1]);

        return new Pair<String, String>(lat, Lng);

    }

    private static String getInnerResult(String toParse) {

        return toParse.replace("</", "").replace(">", "");
    }


    /**
     * Gets the timezone according to geo coordinates
     *
     * @return
     * @throws Exception
     */
    private static DateTimeZone getTimeZoneByCoord(String latitud,
                                                   String longitud) throws Exception {

        int tries = TRYES;
        String status = null;
        String xmlGeoTimeZone = null;
        do {
            String timestamp = String.valueOf((new Date()).getTime() / 1000);
            String urlCoordinates1 = "https://maps.googleapis.com/maps/api/timezone/xml?";
            String urlCoordinates2 = "location=%s,%s&timestamp=%s&sensor=false";
            String address2 = String.format(urlCoordinates2, latitud, longitud,
                    timestamp);
            URL urlToConnecto = new URL(urlCoordinates1.concat(address2));

            HttpURLConnection con = (HttpURLConnection) urlToConnecto
                    .openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-type", TEXT_PLAIN);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod(POST);
            con.setConnectTimeout(TIMEOUT_MILLIS);

            OutputStream outputSteam = con.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(outputSteam);

            oos.flush();
            oos.close();

            InputStream in = con.getInputStream();
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String testSring =  reader.readLine();
            sb.append( testSring);
            while (null!=testSring) {
                testSring = reader.readLine();
                if(null != testSring)
                {
                    sb.append(testSring);
                }
            }
            xmlGeoTimeZone = sb.toString();
            in.close();
            reader.close();


            int response = con.getResponseCode();
            String resMsg = con.getResponseMessage();
            if (response != HttpURLConnection.HTTP_OK) {
                throw new Exception(DATA_NOT_SENT_HTTP_CODE + response
                        + RESPONSE_MSG + resMsg);
            }
            status = getInnerResult(xmlGeoTimeZone.split("status")[1]);
            con.disconnect();
        } while (status.equals(OVER_QUERY_LIMIT)
                && --tries > 0);
        if (tries <= 0) {
            throw new Exception(OVER_QUERY_LIMIT);
        }
        String time_zone_id = getInnerResult(xmlGeoTimeZone.split("time_zone_id")[1]);
        return DateTimeZone.forID(time_zone_id);

    }

    /**
     * Gets the timezone via the location added It uses two webservices provided
     * by the Goolgle API
     *
     * @param location
     * @return
     * @throws Exception
     */
    public static DateTimeZone getTimeZone(String location, String countrycode)
            throws Exception {
        Pair<String, String> coordinate = GoogleAPI.getCoordinates(location,
                countrycode);
        DateTimeZone timezone = GoogleAPI.getTimeZoneByCoord(
                coordinate.getValue0(), coordinate.getValue1());
        return timezone;

    }
}
//