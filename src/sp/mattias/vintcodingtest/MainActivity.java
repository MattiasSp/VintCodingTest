package sp.mattias.vintcodingtest;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;

/**
 * The single and main activity of the Vint Coding Test app, which displays a user's
 * Facebook friends in a list after logging in. It handles the displaying of two
 * fragments; one for unauthenticated users (the login/splash screen) and one for
 * display after login was successful (the main fragment).
 * 
 * @author Mattias Spångmyr
 * @version 2014-06-04
 */
public class MainActivity extends FragmentActivity {
	private static final String TAG = "MainActivity";
	
	private static final int SPLASH = 0;
	private static final int MAIN = 1;
	private static final int FRAGMENT_COUNT = MAIN + 1;
	private Fragment[] mFragments = new Fragment[FRAGMENT_COUNT];
	
	/** This flag is used to enable session state change checks and indicates whether the Activity is visible. */
	private boolean mIsResumed = false;

	private UiLifecycleHelper mUiHelper;
	private Session.StatusCallback mSessionChangeListener = 
	    new Session.StatusCallback() {
	    @Override
	    public void call(Session session, 
	            SessionState state, Exception exception) {
	        onSessionStateChange(session, state, exception);
	    }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Start listening for Facebook Session changes.
		mUiHelper = new UiLifecycleHelper(this, mSessionChangeListener);
		mUiHelper.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);

		FragmentManager fm = getSupportFragmentManager();
	    mFragments[SPLASH] = fm.findFragmentById(R.id.splashFragment);
	    mFragments[MAIN] = fm.findFragmentById(R.id.mainFragment);

	    FragmentTransaction transaction = fm.beginTransaction();
	    for(int i = 0; i < mFragments.length; i++) {
	        transaction.hide(mFragments[i]);
	    }
	    transaction.commit();

/*		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new SplashFragment()).commit();
		}*/
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    mUiHelper.onResume();
	    mIsResumed = true;
	}
	
	@Override
	protected void onResumeFragments() {
	    super.onResumeFragments();
	    Session session = Session.getActiveSession();

	    if (session != null && session.isOpened()) {
	        // if the session is already open,
	        // try to show the authenticated fragment
	        showFragment(MAIN, false);
	    } else {
	        // otherwise present the splash screen
	        // and ask the person to login.
	        showFragment(SPLASH, false);
	    }
	}

	@Override
	public void onPause() {
	    super.onPause();
	    mUiHelper.onPause();
	    mIsResumed = false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    mUiHelper.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    mUiHelper.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    mUiHelper.onSaveInstanceState(outState);
	}
	
	/**
	 * Called to show a Fragment and hide all other Fragments.
	 * @param fragmentIndex The index (in the Fragment array) of the Fragment to show.
	 * @param addToBackStack Whether the Fragment change should be added to the back stack (so that the user can reverse it using the back button).
	 */
	private void showFragment(int fragmentIndex, boolean addToBackStack) {
	    FragmentManager fm = getSupportFragmentManager();
	    FragmentTransaction transaction = fm.beginTransaction();
	    for (int i = 0; i < mFragments.length; i++) {
	        if (i == fragmentIndex) {
	            transaction.show(mFragments[i]);
	        } else {
	            transaction.hide(mFragments[i]);
	        }
	    }
	    if (addToBackStack) {
	        transaction.addToBackStack(null);
	    }
	    transaction.commit();
	}
	
	/**
	 * Called when the Facebook Session State changes (e.g. authentication is
	 * finished) to clear the Fragment back stack and show the appropriate
	 * Fragment.
	 * @param session The Facebook Session whose SessionState changed.
	 * @param state The SessionState that changed to trigger this callback.
	 * @param exception Any exception which occurred.
	 */
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
	    // Only make changes if the activity is visible
	    if (mIsResumed) {
	        FragmentManager manager = getSupportFragmentManager();
	        // Get the number of entries in the back stack
	        int backStackSize = manager.getBackStackEntryCount();
	        // Clear the back stack
	        for (int i = 0; i < backStackSize; i++) {
	            manager.popBackStack();
	        }
	        if (state.isOpened()) {
	            // If the session state is open:
	            // Show the authenticated fragment
	            showFragment(MAIN, false);
	        } else if (state.isClosed()) {
	            // If the session state is closed:
	            // Show the login fragment
	            showFragment(SPLASH, false);
	        }
	    }
	}

	/**
	 * A fragment for the splash screen asking the user to log in using Facebook.
	 */
	public static class SplashFragment extends Fragment {
		private static final String TAG = "SplashFragment";

		public SplashFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_splash, container, false);
			
			// Ask for the friend list permission.
