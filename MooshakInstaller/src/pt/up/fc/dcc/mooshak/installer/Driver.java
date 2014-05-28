package pt.up.fc.dcc.mooshak.installer;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;



public interface Driver {
	
	/**
	 * Initialize the installation visualization
	 * @param maxium number of panels
	 */
	void init(int maximum);
	
	/**
	 * Conclude installation
	 */
	void conclude();
	
	/**
	 * Show the following items in a new panel 
	 * @param part of maximum panels
	 */
	void startPanel(int part);

	/**
	 * No more items are added to this panel. 
	 * When completed the interaction proceed with the given runnable
	 * 
	 * @param continuation (Runnable)
	 */
	void endPanel(Runnable continuation);
	
	/**
	 * No more items are added to this panel. 
	 * The runnable to proceed should be given by 
	 * {@code nextPanel}, {@code goPanel}
	 * 
	 * @param continuation (Runnable)
	 */
	void endPanel();
	
	/**
	 * Change the default continuation with given runnable.
	 * This method is typically called by methods that process user input.
	 * 
	 * @param continuation function (Runnable)
	 */
	void nextPanel(Runnable continuation);
	
	/**
	 * Go immediately to given continuation.
	 * This method is typically called by methods that execute tasks
	 * @param continuation
	 */
	void goPanel(Runnable continuation);
	
	/** 
	 * Show a message in the current panel with given text
	 * @param message	to display
	 */
	void say(String message);
	
	/**
	 * Show an error message
	 * @param message
	 */
	void error(String message);
			
	/**
	 * Add an element to ask a question answerable with a boolean.
	 * When the answer is available a method consuming than answer is invoked
	 * 
	 * @param promptText	text asking for a boolean
	 * @param defaultValue	default or initial value
	 * @param consumer		method that consumes collected value
	 */
	void askBoolean(String promptText, boolean defaultValue,
			Consumer<Boolean> consumer);
	
	/**
	 * Add an element to ask a question answerable with a string.
	 * When the answer is available a method consuming than answer is invoked
	 * 
	 * @param promptText	text asking for a string
	 * @param defaultValue	default or initial value
	 * @param consumer		method that consumes collected value
	 */
	void askString(String promptText, String defaultValue,Consumer<String> consumer);
	
	/**
	 *  Add an element to select a string from a list.
	 *  When the answer is available a method consuming than answer is invoked
	 * @param promptText	text asking for a string
	 * @param values		list of values for selection
	 * @param defaultValue	default or initial value
	 * @param consumer		method that consumes collected value
	 */
	void askList(String promptText, List<String> values, String defaultValue,
			Consumer<String> consumer);
	
	/**
	 * Add an element to ask a question answerable with a path.
	 * When the answer is available a method consuming than answer is invoked
	 * 
	 * @param promptText	text asking for a path
	 * @param defaultValue	default path
	 * @param consumer		method that consumes collected value
	 */
	void askPath(String promptText, Path defaultPath,Consumer<Path> consumer);
	
	/**
	 * Show a progress bar associated with a given task.
	 * The consumer receives an object that knows of to display progress
	 * and launches a task that reports its completion status to this
	 * object. 
	 * 
	 * @param consumer
	 */
	void showProgress(Consumer<Progressable> consumer);
	
	/**
	 * An element implementing this interface shows the progress
	 * of a task, from 0 (task not started yet) 
	 * to a maximum (task complete). A task may 
	 *
	 * @author José Paulo Leal <zp@dcc.fc.up.pt>
	 */
	interface Progressable {
		
		/**
		 * This progress bar will receive updates
		 * @param isUpdatable
		 */
		void updatable(boolean isUpdatable);
		
		/**
		 * The maximum value for this progress bar
		 * @return
		 */
		int getMaximum();
		
		/**
		 * Change in the amount of task completed
		 * @param part
		 */
		void update(int part);
		
		/**
		 * Report errors occurred while executing task
		 * 
		 * @param message
		 */
		void taskError(String message);
	}
	
}
