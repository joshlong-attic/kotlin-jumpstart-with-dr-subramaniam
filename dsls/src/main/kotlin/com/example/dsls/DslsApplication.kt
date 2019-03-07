package com.example.dsls

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router

@SpringBootApplication
class DslsApplication

fun main(args: Array<String>) {
	runApplication<DslsApplication>(*args) {
		val ctx = beans {
			bean {
				router {
					val cr = ref<CustomerRepository>()
					GET("/customers") {
						ServerResponse.ok().body(cr.findAll())
					}
				}
			}
		}
		addInitializers(ctx)
	}
}


@Document
data class Customer(@Id var id: String? = null,
                    var name: String? = null)

interface CustomerRepository : ReactiveCrudRepository<Customer, String>

