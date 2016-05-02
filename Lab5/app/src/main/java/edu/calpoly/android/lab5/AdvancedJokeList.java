package edu.calpoly.android.lab5;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import edu.calpoly.android.lab4.R;
import okhttp3.MediaType;

public class AdvancedJokeList extends AppCompatActivity {

    /**
     * This holds all of the jokes downloaded from specifed url.
     */
    ArrayMap<String, Integer> allJokes = new ArrayMap<>();
    protected ArrayList<Joke> downloadedJokes;

    /**
     * Contains the name of the Author for the jokes.
     */
    protected String m_strAuthorName;

    /**
     * Contains the list of Jokes the Activity will present to the user.
     */
    protected ArrayList<Joke> m_arrJokeList;
    public static String[] JOKES;

    /**
     * Contains the list of filtered Jokes the Activity will present to the user.
     */
    protected ArrayList<Joke> m_arrFilteredJokeList;

    /**
     * Adapter used to bind an AdapterView to List of Jokes.
     */
    protected JokeListAdapter m_jokeAdapter;

    /**
     * ViewGroup used for maintaining a list of Views that each display Jokes.
     */
    //Changed from ListView.
    protected RecyclerView m_vwJokeLayout;

    /**
     * EditText used for entering text for a new Joke to be added to m_arrJokeList.
     */
    protected EditText m_vwJokeEditText;

    /**
     * Button used for creating and adding a new Joke to m_arrJokeList using the
     * text entered in m_vwJokeEditText.
     */
    protected Button m_vwJokeButton;

    /**
     * Menu used for filtering Jokes.
     */
    protected Menu m_vwMenu;

    /**
     * Value used to filter which jokes get displayed to the user.
     */
    protected int m_nFilter;

    /**
     * Key used for storing and retrieving the value of m_nFilter in savedInstanceState.
     */
    protected static final String SAVED_FILTER_VALUE = "m_nFilter";

    /**
     * Key used for storing and retrieving the text in m_vwJokeEditText in savedInstanceState.
     */
    protected static final String SAVED_EDIT_TEXT = "m_vwJokeEditText";

    /**
     * Menu/Submenu MenuItem IDs.
     */
    protected static final int FILTER = Menu.FIRST;
    protected static final int FILTER_LIKE = SubMenu.FIRST;
    protected static final int FILTER_DISLIKE = SubMenu.FIRST + 1;
    protected static final int FILTER_UNRATED = SubMenu.FIRST + 2;
    protected static final int FILTER_SHOW_ALL = SubMenu.FIRST + 3;

    /**
     * Used to handle Contextual Action Mode when long-clicking on a single Joke.
     */
    private ActionMode.Callback mActionModeCallback;
    private ActionMode mActionMode;

    /**
     * The Joke that is currently focused after long-clicking.
     */
    private int selected_position;

    /**
     * The ID of the CursorLoader to be initialized in the LoaderManager and used to load a Cursor.
     */
    private static final int LOADER_ID = 1;

    /**
     * The String representation of the Show All filter. The Show All case
     * needs a String representation of a value that is different from
     * Joke.LIKE, Joke.DISLIKE and Joke.UNRATED. The actual value doesn't
     * matter as long as it's different, since the WHERE clause is set to
     * null when making database operations under this setting.
     */
    public static final String SHOW_ALL_FILTER_STRING = "" + FILTER_SHOW_ALL;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.m_arrJokeList = new ArrayList<>();
        this.m_arrFilteredJokeList = new ArrayList<>();
        this.downloadedJokes = new ArrayList<>();

        this.m_jokeAdapter = new JokeListAdapter(this, this.m_arrFilteredJokeList);
        this.m_strAuthorName = this.getResources().getString(R.string.author_name);

        initLayout();
        initAddJokeListeners();

        for (String s : this.getResources().getStringArray(R.array.jokeList)) {
            addJoke(new Joke(s, this.m_strAuthorName));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        this.m_vwMenu = menu;
        return true;
    }

