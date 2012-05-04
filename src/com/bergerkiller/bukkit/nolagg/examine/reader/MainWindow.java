package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;

import javax.swing.*;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private GraphBox ticktimes1;
	private SelectionBox selection;
	public JTextField filepath;
	public JTextArea locations;
	private JScrollPane locscroll;
	
	public MainWindow() {
		this(870, 572);
	}
	public MainWindow(int width, int height) {
		this(width, height, Toolkit.getDefaultToolkit().getScreenSize());
	}
	private MainWindow(int width, int height, Dimension screensize) {
		this((int) ((screensize.width - width) / 2), (int) ((screensize.height - height) / 2), width, height);
	}
	public MainWindow(int x, int y, int width, int height) {
		super();
		this.setBounds(x, y, width, height);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLayout(null);
		this.init();
		this.setTitle("NoLagg examination file reader by Bergerkiller");
		this.setVisible(true);
	}
	
	public <T extends Component> T append(T component) {
		super.add(component);
		return component;
	}
	
	public GraphArea add(String name, double totalduration) {
		GraphArea ga = this.ticktimes1.addArea();
		this.selection.add(name, ga.color, totalduration);
		return ga;
	}
		
	public void onClick(int index) {
		if (index == -1) {
			if (ExamReader.isSingleSelected) {
				ExamReader.selectPlugin(ExamReader.selectedPlugin);
			} else {
				ExamReader.selectPlugin((PluginInfo) null);
			}
		} else if (ExamReader.selectedPlugin != null) {
			ExamReader.selectSegment(index);
		} else {
			ExamReader.selectPlugin(this.selection.getText(index));
		}
	}
	
	public void reset(int newduration) {
		this.ticktimes1.reset(newduration);
		this.selection.clear();
	}
	public void orderAreas() {
		this.ticktimes1.orderAreas();
	}
	
	public final int selectionWidth = 290;
	public final int selectionHeight = 300;
	final int graphwidth = 600;
	final int yoffset = 40;
	private void init() {
		final MainWindow main = this;
		
		this.selection = this.append(new SelectionBox(graphwidth + 10, yoffset, selectionWidth, selectionHeight) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSelectionChange(int index) {
				main.ticktimes1.setSelection(index);
			}

			@Override
			public void onItemClick(int index) {
				main.onClick(index);
			}
		});
				
		this.ticktimes1 = this.append(new GraphBox(5, yoffset, graphwidth, 500) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSelectionChange(GraphArea newarea) {
				if (newarea == null) {
					main.selection.setSelection(-1);
				} else {
					main.selection.setSelection(newarea.index);
				}
			}

			@Override
			public void onAreaClick(GraphArea area) {
				main.onClick(area == null ? -1 : area.index);
			}
			
		});
		
		this.getContentPane().addHierarchyBoundsListener(new HierarchyBoundsListener(){
            public void ancestorMoved(HierarchyEvent e) {}
            @Override
            public void ancestorResized(HierarchyEvent e) {
                //make sure all contents can fit riiight in :)
            	main.ticktimes1.setSize(main.getWidth() - selectionWidth - 28, main.getHeight() - 80);
            	main.locscroll.setLocation(main.getWidth() - 310, main.getHeight() - 240);
            	main.selection.setBounds(main.getWidth() - 310, yoffset, selectionWidth, main.getHeight() - 285);
            	main.filepath.setSize(main.getWidth() - 130, main.filepath.getHeight());
            }
        });

		this.locations = new JTextArea();
		locations.setEditable(false);
		this.filepath = this.append(new JTextField());
		this.filepath.setBounds(110, 5, 750, 30);
		this.filepath.setEditable(false);
		this.filepath.setDragEnabled(false);
		this.append(locscroll = new JScrollPane(locations)).setSize(this.selectionWidth, 200);

		this.setResizable(true);
		
	}

}