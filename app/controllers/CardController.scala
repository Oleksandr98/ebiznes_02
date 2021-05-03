package controllers

import models.repository.CardRepository
import models.{CardStatuses, WSCardData, WSUpdateCardData}
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CardController @Inject()(val controllerComponents: ControllerComponents, crdRepo: CardRepository) extends BaseController {

  def getCards() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- crdRepo.getAll()
    } yield {
      val json = JsObject(Seq(
        "cards" -> Json.toJson(list)
      ))
      Ok(json)
    }
  }

  def addCard(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSCardData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { cData =>
      crdRepo.create(cData.number, cData.customerId).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created " + id))))
    })
  }

  def closeCard(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    crdRepo.closeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("closed: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def blockCard(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    crdRepo.updateById(id, new WSUpdateCardData(Option.empty, Option.apply(CardStatuses.Blocked.toString), Option.empty)).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("blocked: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def getCard(id: Long): Action[AnyContent] = Action.async { implicit request =>
    crdRepo.getById(id).map {
      case Some(p) => Ok(Json.toJson(p))
      case None => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def modifyCard(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateCardData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { cData =>
      crdRepo.updateById(id, cData).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("updated: " + id))))
    })
  }

}
