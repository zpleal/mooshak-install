package pt.up.fc.dcc.mooshak.installer;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.Border;


public class GUIDriver implements Driver {
	
	private static final int FRAME_HEIGHT 			= 300;
	private static final int FRAME_WIDTH  			= 600;
	
	private static final int PANEL_HEIGHT 			= 240;
	private static final int PANEL_WIDTH  			= 550;

	private static final int BUTTON_WIDTH			=  75;
	private static final int BUTTON_HEIGHT			=  25;
	
	private static final int FRAME_TOP_PARGIN 		=  30;
	private static final int FRAME_LEFT_MARGIN 		=  20;
	private static final int FRAME_RIGHT_MARGIN 	=  20;
	private static final int FRAME_BOTTOM_MARGIN 	=  10;

	private static final int TEXT_SIZE				=  20; 
	private static final int MARGIN_SIZE 			=  10;
	
	private static final Border MARGIN = BorderFactory.createEmptyBorder(
			MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE);
	
	private static final Font BUTTON_FONT = new Font("Dialog",Font.BOLD, 12);
	
	private static final Color BBC = new Color(224,224,240);
	
	private static int PROGRESS_MAXIMUM = 100;
	
	private Set<Runnable> actions = new HashSet<>();
	private Runnable continuation;
	
	// widgets accessed by several methods
	private JPanel currentPanel = makePanel();
	private JTextArea errorMessages;
	private JProgressBar globalProgress = new JProgressBar();
	private Button proceed = new Button("Proceed");
	private Button finish = new Button("Finish");
	
	@Override
	public void init(int maximum) {
		
		try {
			javax.swing.SwingUtilities.invokeAndWait(this::toplevel);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		globalProgress.setMaximum(maximum);
	}
	
	@Override
	public void conclude() {
		proceed.setEnabled(false);
		finish.setEnabled(true);
	}
	
	@Override
	public void startPanel(int part) {

		globalProgress.setValue(part);
		
		currentPanel.removeAll();
		currentPanel.setBackground(BBC);
		currentPanel.revalidate();
		currentPanel.repaint();
		actions.clear();
		proceed.setEnabled(false);
	}

	@Override
	public void nextPanel(Runnable continuation) {
		this.continuation = continuation;
	}
	
	@Override
	public void goPanel(Runnable continuation) {
		
		javax.swing.SwingUtilities.invokeLater(continuation);
	}

	@Override
	public void endPanel(Runnable continuation) {
		currentPanel.revalidate();
		currentPanel.repaint();		
		this.continuation = continuation;
		proceed.setEnabled(true);
	}
	
	@Override
	public void endPanel() {
		
		endPanel(() -> {});
	}

	
	@Override
	public void say(String message) {
		JLabel label = new JLabel("<html>"+message.replaceAll("\n","<br>"));
		label.setBorder(MARGIN);
		currentPanel.add(label);
	}

	

	@Override
	public void askBoolean(String promptText, boolean defaultValue,
			Consumer<Boolean> consumer) {
		JCheckBox check = new JCheckBox();
		check.setText(promptText);
		check.setSelected(defaultValue);
		check.setBackground(BBC);

		actions.add( ()-> { consumer.accept(check.isSelected()); } );
		
		currentPanel.add(check);
	}

	@Override
	public void askString(String promptText, String defaultValue,
			Consumer<String> consumer) {
		JPanel row = new JPanel();
		JTextArea text = new JTextArea(defaultValue,1,TEXT_SIZE);
		text.setSelectionStart(0);
		text.setSelectionEnd(defaultValue.length());
		
		actions.add( ()-> { consumer.accept(text.getText()); } );
		
		row.add(new JLabel(promptText));
		row.add(text);
		
		currentPanel.add(row);
	}
	
	@Override
	public void askList(String promptText, List<String> values, 
			String defaultValue,
			Consumer<String> consumer) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
		JPanel stack = new JPanel();
		JList<String> list = new JList<String>();
		Vector<String> data = new Vector<>();
		int selected = -1;
		int pos = 0;
				
		for(String item: values) {
			data.add(item);
			if(item != null && item.equals(defaultValue))
				selected = pos;
			pos++;
		}
		
		list.setListData(data);
		list.setSelectedIndex(selected);
		
		list.setVisibleRowCount(10);
		Dimension size = list.getPreferredSize();
		size.width = 100;
		size.height = 300;
		list.setPreferredSize(size);
		
		actions.add( ()-> { consumer.accept(list.getSelectedValue()); } );
		
		stack.setLayout(new BoxLayout(stack, BoxLayout.PAGE_AXIS));
		stack.add(new JLabel(promptText));
		stack.add(Box.createVerticalGlue());
		stack.setPreferredSize(size);
		stack.setBackground(BBC);
		
		row.setBackground(BBC);
		
		row.add(stack);
		row.add(list);
		currentPanel.add(row);
	}

