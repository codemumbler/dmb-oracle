package io.github.codemumbler;

import javax.sql.DataSource;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.Statement;

public class ScriptRunner {

	private final DataSource dataSource;

	public ScriptRunner(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void executeScript(final String sql) throws Exception {
		if ( sql == null )
			return;
		StringBuilder sqlStatement = null;
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(true);
			LineNumberReader lineReader = new LineNumberReader(new StringReader(sql));
			String line;
			int closeableStatements = 0;
			while ((line = lineReader.readLine()) != null) {
				line = line.trim();
				if (sqlStatement == null)
					sqlStatement = new StringBuilder();
				if (line.isEmpty() || line.startsWith("--") || line.startsWith("/"))
					continue;
				else {
					sqlStatement.append(line).append("\n");
					if (line.contains("FOR ") && !line.contains("END;"))
						closeableStatements++;
					else if (line.contains("IF") && !line.contains("END IF;"))
						closeableStatements++;
					if (line.contains("END IF;"))
						closeableStatements--;
					else if (line.contains("END;"))
						closeableStatements--;
				}
				if (line.contains(";") && closeableStatements <= 0) {
					Statement statement = connection.createStatement();
					statement.execute(sqlStatement.toString());
					statement.close();
					sqlStatement = null;
					closeableStatements = 0;
				}
			}
		}
	}

	public void executeCreation(final Database database) throws Exception {
		OracleScriptWriter writer = new OracleScriptWriter(database);
		executeScript(writer.writeScript());
	}
}