    /**
     * Method is used to encapsulate the code that initializes and sets the
     * Layout for this Activity.
     */
    protected void initLayout() {
        this.setContentView(R.layout.advanced);
        this.m_vwJokeLayout = (RecyclerView) this.findViewById(R.id.jokeListViewGroup); //jokeListViewGroup was rv
        this.m_vwJokeLayout.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        if (this.m_vwJokeLayout != null) {
            this.m_vwJokeLayout.setAdapter(m_jokeAdapter);
        }

        this.m_vwJokeEditText = (EditText) this.findViewById(R.id.newJokeEditText);
        this.m_vwJokeButton = (Button) this.findViewById(R.id.addJokeButton);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int index = viewHolder.getAdapterPosition();
                Joke jokeToManipulate = m_arrJokeList.get(index);

                if (direction == ItemTouchHelper.LEFT) {

                    String uploadJoke = jokeToManipulate.getJoke();
                    new UploadToServer().execute(uploadJoke);

                    Toast.makeText(AdvancedJokeList.this, "Joke Uploaded", Toast.LENGTH_SHORT).show();
                    m_jokeAdapter.notifyItemChanged(index);

                } else if (direction == ItemTouchHelper.RIGHT) {
                    Toast.makeText(AdvancedJokeList.this, "Joke Deleted", Toast.LENGTH_SHORT).show();
                    m_arrFilteredJokeList.remove(index);

                    //Log.w("aagrover", "index removed:" + index);
                    removeJoke(jokeToManipulate);

                    m_jokeAdapter.notifyItemRemoved(index);
                    //m_jokeAdapter.notifyDataSetChanged();
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(m_vwJokeLayout);
    }

    /**
     * Method is used to encapsulate the code that initializes and sets the
     * Event Listeners which will respond to requests to "Add" a new Joke to the
     * list.
     */
    protected void initAddJokeListeners() {
        this.m_vwJokeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                String jokeText = m_vwJokeEditText.getText().toString();
                if (!jokeText.equals("")) {
                    addJoke(new Joke(jokeText, m_strAuthorName));
                    m_vwJokeEditText.setText("");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(m_vwJokeEditText.getWindowToken(), 0);
                }
            }
        });

        this.m_vwJokeEditText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String jokeText = m_vwJokeEditText.getText().toString();
                    if (jokeText != null && !jokeText.equals("")) {
                        addJoke(new Joke(jokeText, m_strAuthorName));
                        m_vwJokeEditText.setText("");
                        return true;
                    }
                }
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(m_vwJokeEditText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Method used for encapsulating the logic necessary to properly add a new
     * Joke to m_arrJokeList, and display it on screen.
     *
     * @param joke The Joke to add to list of Jokes.
     */
    protected void addJoke(Joke joke) {

        Log.w("contains joke?", "" + downloadedJokes.contains(joke));
        Log.w("joke is", "" + joke);
        if (!m_arrJokeList.contains(joke)) {
            this.m_arrJokeList.add(joke);
            this.m_arrFilteredJokeList.add(joke);
            this.m_jokeAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.submenu_like:
                filterJokeList(FILTER_LIKE);
                return true;

            case R.id.submenu_dislike:
                filterJokeList(FILTER_DISLIKE);
                return true;

            case R.id.submenu_unrated:
                filterJokeList(FILTER_UNRATED);
                return true;

            case R.id.submenu_show_all:
                filterJokeList(FILTER_SHOW_ALL);
                return true;

            case R.id.submenu_download_jokes:

                if (!downloadedJokes.isEmpty()) {
                    Log.w("downloadedJokes size", "is " + downloadedJokes.size());
                    int size = downloadedJokes.size();

                    int delete = size - 1;

                    while (delete != -1) {
                        Log.w("deleting joke number", "" +  delete);

                        removeJoke(downloadedJokes.get(delete));
                        delete--;
                    }

                    Log.w("downloadedJokes size", "is " + downloadedJokes.size());

                    allJokes.clear();
                    this.m_jokeAdapter.notifyDataSetChanged();
                }

                Toast.makeText(AdvancedJokeList.this, "Downloading Jokes", Toast.LENGTH_SHORT).show();

                URL url = null;
                try {
                    url = new URL("http://simexusa.com/aac/getAllJokes.php");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                new DownloadFromServer().execute(url);
                Toast.makeText(AdvancedJokeList.this, "Download Complete", Toast.LENGTH_SHORT).show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void filterJokeList(int filter) {
        syncFilterChanges();
        int rating = 0;

        switch (filter) {
            case FILTER_LIKE:
                rating = Joke.LIKE;
                break;

            case FILTER_DISLIKE:
                rating = Joke.DISLIKE;
                break;

            case FILTER_UNRATED:
                rating = Joke.UNRATED;
                break;

            case FILTER_SHOW_ALL:

                this.m_arrFilteredJokeList.clear();
                for (Joke j : this.m_arrJokeList) {
                    this.m_arrFilteredJokeList.add(j);
                }
                this.m_jokeAdapter.notifyDataSetChanged();
                return;

            default:
                return;
        }

        this.m_arrFilteredJokeList.clear();

        for (int index = 0; index < this.m_arrJokeList.size(); index++) {
            if (this.m_arrJokeList.get(index).getRating() == rating) {
                this.m_arrFilteredJokeList.add(this.m_arrJokeList.get(index));
            }
        }
        this.m_jokeAdapter.notifyDataSetChanged();
    }

    private void syncFilterChanges() {
        for (Joke j : this.m_arrFilteredJokeList) {
            //Has the joke in it already, need rating change
            if (this.m_arrJokeList.contains(j)) {
                //Log.w("syncFilter..", "joke: " + j.getRating());
                this.m_arrJokeList.get(this.m_arrJokeList.indexOf(j)).setRating(j.getRating());
            }
        }
    }

    protected void removeJoke(Joke jv) {
        this.m_arrFilteredJokeList.remove(jv);
        this.m_arrJokeList.remove(jv);
        this.downloadedJokes.remove(jv);
        this.m_jokeAdapter.notifyDataSetChanged();
    }

    // Moved from JokeListAdapter.java
    public static class JokeListAdapter extends RecyclerView.Adapter<JokeViewHolder> {

        /** The application Context in which this JokeListAdapter is being used. */
        private Context m_context;

        /** The data set to which this JokeListAdapter is bound. */
        private List<Joke> m_jokeList;

        public JokeListAdapter(Context context, List<Joke> jokeList) {
            this.m_context = context;
            this.m_jokeList = jokeList;
        }

        public int getCount() {
            return this.m_jokeList.size();
        }

        public Object getItem(int position) {
            return this.m_jokeList.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.joke_view;
        }

        @Override
        public JokeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new JokeViewHolder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
        }

        @Override
        public void onBindViewHolder(JokeViewHolder holder, int position) {
            holder.bind(m_jokeList.get(position));
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return m_jokeList.size();
        }
    }

    public static class JokeViewHolder extends RecyclerView.ViewHolder implements RadioGroup.OnCheckedChangeListener {

        private Joke m_joke;
        private TextView jokeTextView;
        private RadioButton likeButton;
        private RadioButton dislikeButton;
        private RadioGroup likeGroup;

        public JokeViewHolder(View itemView) {
            super(itemView);

            this.jokeTextView = (TextView) itemView.findViewById(R.id.jokeTextView);
            this.likeButton = (RadioButton) itemView.findViewById(R.id.likeButton);
            this.dislikeButton = (RadioButton) itemView.findViewById(R.id.dislikeButton);

            this.likeGroup = (RadioGroup) itemView.findViewById(R.id.ratingRadioGroup);
            this.likeGroup.setOnCheckedChangeListener(this);
        }

        public void bind(Joke joke) {
            setJoke(joke);
        }

        public void setJoke(Joke joke) {
            this.m_joke = joke;
            jokeTextView.setText(joke.getJoke());

            if(this.m_joke.getRating() == Joke.LIKE) {
                this.likeButton.setChecked(true);
            }
            else if(this.m_joke.getRating() == Joke.DISLIKE) {
                this.dislikeButton.setChecked(true);
            }
            else if(this.m_joke.getRating() == Joke.UNRATED) {
                this.likeGroup.clearCheck();
            }
        }

        @Override
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

    class DownloadFromServer extends AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... params) {

            try {
                URL url = params[0];

                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                String joke;
                int i = 0;
                while ((joke = in.readLine()) != null) {
                    allJokes.put(joke, i++);
                }

                in.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return "";
        }

        protected void onPostExecute(String jokes) {

            Joke newJoke;

            for (int addJoke = 0; addJoke < allJokes.size(); addJoke++) {
                if (!allJokes.keyAt(addJoke).equals("")) {
                    newJoke = new Joke(allJokes.keyAt(addJoke), "");
                    addJoke(newJoke);
                    downloadedJokes.add(newJoke);
                    m_jokeAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    class UploadToServer extends AsyncTask<String, Void, String> {

        public final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        @Override
        protected String doInBackground(String... params) {

            URL url = null;

            try {

                String theUrl = "http://simexusa.com/aac/addOneJoke.php?" + "joke=" +
                        URLEncoder.encode(params[0], "UTF-8") + "&author=" +
                        URLEncoder.encode(m_strAuthorName, "UTF-8");

                url = new URL(theUrl);

                url.openStream();

                Log.w("URL", theUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return params[0];
        }
    }
}