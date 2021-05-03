package controllers

import models.repository.TransactionRepository
import models.{TransactionTypes, WSTransactionData, WSUpdateTransactionData}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TransactionController @Inject()(val controllerComponents: ControllerComponents, tranRepo: TransactionRepository) extends BaseController {

  def getTransactions() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- tranRepo.getAll()
    } yield {
      val json = Json.toJson(list)
      Ok(json)
    }
  }

  def modifyTransaction(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateTransactionData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { t =>
      tranRepo.updateById(id, t).map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("updated: " + id)))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: $id",
        ))
      }
    })
  }

  def createSaleTransaction(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSTransactionData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { tData =>
      tranRepo.create(tData, TransactionTypes.Sale.toString).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created " + id))))
    })
  }

  // transactions cannot be removed, can only be reversed
  def reverseTransaction(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    tranRepo.reverseById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("reversed: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

}
