package xyz.pierini.fantacombinator

import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
class DefaultController {
	@RequestMapping("/")
	fun index(): String? {
		return "Welcome to Example Receipt"
	}
}