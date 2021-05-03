package controllers

import models.repository.OrderRepository
import models.{WSOrderData, WSUpdateOrderData}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class OrderController @Inject()(val controllerComponents: ControllerComponents, ordRepo: OrderRepository) extends BaseController {

  def getOrders() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- ordRepo.getAll()
    } yield {
      val json = Json.toJson(list)
      Ok(json)
    }
  }

  def getOrder(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    ordRepo.getByIdComplete(id).map {
      case o if o.nonEmpty => Ok(Json.toJson(o))
      case _ => BadRequest(Json.obj(
      "status" -> "Error",
      "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def createOrder(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSOrderData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { orderData =>
      ordRepo.create(orderData).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created: " + id))))
    })
  }

  // removes order
  def cancelOrder(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    ordRepo.removeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("removed: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def modifyOrder(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateOrderData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { orderData =>
      ordRepo.updateById(id, orderData)
        .flatMap(result =>
          result.flatMap
          (test =>
            test.map {
              case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("updated: " + id)))
              case 0 => BadRequest(Json.obj(
                "status" -> "Error",
                "message" -> s"Not found item by id: $id",
              ))
            }
          )
        )
    })
  }

}
