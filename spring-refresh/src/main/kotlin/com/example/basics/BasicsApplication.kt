package com.example.basics

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource

@SpringBootApplication
class BasicsApplication {

	@Bean
	fun stm(ds: DataSource) = SpringTransactionManager(ds)

	@Bean
	fun tt(ptm: PlatformTransactionManager) = TransactionTemplate(ptm)
}

@Component
class Initializer(@Qualifier("exposed") private val cs: CustomerService) {

	@EventListener(ApplicationReadyEvent::class)
	fun begin() {
		listOf("Dr. Subramaniam", "Josh")
				.map { Customer(name = it) }
				.forEach { this.cs.insert(it) }

		this.cs.all().forEach { println(it) }
	}
}


fun main(args: Array<String>) {
	runApplication<BasicsApplication>(*args)
}

@Service
@Qualifier("jdbc")
class JdbcCustomerService(private val jdbc: JdbcTemplate) : CustomerService {

	override fun insert(c: Customer) {
		this.jdbc.execute("insert into CUSTOMERS(NAME) values(?)") {
			it.setString(1, c.name)
			it.execute()
		}
	}

	override fun all() = this.jdbc.query("select * from CUSTOMERS ") { rs, _ ->
		Customer(id = rs.getInt("ID"), name = rs.getString("NAME"))
	}

}

object Customers : Table() {
	val id = integer("id").primaryKey().autoIncrement()
	val name = varchar("name", 255).nullable()
}

@Service
@Transactional
@Qualifier("exposed")
class ExposedCustomerService(private val tt: TransactionTemplate) :
		CustomerService, InitializingBean {

	override fun afterPropertiesSet() {
		tt.execute {
			SchemaUtils.create(Customers)
		}
	}

	override fun insert(c: Customer) {
		Customers.insert {
			it[Customers.name] = c.name
		}
	}

	override fun all() = Customers
			.selectAll()
			.map { Customer(id = it[Customers.id], name = it[Customers.name]) }
}

interface CustomerService {
	fun insert(c: Customer)
	fun all(): Collection<Customer>
}

data class Customer(var id: Int? = null, var name: String? = null)