	@Override
	public void askPath(String promptText, Path defaultPath,
			Consumer<Path> consumer) {
		JPanel row = new JPanel();
		JLabel label = new JLabel(promptText);
		JTextArea text = new JTextArea(defaultPath.toString(),1,TEXT_SIZE);
		JButton button = new JButton("Browse");
		
		JFileChooser chooser = new JFileChooser(defaultPath.toFile());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		row.setBackground(BBC);
		
		row.add(label);
		row.add(text);
		row.add(button);
		
		button.addActionListener(e -> { 
			switch(chooser.showDialog(currentPanel, "Browse...")) {
			case JFileChooser.APPROVE_OPTION:
				text.setText(chooser.getSelectedFile().toString());
				break;
			case JFileChooser.CANCEL_OPTION:
			case JFileChooser.ERROR_OPTION:
				break;
			}
		});
		
		actions.add( ()-> { consumer.accept(Paths.get(text.getText())); } );
		
		currentPanel.add(row);
	}

	@Override
	public void error(String message) {
		errorMessages.setText(message);
	}

	
	class SwingProgress implements Progressable {

		private JProgressBar progressBar;

		SwingProgress(JProgressBar progressBar) {
			this.progressBar = progressBar;
			progressBar.setMaximum(PROGRESS_MAXIMUM);
		}
		
		@Override
		public void updatable(boolean isUpdating) {
			progressBar.setIndeterminate(! isUpdating);
		}
		
		@Override
		public void update(int progress) {
			progressBar.setValue(progress);
		}

		@Override
		public int getMaximum() {
			return PROGRESS_MAXIMUM;
		}

		@Override
		public void taskError(String message) {
			error(message);
		}
	}
	
	
	@Override
	public void showProgress(Consumer<Progressable> consumer) {
	
		JProgressBar progress = new JProgressBar();
		progress.setIndeterminate(false);
		progress.setMaximum(PROGRESS_MAXIMUM);
		progress.setValue(0);
		progress.setStringPainted(true);
	
		currentPanel.add(progress);
		currentPanel.revalidate();
		currentPanel.repaint();
		
		new SwingWorker<Void,Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				consumer.accept(new SwingProgress(progress));
				return null;
			}
			
		}.execute();
	}
	
	
	// -- Interface initialization and management
	
	private void toplevel() {
		JFrame frame = new JFrame("Mooshak instalation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel top  = new JPanel();
        JPanel footer  = new JPanel();
        
        top.setLayout(new BorderLayout());
       
        footer.setLayout(new BoxLayout(footer,BoxLayout.PAGE_AXIS));
        
        top.add(currentPanel,BorderLayout.CENTER);
        top.add(footer,BorderLayout.PAGE_END);
        
        footer.add(makeGlobalProgress());
        footer.add(makeErrorMessages());
        footer.add(makeButtons());
        
        frame.getContentPane().add(top);
        frame.setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
       
        frame.pack();
        frame.setVisible(true);
        
	}
	
	private JPanel makePanel() {
		JPanel panel;
		Dimension dimension = new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.PAGE_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(
				FRAME_TOP_PARGIN, 
				FRAME_LEFT_MARGIN, 
				FRAME_BOTTOM_MARGIN, 
				FRAME_RIGHT_MARGIN));
		
		panel.setBackground(BBC);
		panel.setPreferredSize(dimension);
		panel.setMinimumSize(dimension);
		panel.setAlignmentX(0.0F);
		return panel;
	}

	private JTextArea makeErrorMessages() {
		errorMessages = new JTextArea();
		
		errorMessages.setForeground(Color.red);
		errorMessages.setBackground(BBC);
		errorMessages.setEditable(false);
		errorMessages.setBorder(MARGIN);
		
		return errorMessages;
	}
	
	private Component makeGlobalProgress() {
		JPanel border = new JPanel();
		
		globalProgress.setIndeterminate(false);
		globalProgress.setMaximum(PROGRESS_MAXIMUM);
		globalProgress.setValue(0);
		globalProgress.setStringPainted(true);
		
		border.add(globalProgress);
		border.setBorder(MARGIN);
		border.setBackground(BBC );
		
		return border;
	}
	
	
	
	private JPanel makeButtons() {
		JPanel buttons = new JPanel();
        Button abort = new Button("Abort");
        Dimension dimension = new Dimension(BUTTON_WIDTH,BUTTON_HEIGHT);

        buttons.add(Box.createHorizontalGlue());
        Arrays.asList(abort,proceed,finish).forEach(b -> {
        	b.setPreferredSize(dimension);
        	b.setMaximumSize(dimension);
        	b.setFont(BUTTON_FONT);
        	buttons.add(b);
        });
        
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        buttons.setBorder(MARGIN);
        buttons.setBackground(BBC);
        
        proceed.addActionListener(e -> { proceed();});
        abort.addActionListener(e -> { 
      
        	switch(JOptionPane.showConfirmDialog(currentPanel,
        			"Do you really want to exit Mooshak's installation?",
        			"Exit confirmation",
        			JOptionPane.YES_NO_OPTION)) {
        	case JOptionPane.YES_OPTION:
        		System.exit(0);
        		break;
        	}
        	
        });
        finish.addActionListener(e -> { System.exit(0); });
        
        finish.setEnabled(false);
        
        return buttons;
	}
	
	/**
	 * Go to next panel, as defined in continuation
	 */
	private void proceed() {
		actions.forEach(r -> { 
			Thread thread = new Thread(r);
			thread.start();
			try {
				thread.join();
			} catch (Exception e) {
				e.printStackTrace();
			}
			} 
		);
		new Thread(continuation).start();
	}

}
