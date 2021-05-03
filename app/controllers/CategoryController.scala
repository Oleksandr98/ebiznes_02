package controllers

import models.repository.CategoryRepository
import models.{WSCategoryData, WSUpdateCategoryData}
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

@Singleton
class CategoryController @Inject()(val controllerComponents: ControllerComponents, catRepo: CategoryRepository) extends BaseController {

  def getCategories() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- catRepo.getAll()
    } yield {
      val json = JsObject(Seq(
        "categories" -> Json.toJson(list)
      ))
      Ok(json)
    }
  }

  def addCategory: Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSCategoryData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { cData =>
      catRepo.create(cData.name, cData.code).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created: " + id))))
    })
  }

  def removeCategory(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    catRepo.removeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> "removed"))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def modifyCategory(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateCategoryData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { c =>
      catRepo.updateById(id, c).map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> "updated"))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: $id",
        ))
      }
    })
  }

  def getCategory(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    catRepo.getById(id).map {
      case Some(p) => Ok(Json.toJson(p))
      case None => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }
}
