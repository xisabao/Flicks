package com.example.flicks;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.flicks.models.Config;
import com.example.flicks.models.Movie;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class MovieDetailsActivity extends AppCompatActivity {

    // the movie to display
    Movie movie;
    Config config;

    // the view objects
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvOverview) TextView tvOverview;
    @Nullable
    @BindView(R.id.ivBackdrop) ImageView ivBackdrop;
    @BindView(R.id.rbVoteAverage) RatingBar rbVoteAverage;

    String ytKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);

        ytKey = null;

        //unwrap the movie passed in via intent, using its simple name as a key
        movie = Parcels.unwrap(getIntent().getParcelableExtra(Movie.class.getSimpleName()));
        Log.d("MovieDetailsActivity", String.format("Showing details for '%s'", movie.getTitle()));
        config = Parcels.unwrap(getIntent().getParcelableExtra(Config.class.getSimpleName()));

        getVideos();

        // set the title and overview
        tvTitle.setText(movie.getTitle());
        tvOverview.setText(movie.getOverview());

        // vote average is 0..10, convert to 0..5 by dividing by 2
        float voteAverage = movie.getVoteAverage().floatValue();
        rbVoteAverage.setRating(voteAverage = voteAverage > 0 ? voteAverage / 2.0f : voteAverage);

        // load the backdrop image
        String imageUrl = config.getImageUrl(config.getBackdropSize(), movie.getBackdropPath());

        // load backdrop image using glide
        Glide.with(this)
                .load(imageUrl)
                .bitmapTransform(new RoundedCornersTransformation(this, 15, 0))
                .placeholder(R.drawable.flicks_backdrop_placeholder)
                .error(R.drawable.flicks_backdrop_placeholder)
                .into(ivBackdrop);

        setupImageViewListener();

    }

    private void setupImageViewListener() {
        Log.d("MovieDetailsActivity", "Setting up listener on details view");
        ivBackdrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MovieDetailsActivity", "YTKEY: " + ytKey);
                if (ytKey != null) {
                    // create the new activity
                    Intent i = new Intent(MovieDetailsActivity.this, MovieTrailerActivity.class);
                    // pass the data being edited
                    i.putExtra("video_id", ytKey);

                    //display the activity
                    startActivity(i);
                } else {
                    Toast.makeText(getApplicationContext(), "No YouTube video found", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void getVideos(){
        // create the url
        String url = MovieListActivity.API_BASE_URL + "/movie/" + movie.id + "/videos";
        // set the request parameters
        RequestParams params = new RequestParams();
        AsyncHttpClient client = new AsyncHttpClient();
        params.put(MovieListActivity.API_KEY_PARAM, getString(R.string.api_key)); // API key, always required
        // execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // load the results into movies list
                try {
                    JSONArray results = response.getJSONArray("results");
                    int i = 0;
                    // find Youtube key in results
                    while (!results.isNull(i)) {
                        JSONObject video = results.getJSONObject(i);
                        if (video.getString("site").equals("YouTube")) {
                            ytKey = video.getString("key");
                            Log.d("MovieDetailsActivity", "Found youtube key: " + ytKey);
                            break;
                        }
                        i++;
                    }
                    if (ytKey == null) {
                        Log.d("MovieDetailsActivity", "Couldn't find YT key");
                    }

                } catch (JSONException e) {
                    Log.e("MovieDetailsActivity", "Failed to parse videos results: ", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e("MovieDetailsActivity", "Failed to reach videos endpoint");
            }
        });

    }

}
