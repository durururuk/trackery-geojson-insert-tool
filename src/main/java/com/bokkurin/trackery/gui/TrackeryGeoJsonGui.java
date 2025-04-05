package com.bokkurin.trackery.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.*;

import com.bokkurin.trackery.areas.AreaUtil;
import com.bokkurin.trackery.areas.InsertType;
import com.bokkurin.trackery.db.ConnectionUtil;
import com.bokkurin.trackery.db.DbConnectionInfo;

public class TrackeryGeoJsonGui extends JFrame {
	private static final String DEFAULT_HOST = "localhost";
	private static final String DEFAULT_DB = "trackery";
	private static final String DEFAULT_USER = "root";

	private final JTextField hostField = new JTextField(DEFAULT_HOST, 15);
	private final JTextField dbField = new JTextField(DEFAULT_DB, 10);
	private final JTextField userField = new JTextField(DEFAULT_USER, 10);
	private final JPasswordField passField = new JPasswordField("", 10);
	private final JTextArea logArea = new JTextArea(15, 50);

	private final transient AreaUtil areaUtil = new AreaUtil(this::log);
	private final transient ConnectionUtil connectionUtil = new ConnectionUtil(this::log);
	private File selectedFile;

	private JButton insertSidoButton;
	private JButton insertSigunguButton;
	private JButton chooseFileButton;
	private JButton testDbConnectionButton;

	public TrackeryGeoJsonGui() {
		setTitle("Trackery GeoJSON Insert Tool");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(900, 600);
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		setupMainTopPanel();
		setupLogPanel(gbc);

		setVisible(true);
	}

	private void setupMainTopPanel() {
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(createDbPanel(), BorderLayout.CENTER);
		leftPanel.add(createTestConnectionButton(), BorderLayout.SOUTH);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(createUploadPanel());
		rightPanel.add(Box.createVerticalStrut(10));
		rightPanel.add(createInsertButtonPanel());

		GridBagConstraints leftGbc = new GridBagConstraints();
		leftGbc.gridx = 0;
		leftGbc.gridy = 0;
		leftGbc.weightx = 0.5;
		leftGbc.fill = GridBagConstraints.BOTH;
		leftGbc.insets = new Insets(0, 50, 0, 100);
		add(leftPanel, leftGbc);

		GridBagConstraints rightGbc = new GridBagConstraints();
		rightGbc.gridx = 1;
		rightGbc.gridy = 0;
		rightGbc.weightx = 0.5;
		rightGbc.fill = GridBagConstraints.BOTH;
		rightGbc.insets = new Insets(0, 50, 0, 100);
		add(rightPanel, rightGbc);
	}

	private JPanel createDbPanel() {
		JPanel dbPanel = new JPanel(new GridLayout(4, 2));
		dbPanel.add(new JLabel("mysql 호스트"));
		dbPanel.add(hostField);
		dbPanel.add(new JLabel("mysql 유저명"));
		dbPanel.add(userField);
		dbPanel.add(new JLabel("mysql 비밀번호"));
		dbPanel.add(passField);
		dbPanel.add(new JLabel("mysql DB명"));
		dbPanel.add(dbField);
		return dbPanel;
	}

	private JButton createTestConnectionButton() {
		testDbConnectionButton = new JButton("DB 연결 테스트");
		testDbConnectionButton.addActionListener(this::testDbConnection);
		return testDbConnectionButton;
	}

	private JPanel createUploadPanel() {
		JPanel uploadPanel = new JPanel();
		uploadPanel.add(new JLabel("geojson 파일"));
		chooseFileButton = new JButton("파일");
		chooseFileButton.addActionListener(this::chooseFile);
		uploadPanel.add(chooseFileButton);
		return uploadPanel;
	}

	private JPanel createInsertButtonPanel() {
		JPanel insertPanel = new JPanel();
		insertSidoButton = new JButton("시도 삽입");
		insertSigunguButton = new JButton("시군구 삽입");

		insertSidoButton.addActionListener(e -> insertData(InsertType.SIDO));
		insertSigunguButton.addActionListener(e -> insertData(InsertType.SIGUNGU));

		insertPanel.add(insertSidoButton);
		insertPanel.add(insertSigunguButton);
		return insertPanel;
	}

	private void setupLogPanel(GridBagConstraints gbc) {
		logArea.setBackground(Color.BLACK);
		logArea.setForeground(Color.WHITE);
		logArea.setCaretColor(Color.WHITE);
		logArea.setEditable(false);
		logArea.setLineWrap(true);
		logArea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(logArea);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		add(scrollPane, gbc);
	}

	private void testDbConnection(ActionEvent e) {
		DbConnectionInfo connectionInfo = gatherDbConnectionInfo();
		connectionUtil.testDbConnection(
			connectionInfo.host(), connectionInfo.database(), connectionInfo.user(),
			connectionInfo.password());
	}

	private void chooseFile(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			selectedFile = chooser.getSelectedFile();
			log("선택된 파일: " + selectedFile.getAbsolutePath());
		}
	}

	private void insertData(InsertType type) {
		if (selectedFile == null) {
			log("먼저 파일을 선택하세요.");
			return;
		}
		DbConnectionInfo connectionInfo = gatherDbConnectionInfo();
		setUiEnabled(false); // 비활성화

		new SwingWorker<Void, String>() {
			@Override
			protected Void doInBackground() {
				try (Connection conn = connectionUtil.getConnection(
					connectionInfo.host(), connectionInfo.database(),
					connectionInfo.user(), connectionInfo.password())) {

					areaUtil.insertAreaData(conn, type, selectedFile, this::publish);
				} catch (SQLException ex) {
					publish("DB 에러 발생: " + ex.getMessage());
				}
				return null;
			}

			@Override
			protected void process(java.util.List<String> chunks) {
				for (String msg : chunks) {
					log(msg);
				}
			}

			@Override
			protected void done() {
				setUiEnabled(true); // 다시 활성화
			}
		}.execute();
	}

	private void setUiEnabled(boolean enabled) {
		hostField.setEnabled(enabled);
		dbField.setEnabled(enabled);
		userField.setEnabled(enabled);
		passField.setEnabled(enabled);
		insertSidoButton.setEnabled(enabled);
		insertSigunguButton.setEnabled(enabled);
		chooseFileButton.setEnabled(enabled);
		testDbConnectionButton.setEnabled(enabled);
	}

	private DbConnectionInfo gatherDbConnectionInfo() {
		String host = hostField.getText();
		String db = dbField.getText();
		String user = userField.getText();
		String pass = new String(passField.getPassword());
		return new DbConnectionInfo(host, db, user, pass);
	}

	private void log(String msg) {
		logArea.append(msg + "\n");
		logArea.setCaretPosition(logArea.getDocument().getLength());
	}
}
