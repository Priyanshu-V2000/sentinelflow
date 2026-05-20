package com.sentinelflow

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.UUID

class PaymentSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val legitPayment = scenario("Legitimate Payments")
    .exec(
      http("POST legitimate payment")
        .post("/api/v1/payments")
        .body(StringBody(session =>
          s"""{
            "transactionId": "TXN-${UUID.randomUUID()}",
            "amount": 1500.00,
            "currency": "INR",
            "merchantId": "amazon-india",
            "cardHash": "hash-${UUID.randomUUID()}",
            "tenantId": "00000000-0000-0000-0000-000000000001",
            "countryCode": "IN",
            "eventTime": "2026-01-01T00:00:00Z"
          }"""
        )).asJson
        .check(status.in(200, 201, 202, 429))
    )

  val fraudPayment = scenario("Fraud Payments")
    .exec(
      http("POST fraud payment")
        .post("/api/v1/payments")
        .body(StringBody(session =>
          s"""{
            "transactionId": "TXN-FRAUD-${UUID.randomUUID()}",
            "amount": 95000.00,
            "currency": "INR",
            "merchantId": "crypto-nigeria",
            "cardHash": "hash-${UUID.randomUUID()}",
            "tenantId": "00000000-0000-0000-0000-000000000001",
            "countryCode": "NG",
            "eventTime": "2026-01-01T00:00:00Z"
          }"""
        )).asJson
        .check(status.in(200, 201, 202, 429))
    )

  val healthCheck = scenario("Health Checks")
    .exec(
      http("GET health")
        .get("/actuator/health")
        .check(status.is(200))
    )

  setUp(
    legitPayment.inject(
      rampUsers(10).during(10.seconds),
      rampUsers(100).during(30.seconds),
      constantUsersPerSec(200).during(60.seconds),
      rampUsers(500).during(30.seconds)
    ),
    fraudPayment.inject(
      nothingFor(5.seconds),
      rampUsers(5).during(10.seconds),
      rampUsers(50).during(30.seconds),
      constantUsersPerSec(50).during(60.seconds)
    ),
    healthCheck.inject(
      constantUsersPerSec(10).during(120.seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.max.lt(5000),
     global.responseTime.mean.lt(100),
     global.successfulRequests.percent.gt(95)
   )
}
