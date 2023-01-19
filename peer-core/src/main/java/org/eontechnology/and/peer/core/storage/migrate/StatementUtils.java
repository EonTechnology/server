package org.eontechnology.and.peer.core.storage.migrate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;

/** Utility functions for working with the Statement object. */
public class StatementUtils {

  public static void runSqlScript(Statement statement, String fileName)
      throws IOException, SQLException {

    StringBuilder sb = new StringBuilder(8192);

    try (InputStream inputStream = statement.getClass().getResourceAsStream(fileName)) {
      try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
        BufferedReader r = new BufferedReader(reader);
        String str = null;

        while ((str = r.readLine()) != null) {
          sb.append(str);
          sb.append("\n");
        }
      }
    }

    statement.executeUpdate(sb.toString());
  }
}
