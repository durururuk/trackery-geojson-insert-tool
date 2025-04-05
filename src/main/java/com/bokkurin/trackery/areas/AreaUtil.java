package com.bokkurin.trackery.areas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

import com.bokkurin.trackery.gui.LogCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AreaUtil {
	private final LogCallback logger;
	private static final String PROPERTIES = "properties";

	private void insertSigungu(Connection conn, JsonNode sigunguNode, Consumer<String> logger) {
		Long sigunguId = Long.valueOf(sigunguNode.get(PROPERTIES).get("SIGUNGU_CD").asText());
		long sidoId = Long.parseLong(String.valueOf(sigunguId).substring(0, 2));
		String sigunguName = sigunguNode.get(PROPERTIES).get("SIGUNGU_NM").asText();
		String geometryJson = sigunguNode.get("geometry").toString();

		logger.accept(String.format("시/군/구 ID : %s, 시/군/구 명 : %s", sigunguId, sigunguName));
		logger.accept(
			String.format("공간 용량 : %.2f MB", geometryJson.getBytes(StandardCharsets.UTF_8).length / 1024.0 / 1024.0));

		try (PreparedStatement stmt = conn.prepareStatement(
			"INSERT INTO juso_sigungu(sgg_id, sgg_name, sgg_border, sd_id) " +
				"VALUES (?, ?, ST_GeomFromGeoJSON(?), ?)")) {
			stmt.setLong(1, sigunguId);
			stmt.setString(2, sigunguName);
			stmt.setString(3, geometryJson);
			stmt.setLong(4, sidoId);
			stmt.executeUpdate();

			logger.accept(String.format("삽입 성공 : %s", sigunguName));
		} catch (SQLException e) {
			logger.accept(String.format("SQLException: %s", e.getMessage()));
		}
	}

	private void insertSido(Connection conn, JsonNode sidoNode, Consumer<String> logger) {
		long sidoId = Long.parseLong(sidoNode.get(PROPERTIES).get("SIDO_CD").asText());
		String sidoName = sidoNode.get(PROPERTIES).get("SIDO_NM").asText();
		String geometryJson = sidoNode.get("geometry").toString();

		logger.accept(String.format("시/도 ID : %s, 시/도명 : %s", sidoId, sidoName));
		logger.accept(
			String.format("공간 용량 : %.2f MB", geometryJson.getBytes(StandardCharsets.UTF_8).length / 1024.0 / 1024.0));

		try (PreparedStatement stmt = conn.prepareStatement(
			"INSERT INTO juso_sido(sd_id, sd_name, sd_border) " +
				"VALUES (?, ?, ST_GeomFromGeoJSON(?))"
		)) {
			stmt.setLong(1, sidoId);
			stmt.setString(2, sidoName);
			stmt.setString(3, geometryJson);
			stmt.executeUpdate();
			logger.accept(String.format("삽입 성공 : %s", sidoName));
		} catch (SQLException e) {
			logger.accept(String.format("SQLException: %s", e.getMessage()));
		}
	}

	public void insertAreaData(Connection conn, InsertType type, File geoJsonFIle, Consumer<String> logger) {
		try (conn) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(new FileInputStream(geoJsonFIle));

			if (!type.getValue().equals(root.get("name").asText())) {
				logger.accept("GeoJSON name 필드가 '" + type + "'이 아닙니다.");
				return;
			}

			JsonNode features = root.get("features");

			for (JsonNode feature : features) {
				if (type == InsertType.SIDO) {
					insertSido(conn, feature, logger);
				} else {
					insertSigungu(conn, feature, logger);
				}
			}
		} catch (SQLException ex) {
			logger.accept(String.format("삽입 중 에러 발생: %s", ex.getMessage()));
		} catch (IOException ex) {
			logger.accept(String.format("유효하지 않은 파일입니다 : %s", ex.getMessage()));
		}
	}
}
