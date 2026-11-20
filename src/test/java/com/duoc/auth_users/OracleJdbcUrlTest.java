package com.duoc.auth_users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OracleJdbcUrlTest {

	@Test
	void convertsTnsAliasToWalletBackedJdbcUrl() {
		assertEquals("jdbc:oracle:thin:@librosdb_high?TNS_ADMIN=/app/wallet",
				OracleJdbcUrl.normalize(" librosdb_high ", null));
	}

	@Test
	void usesConfiguredTnsAdminDirectoryForAlias() {
		assertEquals("jdbc:oracle:thin:@librosdb_high?TNS_ADMIN=C:/oracle/wallet",
				OracleJdbcUrl.normalize("librosdb_high", "C:/oracle/wallet"));
	}

	@Test
	void leavesFullJdbcUrlUnchanged() {
		String jdbcUrl = "jdbc:oracle:thin:@librosdb_high?TNS_ADMIN=/custom/wallet";

		assertEquals(jdbcUrl, OracleJdbcUrl.normalize(jdbcUrl, "/app/wallet"));
	}

	@Test
	void rejectsInvalidTnsAlias() {
		assertThrows(IllegalArgumentException.class, () -> OracleJdbcUrl.normalize("librosdb_high?unexpected=true", null));
	}

}
