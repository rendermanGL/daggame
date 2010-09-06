package com.game.Scenes;

import java.util.Vector;

import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import com.game.Map;
import com.game.MsgType;
import com.game.Player;
import com.game.Preferences;
import com.game.R;
import com.game.InputDevice.AIInputDevice;
import com.game.InputDevice.BallInputDevice;
import com.game.InputDevice.InputDevice;
import com.game.InputDevice.TouchInputDevice;

/**
 * A specific scene for the "Play" screen.
 * @author Ying
 *
 */
public class PlayScene extends Scene 
{
	/**
	 * Specifies the state of the plays scene
	 * @author Ying
	 *
	 */
	public enum GameState
	{
		/**
		 * When the state is created but not yet ready
		 */
		UNINITIALIZED,
		
		/**
		 * State is ready to run, have fun kids!
		 */
		PLAYING,
		
		/**
		 * Game paused
		 */
		PAUSED
	}
	
	/**
	 * Current state of the game
	 */
	private GameState gameState;
	
	/**
	 * Communicates with the renderer.
	 */
	private Handler sendRenderer;
	
	private Map map;
	
	private boolean mShowTileMap;
	
	/**
	 * List of all the players in the game
	 */
	private Vector<Player> players;
	
	/**
	 * Sends a message when a trackball message arrives via the handler
	 */
	public Handler trackballEvent;
	
	/**
	 * Sends a message when a touch message arrives via the handler
	 */
	public Handler touchEvent;
	
	/**
	 * Flag used to check if we are ready to play (checks renderer handle)
	 */
	private boolean renderHandleSet = false;
	
	/**
	 * Glag used to check if we are ready to play (checks map loaded)
	 */
	private boolean mapLoaded = false;
	
	/**
	 * Initializes and sets the handler callback.
	 */
	public PlayScene()
	{
		super();
		
		this.gameState = GameState.UNINITIALIZED;
		this.players = new Vector<Player>();
		this.trackballEvent = null;
		this.touchEvent = null;

		CreatePlayers();

		mShowTileMap = true;
		
		map = null;

		this.handler = new Handler() 
		{
	        public void handleMessage(Message msg) 
	        {
	        	if(msg.what == MsgType.TOUCH_EVENT.ordinal())
	        	{       		
	        		if( touchEvent != null )
	        		{
	        			// If there is some input event registered to the touch events
	        			// send him the message. Otherwise we ignore it.
	        			touchEvent.sendMessage(touchEvent.obtainMessage(MsgType.TOUCH_EVENT.ordinal(), msg.obj));
	        		}
	        		
	        		//Log.i("PlayScene Handler: ", "Motion event: " + event.getX() + ", " + event.getY());
	        	}
	        	else if(msg.what == MsgType.TRACKBALL_EVENT.ordinal())
				{
	        		
	        		if( trackballEvent != null )
	        		{
	        			// If there is some input event registered to the trackball events
	        			// send him the message. Otherwise we ignore it.
	        			trackballEvent.sendMessage(trackballEvent.obtainMessage(MsgType.TRACKBALL_EVENT.ordinal(), msg.obj));
	        		}
				}
	        	else if (msg.what == MsgType.SCENE_CALL_START.ordinal())
	        	{
	        		Start();
	        	}
	        	else if(msg.what == MsgType.STOP_SCENE.ordinal())
	        	{
	        		runScene = false;
	        	}
	        	else if (msg.what == MsgType.RENDERER_LOGIC_HANDLER_LINK.ordinal())
	        	{	        		        	
	        		sendRenderer = (Handler)msg.obj;
	        		Log.i("PlayScene", "Received renderer handle!");
	        		
	        		// Notifies that the renderer handle is ready
	        		renderHandleSet = true;	        		
	        		
	        		/*if(mShowTileMap){
	        			sendRenderer.sendMessage(sendRenderer.obtainMessage(MsgType.NEW_TILEMAP.ordinal(),map.getBitmap().getWidth()/Constants.TileWidth,map.getBitmap().getHeight()/Constants.TileWidth, map.getTileMap()));	
	        		}
	        		else{
	        			sendRenderer.sendMessage(sendRenderer.obtainMessage(MsgType.NEW_BITMAP.ordinal(), map.getBitmap().getWidth(), map.getBitmap().getHeight(), map.getBitmap()));
	        		}*/
	        	}
	        	
	        }
	    };
	    
	    gameState = GameState.PLAYING;
	}

	@Override
	public void End() 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void Start() 
	{
		if(refActivity == null )
		{
			Log.e("PlayScene","Reference pointer to activity broken!");
		}
		
		// A thread for background loading of the map
		Thread t = new Thread() 
		{
            public void run() {
            	map = new Map(refActivity,R.drawable.map_size480_1);
            	actHandlerRef.sendEmptyMessage(MsgType.ACTIVITY_DISMISS_LOAD_DIALOG.ordinal());
            	mapLoaded = true;
            }
        };
        t.start();
		
	}

	/**
	 * Updates the game each update step until the thread is stopped
	 * TODO: Set fps limit (must be easily deactivated for debugging speed)
	 */
	@Override
	public void Update() 
	{
		// Logic not dependent on game state
		if(actHandlerRef != null)
		{
			actHandlerRef.sendEmptyMessage(MsgType.UPDATE_PROFILER.ordinal());
		}
		
		// Logic only to run in playing (un-paused) mode
		if(SceneReady())
		{
			Gameplay();
		}
	}
	
	/**
	 * Logic to be called only when the gameState is "Playing"
	 * Normal game logic goes here, as it will automatically stop updating
	 * when the game state changes.
	 */
	private void Gameplay()
	{
		for(int i = 0; i < this.players.size(); i++)
		{
			this.players.elementAt(i).Update();
		}
	}
	
	/**
	 * Creates the list of players and their InputDevices
	 * TODO
	 */
	private void CreatePlayers()
	{
		if(Preferences.Get().multiplayerGame)
		{
			// Multiplayer game
			
		}
		else
		{
			// Single player game
			Player newPlayer;
			
			// Add player 1, input device checked from Preferences
			InputDevice inputDevice = null;
			
			switch (Preferences.Get().singleControlMode) 
			{
				case 0:
					// Touch mode
					inputDevice = new TouchInputDevice(this);
					break;
				
				case 1:
					// Trackball
					inputDevice = new BallInputDevice(this);
					break;
	
				default:
					Log.e("PlayScene", "Input device requested for player not implemented yet!");				
					break;
			}
			newPlayer = new Player(0, inputDevice);
			this.players.add(newPlayer);
			
			// Add all the opponents
			for(int i = 0; i < Preferences.Get().singleNumberOpponents; i++ )
			{
				newPlayer = new Player(i+1, new AIInputDevice(this));
			}			
		}
	}
	
	/**
	 * Checks if the scene is ready to execute gameplay
	 * @return True if it is, false if it isn't
	 */
	private boolean SceneReady()
	{
		return (gameState == GameState.PLAYING) && renderHandleSet && mapLoaded; 
	}

}
