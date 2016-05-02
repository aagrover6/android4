package edu.calpoly.android.lab5;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import edu.calpoly.android.lab4.R;

public class JokeView extends RecyclerView implements OnCheckedChangeListener {

	/** Radio buttons for liking or disliking a joke. */
	private RadioButton m_vwLikeButton;
	private RadioButton m_vwDislikeButton;
	
	/** The container for the radio buttons. */
	private RadioGroup m_vwLikeGroup;

	/** Displays the joke text. */
	private TextView m_vwJokeText;
	
	/** The data version of this View, containing the joke's information. */
	private Joke m_joke;


	/**
	 * Basic Constructor that takes only an application Context.
	 * 
	 * @param context
	 *            The application Context in which this view is being added. 
	 *            
	 * @param joke
	 * 			  The Joke this view is responsible for displaying.
	 */
	public JokeView(Context context, Joke joke) {
		super(context);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.joke_view, this, true);
		this.m_vwJokeText = (TextView)findViewById(R.id.jokeTextView);
		this.m_vwLikeButton = (RadioButton)findViewById(R.id.likeButton);
		this.m_vwDislikeButton = (RadioButton)findViewById(R.id.dislikeButton);
		this.m_vwLikeGroup = (RadioGroup)findViewById(R.id.ratingRadioGroup);
		this.m_vwLikeGroup.setOnCheckedChangeListener(this);
		this.setJoke(joke);
	}

	/**
	 * Mutator method for changing the Joke object this View displays. This View
	 * will be updated to display the correct contents of the new Joke.
	 * 
	 * @param joke
	 *            The Joke object which this View will display.
	 */
	public void setJoke(Joke joke) {
		this.m_joke = joke;
		this.m_vwJokeText.setText(m_joke.getJoke());
		if(this.m_joke.getRating() == Joke.LIKE) {
			this.m_vwLikeButton.setChecked(true);
		}
		else if(this.m_joke.getRating() == Joke.DISLIKE) {
			this.m_vwDislikeButton.setChecked(true);
		}
		else if(this.m_joke.getRating() == Joke.UNRATED) {
			this.m_vwLikeGroup.clearCheck();
		}
	}

	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (checkedId == R.id.likeButton) {
			this.m_joke.setRating(Joke.LIKE);
		}
		else if (checkedId == R.id.dislikeButton) {
			this.m_joke.setRating(Joke.DISLIKE);
		}
		else if (checkedId == -1) {
			this.m_joke.setRating(Joke.UNRATED);
		}
	}
}