/*			LoginButton loginButton = (LoginButton) rootView.findViewById(R.id.login_button);
			loginButton.setFragment(this);
			loginButton.setReadPermissions(Arrays.asList("user_friends"));*/
			
			return rootView;
		}
	}
	
	/**
	 * A fragment for the main screen which downloads and shows
	 * the user's Facebook friend list.
	 */
	public static class MainFragment extends ListFragment {
		private static final String TAG = "MainFragment";
		/** Used to determine whether the Session's info should be updated in
		 * the onActivityResult() method. */
		private static final int REAUTH_ACTIVITY_CODE = 100;
		private ListFragment _this;
		
		private UiLifecycleHelper mUiHelper;
		private Session.StatusCallback mSessionChangeListener = 
		    new Session.StatusCallback() {
		    @Override
		    public void call(final Session session,
		            final SessionState state, final Exception exception) {
		        onSessionStateChange(session, state, exception);
		    }
		};
		
		private FriendListAdapter mFriendListAdapter;
		
		private View mRootView;
		private ProfilePictureView mProfilePictureView;
		private TextView mUserNameView;

		/**
		 * Basic empty constructor. Initialization is done in "onCreate" and "onCreateView" instead.
		 */
		public MainFragment() {
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
		    super.onCreate(savedInstanceState);
		    _this = this;
		    mUiHelper = new UiLifecycleHelper(getActivity(), mSessionChangeListener);
		    mUiHelper.onCreate(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mRootView = inflater.inflate(R.layout.fragment_main, container, false);
			
			// Find and setup the layout elements.
			mProfilePictureView = (ProfilePictureView) mRootView.findViewById(R.id.main_profile_pic);
			mProfilePictureView.setCropped(true);
			mUserNameView = (TextView) mRootView.findViewById(R.id.main_user_name);
			
			// Set the header text of the main fragment.
		    ((TextView) mRootView.findViewById(R.id.main_heading)).setText(
		    		getString(R.string.main_user_heading_a) +
		    		getString(R.string.app_name) +
		    		getString(R.string.main_user_heading_b));
			
			// Check for an open session
		    Session session = Session.getActiveSession();
		    if (session != null && session.isOpened()) {
		        // Get the user's data
		        makeMeRequest(session);
		    }
			
			return mRootView;
		}
		
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
		    super.onActivityResult(requestCode, resultCode, data);
		    if (requestCode == REAUTH_ACTIVITY_CODE) {
		        mUiHelper.onActivityResult(requestCode, resultCode, data);
		    }
		}
		
		@Override
		public void onResume() {
		    super.onResume();
		    mUiHelper.onResume();
		}
		
		@Override
		public void onSaveInstanceState(Bundle bundle) {
		    super.onSaveInstanceState(bundle);
		    mUiHelper.onSaveInstanceState(bundle);
		}

		@Override
		public void onPause() {
		    super.onPause();
		    mUiHelper.onPause();
		}

		@Override
		public void onDestroy() {
		    super.onDestroy();
		    mUiHelper.onDestroy();
		}
		
		/**
		 * Called when the Facebook Session State changes (e.g. authentication is
		 * finished) to start a request for the user's Facebook data.
		 * @param session The Facebook Session whose SessionState changed.
		 * @param state The SessionState that changed to trigger this callback.
		 * @param exception Any exception which occurred.
		 */
		private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
		    if (session != null && session.isOpened()) {
		        // Get the user's data.
		        makeMeRequest(session);
		    }
		}
		
		/**
		 * The method which requests the user's Facebook data, both personal data
		 * and from the user's friend list, in two separate requests.
		 * @param session The Facebook Session to use for the request.
		 */
		private void makeMeRequest(final Session session) {
		    // Make an API call to get user data and define a 
		    // new callback to handle the response.
		    Request request = Request.newMeRequest(session, 
		            new Request.GraphUserCallback() {
		        @Override
		        public void onCompleted(GraphUser user, Response response) {
		            // If the response is successful
		            if (session == Session.getActiveSession()) {
		                if (user != null) {
		                    // Set the id for the ProfilePictureView
		                    // view that in turn displays the profile picture.
		                    mProfilePictureView.setProfileId(user.getId());
		                    // Set the Textview's text to the user's name.
		                    mUserNameView.setText(user.getName());
		                }
		            }
		            if (response.getError() != null) {
		            	Log.e(TAG, response.getError().getErrorMessage());
		            }
		        }
		    });
		    request.executeAsync();
		    
		    // Make an API call to get user's friend list data and
		    // define a separate callback to handle the response.
		    Request friendRequest = Request.newMyFriendsRequest(session, 
	            new GraphUserListCallback(){
	                @Override
	                public void onCompleted(List<GraphUser> friends,
	                        Response response) {
	                	
	                	// If the response is successful
			            if (session == Session.getActiveSession()) {
			                if (friends != null) {
			                    // Setup the adapter for the ListView.
			        			mFriendListAdapter = new FriendListAdapter(mRootView.getContext(), R.id.list_item, friends);
			        			_this.setListAdapter(mFriendListAdapter);
			        			
			        			if (friends.size() < 1) { // If there are no friends to display, change the text in the "empty list view".
			        				((TextView) mRootView.findViewById(android.R.id.empty)).setText(R.string.no_friends);
			        			}
			                }
			            }
			            if (response.getError() != null) {
			            	Log.e(TAG, response.getError().getErrorMessage());
			            }
	                }
	        });
	        Bundle params = new Bundle();
	        params.putString("fields", "id,name,picture");
	        friendRequest.setParameters(params);
	        friendRequest.executeAsync();
		}
	}
	
	/**
	 * This adapter handles mapping the friend list data returned
	 * by the request to each row in the ListView.
	 */
	protected static class FriendListAdapter extends ArrayAdapter<GraphUser> {
		private static final String TAG = "FriendListAdapter";
		
		private List<GraphUser> mFriends;
		private Context mContext;
		
		private static class ViewHolder {
			public ProfilePictureView profilePic;
			public TextView text;
		}
		
		/**
		 * Basic constructor for the list adapter.
		 * @param context The current context.
		 * @param resource The resource ID for the "list_item" layout file to use when instantiating the row's views.
		 * @param friends The friend objects to represent in the ListView.
		 */
		public FriendListAdapter(Context context, int resource,
				List<GraphUser> friends) {
			super(context, resource, friends);
			mFriends = friends; // Store the friend data.
			mContext = context; // Save the context reference to access it in the GetView method.
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View listItemView = convertView;

			// If the list item has not already been created, inflate it.
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				listItemView = inflater.inflate(R.id.list_item, parent, false);
				
				// Store the view references in the ViewHolder.
				ViewHolder newViewHolder = new ViewHolder();
				newViewHolder.text = (TextView) listItemView.findViewById(R.id.friend_user_name);
				newViewHolder.profilePic = (ProfilePictureView) listItemView.findViewById(R.id.friend_profile_pic);
			    listItemView.setTag(newViewHolder);
			}
				
			// Add the data to the list item.
			ViewHolder viewHolder = (ViewHolder) listItemView.getTag();
			viewHolder.text.setText(mFriends.get(position).getName());		    
		    viewHolder.profilePic.setProfileId(mFriends.get(position).getId());
			
			return listItemView;
		}
	}
}
