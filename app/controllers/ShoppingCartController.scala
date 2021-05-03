package controllers

import models.repository.ShoppingCartRepository
import models.{WSShoppingCartData, WSShoppingCartProductData, WSShoppingCartProductRemData, WSUpdateShoppingCartData}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ShoppingCartController @Inject()(val controllerComponents: ControllerComponents, shcRepo: ShoppingCartRepository) extends BaseController {

  def createCart(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSShoppingCartData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { shcData =>
      shcRepo.create(shcData).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created " + id))))
    })
  }

  def getCarts() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- shcRepo.getAll()
    } yield {
      Ok(Json.toJson(list))
    }
  }

  def getCart(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    shcRepo.getById(id).map {
      case Some(p) => Ok(Json.toJson(p))
      case None => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def addToCart(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSShoppingCartProductData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { data =>
      shcRepo.addProduct(data.shcId, data.prdId, data.quantity).flatMap(x => x.map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> "updated"))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: ${data.shcId}",
        ))
      })
    })
  }

  def removeFromCart(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSShoppingCartProductRemData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { data =>
      shcRepo.removeProduct(data.shcId, data.prdId).flatMap(x => x.map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> "updated"))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: ${data.shcId}",
        ))
      })
    })
  }

  def modifyCart(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateShoppingCartData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { shCart =>
      shcRepo.updateById(id, shCart).map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> "updated"))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: $id",
        ))
      }
    })
  }

  def removeCart(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    shcRepo.removeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("removed: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

}
