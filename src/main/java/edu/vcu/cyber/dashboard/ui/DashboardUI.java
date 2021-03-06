package edu.vcu.cyber.dashboard.ui;

import edu.vcu.cyber.dashboard.cybok.CybokQueryHandler;
import edu.vcu.cyber.dashboard.cybok.queries.UpdateQuery;
import edu.vcu.cyber.dashboard.data.*;
import edu.vcu.cyber.dashboard.graph.interpreters.AVGraphInterpreter;
import edu.vcu.cyber.dashboard.project.AppSession;
import edu.vcu.cyber.dashboard.ui.graphpanel.AVGraphPanel;
import edu.vcu.cyber.dashboard.ui.graphpanel.GraphPanel;
import edu.vcu.cyber.dashboard.ui.graphpanel.EditableGraphPanel;
import edu.vcu.cyber.dashboard.util.ApplicationSettings;
import edu.vcu.cyber.dashboard.util.GraphExporter;
import jdk.nashorn.internal.scripts.JO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class DashboardUI extends JFrame implements ActionListener
{


	private GraphPanel topGraphPanel;
	private GraphPanel avGraphPanel;
	private GraphPanel specGraphPanel;

	private JPanel contentPane;
	private JTabbedPane tabs;

	private JSplitPane sp;
	private JSplitPane updown;

	private BucketPanel bucket;

	private JLabel statusLabel;

	public void setStatusLabel(String statusText)
	{
		statusLabel.setText("\t" + statusText);
	}

	public DashboardUI()
	{
		setTitle("Security Analyst Dashboard");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		initComponents();
		setupMenu();

		contentPane.setOpaque(true);
		setContentPane(contentPane);
		pack();
	}

	public JSplitPane getGraphSplitPane()
	{
		return sp;
	}

	public JTabbedPane getGraphTabs()
	{
		return tabs;
	}

	public JSplitPane getBucketSplitPane()
	{
		return updown;
	}

	public BucketPanel getBucketPanel()
	{
		return bucket;
	}

	private void initComponents()
	{
//		JToolBar toolBar = new JToolBar();
//		toolBar.setFloatable(false);
//		JButton as_btn = new JButton("Attack Surfaces");
//		as_btn.addActionListener(this);
//		toolBar.add(as_btn);

		statusLabel = new JLabel(" ");


		contentPane = new JPanel(new BorderLayout(5, 5));

//		contentPane.add(toolBar, BorderLayout.NORTH);

		sp = new JSplitPane();
		sp.setPreferredSize(new Dimension(1400, 700));
		sp.setDividerLocation(700);
		sp.setDividerSize(5);

		topGraphPanel = new EditableGraphPanel(GraphType.TOPOLOGY);
		avGraphPanel = new AVGraphPanel(GraphType.ATTACKS);
		specGraphPanel = new EditableGraphPanel(GraphType.SPECIFICATIONS);

		sp.setLeftComponent(topGraphPanel);


		tabs = new JTabbedPane();
		tabs.add("Specification", specGraphPanel);
		tabs.add("Attack Vector Space", avGraphPanel);
//		tabs.add("Search", new JPanel(new BorderLayout()));

		sp.setRightComponent(tabs);


		contentPane.add(sp, BorderLayout.CENTER);
		contentPane.add(statusLabel, BorderLayout.SOUTH);

	}

	private void setupMenu()
	{
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
//		fileMenu.add("New").addActionListener(this);
		fileMenu.add("Load").addActionListener(this);
		fileMenu.add("Save").addActionListener(this);
//		fileMenu.addSeparator();
		fileMenu.add("Export Graph").addActionListener(this);
//		fileMenu.add("Import").addActionListener(this);
//		fileMenu.addSeparator();
		fileMenu.add("Exit").addActionListener(this);

		JMenu cybokMenu = new JMenu("Cybok");
		cybokMenu.add("Configure").addActionListener(this);
		cybokMenu.add("Redo Analysis").addActionListener(this);
		cybokMenu.add("Update Cybok").addActionListener(this);

		JMenu viewMenu = new JMenu("View");
		viewMenu.add(new JCheckBoxMenuItem("Bucket")).addActionListener(this);

		JMenu filterMenu = new JMenu("Filter");
		filterMenu.add(new JCheckBoxMenuItem("Show Deleted")).addActionListener(this);
		filterMenu.add(new JCheckBoxMenuItem("Show Hidden")).addActionListener(this);
		filterMenu.add(new JCheckBoxMenuItem("Show CVEs")).addActionListener(this);

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		if (CybokQueryHandler.isCybokInstalled())
		{
			menuBar.add(cybokMenu);
		}
		menuBar.add(filterMenu);

		setJMenuBar(menuBar);
	}

	public void display()
	{
		setVisible(true);
	}

	public GraphPanel getTopGraphPanel()
	{
		return topGraphPanel;
	}

	public GraphPanel getAvGraphPanel()
	{
		return avGraphPanel;
	}

	public GraphPanel getSpecGraphPanel()
	{
		return specGraphPanel;
	}

	public GraphPanel getGraphPanel(GraphType type)
	{
		switch (type)
		{
			case ATTACKS:
				return avGraphPanel;
			case SPECIFICATIONS:
				return specGraphPanel;
			case ATTACK_SURFACE:
			case TOPOLOGY:
				return topGraphPanel;
		}
		System.out.println("??" + type);
		return null;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		switch (e.getActionCommand())
		{
			case "Exit":
				System.exit(0);
				break;
			case "Attack Surfaces":
				AppSession.getInstance().toggleAttackSurfaces();
				break;

			case "Bucket":
				showBucket(updown == null);
				break;

			case "Show Node ID":

				AVGraphInterpreter.showNodeID = !AVGraphInterpreter.showNodeID;
				break;

			case "Show Deleted":
				AttackVectors.showDeletedNodes = !AttackVectors.showDeletedNodes;
				AttackVectors.update(AppSession.getInstance().getAvGraph());
				break;

			case "Show Hidden":
				AttackVectors.showDeletedNodes = !AttackVectors.showDeletedNodes;
				AttackVectors.update(AppSession.getInstance().getAvGraph());
				break;

			case "Show CVEs":
				AttackVectors.showCVENodes = !AttackVectors.showCVENodes;
				AttackVectors.getAllAttackVectors().forEach(av ->
				{
					if (av.type == AttackType.CVE)
					{
						av.hidden = !AttackVectors.showCVENodes;
					}
				});

				AttackVectors.update(AppSession.getInstance().getAvGraph());
				break;


			// power to getting tired of repositioning all of the nodes!
			case "Save":
				ApplicationSettings.saveAll(this);
				break;

			case "Load":
				ApplicationSettings.loadAll(this);
				break;

			case "Export Graph":
				GraphData graphData = AppSession.getFocusedGraphData();
				if (graphData != null)
				{
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setMultiSelectionEnabled(false);
					fileChooser.setSelectedFile(new File("./export/", graphData.getGraphType().name().toLowerCase() + ".graphml"));
					int ret = fileChooser.showSaveDialog(this);
					if (ret == JFileChooser.APPROVE_OPTION)
					{
						GraphExporter.exportGraph(AppSession.getSelectedGraphData(), fileChooser.getSelectedFile());
					}
				}
				break;
			case "Update Cybok":
				int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to do this?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION)
				{
					CybokQueryHandler.sendQuery(new UpdateQuery());
				}
				break;
			case "Configure":
				// TODO: allow to specify cybok install location
				break;

			case "Redo Analysis":
				SystemAnalysis.doAnalysis();
				break;
		}
	}


	public void showBucket(boolean show)
	{
		if (show)
		{
			if (bucket == null)
			{

				bucket = BucketPanel.showBucket(true);

				contentPane.remove(sp);

				updown = new JSplitPane();
				updown.setOrientation(JSplitPane.VERTICAL_SPLIT);
				updown.setDividerSize(5);
				updown.setTopComponent(sp);
				updown.setBottomComponent(bucket);
				contentPane.add(updown, BorderLayout.CENTER);
				updown.setDividerLocation(contentPane.getHeight() / 3 * 2);
			}

		}
		else
		{
			if (bucket != null)
			{
				updown.remove(sp);
				contentPane.remove(updown);
				updown = null;
				contentPane.add(sp, BorderLayout.CENTER);
				bucket = null;
			}
		}

		contentPane.updateUI();
	}
}
