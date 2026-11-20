package com.duoc.auth_users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthUsersApplication {

	public static void main(String[] args) {
		// En AWS: descarga la wallet de Oracle desde S3 (si ORACLE_WALLET_S3_* esta configurado)
		OracleWalletBootstrap.loadFromEnvironment();
		// DB_URL puede ser el alias TNS (ej. midb_high): se completa con TNS_ADMIN
		String databaseUrl = System.getenv("DB_URL");
		if (databaseUrl != null && !databaseUrl.isBlank()) {
			System.setProperty("DB_URL", OracleJdbcUrl.normalize(databaseUrl, System.getenv("TNS_ADMIN")));
		}
		SpringApplication.run(AuthUsersApplication.class, args);
	}

}
