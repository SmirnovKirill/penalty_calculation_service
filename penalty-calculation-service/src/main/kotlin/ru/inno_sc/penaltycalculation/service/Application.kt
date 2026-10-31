package ru.inno_sc.penaltycalculation.service

import kotlin.concurrent.thread
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.web.server.context.WebServerApplicationContext
import org.springframework.context.annotation.Import
import ru.inno_sc.penaltycalculation.service.config.CommonConfig

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(CommonConfig::class)
class Application

fun main(args: Array<String>) {
  val context = runApplication<Application>(*args)
  if (context.environment.getProperty("desktop", Boolean::class.java, false)) {
    val port = (context as WebServerApplicationContext).webServer!!.port
    println("PENALTY_CALCULATION_SERVICE_DESKTOP_PORT=$port")
    thread(name = "desktop-parent", isDaemon = true) {
      context.use {
        while (System.`in`.read() != -1) Unit
      }
    }
  }
}
