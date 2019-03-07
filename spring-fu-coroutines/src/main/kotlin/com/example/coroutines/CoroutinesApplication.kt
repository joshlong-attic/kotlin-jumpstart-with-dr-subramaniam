package com.example.coroutines

import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.data.mongodb.core.*
import org.springframework.fu.kofu.mongo.mongodb
import org.springframework.fu.kofu.web.server
import org.springframework.fu.kofu.webApplication
import org.springframework.http.MediaType
import org.springframework.web.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok


class CustomerHandler(private val repository: CustomerRepository) {

	suspend fun all(x: ServerRequest) = ok()
			.contentType(MediaType.APPLICATION_JSON_UTF8)
			.bodyAndAwait(repository.all())

}

val app = webApplication {
	beans {
		bean<CustomerHandler>()
		bean<CustomerRepository>()
	}
	mongodb()
	server {
		codecs {
			string()
			jackson()
		}
		coRouter {
			val ch = ref<CustomerHandler>()
			GET("/all", ch::all)
		}
	}
	listener<ApplicationReadyEvent> {
		runBlocking {
			val repo = ref<CustomerRepository>()
			listOf("A", "B", "C", "D")
					.map { Customer(name = it) }
					.forEach { repo.insert(it) }
		}
	}
}

class CustomerRepository(
		private val mongo: ReactiveFluentMongoOperations) {

	suspend fun insert(c: Customer) = mongo.insert<Customer>().oneAndAwait(c)

	suspend fun all() = this.mongo.query<Customer>().awaitAll()
}

data class Customer(val id: String? = null, val name: String)

fun main() {
	app.run()
}
