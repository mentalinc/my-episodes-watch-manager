package eu.vranckaert.episodeWatcher.service;

import android.arch.persistence.room.Room;
import android.util.Log;

import eu.vranckaert.episodeWatcher.constants.MyEpisodeConstants;
import eu.vranckaert.episodeWatcher.database.SeriesDAO;
import eu.vranckaert.episodeWatcher.domain.Show;
import eu.vranckaert.episodeWatcher.domain.User;
import eu.vranckaert.episodeWatcher.enums.ShowAction;
import eu.vranckaert.episodeWatcher.enums.ShowType;
import eu.vranckaert.episodeWatcher.exception.InternetConnectivityException;
import eu.vranckaert.episodeWatcher.exception.LoginFailedException;
import eu.vranckaert.episodeWatcher.exception.ShowAddFailedException;
import eu.vranckaert.episodeWatcher.exception.UnsupportedHttpPostEncodingException;
import eu.vranckaert.episodeWatcher.utils.StringUtils;



import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import eu.vranckaert.episodeWatcher.database.AppDatabase;



import java.io.BufferedReader;
import org.json.JSONObject;
import org.json.JSONException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStreamReader;
import java.io.InputStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class ShowService {
    private static final String LOG_TAG = ShowService.class.getSimpleName();

    private final UserService userService;


    public ShowService() {
        userService = new UserService();
    }

    public List<Show> searchShows(String search, User user) throws UnsupportedHttpPostEncodingException, InternetConnectivityException, LoginFailedException {
        HttpClient httpClient = new DefaultHttpClient();
        String username = user.getUsername();
        userService.login(httpClient, username, user.getPassword());

    	HttpPost post = new HttpPost(MyEpisodeConstants.MYEPISODES_SEARCH_PAGE);

    	List <NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair(MyEpisodeConstants.MYEPISODES_SEARCH_PAGE_PARAM_SHOW, search));
        nvps.add(new BasicNameValuePair(MyEpisodeConstants.MYEPISODES_FORM_PARAM_ACTION, MyEpisodeConstants.MYEPISODES_SEARCH_PAGE_PARAM_ACTION_VALUE));

        try {
			post.setEntity(new UrlEncodedFormEntity(nvps));
		} catch (UnsupportedEncodingException e) {
			String message = "Could not start search because the HTTP post encoding is not supported";
			Log.e(LOG_TAG, message, e);
			throw new UnsupportedHttpPostEncodingException(message, e);
		}

		String responsePage;
        HttpResponse response;
        try {
            response = httpClient.execute(post);
            responsePage = EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException | UnknownHostException e) {
            String message = "Could not connect to host.";
			Log.e(LOG_TAG, message, e);
			throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Search on MyEpisodes failed.";
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        List<Show> shows = extractSearchResults(responsePage);

        Log.d(LOG_TAG, shows.size() + " shows found for search value " + search);

        return shows;
    }

    /**
     * Extract a list of shows from the MyEpisodes.com HTML output!
     * @param html The MyEpisodes.com HTML output
     * @return A List of {@link eu.vranckaert.episodeWatcher.domain.Show} instances.
     */
    private List<Show> extractSearchResults(String html) {
        List<Show> shows = new ArrayList<>();
        if(html.contains("No results found.")) {
            return shows;
        }
        String[] split = html.split(MyEpisodeConstants.MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_SEARCH_RESULTS);
        if(split.length == 2) {
            split = split[1].split(MyEpisodeConstants.MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_TABLE_END_TAG);
            if(split.length > 0) {
                split = split[0].split(MyEpisodeConstants.MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_TD_START_TAG);
                for(int i=0; i<split.length; i++) {
                    if(i>0) {
                        String showName;
                        String showId;

                        String htmlPart = split[i];
                        htmlPart = htmlPart.replace("href=\"views.php?type=epsbyshow&showid=", "");

                        //Get the showid
                        htmlPart = htmlPart.replace("href=\"/epsbyshow/", "");
                        int indexOfSlash = htmlPart.indexOf("/");
                        showId = htmlPart.substring(0, indexOfSlash);
                        int closingIndex = htmlPart.indexOf("\">");
                        htmlPart = htmlPart.substring(closingIndex+2);

//                        //Get the showid
//                        String showSeperator = "\">";
//                        int showIdSeperatorIndex = StringUtils.indexOf(htmlPart, showSeperator);
//                        showId = htmlPart.substring(0, showIdSeperatorIndex);
//                        htmlPart = htmlPart.replace(showId + showSeperator, "");
                        //Get the showName
                        showName = htmlPart.substring(0, StringUtils.indexOf(htmlPart, "</a></td>"));

                        shows.add(new Show(showName, showId));
                    }
                }
            }
        }
        return shows;
    }

    public void addShow(String myEpsidodesShowId, User user) throws InternetConnectivityException, LoginFailedException, UnsupportedHttpPostEncodingException, ShowAddFailedException {
        HttpClient httpClient = new DefaultHttpClient();
        userService.login(httpClient, user.getUsername(), user.getPassword());
        String url = MyEpisodeConstants.MYEPISODES_ADD_SHOW_PAGE + myEpsidodesShowId;
        HttpGet get = new HttpGet(url);

        int status;

        try {
            HttpResponse response = httpClient.execute(get);
        	status = response.getStatusLine().getStatusCode();
        } catch (UnknownHostException e) {
			String message = "Could not connect to host.";
			Log.e(LOG_TAG, message, e);
			throw new InternetConnectivityException(message, e);
		} catch (IOException e) {
            String message = "Adding the show status failed for URL " + url;
            Log.w(LOG_TAG, message, e);
            throw new ShowAddFailedException(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        if (status != 200) {
            String message = "Adding the show status failed with status code " + status + " for URL " + url;
            Log.w(LOG_TAG, message);
            throw new ShowAddFailedException(message);
        } else {
            Log.i(LOG_TAG, "Successfully added the show from url " + url);
        }
    }

    public List<Show> getFavoriteOrIgnoredShows(User user, ShowType showType) throws UnsupportedHttpPostEncodingException, InternetConnectivityException, LoginFailedException {
        HttpClient httpClient = new DefaultHttpClient();
        userService.login(httpClient, user.getUsername(), user.getPassword());

        HttpGet get = new HttpGet(MyEpisodeConstants.MYEPISODES_FAVO_IGNORE_PAGE);

		String responsePage;
        HttpResponse response;
        try {
            response = httpClient.execute(get);
            responsePage = EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException | UnknownHostException e) {
            String message = "Could not connect to host.";
			Log.e(LOG_TAG, message, e);
			throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Search on MyEpisodes failed.";
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);
        }

        List<Show> shows = parseShowsHtml(responsePage, showType);

        Log.d(LOG_TAG, shows.size() + " show(s) found!");

        return shows;
    }

    private List<Show> parseShowsHtml(String html, ShowType showType) {
        List<Show> shows = new ArrayList<>();

        String startTag = "<select id=\"";
        String endTag = "</select>";

        String optionStartTag = "<option value=\"";
        String optionEndTag = "</option>";

        switch(showType) {
            case FAVOURITE_SHOWS:
                startTag += "shows\"";
                break;
            case IGNORED_SHOWS:
                startTag += "ignored_shows\"";
                break;
        }
        int startPosition = html.indexOf(startTag);

        if(startPosition == -1) {
            return shows;
        }

        String selectTag = html.substring(startPosition, html.length());
        int endPosition = selectTag.indexOf(endTag);
        selectTag = selectTag.substring(0, endPosition);

        AppDatabase database =  Room.databaseBuilder(eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .build();

        int ep = 1;

        while(selectTag.length() > 0) {
            int startPosistionOption = selectTag.indexOf(optionStartTag);
            int endPositionOption = selectTag.indexOf(optionEndTag);

            if(startPosistionOption == -1 || endPositionOption == -1 || endPositionOption < startPosistionOption) {
                break;
            }

            String optionTag = selectTag.substring(startPosistionOption + optionStartTag.length(), endPositionOption);
            selectTag = selectTag.replace(optionStartTag+optionTag+optionEndTag, "");

            String[] values = optionTag.split("\">");
            if(values.length != 2) {
                break;
            }

            Show show = new Show(values[1].trim(), values[0].trim());
            shows.add(show);
            Log.d(LOG_TAG, "Show found: " + show.getShowName() + " (" + show.getMyEpisodeID() + ")");

            //check if the show runtime is already in the database
            // if not then go and get the runtime

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(show.getMyEpisodeID());


            if(showRuntime == null ){
                Log.d(LOG_TAG, "Show NOT found in Database");
                try {
                    ShowsRuntime(show.getShowName(), show.getMyEpisodeID(), database);
                }catch (Exception e)
                {
                    e.printStackTrace();
                }

            }else {
                //do nothing as already exists no need to add
                Log.d(LOG_TAG, "Show ID found in Database: " + showRuntime.showName + "(" + showRuntime.showMyEpsID + ")");
            }
        }
        return shows;
    }


    private void  ShowsRuntime(String show, String myEpsID, AppDatabase database){

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        String tvMazeAPIURL = "https://api.tvmaze.com/singlesearch/shows?q=";
        //Add the show to the end of the URL
        //need to pull the show out of the list one at a time may?

       // for (int i = 0; i < show.size(); i++) {
        //for now just work with the first item will build to work with all in time.

        System.out.println(show);
       // Log.d("Response: ", "> " + show);
        show = show.replace("#","");
        tvMazeAPIURL += show;

        try {
            URL url = new URL(tvMazeAPIURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            int code = connection.getResponseCode();
            Log.d(LOG_TAG, "API HTTP Status Code: " + code);

            if(code == 429){
                Thread.sleep(10000);
                //wait 10 seconds then try again
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
            }


            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder buffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            //    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

            }


            String jsonString = buffer.toString();
            JSONObject jObj;
            String showNameString = "";
            String showRuntimeString = "";
            String tvmazeShowID = "";

            try
            {
                jObj = new JSONObject(jsonString);
                showNameString = jObj.getString("name");
                showRuntimeString = jObj.getString("runtime");
                tvmazeShowID = jObj.getString("id");

            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

        //    Log.d("showNameString: ", "> " + showNameString);
        //    Log.d("showRuntimeString: ", "> " + showRuntimeString);
        //    Log.d("showTVmazeID: ", "> " + tvmazeShowID);

            //now put the three values into a database....
            SeriesDAO seriesDAO = database.getSeriesDAO();

            //Inserting an episodeRuntime
            EpisodeRuntime epsRunTime = new EpisodeRuntime();
            epsRunTime.setshowMyepsID(myEpsID);
            epsRunTime.setShowName(showNameString);
            epsRunTime.setShowTVMazeID(tvmazeShowID);
            epsRunTime.setShowRuntime(showRuntimeString);

            Log.d("epsRunTime: ",  epsRunTime.toString());

            seriesDAO.insert(epsRunTime);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            //database.close();
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public List<Show> markShow(User user, Show show, ShowAction showAction, ShowType showType) throws LoginFailedException, InternetConnectivityException, UnsupportedHttpPostEncodingException {
        HttpClient httpClient = new DefaultHttpClient();
        userService.login(httpClient, user.getUsername(), user.getPassword());

        String url = "";
        switch(showAction) {
            case IGNORE:
                Log.d(LOG_TAG, "IGNORING SHOWS");
                url = MyEpisodeConstants.MYEPISODES_FAVO_IGNORE_ULR;
                break;
            case UNIGNORE:
                Log.d(LOG_TAG, "UNIGNORING SHOWS");
                url = MyEpisodeConstants.MYEPISODES_FAVO_UNIGNORE_ULR;
                break;
            case DELETE:
                Log.d(LOG_TAG, "DELETING SHOWS");
                url = MyEpisodeConstants.MYEPISODES_FAVO_REMOVE_ULR;
                break;
        }

        HttpGet get = new HttpGet(url + show.getMyEpisodeID());

        try {
            httpClient.execute(get);
        } catch (ClientProtocolException | UnknownHostException e) {
            String message = "Could not connect to host.";
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Markin shows on MyEpisodes failed.";
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        return getFavoriteOrIgnoredShows(user, showType);
    }
}
