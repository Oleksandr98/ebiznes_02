package controllers

import models.repository.OffersRepository
import models.{WSOfferData, WSUpdateOfferData}
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class OffersController @Inject()(val controllerComponents: ControllerComponents, ofrRepo: OffersRepository) extends BaseController {

  def getOffers() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- ofrRepo.getAll()
    } yield {
      val json = JsObject(Seq(
        "coupons" -> Json.toJson(list)
      ))
      Ok(json)
    }
  }

  def getOffer(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    ofrRepo.getById(id).map {
      case Some(o) => Ok(Json.toJson(o))
      case None => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def addOffer(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSOfferData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { offerData =>
      ofrRepo.create(offerData).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created: " + id))))
    })
  }

  def removeOffer(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    ofrRepo.removeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("removed: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def modifyOffer(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateOfferData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { ofrData =>
      ofrRepo.updateById(id, ofrData).map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("updated: " + id)))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: $id",
        ))
      }
    })
  }

}
