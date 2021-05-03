package controllers

import models.repository.CustomerRepository
import models.{WSCustomerData, WSUpdateCustomerData}
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CustomerController @Inject()(val controllerComponents: ControllerComponents, ctmRepo: CustomerRepository) extends BaseController {


  def getCustomers() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- ctmRepo.getAll()
    } yield {
      val json = JsObject(Seq(
        "customers" -> Json.toJson(list)
      ))
      Ok(json)
    }
  }

  // createCustomer
  def enrollCustomer(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSCustomerData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { cData =>
      ctmRepo.create(cData).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created " + id))))
    })
  }

  def modifyCustomer(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateCustomerData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { c =>
      ctmRepo.updateById(id, c).map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("updated: " + id)))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: $id",
        ))
      }
    })
  }

  def blockCustomer(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    ctmRepo.blockOrUnblockById(id, true).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("status updated: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  // customer cannot be removed, but can be closed
  def closeCustomer(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    ctmRepo.closeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("closed: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def unblockCustomer(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    ctmRepo.blockOrUnblockById(id, false).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("status updated: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

}
