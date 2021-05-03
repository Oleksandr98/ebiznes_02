package controllers

import models.repository.ProductRepository
import models.{WSProductData, WSUpdateProductData}
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProductController @Inject()(val controllerComponents: ControllerComponents, prdRepo: ProductRepository) extends BaseController {

  def getProducts() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- prdRepo.getAll()
    } yield {
      val json = JsObject(Seq(
        "products" -> Json.toJson(list)
      ))
      Ok(json)
    }
  }

  def addProduct: Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSProductData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { pData =>
      prdRepo.create(pData.name, pData.code, pData.description, pData.categoryId, pData.value).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created " + id))))
    })
  }

  def updateProduct(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateProductData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { p =>
      prdRepo.updateById(id, p).map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> "updated"))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: $id",
        ))
      }
    })
  }

  def getProduct(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    prdRepo.getById(id).map {
      case Some(p) => Ok(Json.toJson(p))
      case None => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def removeProduct(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    prdRepo.removeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> "removed"))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

}
