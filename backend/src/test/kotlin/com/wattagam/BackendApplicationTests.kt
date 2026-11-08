package com.wattagam

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
	properties = ["spring.flyway.locations=classpath:db/migration,classpath:db/test-migration"],
)
@Testcontainers
class BackendApplicationTests {

	@Autowired
	private lateinit var dataSource: DataSource

	@Autowired
	private lateinit var flyway: Flyway

	@PersistenceContext
	private lateinit var entityManager: EntityManager

	@Test
	fun contextLoads() {
		dataSource.connection.use { connection ->
			assertEquals("MySQL", connection.metaData.databaseProductName)
		}
		assertTrue(flyway.info().applied().any { it.script == "R__database_probe.sql" })
	}

	@Test
	@Transactional
	fun jpaRoundTripsUnicodeThroughTheMigratedSchema() {
		val content = "와따감 공통 DB 테스트 🚀"
		entityManager.createNativeQuery("INSERT INTO test_database_probe (id, content) VALUES (:id, :content)")
			.setParameter("id", 1L)
			.setParameter("content", content)
			.executeUpdate()

		val stored = entityManager.createNativeQuery("SELECT content FROM test_database_probe WHERE id = :id")
			.setParameter("id", 1L)
			.singleResult

		assertEquals(content, stored)
	}

	companion object {
		@Container
		@ServiceConnection
		@JvmStatic
		val mysql = MySQLContainer("mysql:8.4.13")
			.withCommand(
				"--character-set-server=utf8mb4",
				"--collation-server=utf8mb4_0900_ai_ci",
				"--default-time-zone=+00:00",
			)
	}

}
