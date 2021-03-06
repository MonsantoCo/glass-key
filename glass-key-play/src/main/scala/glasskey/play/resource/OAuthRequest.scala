package glasskey.play.resource

import glasskey.config.OAuthConfig
import glasskey.model.ValidatedData
import glasskey.play.resource.validation.PingIdentityAccessTokenValidatorFormats
import glasskey.resource.ProtectedResource
import glasskey.resource.validation.Validator
import play.api.libs.ws.WSResponse
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

class OAuthRequest[A](val user: ValidatedData, request: Request[A])(implicit ec: ExecutionContext, env: PlayResourceRuntimeEnvironment[WSResponse]) extends WrappedRequest[A](request)

trait OAuthAction extends ActionBuilder[OAuthRequest] {

  import PingIdentityAccessTokenValidatorFormats._
  import glasskey.model._
  import glasskey.resource.validation.PingIdentityProtectedResourceHandler
  import glasskey.resource.{ProtectedResource, ProtectedResourceHandler}
  import play.api.libs.json.Json
  import play.api.mvc.Results._

  import scala.concurrent.Future

  def resource: ProtectedResource

  def tokenValidators : Iterable[Validator[WSResponse]]

  implicit val env : PlayResourceRuntimeEnvironment[WSResponse]
  implicit val ec: ExecutionContext

  implicit def play2protectedResourceRequest[A](request: Request[A]): ProtectedResourceRequest = {
    def getParam(request: Request[A]): Map[String, Seq[String]] = {
      val form = request.body match {
        case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined =>
          body.asFormUrlEncoded.get

        case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined =>
          body.asMultipartFormData.get.asFormUrlEncoded

        case body: Map[_, _] =>
          body.asInstanceOf[Map[String, Seq[String]]]

        case body: play.api.mvc.MultipartFormData[_] =>
          body.asFormUrlEncoded

        case _ =>
          Map.empty[String, Seq[String]]
      }

      form ++ request.queryString.map {
        case (k, v) => k -> (v ++ form.getOrElse(k, Nil))
      }
    }

    val param: Map[String, Seq[String]] = getParam(request)
    ProtectedResourceRequest(request.headers.toMap, param)
  }

  def invokeBlock[A](request: Request[A], block: (OAuthRequest[A]) => Future[Result]): Future[Result] = {
    def authorize(request: Request[A], handler: ProtectedResourceHandler[ValidatedData, ValidatedToken])(callback: ValidatedData => Future[Result]): Future[Result] =
      resource.handleRequest[ProtectedResourceRequest](request, handler).flatMap { authInfo: ValidatedData =>
        callback(authInfo)
      }.recover {
        case e: OAuthException => responseOAuthError(e)
      }

    try {
      authorize(request, new PingIdentityProtectedResourceHandler(tokenValidators)) { authInfo =>
        block(new OAuthRequest(authInfo, request))
      }
    }
    catch {
      case e: OAuthException => Future.successful(responseOAuthError(e))
    }

  }

  def addWWWAuthHeader(result: Result, errorString: String): Result = result.withHeaders("WWW-Authenticate" -> errorString)

  def responseOAuthError(e: ValidationError): Result =
    addWWWAuthHeader(Unauthorized(Json.toJson(e)), s"${OAuthConfig.providerConfig.authHeaderPrefix} " + toOAuthErrorString(e))
}

object OAuthAction {
  def apply[T](block: OAuthRequest[AnyContent] => play.api.mvc.Result)(implicit ec:ExecutionContext,
                                                                       env: PlayResourceRuntimeEnvironment[WSResponse]): Action[AnyContent] =
  {
    val action = createAction(env)
    action.apply(block)
  }

  def apply[T](block: OAuthRequest[AnyContent] => play.api.mvc.Result,
               jwksUri: String, tokenValidators: Iterable[Validator[WSResponse]])(implicit ec:ExecutionContext,
                                                                                  env: PlayResourceRuntimeEnvironment[WSResponse]): Action[AnyContent] =
  {
    val action = createAction[T](tokenValidators, env)
    action.apply(block)
  }

  def createAction(resourceEnv: PlayResourceRuntimeEnvironment[WSResponse])(implicit exc:ExecutionContext): OAuthAction = new OAuthAction {
    override implicit val ec: ExecutionContext = exc
    override val resource = ProtectedResource.apply
    override val tokenValidators = env.tokenValidators
    override implicit val env: PlayResourceRuntimeEnvironment[WSResponse] = resourceEnv
  }

  def createAction[T](validators: Iterable[Validator[WSResponse]],
                      resourceEnv: PlayResourceRuntimeEnvironment[WSResponse])(implicit exc: ExecutionContext): OAuthAction = new OAuthAction {
    override implicit val ec: ExecutionContext = exc
    override val resource = ProtectedResource.apply
    override def tokenValidators: Iterable[Validator[WSResponse]] = validators
    override implicit val env: PlayResourceRuntimeEnvironment[WSResponse] = resourceEnv
  }
}
