package xyz.pierini.fantacombinator.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StartupService {
	@Autowired
	private val combinatorService: CombinatorService? = null

	@EventListener(ApplicationReadyEvent::class)
	fun startup() {
		combinatorService!!.init()
		System.exit(0)
	}
}