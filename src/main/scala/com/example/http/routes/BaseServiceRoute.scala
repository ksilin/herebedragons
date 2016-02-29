package com.example.http.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.ActorMaterializer
import com.example.utils.{HttpConfig, JsonProtocol}

import scala.concurrent.ExecutionContext

trait BaseServiceRoute extends JsonProtocol with SprayJsonSupport with HttpConfig {
  protected implicit def executor: ExecutionContext
  protected implicit def materializer: ActorMaterializer
}
