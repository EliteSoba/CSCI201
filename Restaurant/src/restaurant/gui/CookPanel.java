package restaurant.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import restaurant.CookAgent;
import restaurant.Menu;

public class CookPanel extends JPanel implements ActionListener{

	CookAgent cook;
	JCheckBox willReorder;
	JButton foodButtons[];
	Menu menu;
	public CookPanel(CookAgent c) {
		cook = c;
		willReorder = new JCheckBox("Allow reordering from markets?",true);
		this.setLayout(new BoxLayout(this, 1));
		add(new JLabel(c.getName()));
		add(willReorder);
		menu = new Menu();
		foodButtons = new JButton[menu.choices.length];
		for (int i = 0; i < foodButtons.length; i++) {
			foodButtons[i] = new JButton("Empty Cook's Stock of " + menu.choices[i]);
			foodButtons[i].addActionListener(this);
			foodButtons[i].setActionCommand(menu.choices[i]);
			add(foodButtons[i]);
		}
		
	}
	
	public static void main(String args[]) {
		JFrame test = new JFrame("test");
		test.add(new CookPanel(new CookAgent("W. Puck", null)));
		test.setVisible(true);
		test.pack();
	}

	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == willReorder) {
			cook.setReordering(willReorder.isSelected());
			return;
		}
		for (int i = 0; i < menu.choices.length; i++) {
			if (arg0.getActionCommand().equalsIgnoreCase(menu.choices[i])) {
				cook.removeFood(arg0.getActionCommand());
			}
		}
		
	}
	
	
}
