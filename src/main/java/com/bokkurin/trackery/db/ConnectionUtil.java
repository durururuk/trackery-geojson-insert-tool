package com.bokkurin.trackery.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.bokkurin.trackery.gui.LogCallback;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConnectionUtil {
	private final LogCallback logger;

	public Connection getConnection(String host, String dbName, String username, String password) throws SQLException {
		String url = "jdbc:mysql://" + host + "/" + dbName + "?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
		return DriverManager.getConnection(url, username, password);
	}

	public void testDbConnection(String hostField, String dbField, String userField, String passField) {
		try (Connection ignored = getConnection(hostField, dbField, userField, passField)) {
			logger.log("DB 연결 성공");
		} catch (SQLException ex) {
			logger.log("DB 연결 실패: " + ex.getMessage());
		}
	}
}
