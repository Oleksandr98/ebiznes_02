package controllers

import models.repository.LocationRepository
import models.{WSLocationData, WSUpdateLocationData}
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class LocationController @Inject()(val controllerComponents: ControllerComponents, lcnRepo: LocationRepository) extends BaseController {

  def getLocations() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- lcnRepo.getAll()
    } yield {
      val json = JsObject(Seq(
        "locations" -> Json.toJson(list)
      ))
      Ok(json)
    }
  }

  def addLocation(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSLocationData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    },
      {
        locData =>
          try {
            lcnRepo.create(locData).map(id =>
              Ok(Json.obj("status" -> "OK", "message" -> ("created " + id))))
          }
          catch {
            case ex: NoSuchElementException => Future.successful(BadRequest(Json.obj(
              "status" -> "Error",
              "message" -> ex.getMessage
            )))
            case _ => Future.successful(InternalServerError("unknown error"))
          }
      })
  }

  def getLocation(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    lcnRepo.getById(id).map {
      case Some(p) => Ok(Json.toJson(p))
      case None => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def modifyLocation(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateLocationData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { loc =>
      try {
        lcnRepo.updateById(id, loc).map {
          case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("updated: " + id)))
          case 0 => BadRequest(Json.obj(
            "status" -> "Error",
            "message" -> s"Not found item by id: $id",
          ))
        }
      }
      catch {
        case ex: NoSuchElementException => Future.successful(BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> ex.getMessage
        )))
        case _ => Future.successful(InternalServerError("unknown error"))
      }
    })
  }

  def removeLocation(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    lcnRepo.removeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("removed: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

}
