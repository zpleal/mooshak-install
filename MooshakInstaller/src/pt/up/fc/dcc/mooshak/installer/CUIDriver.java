package pt.up.fc.dcc.mooshak.installer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Simple driver for command line interface
 * Uses standard input, output and error for interaction
 *
 * @author José Paulo Leal <zp@dcc.fc.up.pt>
 */
public class CUIDriver implements Driver {
	
	
	private Scanner in = new Scanner(System.in);
	private int MAXIMUM = 50;
	
	@Override
	public void init(int maximum) {}
	
	@Override
	public void conclude() {}
	
	@Override
	public void startPanel(int maximum) {}

	

	@Override
	public void nextPanel(Runnable continuation) {
		continuation.run();
	}
	
	@Override
	public void goPanel(Runnable continuation) {
		continuation.run();
	}
	
	@Override
	public void endPanel(Runnable continuation) { 
		continuation.run();
	}
	
	@Override
	public void endPanel() { 
	}
	
	@Override
	public void say(String message) {
		System.out.println(message);
	}
	
	@Override
	public void error(String message) {
		System.err.println(message);
		
	}

	@Override
	public void askString(String promptText, String defaultValue,
			Consumer<String> consumer) {
		
		ask(simplePrompt(promptText,defaultValue),defaultValue,x -> x,consumer);
	}
	
	@Override
	public void askBoolean(String promptText, boolean defaultValue,
			Consumer<Boolean> consumer) {
		String textDefaultValue = defaultValue?"Yes":"No";
		String prompt=simplePrompt(promptText+" (yes or no)",textDefaultValue);
		
		ask(prompt,textDefaultValue, 
				answer -> {
					Boolean value = defaultValue;
					switch(answer.charAt(0)) {
						case 'Y':
						case 'y':
							value = true;
							break;
						case 'N':
						case 'n':
							value = false;
							break;
						default:
							new Exception("yes or no?");
					}
					return value;
				},
				consumer);
	}	

	@Override
	public void askList(String promptText, List<String> values, 
			String defaultValue,
			Consumer<String> consumer) {
		
		String prompt=listPrompt(promptText,values,defaultValue);
		
		ask(prompt,
				defaultValue, 
				value -> { 
					if(value.equals(defaultValue))
						return value;
					else
						return values.get(Integer.parseInt(value)); 
				},consumer);
	}

	@Override
	public void askPath(String promptText, Path defaultPath,
			Consumer<Path> consumer) {
		String textDefaultValue = defaultPath.toString();
		String prompt=simplePrompt(promptText,textDefaultValue);
		
		ask(prompt,textDefaultValue,Paths::get,consumer);
	}
	
	private <T> void  ask(
			String prompt, 
			String defaultValue,
			Function<String,T> packer,
			Consumer<T> consumer) {
		T value = null;
		
				
		System.out.print(prompt);
		
		while(value == null) {
			String answer = in.nextLine();
			
			if("".equals(answer))
				value = packer.apply(defaultValue);
			else {
				try {		
					value = packer.apply(answer);
				} catch(Exception cause) {
					System.err.println(cause.toString());
					System.err.flush();
					System.out.println(prompt);
				}
			}
			
		}
		
		consumer.accept(value);	
	}
	
	/**
	 * Simple prompt for typed values
	 * @param promptText
	 * @param defaultValue
	 * @return
	 */
	private String simplePrompt(String promptText, String defaultValue) {
		StringBuilder prompt = new StringBuilder();

		prompt.append(promptText);
		prompt.append(' ');
		prompt.append('[');
		prompt.append(defaultValue.toString());
		prompt.append(']');
		prompt.append(' ');

		return prompt.toString();
	}

	/**
	 * Prompt for list of values
	 * @param promptText
	 * @param values
	 * @param defaultValue
	 * @return
	 */
	private String listPrompt(String promptText, List<String> values,
			String defaultValue) {
		StringBuilder prompt = new StringBuilder();
		int pos = 0;
		
		prompt.append(promptText);
		prompt.append(" (enter option number)");
		prompt.append('\n');
		for(String item: values) {
			boolean isDefault = item != null && item.equals(defaultValue);
			prompt.append('\t');
			prompt.append(pos++);
			prompt.append(" - ");
			if(isDefault)
				prompt.append('[');
			prompt.append(item);
			if(isDefault)
				prompt.append(']');
			prompt.append('\n');
		}
		
		return prompt.toString();
	}
	
	/**
	 * Progress bar using standard input/output.Bars are displayed as 
	 * strings of sharp characters (#) and updated by using backspaces. 
	 * If the console does not support backspace (as in Eclipse) then
	 * use a system property to disable its use.
	 * 
	 * @author José Paulo Leal <zp@dcc.fc.up.pt>
	 */
	class StdioProgress implements Progressable {
		
		@Override
		public void updatable(boolean isUpdatable) {
			if(isUpdatable)
				writeProgress(0);
		}

		@Override
		public int getMaximum() {
			return MAXIMUM;
		}

		@Override
		public void update(int part) {
			cleanProgress();
			writeProgress(part);
			if(part == MAXIMUM) 
				System.out.println("");
		}
		
		private void writeProgress(int n) {
			System.out.print("[");
			for(int i=0; i<n; i++)
				System.out.print("#");
			for(int i=n; i<MAXIMUM; i++)
				System.out.print(" ");
			System.out.print("]");
		}
			
		private void cleanProgress() {
			if(Utils.hasBackspace())
				for(int i=0; i< MAXIMUM+2; i++)
					System.out.print("\b");
			else
				System.out.print("\n");
		}

		@Override
		public void taskError(String message) {
			error(message);
		}
		
	}
	
	@Override
	public void showProgress(Consumer<Progressable> consumer) {
		consumer.accept(new StdioProgress());	
	}


}
